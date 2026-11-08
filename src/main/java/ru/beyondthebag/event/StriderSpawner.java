package ru.beyondthebag.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.item.DimensionBagItem;
import ru.beyondthebag.registry.ModEntities;
import ru.beyondthebag.entity.StriderEntity;

/** Controlled server-side spawning for the first expedition creature. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class StriderSpawner {
    private static final Map<UUID, Long> NEXT_ATTEMPT = new ConcurrentHashMap<>();
    private StriderSpawner() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || !player.level().dimension().equals(DimensionBagItem.UNSTABLE_WORLD)) return;

        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (now < NEXT_ATTEMPT.getOrDefault(player.getUUID(), 0L)) return;
        NEXT_ATTEMPT.put(player.getUUID(), now + 240L);

        PocketData data = PocketData.get(player.server.overworld());
        PocketData.Expedition expedition = data.getExpedition(player.getUUID()).orElse(null);
        if (expedition == null || expedition.instability() < 2) return;

        if (!level.getEntitiesOfClass(StriderEntity.class,
                new AABB(player.blockPosition()).inflate(40.0D)).isEmpty()) return;

        for (int attempt = 0; attempt < 16; attempt++) {
            int dx = level.random.nextInt(25) - 12;
            int dz = level.random.nextInt(25) - 12;
            if (Math.abs(dx) + Math.abs(dz) < 8) continue;

            // Expedition rooms are built on Y=72. Do not use the dimension heightmap:
            // natural terrain can be far above or below the actual maze.
            int x = player.blockPosition().getX() + dx;
            int z = player.blockPosition().getZ() + dz;
            BlockPos spawn = new BlockPos(x, 73, z);
            BlockPos floorPos = spawn.below();
            BlockState floor = level.getBlockState(floorPos);
            if (!isMazeFloor(floor)
                    || !level.getBlockState(spawn).isAir()
                    || !level.getBlockState(spawn.above()).isAir()
                    || !level.getBlockState(spawn.above(2)).isAir()) continue;
            if (!level.getBlockState(floorPos).isFaceSturdy(level, floorPos, Direction.UP)) continue;
            if (level.getBlockState(spawn).is(Blocks.MAGMA_BLOCK)
                    || level.getBlockState(spawn).is(Blocks.SOUL_FIRE)) continue;

            StriderEntity creature = ModEntities.STRIDER.get().create(level);
            if (creature == null) return;
            creature.moveTo(x + 0.5D, spawn.getY(), z + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            creature.setPersistenceRequired();
            level.addFreshEntity(creature);
            return;
        }
    }

    private static boolean isMazeFloor(BlockState state) {
        return state.is(Blocks.DEEPSLATE_TILES)
                || state.is(Blocks.POLISHED_DEEPSLATE)
                || state.is(Blocks.CRACKED_DEEPSLATE_BRICKS)
                || state.is(Blocks.BLACKSTONE);
    }
}
