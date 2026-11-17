package ru.beyondthebag.event;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.ExpeditionType;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.entity.GuardianEntity;
import ru.beyondthebag.item.DimensionBagItem;
import ru.beyondthebag.registry.ModItems;
import ru.beyondthebag.world.MazeLayout;

/** Handles the physical return gate placed in a separate chamber after the boss dies. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class UnstableGateEvents {
    private UnstableGateEvents() {}

    @SubscribeEvent
    public static void onGuardianDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof GuardianEntity guardian)
                || !guardian.isExpeditionBoss()
                || !(guardian.level() instanceof ServerLevel level)
                || !level.dimension().equals(DimensionBagItem.UNSTABLE_WORLD)) return;

        guardian.spawnAtLocation(new ItemStack(ModItems.GUARDIAN_HEART.get()));
        PocketData.get(level.getServer().overworld()).markExpeditionBossDefeated(guardian.getExpeditionRunId());
        Set<Long> handledRuns = new HashSet<>();
        // Resolve every participant's run, not just the first player returned by the
        // entity query. This keeps party expeditions correct when players are spread out.
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            PocketData data = PocketData.get(player.server.overworld());
            PocketData.Expedition expedition = data.getExpedition(player.getUUID()).orElse(null);
            if (expedition == null || expedition.runId() != guardian.getExpeditionRunId()
                    || !handledRuns.add(expedition.runId())) continue;

            int originX = Math.floorMod((int) (expedition.seed() >>> 32), 8192) * 16;
            int originZ = Math.floorMod((int) expedition.seed(), 8192) * 16;
            BlockPos center;
            if (expedition.type() == ExpeditionType.DUNGEON) {
                center = ru.beyondthebag.world.DungeonWorldBuilder.gatePosition(originX, originZ);
            } else if (expedition.type() == ExpeditionType.DISTORTION) {
                center = ru.beyondthebag.world.DistortionWorldBuilder.gatePosition(originX, originZ);
            } else {
                MazeLayout layout = MazeLayout.generate(expedition.seed(), expedition.instability());
                if (layout.rooms().size() < 2) continue;
                MazeLayout.Room exitRoom = layout.rooms().get(layout.rooms().size() - 2);
                center = new BlockPos(originX + exitRoom.x() * 28, 73,
                        originZ + exitRoom.z() * 28);
            }

            // Place the portal immediately during the death event. It is never built
            // in the boss arena: each route has a dedicated exit chamber.
            level.setBlock(center, Blocks.RESPAWN_ANCHOR.defaultBlockState(), 3);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    level.setBlock(center.offset(dx, 0, dz),
                            Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
                }
            }
            level.playSound(null, center, SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.BLOCKS, 2.0F, 0.65F);
            for (ServerPlayer member : level.getEntitiesOfClass(ServerPlayer.class,
                    new AABB(center).inflate(160.0D))) {
                member.displayClientMessage(Component.literal(
                        "§dПространство дрогнуло. В другой комнате проявились врата выхода."), false);
            }
        }
    }

    @SubscribeEvent
    public static void onGateActivated(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(DimensionBagItem.UNSTABLE_WORLD)) return;

        BlockPos gate = event.getPos();
        if (!level.getBlockState(gate).is(Blocks.RESPAWN_ANCHOR)
                || !isGateFrame(level, gate)) return;

        event.setCanceled(true);
        PocketData data = PocketData.get(player.server.overworld());
        PocketData.Expedition expedition = data.getExpedition(player.getUUID()).orElse(null);
        if (expedition == null) {
            player.displayClientMessage(Component.literal("§cЭкспедиция больше не распознана."), true);
            return;
        }

        // Completion is persisted by run ID when the guardian dies. This avoids
        // scanning a huge AABB (which can miss distant rooms and load excess chunks).
        boolean bossAlive = !data.isExpeditionBossDefeated(expedition.runId());
        if (bossAlive) {
            player.displayClientMessage(Component.literal(
                    "§5Врата запечатаны. Победите Хранителя измерения."), true);
            return;
        }

        PocketData.Home home = data.getHome(player.getUUID()).orElse(null);
        if (home == null) {
            player.displayClientMessage(Component.literal("§cМешок не помнит точку возвращения."), true);
            return;
        }

        ServerLevel destination = player.server.getLevel(
                ResourceKey.create(Registries.DIMENSION, home.dimension()));
        if (destination == null) {
            player.displayClientMessage(Component.literal("§cТочка возвращения сейчас недоступна."), true);
            return;
        }

        player.teleportTo(destination, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        data.clearExpedition(player.getUUID());
        clearExpeditionEffects(player);
        player.displayClientMessage(Component.literal("§aВрата вынесли вас из нестабильного измерения."), false);
        player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 0.8F);
    }

    private static boolean isGateFrame(ServerLevel level, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (!level.getBlockState(center.offset(dx, 0, dz)).is(Blocks.CRYING_OBSIDIAN)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void clearExpeditionEffects(ServerPlayer player) {
        var tag = player.getPersistentData();
        tag.remove("beyondthebag_sanity");
        tag.remove("beyondthebag_last_sanity_tick");
        tag.remove("beyondthebag_expedition_entered_at");
        tag.remove("beyondthebag_last_darkness_tick");
        tag.remove("beyondthebag_last_rest_scan_tick");
        tag.remove("beyondthebag_last_rest_tick");
    }
}