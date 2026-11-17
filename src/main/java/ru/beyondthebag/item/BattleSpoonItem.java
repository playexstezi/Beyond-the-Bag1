package ru.beyondthebag.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

public final class BattleSpoonItem extends SwordItem {
    public BattleSpoonItem(Properties properties) { super(Tiers.IRON, 4, -2.25F, properties); }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!target.level().isClientSide) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 50, 0));
        }
        return result;
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0));
            player.getCooldowns().addCooldown(this, 20 * 22);
            stack.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
            player.playSound(SoundEvents.IRON_GOLEM_ATTACK, 0.9F, 1.25F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
