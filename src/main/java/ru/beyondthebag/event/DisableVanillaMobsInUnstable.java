package ru.beyondthebag.event;

import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.entity.DistortedEntity;
import ru.beyondthebag.entity.GuardianEntity;
import ru.beyondthebag.entity.MimicEntity;
import ru.beyondthebag.entity.StriderEntity;
import ru.beyondthebag.entity.WatcherEntity;
import ru.beyondthebag.item.DimensionBagItem;

/** Keeps the unstable dimension populated only by Beyond the Bag creatures. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class DisableVanillaMobsInUnstable {
    private DisableVanillaMobsInUnstable() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().dimension().equals(DimensionBagItem.UNSTABLE_WORLD)
                || !(event.getEntity() instanceof Mob mob)) return;

        boolean isOurs = mob instanceof StriderEntity
                || mob instanceof WatcherEntity
                || mob instanceof DistortedEntity
                || mob instanceof GuardianEntity
                || mob instanceof MimicEntity;
        if (!isOurs) event.setCanceled(true);
    }
}
