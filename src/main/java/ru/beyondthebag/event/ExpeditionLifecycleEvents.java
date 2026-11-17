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
        ServerLevel overworld = player.server.overworld();
        PocketData data = PocketData.get(overworld);
        PocketData.Home home = data.getHome(player.getUUID()).orElse(null);
        ServerLevel destination = overworld;
        double x = overworld.getSharedSpawnPos().getX() + 0.5D;
        double y = overworld.getSharedSpawnPos().getY() + 1.0D;
        double z = overworld.getSharedSpawnPos().getZ() + 0.5D;
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        // Death in the private realm is a route back to the ordinary world,
        // never a respawn over the void or at a stale private-realm coordinate.
        if (home != null && home.dimension().equals(overworld.dimension().location())) {
            x = home.x();
            y = home.y();
            z = home.z();
            yaw = home.yaw();
            pitch = home.pitch();
        }
        BlockPos safe = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(x, y, z));
        if (!overworld.getBlockState(safe.below()).isSolid()) {
            safe = overworld.getSharedSpawnPos().above();
            x = safe.getX() + 0.5D;
            y = safe.getY();
            z = safe.getZ() + 0.5D;
        } else {
            y = Math.max(y, safe.getY());
        }
        player.teleportTo(destination, x, y, z, yaw, pitch);
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
