package ru.beyondthebag.item;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ru.beyondthebag.data.PocketData;

/**
 * The bag's first functional travel feature:
 * normal use returns to the saved entry point; sneak-use records/updates it.
 * Personal-dimension routing will replace this temporary same-world anchor flow
 * once the personal dimension is registered.
 */
public final class DimensionBagItem extends Item {
    public DimensionBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                   InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        PocketData data = PocketData.get(serverLevel);
        if (serverPlayer.isShiftKeyDown()) {
            PocketData.Home home = new PocketData.Home(serverLevel.dimension().location(),
                    serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    serverPlayer.getYRot(), serverPlayer.getXRot());
            data.setHome(serverPlayer.getUUID(), home);
            serverPlayer.displayClientMessage(Component.translatable("message.beyondthebag.home_saved"), true);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        Optional<PocketData.Home> saved = data.getHome(serverPlayer.getUUID());
        if (saved.isEmpty()) {
            PocketData.Home home = new PocketData.Home(serverLevel.dimension().location(),
                    serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    serverPlayer.getYRot(), serverPlayer.getXRot());
            data.setHome(serverPlayer.getUUID(), home);
            serverPlayer.displayClientMessage(Component.translatable("message.beyondthebag.home_first_saved"), true);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        PocketData.Home home = saved.get();
        ResourceLocation dimensionId = home.dimension();
        ServerLevel destination = serverPlayer.server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, dimensionId));
        if (destination == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.beyondthebag.home_unavailable"), true);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        serverPlayer.teleportTo(destination, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        serverPlayer.displayClientMessage(Component.translatable("message.beyondthebag.returned"), true);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
