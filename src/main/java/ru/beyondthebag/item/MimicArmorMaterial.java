package ru.beyondthebag.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import ru.beyondthebag.registry.ModItems;

public enum MimicArmorMaterial implements ArmorMaterial {
    MIMIC;
    @Override public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 286;
            case CHESTPLATE -> 416;
            case LEGGINGS -> 390;
            case BOOTS -> 338;
        };
    }
    @Override public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 3;
            case CHESTPLATE -> 7;
            case LEGGINGS -> 6;
            case BOOTS -> 3;
        };
    }
    @Override public int getEnchantmentValue() { return 18; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_CHAIN; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.MIMIC_FRAGMENT.get()); }
    @Override public String getName() { return "minecraft:chainmail"; }
    @Override public float getToughness() { return 1.5F; }
    @Override public float getKnockbackResistance() { return 0.05F; }
}
