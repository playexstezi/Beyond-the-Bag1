package ru.beyondthebag.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.data.PocketData;
import ru.beyondthebag.item.DimensionBagItem;
import ru.beyondthebag.world.AnomalyDirector;

/** Server-authoritative ambient anomaly cues during unstable expeditions. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class AnomalyEvents {
    private static final Map<UUID, Long> NEXT_ROLL = new ConcurrentHashMap<>();
    private AnomalyEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || !player.level().dimension().equals(DimensionBagItem.UNSTABLE_WORLD)) return;

        long now = player.server.overworld().getGameTime();
        long next = NEXT_ROLL.getOrDefault(player.getUUID(), 0L);
        if (now < next) return;
        NEXT_ROLL.put(player.getUUID(), now + 200L);

        PocketData data = PocketData.get(player.server.overworld());
        PocketData.Expedition expedition = data.getExpedition(player.getUUID()).orElse(null);
        if (expedition == null) return;

        AnomalyDirector.roll(expedition.instability(), 35, RandomSource.create())
                .ifPresent(anomaly -> trigger(player, anomaly));
    }

    private static void trigger(ServerPlayer player, AnomalyDirector.Event anomaly) {
        switch (anomaly) {
            case DISTANT_WHISPER -> {
                player.displayClientMessage(Component.literal("§8За стеной кто-то тихо произносит ваше имя..."), true);
                player.playNotifySound(SoundEvents.AMBIENT_CAVE, SoundSource.AMBIENT, 0.7F, 0.65F);
            }
            case DISTANT_FOOTSTEPS -> {
                player.displayClientMessage(Component.literal("§7Вы слышите шаги неподалёку."), true);
                player.playNotifySound(SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.AMBIENT, 0.45F, 0.7F);
            }
            case LIGHT_FLICKER -> {
                player.displayClientMessage(Component.literal("§8Свет на мгновение меркнет."), true);
                player.playNotifySound(SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.AMBIENT, 0.5F, 1.4F);
            }
            case FALSE_DOOR -> {
                player.displayClientMessage(Component.literal("§5Впереди будто появилась дверь. Моргнув, вы больше её не видите."), true);
                player.playNotifySound(SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.AMBIENT, 0.55F, 0.8F);
            }
            case CORRIDOR_SHIFT -> {
                player.displayClientMessage(Component.literal("§5Пространство вокруг вас едва заметно дрогнуло."), true);
                player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.AMBIENT, 0.35F, 0.55F);
            }
            case HIDDEN_CACHE -> {
                player.displayClientMessage(Component.literal("§dВы замечаете слабое мерцание где-то в стороне."), true);
                player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.65F, 0.7F);
            }
            case FLOOR_TRAP -> {
                player.displayClientMessage(Component.literal("§cПол под ногами опасно затрещал!"), true);
                player.playNotifySound(SoundEvents.DEEPSLATE_BREAK, SoundSource.AMBIENT, 0.8F, 0.65F);
            }
            case ENTITY_SIGHTING -> {
                player.displayClientMessage(Component.literal("§4На краю зрения мелькнул силуэт."), true);
                player.playNotifySound(SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.AMBIENT, 0.45F, 0.8F);
            }
            case RIFT_SURGE -> {
                player.displayClientMessage(Component.literal("§5Аномалия усиливается. Здесь что-то изменилось."), true);
                player.playNotifySound(SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.AMBIENT, 0.8F, 0.55F);
            }
        }
    }
}
