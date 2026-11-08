package ru.beyondthebag.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import ru.beyondthebag.data.PocketData;

/** The bag provides entry to the owner's permanent personal realm and safe return. */
public final class DimensionBagItem extends Item {
    public static final ResourceKey<Level> PERSONAL_WORLD = ResourceKey.create(
            Registries.DIMENSION, new ResourceLocation("beyondthebag", "personal_world"));

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
            data.setHome(serverPlayer.getUUID(), new PocketData.Home(
                    serverLevel.dimension().location(), serverPlayer.getX(), serverPlayer.getY(),
                    serverPlayer.getZ(), serverPlayer.getYRot(), serverPlayer.getXRot()));
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.beyondthebag.home_saved"), true);
            serverPlayer.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.8F, 1.25F);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        if (serverLevel.dimension().equals(PERSONAL_WORLD)) {
            PocketData.Home home = data.getHome(serverPlayer.getUUID()).orElse(null);
            if (home == null) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.beyondthebag.expedition_ended_no_home"), false);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
            ServerLevel destination = serverPlayer.server.getLevel(
                    ResourceKey.create(Registries.DIMENSION, home.dimension()));
            if (destination == null) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.beyondthebag.home_unavailable"), false);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
            serverPlayer.teleportTo(destination, home.x(), home.y(), home.z(),
                    home.yaw(), home.pitch());
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.beyondthebag.expedition_returned"), false);
            serverPlayer.playNotifySound(SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 0.8F, 0.9F);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        ServerLevel personal = serverPlayer.server.getLevel(PERSONAL_WORLD);
        if (personal == null) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.beyondthebag.personal_world_unavailable"), false);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        data.setHome(serverPlayer.getUUID(), new PocketData.Home(
                serverLevel.dimension().location(), serverPlayer.getX(), serverPlayer.getY(),
                serverPlayer.getZ(), serverPlayer.getYRot(), serverPlayer.getXRot()));
        PocketData.PersonalRealm realm = data.getOrCreatePersonalRealm(serverPlayer.getUUID());
        BlockPos probe = new BlockPos(realm.centerX(), personal.getSeaLevel(), realm.centerZ());
        int surfaceY = personal.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                probe.getX(), probe.getZ());
        BlockPos safe = new BlockPos(probe.getX(), Math.max(personal.getMinBuildHeight() + 1,
                surfaceY), probe.getZ());
        personal.getChunkAt(safe);
        serverPlayer.teleportTo(personal, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5,
                serverPlayer.getYRot(), serverPlayer.getXRot());
        serverPlayer.displayClientMessage(Component.translatable(
                "message.beyondthebag.personal_world_entered"), false);
        serverPlayer.playNotifySound(SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.8F, 1.1F);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.beyondthebag.primary")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.beyondthebag.secondary")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.beyondthebag.warning")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
