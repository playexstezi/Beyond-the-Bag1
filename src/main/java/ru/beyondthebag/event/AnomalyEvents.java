package ru.beyondthebag.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
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
                player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.7F, 0.65F);
            }
            case DISTANT_FOOTSTEPS -> {
                player.displayClientMessage(Component.literal("§7Вы слышите шаги неподалёку."), true);
                player.playNotifySound(SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.AMBIENT, 0.45F, 0.7F);
            }
            case LIGHT_FLICKER -> {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 35, 0, false, false, true));
                player.displayClientMessage(Component.literal("§8Свет на мгновение меркнет."), true);
                player.playNotifySound(SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.AMBIENT, 0.5F, 1.4F);
            }
            case FALSE_DOOR -> {
                player.displayClientMessage(Component.literal("§5Впереди будто появилась дверь. Моргнув, вы больше её не видите."), true);
                player.playNotifySound(SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.AMBIENT, 0.55F, 0.8F);
            }
            case CORRIDOR_SHIFT -> {
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            player.getX(), player.getY() + 1.0D, player.getZ(),
                            24, 0.55D, 0.45D, 0.55D, 0.02D);
                }
                player.displayClientMessage(Component.literal("§5Пространство вокруг вас едва заметно дрогнуло."), true);
                player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.AMBIENT, 0.35F, 0.55F);
            }
            case HIDDEN_CACHE -> {
                if (player.level() instanceof ServerLevel serverLevel) {
                    double angle = player.getYRot() * (Math.PI / 180.0D);
                    double x = player.getX() - Math.sin(angle) * 5.0D;
                    double z = player.getZ() + Math.cos(angle) * 5.0D;
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                            x, player.getY() + 0.8D, z, 14, 0.45D, 0.35D, 0.45D, 0.015D);
                }
                player.displayClientMessage(Component.literal("§dВы замечаете слабое мерцание где-то в стороне."), true);
                player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.65F, 0.7F);
            }
            case FLOOR_TRAP -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 0, false, false, true));
                player.hurt(player.damageSources().magic(), 2.0F);
                player.displayClientMessage(Component.literal("§cПол под ногами опасно затрещал!"), true);
                player.playNotifySound(SoundEvents.DEEPSLATE_BREAK, SoundSource.AMBIENT, 0.8F, 0.65F);
            }
            case ENTITY_SIGHTING -> {
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SOUL,
                            player.getX() - player.getLookAngle().x * 7.0D,
                            player.getY() + 0.7D,
                            player.getZ() - player.getLookAngle().z * 7.0D,
                            10, 0.35D, 0.6D, 0.35D, 0.01D);
                }
                player.displayClientMessage(Component.literal("§4На краю зрения мелькнул силуэт."), true);
                player.playNotifySound(SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.AMBIENT, 0.45F, 0.8F);
            }
            case RIFT_SURGE -> {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            player.getX(), player.getY() + 1.0D, player.getZ(),
                            40, 0.8D, 0.8D, 0.8D, 0.04D);
                }
                player.displayClientMessage(Component.literal("§5Аномалия усиливается. Здесь что-то изменилось."), true);
                player.playNotifySound(SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.AMBIENT, 0.8F, 0.55F);
            }
        }
    }
}
