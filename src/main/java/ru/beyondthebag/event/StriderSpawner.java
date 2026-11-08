package ru.beyondthebag.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.entity.*;
import ru.beyondthebag.item.DimensionBagItem;
import ru.beyondthebag.registry.ModEntities;

/** Spawns the complete expedition creature roster inside maze corridors and rooms. */
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
        NEXT_ATTEMPT.put(player.getUUID(), now + 180L);

        PocketData data = PocketData.get(player.server.overworld());
        PocketData.Expedition expedition = data.getExpedition(player.getUUID()).orElse(null);
        if (expedition == null) return;

        AABB area = new AABB(player.blockPosition()).inflate(40.0D);
        long nearby = level.getEntitiesOfClass(Mob.class, area,
                mob -> mob instanceof StriderEntity || mob instanceof WatcherEntity
                        || mob instanceof DistortedEntity || mob instanceof GuardianEntity
                        || mob instanceof MimicEntity).size();
        if (nearby >= 4) return;

        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = level.random.nextInt(33) - 16;
            int dz = level.random.nextInt(33) - 16;
            if (Math.abs(dx) + Math.abs(dz) < 9) continue;
            int x = player.blockPosition().getX() + dx;
            int z = player.blockPosition().getZ() + dz;
            BlockPos spawn = new BlockPos(x, 73, z);
            BlockPos floorPos = spawn.below();
            BlockState floor = level.getBlockState(floorPos);
            if (!isMazeFloor(floor)
                    || !level.getBlockState(spawn).isAir()
                    || !level.getBlockState(spawn.above()).isAir()
                    || !level.getBlockState(spawn.above(2)).isAir()
                    || !floor.isFaceSturdy(level, floorPos, Direction.UP)) continue;

            Mob creature = createCreature(level, expedition.instability());
            if (creature == null) return;
            creature.moveTo(x + 0.5D, spawn.getY(), z + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            creature.setPersistenceRequired();
            level.addFreshEntity(creature);
            return;
        }
    }

    private static Mob createCreature(ServerLevel level, int instability) {
        int roll = level.random.nextInt(100);
        if (instability >= 4 && roll < 8) return ModEntities.GUARDIAN.get().create(level);
        if (instability >= 3 && roll < 25) return ModEntities.DISTORTED.get().create(level);
        if (instability >= 1 && roll < 43) return ModEntities.MIMIC.get().create(level);
        if (roll < 72) return ModEntities.WATCHER.get().create(level);
        return ModEntities.STRIDER.get().create(level);
    }

    private static boolean isMazeFloor(BlockState state) {
        return state.is(Blocks.DEEPSLATE_TILES)
                || state.is(Blocks.POLISHED_DEEPSLATE)
                || state.is(Blocks.CRACKED_DEEPSLATE_BRICKS)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.POLISHED_BLACKSTONE_BRICKS);
    }
}
