package ru.beyondthebag.item;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ru.beyondthebag.data.PocketData;

/**
 * Main gameplay controller. Sneak-use binds the return point; normal use
 * starts a fresh expedition or, when one is active, ends it and returns home.
 */
public final class DimensionBagItem extends Item {
    public DimensionBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level,
            net.minecraft.world.entity.player.Player player, InteractionHand hand) {
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
            serverPlayer.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.25F);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        Optional<PocketData.Expedition> active = data.getExpedition(serverPlayer.getUUID());
        if (active.isEmpty()) {
            long seed = ThreadLocalRandom.current().nextLong();
            long runId = ThreadLocalRandom.current().nextLong();
            data.startExpedition(serverPlayer.getUUID(), seed, runId);
            serverPlayer.displayClientMessage(Component.translatable("message.beyondthebag.expedition_started"), false);
            serverPlayer.playNotifySound(SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.75F, 0.8F);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        data.clearExpedition(serverPlayer.getUUID());
        Optional<PocketData.Home> saved = data.getHome(serverPlayer.getUUID());
        if (saved.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("message.beyondthebag.expedition_ended_no_home"), false);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        PocketData.Home home = saved.get();
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, home.dimension());
        ServerLevel destination = serverPlayer.server.getLevel(dimensionKey);
        if (destination == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.beyondthebag.home_unavailable"), false);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        serverPlayer.teleportTo(destination, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        serverPlayer.displayClientMessage(Component.translatable("message.beyondthebag.expedition_returned"), false);
        serverPlayer.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.9F);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
