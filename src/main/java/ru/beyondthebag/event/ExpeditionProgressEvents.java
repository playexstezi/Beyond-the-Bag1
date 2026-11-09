package ru.beyondthebag.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.item.DimensionBagItem;

/**
 * Server-authoritative expedition progression: elapsed time raises instability,
 * while prolonged exposure erodes the explorer's in-game composure.
 */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class ExpeditionProgressEvents {
    private static final String SANITY_KEY = "beyondthebag_sanity";
    private static final String LAST_SANITY_TICK_KEY = "beyondthebag_last_sanity_tick";
    private static final String LAST_DARKNESS_TICK_KEY = "beyondthebag_last_darkness_tick";

    private ExpeditionProgressEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || !player.level().dimension().equals(DimensionBagItem.UNSTABLE_WORLD)) return;

        PocketData data = PocketData.get(player.server.overworld());
        PocketData.Expedition expedition = data.getExpedition(player.getUUID()).orElse(null);
        if (expedition == null) return;

        long now = player.server.overworld().getGameTime();
        long elapsed = Math.max(0L, now - expedition.startedAt());

        // The expedition grows more hostile every three minutes, up to level four.
        int targetLevel = (int) Math.min(4L, elapsed / 3_600L);
        if (targetLevel > expedition.instability()) {
            data.setInstability(player.getUUID(), targetLevel);
            player.displayClientMessage(Component.literal(
                    "§5Нестабильность повышается: §d" + targetLevel + "§5/4"), true);
            expedition = data.getExpedition(player.getUUID()).orElse(expedition);
        }

        var persistent = player.getPersistentData();
        // Moss-and-campfire rest chambers slowly restore composure while the explorer stays nearby.
        long lastRest = persistent.getLong("beyondthebag_last_rest_tick");
        if (now - lastRest >= 100L && isNearRestChamber(player)) {
            int currentSanity = persistent.contains(SANITY_KEY) ? persistent.getInt(SANITY_KEY) : 100;
            persistent.putInt(SANITY_KEY, Math.min(100, currentSanity + 2));
            persistent.putLong("beyondthebag_last_rest_tick", now);
            player.displayClientMessage(Component.literal("§aТихая комната помогает восстановить рассудок."), true);
        }
        if (!persistent.contains(SANITY_KEY)) persistent.putInt(SANITY_KEY, 100);
        if (!persistent.contains(LAST_SANITY_TICK_KEY)) persistent.putLong(LAST_SANITY_TICK_KEY, now);
        long lastTick = persistent.getLong(LAST_SANITY_TICK_KEY);
        if (now - lastTick < 1_200L) return;

        persistent.putLong(LAST_SANITY_TICK_KEY, now);
        int sanity = Math.max(0, persistent.getInt(SANITY_KEY)
                - (2 + expedition.instability()));
        persistent.putInt(SANITY_KEY, sanity);

        if (sanity <= 60 && sanity > 30) {
            player.displayClientMessage(Component.literal("§7Рассудок: §e" + sanity
                    + "% §8— звуки лабиринта начинают сбивать с толку."), true);
        } else if (sanity <= 30 && sanity > 0) {
            player.displayClientMessage(Component.literal("§7Рассудок: §c" + sanity
                    + "% §8— аномалии всё сильнее влияют на восприятие."), true);
            long lastDarkness = persistent.getLong(LAST_DARKNESS_TICK_KEY);
            if (now - lastDarkness >= 240L) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 35, 0,
                        false, false, true));
                persistent.putLong(LAST_DARKNESS_TICK_KEY, now);
            }
        } else if (sanity == 0) {
            player.displayClientMessage(Component.literal(
                    "§4Рассудок истощён. Постарайся завершить экспедицию."), true);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0,
                    false, false, true));
        }

    private static boolean isNearRestChamber(ServerPlayer player) {
        var level = player.serverLevel();
        var pos = player.blockPosition();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                var floor = pos.offset(dx, -1, dz);
                if (level.getBlockState(floor).is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK)
                        && level.getBlockState(floor.above()).is(net.minecraft.world.level.block.Blocks.CAMPFIRE)) {
                    return true;
                }
            }
        }
        return false;
    }
}
