package ru.beyondthebag.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ru.beyondthebag.network.MimicNetwork;

public class TrueSoulItem extends Item {
    public TrueSoulItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            var data = serverPlayer.getPersistentData();
            data.putBoolean("beyondthebag_mimic_disguise_active", false);
            data.remove("beyondthebag_mimic_transform_start");
            data.remove("beyondthebag_mimic_transform_stage");
            MimicNetwork.broadcastForm(serverPlayer.getUUID(), "", false);
            serverPlayer.displayClientMessage(Component.literal("§bИстинная душа возвращает твой облик."), true);
            level.playSound(null, serverPlayer.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 0.9F, 0.75F);
            if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
