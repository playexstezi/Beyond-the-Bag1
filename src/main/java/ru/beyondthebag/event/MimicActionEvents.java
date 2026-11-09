package ru.beyondthebag.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.entity.MimicEntity;

/** Queues player block actions for delayed imitation in the mimic's own surroundings. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MimicActionEvents {
    private MimicActionEvents() {}

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative()) return;
        BlockState state = event.getPlacedBlock();
        if (state.isAir() || state.hasBlockEntity()) return;
        queueAction(player, event.getPos(), state, true);
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        Player breaker = event.getPlayer();
        if (!(breaker instanceof ServerPlayer player) || player.isCreative()) return;
        BlockState state = event.getState();
        if (state.isAir() || state.hasBlockEntity()) return;
        queueAction(player, event.getPos(), state, false);
    }

    private static void queueAction(ServerPlayer player, BlockPos source, BlockState state, boolean place) {
        BlockPos relative = source.subtract(player.blockPosition());
        if (Math.abs(relative.getX()) > 4 || Math.abs(relative.getY()) > 3
                || Math.abs(relative.getZ()) > 4) return;
        for (MimicEntity mimic : player.level().getEntitiesOfClass(MimicEntity.class,
                new AABB(player.blockPosition()).inflate(40.0D),
                entity -> player.getUUID().equals(entity.getCopiedPlayerId()) && entity.isAlive())) {
            mimic.imitateBlockAction(relative, state, place);
        }
    }
}
