package ru.beyondthebag.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.entity.MimicEntity;
import ru.beyondthebag.item.DimensionBagItem;
import ru.beyondthebag.registry.ModEntities;

/**
 * Mimics are signature encounters, not ordinary ambient mobs:
 * uncommon in the maze and exceptionally rare in the Overworld.
 */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MimicSpawner {
    private static final Map<UUID, Long> NEXT_OVERWORLD_ATTEMPT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_EXPEDITION_ATTEMPT = new ConcurrentHashMap<>();

    private static final long OVERWORLD_INTERVAL = 24_000L; // 20 minutes between rolls
    private static final long EXPEDITION_INTERVAL = 2_400L; // 2 minutes between rolls

    private MimicSpawner() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) return;

        if (level.dimension().equals(Level.OVERWORLD)) {
            attemptOverworldEncounter(player, level);
        } else if (level.dimension().equals(DimensionBagItem.UNSTABLE_WORLD)) {
            attemptExpeditionEncounter(player, level);
        }
    }

    private static void attemptOverworldEncounter(ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();
        if (now < NEXT_OVERWORLD_ATTEMPT.getOrDefault(player.getUUID(), 0L)) return;
        NEXT_OVERWORLD_ATTEMPT.put(player.getUUID(), now + OVERWORLD_INTERVAL);

        if (!isIsolated(player, level, 32.0D)) return;
        if (countNearby(level, player.blockPosition(), 128.0D) >= 1) return;

        // 1 in 24 rolls, with a roll only every twenty minutes per player.
        if (level.random.nextInt(24) != 0) return;
        spawnAtDistance(player, level, 18, 32);
    }

    private static void attemptExpeditionEncounter(ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();
        if (now < NEXT_EXPEDITION_ATTEMPT.getOrDefault(player.getUUID(), 0L)) return;
        NEXT_EXPEDITION_ATTEMPT.put(player.getUUID(), now + EXPEDITION_INTERVAL);

        if (!isIsolated(player, level, 28.0D)) return;
        if (countNearby(level, player.blockPosition(), 80.0D) >= 2) return;

        // The maze is their hunting ground: more likely than the Overworld,
        // but still spaced out so a mimic remains an encounter worth noticing.
        if (level.random.nextInt(6) != 0) return;
        spawnAtDistance(player, level, 12, 24);
    }

    private static boolean isIsolated(ServerPlayer player, ServerLevel level, double radius) {
        AABB area = player.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && !p.isSpectator()).size() == 1;
    }

    private static int countNearby(ServerLevel level, BlockPos center, double radius) {
        return level.getEntitiesOfClass(MimicEntity.class,
                new AABB(center).inflate(radius)).size();
    }

    private static void spawnAtDistance(ServerPlayer player, ServerLevel level,
                                        int minDistance, int maxDistance) {
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = minDistance + level.random.nextDouble() * (maxDistance - minDistance);
            int x = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * distance);
            double dx = x + 0.5D - player.getX();
            double dz = z + 0.5D - player.getZ();
            double distanceSqr = dx * dx + dz * dz;
            if (distanceSqr < (double) minDistance * minDistance
                    || distanceSqr > (double) maxDistance * maxDistance) continue;

            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, player.blockPosition().getY(), z));
            if (!level.hasChunkAt(surface) || !level.getBlockState(surface).isAir()
                    || !level.getBlockState(surface.above()).isAir()
                    || !level.getBlockState(surface.below()).isSolid()) continue;

            // Keep the encounter outside the player's immediate view where possible.
            if (player.hasLineOfSight(net.minecraft.world.phys.Vec3.atCenterOf(surface))) continue;

            MimicEntity mimic = ModEntities.MIMIC.get().create(level);
            if (mimic == null) return;
            mimic.moveTo(surface.getX() + 0.5D, surface.getY(), surface.getZ() + 0.5D,
                    player.getYRot(), 0.0F);
            mimic.setPersistenceRequired();
            level.addFreshEntity(mimic);
            return;
        }
    }
}
