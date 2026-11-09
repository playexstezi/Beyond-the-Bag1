package ru.beyondthebag.event;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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
