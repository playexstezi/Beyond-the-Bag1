package ru.beyondthebag.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.registry.ModItems;

@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MimicArmorEvents {
    private MimicArmorEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) return;
        boolean fullSet = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MIMIC_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.MIMIC_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.MIMIC_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.MIMIC_BOOTS.get());
        if (!fullSet) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, 0, true, false, true));
        if (player.isCrouching())
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 35, 0, true, false, true));
    }
}