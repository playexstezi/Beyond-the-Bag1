package ru.beyondthebag.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
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
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var death = player.getLastDeathLocation().orElse(null);
        if (death == null || !death.dimension().equals(DimensionBagItem.PERSONAL_WORLD)) return;
        ServerLevel personal = player.server.getLevel(DimensionBagItem.PERSONAL_WORLD);
        if (personal == null) return;
        PocketData.PersonalRealm realm = PocketData.get(personal).getOrCreatePersonalRealm(player.getUUID());
        BlockPos spawn = personal.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(realm.centerX(), 0, realm.centerZ()));
        player.teleportTo(personal, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().dimension().equals(DimensionBagItem.PERSONAL_WORLD)) {
            PocketData.get(player.server.overworld()).setPersonalPosition(player.getUUID(),
                    new PocketData.RealmPosition(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        }
        if (player.level().dimension().equals(DimensionBagItem.UNSTABLE_WORLD)) {
            PocketData.get(player.server.overworld()).clearExpedition(player.getUUID());
        }
    }
}
