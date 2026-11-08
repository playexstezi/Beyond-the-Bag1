package ru.beyondthebag.event;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.entity.MimicEntity;

/** Records player building/mining and replays it later near the mimic. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MimicBuildingEvents {
    private MimicBuildingEvents() {}

    @SubscribeEvent
    public static void onPlayerPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) return;
        BlockPos source = event.getPos();
        BlockState state = level.getBlockState(source);
        if (unsafeToCopy(state) || level.getBlockEntity(source) != null) return;

        for (MimicEntity mimic : nearbyMimics(level, player)) {
            BlockPos destination = translatedPos(player, mimic, source);
            if (level.isLoaded(destination) && !destination.equals(source)) {
                mimic.imitateBlockAction(destination, state, true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerBreak(BlockEvent.BreakEvent event) {
        Player breaker = event.getPlayer();
        if (!(breaker instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) return;
        BlockPos source = event.getPos();
        BlockState state = level.getBlockState(source);
        if (unsafeToCopy(state) || level.getBlockEntity(source) != null) return;

        for (MimicEntity mimic : nearbyMimics(level, player)) {
            BlockPos destination = translatedPos(player, mimic, source);
            if (level.isLoaded(destination) && !destination.equals(source)) {
                mimic.imitateBlockAction(destination, state, false);
            }
        }
    }

    private static boolean unsafeToCopy(BlockState state) {
        return state.isAir() || state.hasBlockEntity();
    }

    private static List<MimicEntity> nearbyMimics(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(48.0D);
        UUID playerId = player.getUUID();
        return level.getEntitiesOfClass(MimicEntity.class, area,
                mimic -> playerId.equals(mimic.getCopiedPlayerId()) && mimic.isAlive());
    }

    private static BlockPos translatedPos(ServerPlayer player, MimicEntity mimic, BlockPos source) {
        return mimic.blockPosition().offset(
                source.getX() - player.blockPosition().getX(),
                source.getY() - player.blockPosition().getY(),
                source.getZ() - player.blockPosition().getZ());
    }
}
