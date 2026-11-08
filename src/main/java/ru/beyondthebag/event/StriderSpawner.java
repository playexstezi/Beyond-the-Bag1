package ru.beyondthebag.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
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

        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = level.random.nextInt(25) - 12;
            int dz = level.random.nextInt(25) - 12;
            if (Math.abs(dx) + Math.abs(dz) < 8) continue;

            BlockPos feet = player.blockPosition().offset(dx, 0, dz);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    feet.getX(), feet.getZ());
            BlockPos spawn = new BlockPos(feet.getX(), y, feet.getZ());
            if (!level.getBlockState(spawn.below()).isFaceSturdy(level, spawn.below(), net.minecraft.core.Direction.UP)
                    || !level.getBlockState(spawn).isAir()
                    || !level.getBlockState(spawn.above()).isAir()) continue;

            StriderEntity creature = ModEntities.STRIDER.get().create(level);
            if (creature == null) return;
            creature.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            creature.setPersistenceRequired();
            level.addFreshEntity(creature);
            return;
        }
    }
}
