package ru.beyondthebag;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ru.beyondthebag.registry.ModEntities;
import ru.beyondthebag.registry.ModItems;

@Mod(BeyondTheBag.MOD_ID)
public final class BeyondTheBag {
    public static final String MOD_ID = "beyondthebag";

    public BeyondTheBag() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(bus);
        ModEntities.register(bus);
        bus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.DIMENSION_BAG);
        }
    }
}
