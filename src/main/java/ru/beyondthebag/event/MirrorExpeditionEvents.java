package ru.beyondthebag.event;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.ExpeditionType;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.item.DimensionBagItem;
import ru.beyondthebag.world.MirrorWorldBuilder;

@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MirrorExpeditionEvents {
    private static final String PROGRESS = "beyondthebag_mirror_progress";
    private static final String RUN = "beyondthebag_mirror_run";
    private MirrorExpeditionEvents() {}

    @SubscribeEvent
    public static void onShardTouched(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(DimensionBagItem.UNSTABLE_WORLD)) return;

        PocketData.Expedition expedition = PocketData.get(player.server.overworld())
                .getExpedition(player.getUUID()).orElse(null);
        if (expedition == null || expedition.type() != ExpeditionType.MIRROR) return;

        int originX = Math.floorMod((int) (expedition.seed() >>> 32), 8192) * 16;
        int originZ = Math.floorMod((int) expedition.seed(), 8192) * 16;
        var tag = player.getPersistentData();
        int stage = tag.getLong(RUN) == expedition.runId() ? tag.getInt(PROGRESS) : 0;
        if (stage >= 3) return;

        int selected = -1;
        for (int option = 0; option < 3; option++) {
            if (event.getPos().equals(MirrorWorldBuilder.shardPosition(originX, originZ, stage, option))) {
                selected = option;
                break;
            }
        }
        if (selected < 0) return;
        event.setCanceled(true);

        if (selected != MirrorWorldBuilder.answer(expedition.seed(), stage)) {
            tag.putLong(RUN, expedition.runId());
            tag.putInt(PROGRESS, 0);
            player.displayClientMessage(Component.literal("§cОтражение исказилось. Придётся начать последовательность заново."), true);
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 0.8F, 0.65F);
            return;
        }

        stage++;
        tag.putLong(RUN, expedition.runId());
        tag.putInt(PROGRESS, stage);
        player.displayClientMessage(Component.literal("§bОтражение восстановлено: " + stage + "/3"), true);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME.get(), SoundSource.PLAYERS, 0.9F, 1.1F);

        if (stage == 3) {
            BlockPos gate = MirrorWorldBuilder.gatePosition(originX, originZ);
            level.setBlock(gate, Blocks.RESPAWN_ANCHOR.defaultBlockState(), 3);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0)
                    level.setBlock(gate.offset(dx, 0, dz), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            }
            player.displayClientMessage(Component.literal("§5Зеркало раскололось. Настоящий выход проявился в последней комнате!"), false);
            level.playSound(null, gate, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.8F, 0.75F);
        }
    }
}