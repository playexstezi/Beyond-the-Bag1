package ru.beyondthebag.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.item.DimensionBagItem;

/**
 * Keeps the unstable expedition maze from being bypassed by mining or placing blocks.
 * Personal realms remain fully editable and have no territory protection.
 */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class PersonalRealmProtection {
    private PersonalRealmProtection() {}

    private static boolean isUnstable(Level level) {
        return level instanceof ServerLevel serverLevel
                && serverLevel.dimension().equals(DimensionBagItem.UNSTABLE_WORLD);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (isUnstable(event.getPlayer().level())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && isUnstable(player.level())) {
            event.setCanceled(true);
        }
    }
}
