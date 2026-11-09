package ru.beyondthebag.event;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.item.DimensionBagItem;

/** Server-side interactions for shrine and compass puzzle rooms. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class ShrinePuzzleEvents {
    private static final String SHRINE_COOLDOWN = "beyondthebag_shrine_cooldown";
    private static final String PUZZLE_SOLVED = "beyondthebag_puzzle_solved_run";

    private ShrinePuzzleEvents() {}

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.level().dimension().equals(DimensionBagItem.UNSTABLE_WORLD)) return;

        BlockPos pos = event.getPos();
        if (!player.level().getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) return;
        PocketData data = PocketData.get(player.server.overworld());

        // A framed anchor is the final exit gate; unframed anchors are shrines.
        if (isGateFrame(player, pos)) return;
        event.setCanceled(true);
        long now = player.server.overworld().getGameTime();
        long readyAt = player.getPersistentData().getLong(SHRINE_COOLDOWN);
        if (now < readyAt) {
            player.displayClientMessage(Component.literal("§dСвятилище ещё восстанавливается."), true);
            return;
        }
        int sanity = player.getPersistentData().contains("beyondthebag_sanity")
                ? player.getPersistentData().getInt("beyondthebag_sanity") : 100;
        player.getPersistentData().putInt("beyondthebag_sanity", Math.min(100, sanity + 25));
        player.getPersistentData().putLong(SHRINE_COOLDOWN, now + 6000L);
        data.getExpedition(player.getUUID()).ifPresent(expedition -> {
            if (expedition.instability() > 0) data.setInstability(player.getUUID(), expedition.instability() - 1);
        });
        player.displayClientMessage(Component.literal("§dСвятилище успокаивает разум и ослабляет нестабильность."), true);
    }

    @SubscribeEvent
    public static void onPuzzleClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.level().dimension().equals(DimensionBagItem.UNSTABLE_WORLD)) return;
        BlockPos pos = event.getPos();
        if (!player.level().getBlockState(pos).is(Blocks.LODESTONE)
                || !player.getMainHandItem().is(Items.COMPASS)) return;

        event.setCanceled(true);
        long runId = PocketData.get(player.server.overworld())
                .getExpedition(player.getUUID()).map(PocketData.Expedition::runId).orElse(0L);
        if (runId == 0L || player.getPersistentData().getLong(PUZZLE_SOLVED) == runId) {
            player.displayClientMessage(Component.literal("§7Загадка этого похода уже решена."), true);
            return;
        }
        BlockPos lamp = pos.west(2);
        if (!player.level().getBlockState(lamp).is(Blocks.REDSTONE_LAMP)) return;
        player.level().setBlock(lamp,
                Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true), 3);
        player.getPersistentData().putLong(PUZZLE_SOLVED, runId);
        int sanity = player.getPersistentData().contains("beyondthebag_sanity")
                ? player.getPersistentData().getInt("beyondthebag_sanity") : 100;
        player.getPersistentData().putInt("beyondthebag_sanity", Math.min(100, sanity + 15));
        player.displayClientMessage(Component.literal("§bКомпас настроил загадочный камень. Загадка решена! Рассудок восстановлен."), false);
    }

    private static boolean isGateFrame(ServerPlayer player, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (!player.level().getBlockState(center.offset(dx, 0, dz)).is(Blocks.CRYING_OBSIDIAN)) return false;
            }
        }
        return true;
    }
}
