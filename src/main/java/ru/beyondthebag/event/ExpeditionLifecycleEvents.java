package ru.beyondthebag.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.item.DimensionBagItem;

/**
 * Keeps rendezvous membership aligned with actual player presence.
 * A player who leaves the unstable dimension must not remain eligible
 * as a stale member of a later cooperative expedition.
 */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class ExpeditionLifecycleEvents {
    private ExpeditionLifecycleEvents() {}

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getFrom().equals(DimensionBagItem.UNSTABLE_WORLD)) return;
        if (event.getTo().equals(DimensionBagItem.UNSTABLE_WORLD)) return;

        ServerLevel level = player.server.overworld();
        PocketData.get(level).clearExpedition(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(DimensionBagItem.UNSTABLE_WORLD)) return;

        PocketData.get(player.server.overworld()).clearExpedition(player.getUUID());
    }
}
