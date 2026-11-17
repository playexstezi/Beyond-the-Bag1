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
import ru.beyondthebag.world.RiddleWorldBuilder;

@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class RiddleExpeditionEvents {
    private static final String PROGRESS = "beyondthebag_riddle_progress";
    private static final String RUN = "beyondthebag_riddle_run";
    private RiddleExpeditionEvents() {}

    @SubscribeEvent
    public static void onPadPressed(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(DimensionBagItem.UNSTABLE_WORLD)
                ) return;
        PocketData.Expedition expedition = PocketData.get(player.server.overworld())
                .getExpedition(player.getUUID()).orElse(null);
        if (expedition == null || expedition.type() != ExpeditionType.RIDDLE) return;

        int ox = Math.floorMod((int)(expedition.seed() >>> 32),8192)*16;
        int oz = Math.floorMod((int)expedition.seed(),8192)*16;
        var tag = player.getPersistentData();
        int stage = tag.getLong(RUN) == expedition.runId() ? tag.getInt(PROGRESS) : 0;
        if (stage >= 3) return;
        int selected = -1;
        for (int option=0; option<3; option++)
            if (event.getPos().equals(RiddleWorldBuilder.padPosition(ox,oz,stage,option))) selected=option;
        if (selected < 0) return;
        event.setCanceled(true);
        if (selected != RiddleWorldBuilder.answer(expedition.seed(),stage)) {
            tag.putLong(RUN,expedition.runId());
            tag.putInt(PROGRESS,0);
            player.displayClientMessage(Component.literal("§cНеверный символ. Последовательность сброшена."),true);
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS,SoundSource.PLAYERS,0.8F,0.6F);
            return;
        }
        stage++;
        tag.putLong(RUN,expedition.runId());
        tag.putInt(PROGRESS,stage);
        player.displayClientMessage(Component.literal("§dЗагадка решена: "+stage+"/3"),true);
        player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME,SoundSource.PLAYERS,0.9F,1.2F);
        if (stage == 3) {
            BlockPos gate=RiddleWorldBuilder.gatePosition(ox,oz);
            level.setBlock(gate,Blocks.RESPAWN_ANCHOR.defaultBlockState(),3);
            for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++)
                if(dx!=0||dz!=0) level.setBlock(gate.offset(dx,0,dz),Blocks.CRYING_OBSIDIAN.defaultBlockState(),3);
            player.displayClientMessage(Component.literal("§5Все загадки решены. Врата выхода открыты!"),false);
            level.playSound(null,gate,SoundEvents.RESPAWN_ANCHOR_CHARGE,SoundSource.BLOCKS,1.8F,0.8F);
        }
    }
}