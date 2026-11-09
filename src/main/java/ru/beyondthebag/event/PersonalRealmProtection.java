package ru.beyondthebag.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.item.DimensionBagItem;

/** Protects the unstable maze and enforces ownership of personal-realm plots. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class PersonalRealmProtection {
    private PersonalRealmProtection() {}

    private static boolean isDimension(Level level, net.minecraft.resources.ResourceKey<Level> key) {
        return level instanceof ServerLevel serverLevel && serverLevel.dimension().equals(key);
    }

    private static boolean canEditPersonalRealm(ServerLevel level, Player player, BlockPos pos) {
        PocketData data = PocketData.get(level);
        return data.getRealmOwnerAt(pos.getX(), pos.getZ())
                .map(owner -> data.canAccessRealm(owner, player.getUUID()))
                .orElse(false);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = event.getPlayer().level();
        if (isDimension(level, DimensionBagItem.UNSTABLE_WORLD)) {
            event.setCanceled(true);
            return;
        }
        if (isDimension(level, DimensionBagItem.PERSONAL_WORLD)
                && level instanceof ServerLevel serverLevel
                && !canEditPersonalRealm(serverLevel, event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Level level = player.level();
        if (isDimension(level, DimensionBagItem.UNSTABLE_WORLD)) {
            event.setCanceled(true);
            return;
        }
        if (isDimension(level, DimensionBagItem.PERSONAL_WORLD)
                && level instanceof ServerLevel serverLevel
                && !canEditPersonalRealm(serverLevel, player, event.getPos())) {
            event.setCanceled(true);
        }
    }
}
