package ru.beyondthebag.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.item.DimensionBagItem;
import ru.beyondthebag.registry.ModItems;

@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class ExpeditionStabilizerEvents {
    private ExpeditionStabilizerEvents() {}

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof Level level)
                || !level.dimension().equals(DimensionBagItem.UNSTABLE_WORLD)
                || !player.getItemInHand(event.getHand()).is(ModItems.EXPEDITION_STABILIZER.get())) return;

        event.setCanceled(true);
        PocketData data = PocketData.get(player.server.overworld());
        PocketData.Expedition expedition = data.getExpedition(player.getUUID()).orElse(null);
        if (expedition == null || expedition.instability() <= 0) {
            player.displayClientMessage(Component.translatable("message.beyondthebag.stabilizer_empty"), true);
            return;
        }

        data.setInstability(player.getUUID(), expedition.instability() - 1);
        if (!player.getAbilities().instabuild) player.getItemInHand(event.getHand()).shrink(1);
        player.displayClientMessage(Component.translatable("message.beyondthebag.stabilizer_used"), true);
        player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.PLAYERS, 0.9F, 1.25F);
    }
}
