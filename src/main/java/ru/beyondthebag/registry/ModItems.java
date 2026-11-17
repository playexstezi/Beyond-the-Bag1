package ru.beyondthebag.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.item.DimensionBagItem;

public final class ModItems {
    private ModItems() {}
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BeyondTheBag.MOD_ID);
    public static final RegistryObject<Item> MIMIC_FRAGMENT = ITEMS.register("mimic_fragment",
        () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DIMENSION_BAG = ITEMS.register("dimension_bag",
        () -> new DimensionBagItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> MIRROR_FRAGMENT = ITEMS.register("mirror_fragment",
        () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DISTORTION_CORE = ITEMS.register("distortion_core",
        () -> new Item(new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> GUARDIAN_HEART = ITEMS.register("guardian_heart",
        () -> new Item(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> EXPEDITION_STABILIZER = ITEMS.register("expedition_stabilizer",
        () -> new Item(new Item.Properties().stacksTo(16).fireResistant()));
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
