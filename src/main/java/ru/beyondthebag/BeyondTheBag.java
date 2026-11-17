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
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.MIMIC_HELMET);
            event.accept(ModItems.MIMIC_CHESTPLATE);
            event.accept(ModItems.MIMIC_LEGGINGS);
            event.accept(ModItems.MIMIC_BOOTS);
            event.accept(ModItems.BATTLE_SPOON);
            event.accept(ModItems.MIMIC_WINGS);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.DIMENSION_BAG);
            event.accept(ModItems.MIRROR_FRAGMENT);
            event.accept(ModItems.DISTORTION_CORE);
            event.accept(ModItems.GUARDIAN_HEART);
            event.accept(ModItems.EXPEDITION_STABILIZER);
        }
    }
}
