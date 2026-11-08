package ru.beyondthebag.event;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.item.DimensionBagItem;

/** The lodestone at the maze's exit gate returns an explorer to their saved entry point. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class UnstableExitEvents {
    private UnstableExitEvents() {}

    @SubscribeEvent
    public static void onExitGateUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(DimensionBagItem.UNSTABLE_WORLD)
                || !level.getBlockState(event.getPos()).is(Blocks.LODESTONE)) return;

        event.setCanceled(true);
        PocketData data = PocketData.get(player.server.overworld());
        PocketData.Home home = data.getHome(player.getUUID()).orElse(null);
        if (home == null) {
            player.displayClientMessage(Component.literal("Выход не может найти точку возвращения."), false);
            return;
        }
        ResourceKey<net.minecraft.world.level.Level> key =
                ResourceKey.create(Registries.DIMENSION, home.dimension());
        ServerLevel destination = player.server.getLevel(key);
        if (destination == null) {
            player.displayClientMessage(Component.literal("Точка возвращения сейчас недоступна."), false);
            return;
        }

        player.teleportTo(destination, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        data.clearExpedition(player.getUUID());
        player.displayClientMessage(Component.literal("Экспедиция завершена. Вы нашли выход."), false);
        player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 0.8F);
    }
}
