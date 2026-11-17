package ru.beyondthebag.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        Player player = event.player;
        if (player.tickCount % 40 != 0) return;
        boolean chestPiece = player.getInventory().armor.get(2).is(ModItems.MIMIC_CHESTPLATE.get())
                || player.getInventory().armor.get(2).is(ModItems.MIMIC_WINGS.get());
        boolean fullSet = player.getInventory().armor.get(3).is(ModItems.MIMIC_HELMET.get())
                && chestPiece
                && player.getInventory().armor.get(1).is(ModItems.MIMIC_LEGGINGS.get())
                && player.getInventory().armor.get(0).is(ModItems.MIMIC_BOOTS.get());
        if (!fullSet) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0, true, false));
        if (player.isCrouching()) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, true, false, false));
        }
    }
}
