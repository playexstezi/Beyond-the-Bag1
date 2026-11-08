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
import ru.beyondthebag.registry.ModEntities;

/** Rare Overworld encounters. A mimic spawns only near one isolated player. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MimicSpawner {
    private static final Map<UUID, Long> NEXT_ATTEMPT = new ConcurrentHashMap<>();
    private MimicSpawner() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(Level.OVERWORLD)) return;

        long now = level.getGameTime();
        if (now < NEXT_ATTEMPT.getOrDefault(player.getUUID(), 0L)) return;
        NEXT_ATTEMPT.put(player.getUUID(), now + 1200L);

        AABB area = new AABB(player.blockPosition()).inflate(24.0D);
        long nearbyPlayers = level.getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && !p.isSpectator()).size();
        if (nearbyPlayers == 0) return;
        if (!level.getEntitiesOfClass(MimicEntity.class, area).isEmpty()) return;
        if (level.random.nextInt(5) != 0) return;

        for (int attempt = 0; attempt < 12; attempt++) {
            int x = player.blockPosition().getX() + level.random.nextInt(25) - 12;
            int z = player.blockPosition().getZ() + level.random.nextInt(25) - 12;
            if (Math.abs(x - player.getX()) + Math.abs(z - player.getZ()) < 8) continue;
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, player.blockPosition().getY(), z));
            if (!level.getBlockState(surface).isAir() || !level.getBlockState(surface.above()).isAir()) continue;

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
