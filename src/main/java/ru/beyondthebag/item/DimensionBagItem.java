package ru.beyondthebag.item;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import ru.beyondthebag.world.MazeWorldBuilder;

/** The bag randomly opens a permanent personal realm or an unstable expedition. */
public final class DimensionBagItem extends Item {
    public static final ResourceKey<Level> PERSONAL_WORLD = ResourceKey.create(
            Registries.DIMENSION, new ResourceLocation("beyondthebag", "personal_world"));
    public static final ResourceKey<Level> UNSTABLE_WORLD = ResourceKey.create(
            Registries.DIMENSION, new ResourceLocation("beyondthebag", "unstable_world"));

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
            if (serverLevel.dimension().equals(PERSONAL_WORLD)
                    || serverLevel.dimension().equals(UNSTABLE_WORLD)) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.beyondthebag.home_save_inside_realm"), true);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
            data.setHome(serverPlayer.getUUID(), new PocketData.Home(
                    serverLevel.dimension().location(), serverPlayer.getX(), serverPlayer.getY(),
                    serverPlayer.getZ(), serverPlayer.getYRot(), serverPlayer.getXRot()));
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.beyondthebag.home_saved"), true);
            serverPlayer.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.8F, 1.25F);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        if (serverLevel.dimension().equals(UNSTABLE_WORLD)) {
            serverPlayer.displayClientMessage(Component.literal(
                    "В нестабильности мешок не работает. Найди врата выхода в лабиринте."), false);
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
            boolean fromUnstable = serverLevel.dimension().equals(UNSTABLE_WORLD);
            serverPlayer.teleportTo(destination, home.x(), home.y(), home.z(),
                    home.yaw(), home.pitch());
            data.clearExpedition(serverPlayer.getUUID());
            serverPlayer.displayClientMessage(Component.translatable(
                    fromUnstable ? "message.beyondthebag.expedition_returned"
                            : "message.beyondthebag.expedition_returned"), false);
            serverPlayer.playNotifySound(SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 0.8F, 0.9F);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        boolean unstableRoute = ThreadLocalRandom.current().nextBoolean();
        ResourceKey<Level> targetKey = unstableRoute ? UNSTABLE_WORLD : PERSONAL_WORLD;
        ServerLevel destination = serverPlayer.server.getLevel(targetKey);
        if (destination == null) {
            serverPlayer.displayClientMessage(Component.translatable(
                    unstableRoute ? "message.beyondthebag.unstable_world_unavailable"
                            : "message.beyondthebag.personal_world_unavailable"), false);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        // Only replace the return anchor after the selected destination is confirmed loaded.
        data.setHome(serverPlayer.getUUID(), new PocketData.Home(
                serverLevel.dimension().location(), serverPlayer.getX(), serverPlayer.getY(),
                serverPlayer.getZ(), serverPlayer.getYRot(), serverPlayer.getXRot()));

        if (unstableRoute) {
            // Reconcile saved rendezvous entries with players who are physically present.
            java.util.Set<java.util.UUID> activeMembers = destination.players().stream()
                    .map(ServerPlayer::getUUID)
                    .collect(java.util.stream.Collectors.toSet());
            data.retainActiveExpeditionMembers(activeMembers);
            long seed = ThreadLocalRandom.current().nextLong();
            long runId = ThreadLocalRandom.current().nextLong();
            PocketData.Expedition expedition = data.joinOrStartExpedition(
                    serverPlayer.getUUID(), seed, runId,
                    serverPlayer.server.overworld().getGameTime(), 1200L);
            // Members of the same rendezvous share one deterministic entry point.
            int expeditionX = Math.floorMod((int) (expedition.seed() >>> 32), 8192) * 16;
            int expeditionZ = Math.floorMod((int) expedition.seed(), 8192) * 16;
            BlockPos spawn = MazeWorldBuilder.buildIfNeeded(
                    destination, data, expedition, expeditionX, expeditionZ);
            serverPlayer.teleportTo(destination, spawn.getX() + 0.5, spawn.getY(),
                    spawn.getZ() + 0.5, serverPlayer.getYRot(), serverPlayer.getXRot());
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.beyondthebag.unstable_world_entered"), false);
            serverPlayer.displayClientMessage(Component.literal(
                    "Экспедиция: " + Long.toUnsignedString(expedition.runId(), 16)), true);
            serverPlayer.playNotifySound(SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.PLAYERS, 0.8F, 0.7F);
        } else {
            data.clearExpedition(serverPlayer.getUUID());
            PocketData.PersonalRealm realm = data.getOrCreatePersonalRealm(serverPlayer.getUUID());
            BlockPos spawn = findSpawn(destination, realm.centerX(), realm.centerZ());
            serverPlayer.teleportTo(destination, spawn.getX() + 0.5, spawn.getY(),
                    spawn.getZ() + 0.5, serverPlayer.getYRot(), serverPlayer.getXRot());
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.beyondthebag.personal_world_entered"), false);
            serverPlayer.playNotifySound(SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 0.8F, 1.1F);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static BlockPos findSpawn(ServerLevel level, int x, int z) {
        // Generate/load terrain before reading its heightmap.
        level.getChunkAt(new BlockPos(x, level.getSeaLevel(), z));

        for (int radius = 0; radius <= 16; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    int candidateX = x + dx;
                    int candidateZ = z + dz;
                    level.getChunkAt(new BlockPos(candidateX, level.getSeaLevel(), candidateZ));
                    int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidateX, candidateZ);
                    BlockPos feet = new BlockPos(candidateX, surfaceY, candidateZ);
                    BlockPos floor = feet.below();
                    if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                            && level.getBlockState(feet).isAir()
                            && level.getBlockState(feet.above()).isAir()
                            && level.getFluidState(feet).isEmpty()
                            && level.getFluidState(feet.above()).isEmpty()) return feet;
                }
            }
        }

        // Fallback: clear a landing space and make sure it has a solid floor.
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos feet = new BlockPos(x, surfaceY, z);
        level.setBlock(feet.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(feet, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(feet.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        return feet;
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
