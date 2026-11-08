package ru.beyondthebag.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Reusable artifact; travel and menu actions will be server-authoritative. */
public final class DimensionBagItem extends Item {
    public DimensionBagItem(Properties properties) { super(properties); }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.beyondthebag.menu_pending"), true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
