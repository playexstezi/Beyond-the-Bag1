package ru.beyondthebag.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.entity.MimicEntity;

@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MimicActionEvents {
    private MimicActionEvents() {}

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative()) return;
        BlockState state = event.getPlacedBlock();
        if (state.isAir() || state.hasBlockEntity()) return;
        mirrorAction(player, event.getPos(), state, true);
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null || player.isCreative()) return;
        BlockState state = event.getState();
        if (state.isAir() || state.hasBlockEntity()) return;
        mirrorAction(player, event.getPos(), state, false);
    }

    private static void mirrorAction(ServerPlayer player, BlockPos sourcePos, BlockState state, boolean place) {
        double yaw = Math.toRadians(player.getYRot());
        int sideX = (int) Math.round(Math.cos(yaw) * 2.0D);
        int sideZ = (int) Math.round(Math.sin(yaw) * 2.0D);
        BlockPos mirroredPos = sourcePos.offset(sideX, 0, sideZ);
        for (MimicEntity mimic : player.level().getEntitiesOfClass(MimicEntity.class,
                new AABB(player.blockPosition()).inflate(24.0D),
                entity -> player.getUUID().equals(entity.getCopiedPlayerId()))) {
            mimic.imitateBlockAction(mirroredPos, state, place);
        }
    }
}