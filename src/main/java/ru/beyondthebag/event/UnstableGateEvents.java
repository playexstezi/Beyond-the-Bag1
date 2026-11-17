package ru.beyondthebag.event;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import ru.beyondthebag.world.MazeLayout;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.entity.GuardianEntity;
import net.minecraft.world.phys.AABB;
import ru.beyondthebag.item.DimensionBagItem;

/** Handles the physical return gate placed in the final chamber of an expedition. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class UnstableGateEvents {
    private UnstableGateEvents() {}

    @SubscribeEvent
    public static void onGuardianDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof GuardianEntity guardian)
                || !guardian.isExpeditionBoss()
                || !(guardian.level() instanceof ServerLevel level)
                || !level.dimension().equals(DimensionBagItem.UNSTABLE_WORLD)) return;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                guardian.getBoundingBox().inflate(160.0D), p -> p.isAlive() && !p.isSpectator())) {
            PocketData data = PocketData.get(player.server.overworld());
            PocketData.Expedition expedition = data.getExpedition(player.getUUID()).orElse(null);
            if (expedition == null) continue;
            MazeLayout layout = MazeLayout.generate(expedition.seed(), expedition.instability());
            if (layout.rooms().size() < 2) continue;
            MazeLayout.Room exitRoom = layout.rooms().get(layout.rooms().size() - 2);
            int originX = Math.floorMod((int) (expedition.seed() >>> 32), 8192) * 16;
            int originZ = Math.floorMod((int) expedition.seed(), 8192) * 16;
            BlockPos center = new BlockPos(originX + exitRoom.x() * 28, 73, originZ + exitRoom.z() * 28);
            level.setBlock(center, Blocks.RESPAWN_ANCHOR.defaultBlockState(), 3);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) level.setBlock(center.offset(dx, 0, dz),
                        Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            }
            level.playSound(null, center, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 2.0F, 0.65F);
            for (ServerPlayer member : level.getEntitiesOfClass(ServerPlayer.class, new AABB(center).inflate(128.0D)))
                member.displayClientMessage(Component.literal("§dПространство дрогнуло. В другой комнате проявились врата выхода."), false);
            return;
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

        // The final gate is sealed while the chamber guardian is alive.
        boolean bossAlive = !level.getEntitiesOfClass(GuardianEntity.class,
                new AABB(gate).inflate(48.0D), GuardianEntity::isExpeditionBoss)
                .stream().filter(GuardianEntity::isAlive).toList().isEmpty();
        if (bossAlive) {
            player.displayClientMessage(Component.literal(
                    "§5Врата запечатаны. Победите Хранителя измерения."), true);
            return;
        }

        PocketData data = PocketData.get(player.server.overworld());
        PocketData.Home home = data.getHome(player.getUUID()).orElse(null);
        if (home == null) {
            player.displayClientMessage(Component.literal("§cМешок не помнит точку возвращения."), true);
            return;
        }

        ServerLevel destination = player.server.getLevel(
                net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION, home.dimension()));
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
        var data = player.getPersistentData();
        data.remove("beyondthebag_sanity");
        data.remove("beyondthebag_last_sanity_tick");
        data.remove("beyondthebag_expedition_entered_at");
        data.remove("beyondthebag_last_darkness_tick");
        data.remove("beyondthebag_last_rest_scan_tick");
        data.remove("beyondthebag_last_rest_tick");
    }
}
