package ru.beyondthebag.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.item.DimensionBagItem;

/** Prevents players from modifying blocks inside the unstable maze. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class PersonalRealmProtection {
    private PersonalRealmProtection() {}

    private static boolean isUnstableDimension(Level level) {
        return level.dimension().equals(DimensionBagItem.UNSTABLE_WORLD);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (isUnstableDimension(event.getPlayer().level())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && isUnstableDimension(player.level())) {
            event.setCanceled(true);
        }
    }
}
