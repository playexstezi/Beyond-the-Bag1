package ru.beyondthebag.event;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.registry.ModItems;

@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MimicFormEvents {
    private static final String FORM_KEY = "beyondthebag_mimic_form";
    private static final String NAME_KEY = "beyondthebag_mimic_form_name";
    private static final String COOLDOWN_KEY = "beyondthebag_mimic_form_ready";
    private MimicFormEvents() {}

    private static boolean hasFullSet(Player player) {
        boolean chest = player.getInventory().armor.get(2).is(ModItems.MIMIC_CHESTPLATE.get())
                || player.getInventory().armor.get(2).is(ModItems.MIMIC_WINGS.get());
        return player.getInventory().armor.get(3).is(ModItems.MIMIC_HELMET.get())
                && chest
                && player.getInventory().armor.get(1).is(ModItems.MIMIC_LEGGINGS.get())
                && player.getInventory().armor.get(0).is(ModItems.MIMIC_BOOTS.get());
    }

    @SubscribeEvent
    public static void onCopyForm(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !player.isShiftKeyDown()
                || !player.getItemInHand(event.getHand()).is(ModItems.MIMIC_FRAGMENT.get())
                || !hasFullSet(player)) return;

        LivingEntity target = event.getTarget() instanceof LivingEntity living ? living : null;
        if (target == null || target == player) return;
        String form = target instanceof Player ? "minecraft:player"
                : String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(target.getType()));
        String display = target instanceof Player ? target.getName().getString()
                : target.getType().getDescription().getString();
        player.getPersistentData().putString(FORM_KEY, form);
        player.getPersistentData().putString(NAME_KEY, display);
        player.displayClientMessage(Component.translatable("message.beyondthebag.mimic_form_selected", display), true);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.35F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onActivateForm(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !player.getItemInHand(event.getHand()).is(ModItems.MIMIC_FRAGMENT.get())
                || !hasFullSet(player)
                || !(player.level() instanceof ServerLevel level)) return;

        event.setCanceled(true);
        var data = player.getPersistentData();
        String form = data.getString(FORM_KEY);
        if (form.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.beyondthebag.mimic_form_none"), true);
            return;
        }
        long now = level.getGameTime();
        long ready = data.getLong(COOLDOWN_KEY);
        if (now < ready) {
            player.displayClientMessage(Component.translatable("message.beyondthebag.mimic_form_cooldown",
                    (ready - now + 19) / 20), true);
            return;
        }

        String name = data.getString(NAME_KEY);
        switch (form) {
            case "minecraft:spider", "minecraft:cave_spider" -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 1));
                for (Monster mob : level.getEntitiesOfClass(Monster.class,
                        player.getBoundingBox().inflate(4.0D), Monster::isAlive)) {
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                }
            }
            case "minecraft:enderman" -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 1));
                Vec3 look = player.getLookAngle().normalize();
                boolean moved = false;
                for (int distance = 8; distance >= 3; distance--) {
                    Vec3 destination = player.position().add(look.scale(distance));
                    if (player.randomTeleport(destination.x, destination.y, destination.z, true)) {
                        moved = true;
                        break;
                    }
                }
                if (!moved) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
            }
            case "minecraft:creeper" -> {
                for (Monster mob : level.getEntitiesOfClass(Monster.class,
                        player.getBoundingBox().inflate(4.0D), Monster::isAlive)) {
                    if (mob.hurt(level.damageSources().playerAttack(player), 6.0F)) {
                        mob.knockback(1.0D, player.getX() - mob.getX(), player.getZ() - mob.getZ());
                    }
                }
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                        SoundSource.PLAYERS, 0.8F, 1.1F);
            }
            case "minecraft:zombie", "minecraft:husk", "minecraft:drowned" -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
            }
            case "minecraft:skeleton", "minecraft:stray", "minecraft:bogged" -> {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 160, 1));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
            }
            case "minecraft:phantom" -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
            }
            case "minecraft:player" -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 1));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
            }
            default -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0));
            }
        }
        data.putLong(COOLDOWN_KEY, now + 20 * 25);
        player.displayClientMessage(Component.translatable("message.beyondthebag.mimic_form_used", name), true);
        player.playNotifySound(SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.2F);
    }

    @SubscribeEvent
    public static void onFormTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || event.player.tickCount % 40 != 0 || !hasFullSet(event.player)) return;
        Player player = event.player;
        String form = player.getPersistentData().getString(FORM_KEY);
        switch (form) {
            case "minecraft:spider", "minecraft:cave_spider" ->
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 80, 0, true, false));
            case "minecraft:enderman" ->
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, true, false));
            case "minecraft:phantom" ->
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false));
            case "minecraft:zombie", "minecraft:husk", "minecraft:drowned" ->
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, true, false));
            case "minecraft:skeleton", "minecraft:stray", "minecraft:bogged" ->
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 80, 0, true, false));
            default -> { }
        }
    }
}
