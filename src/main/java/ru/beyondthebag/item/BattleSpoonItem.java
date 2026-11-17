package ru.beyondthebag.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A heavy cooking spoon repurposed as a close-combat weapon. */
public final class BattleSpoonItem extends SwordItem {
    public BattleSpoonItem(Tier tier, int damage, float speed, Item.Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            int hitCount = 0;
            for (Monster monster : level.getEntitiesOfClass(Monster.class,
                    player.getBoundingBox().inflate(3.25D),
                    target -> target.isAlive() && !target.isAlliedTo(player))) {
                if (monster.hurt(level.damageSources().playerAttack(player), 4.0F)) {
                    monster.knockback(0.85D, player.getX() - monster.getX(),
                            player.getZ() - monster.getZ());
                    monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            60, 0, false, true, true));
                    hitCount++;
                }
            }
            player.getCooldowns().addCooldown(this, 200);
            stack.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 0.9F, 0.85F);
            if (hitCount > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                        40, 0, false, false, true));
            }
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.beyondthebag.battle_spoon"));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean hit = super.hurtEnemy(stack, target, attacker);
        if (hit && !attacker.level().isClientSide) {
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 0, false, false, true));
            if (attacker.getRandom().nextFloat() < 0.35F) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 0, false, true, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0, false, true, true));
            }
        }
        return hit;
    }
}
