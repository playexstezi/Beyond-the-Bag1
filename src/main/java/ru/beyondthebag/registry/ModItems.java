package ru.beyondthebag.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
import ru.beyondthebag.item.MimicWingsArmorItem;
import net.minecraft.world.item.Tiers;
import ru.beyondthebag.item.BattleSpoonItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.item.DimensionBagItem;
import ru.beyondthebag.item.TrueSoulItem;

public final class ModItems {
    private ModItems() {}
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BeyondTheBag.MOD_ID);
    public static final RegistryObject<Item> TRUE_SOUL = ITEMS.register("true_soul", () -> new TrueSoulItem(new Item.Properties().stacksTo(1).fireResistant()));
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
    public static final RegistryObject<Item> MIMIC_HELMET = ITEMS.register("mimic_helmet",
        () -> new ArmorItem(ru.beyondthebag.item.MimicArmorMaterial.MIMIC, ArmorItem.Type.HELMET, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> MIMIC_CHESTPLATE = ITEMS.register("mimic_chestplate",
        () -> new ArmorItem(ru.beyondthebag.item.MimicArmorMaterial.MIMIC, ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> MIMIC_LEGGINGS = ITEMS.register("mimic_leggings",
        () -> new ArmorItem(ru.beyondthebag.item.MimicArmorMaterial.MIMIC, ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> MIMIC_BOOTS = ITEMS.register("mimic_boots",
        () -> new ArmorItem(ru.beyondthebag.item.MimicArmorMaterial.MIMIC, ArmorItem.Type.BOOTS, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> MIMIC_WINGS = ITEMS.register("mimic_wings",
        () -> new MimicWingsArmorItem(ru.beyondthebag.item.MimicArmorMaterial.MIMIC, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> BATTLE_SPOON = ITEMS.register("battle_spoon",
        () -> new BattleSpoonItem(Tiers.IRON, 4, -2.4F, new Item.Properties().durability(480).fireResistant()));
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
