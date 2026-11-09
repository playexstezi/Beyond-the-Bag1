package ru.beyondthebag.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** A crystallized fragment of a mimic's defensive instincts. */
public final class MimicShardItem extends Item {
    public MimicShardItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getCooldowns().isOnCooldown(this)) {
            if (!level.isClientSide) {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 8, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 20, 0));
                if (!player.getAbilities().instabuild) stack.shrink(1);
                player.getCooldowns().addCooldown(this, 20 * 90);
                player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 0.65F);
                player.displayClientMessage(Component.literal("Осколок мимика укрыл тебя защитной оболочкой."), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ: защитная оболочка на 30 секунд").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Поглощение II, сопротивление и скорость").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Перезарядка: 90 секунд").withStyle(ChatFormatting.DARK_GRAY));
    }
}
