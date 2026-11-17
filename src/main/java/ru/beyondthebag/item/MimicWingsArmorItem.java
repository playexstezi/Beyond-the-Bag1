package ru.beyondthebag.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/** Mimic chest armour that also supports normal elytra gliding. */
public final class MimicWingsArmorItem extends ArmorItem {
    public MimicWingsArmorItem(ArmorMaterial material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!entity.level().isClientSide && flightTicks % 20 == 0) {
            stack.hurtAndBreak(1, entity, wearer -> wearer.broadcastBreakEvent(EquipmentSlot.CHEST));
        }
        return true;
    }
}
