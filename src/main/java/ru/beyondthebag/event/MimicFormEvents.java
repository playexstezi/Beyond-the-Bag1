package ru.beyondthebag.event;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
    private static final String MOB_TYPE_KEY = "beyondthebag_mimic_mob_type";
    private static final String SPEED_KEY = "beyondthebag_mimic_speed";
    private static final String HEALTH_KEY = "beyondthebag_mimic_health";
    private static final String DAMAGE_KEY = "beyondthebag_mimic_damage";
    private static final String FORMS_KEY = "beyondthebag_mimic_saved_forms";
    private static final String FORM_INDEX_KEY = "beyondthebag_mimic_form_index";
    private static final String TRANSFORM_START_KEY = "beyondthebag_mimic_transform_start";
    private static final String TRANSFORM_STAGE_KEY = "beyondthebag_mimic_transform_stage";
    private MimicFormEvents() {}

    private static boolean hasFullSet(Player player) {
        boolean chest = player.getInventory().armor.get(2).is(ModItems.MIMIC_CHESTPLATE.get())
                || player.getInventory().armor.get(2).is(ModItems.MIMIC_WINGS.get());
        return player.getInventory().armor.get(3).is(ModItems.MIMIC_HELMET.get())
                && chest
                && player.getInventory().armor.get(1).is(ModItems.MIMIC_LEGGINGS.get())
                && player.getInventory().armor.get(0).is(ModItems.MIMIC_BOOTS.get());
    }




    private static void saveForm(CompoundTag playerData) {
        ListTag forms = playerData.getList(FORMS_KEY, Tag.TAG_COMPOUND);
        String id = playerData.getString(FORM_KEY);
        for (int i = 0; i < forms.size(); i++) {
            if (forms.getCompound(i).getString("id").equals(id)) {
                forms.remove(i);
                break;
            }
        }
        CompoundTag saved = new CompoundTag();
        saved.putString("id", id);
        saved.putString("name", playerData.getString(NAME_KEY));
        saved.putString("mobType", playerData.getString(MOB_TYPE_KEY));
        saved.putFloat("speed", playerData.getFloat(SPEED_KEY));
        saved.putFloat("health", playerData.getFloat(HEALTH_KEY));
        saved.putFloat("damage", playerData.getFloat(DAMAGE_KEY));
        forms.add(saved);
        while (forms.size() > 8) forms.remove(0);
        playerData.put(FORMS_KEY, forms);
        playerData.putInt(FORM_INDEX_KEY, forms.size() - 1);
    }

    private static void cycleSavedForm(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        ListTag forms = data.getList(FORMS_KEY, Tag.TAG_COMPOUND);
        if (forms.isEmpty()) {
            player.displayClientMessage(Component.literal("§cСначала скопируй хотя бы одну форму."), true);
            return;
        }
        int index = Math.floorMod(data.getInt(FORM_INDEX_KEY) + 1, forms.size());
        CompoundTag saved = forms.getCompound(index);
        data.putInt(FORM_INDEX_KEY, index);
        data.putString(FORM_KEY, saved.getString("id"));
        data.putString(NAME_KEY, saved.getString("name"));
        data.putString(MOB_TYPE_KEY, saved.getString("mobType"));
        data.putFloat(SPEED_KEY, saved.getFloat("speed"));
        data.putFloat(HEALTH_KEY, saved.getFloat("health"));
        data.putFloat(DAMAGE_KEY, saved.getFloat("damage"));
        player.displayClientMessage(Component.literal("§bАктивная форма: §f" + saved.getString("name")
                + " §7(" + (index + 1) + "/" + forms.size() + ")"), true);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    private static double safeAttribute(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        var instance = entity.getAttribute(attribute);
        return instance == null ? 0.0D : instance.getValue();
    }

    private static String mobTypeKey(LivingEntity target) {
        if (target instanceof net.minecraft.world.entity.monster.AbstractIllager) return "ILLAGER";
        if (target.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD) return "ARTHROPOD";
        if (target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) return "UNDEAD";
        if (target.getMobType() == net.minecraft.world.entity.MobType.WATER) return "WATER";
        return "OTHER";
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
        var data = player.getPersistentData();
        data.putString(FORM_KEY, form);
        data.putString(NAME_KEY, display);
        data.putString(MOB_TYPE_KEY, mobTypeKey(target));
        data.putFloat(SPEED_KEY, (float) safeAttribute(target, Attributes.MOVEMENT_SPEED));
        data.putFloat(HEALTH_KEY, target.getMaxHealth());
        data.putFloat(DAMAGE_KEY, (float) safeAttribute(target, Attributes.ATTACK_DAMAGE));
        saveForm(data);
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
        if (player.isShiftKeyDown()) {
            cycleSavedForm(player);
            return;
        }
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
        // A short, synchronized sound sequence sells the painful magical change.
        data.putLong(TRANSFORM_START_KEY, now);
        data.putInt(TRANSFORM_STAGE_KEY, 0);
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
            default -> applyAdaptiveForm(player, data);
        }
        data.putLong(COOLDOWN_KEY, now + 20 * 25);
        player.displayClientMessage(Component.translatable("message.beyondthebag.mimic_form_used", name), true);
        player.playNotifySound(SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.2F);
    }


    /**
     * Универсальная способность для любых зарегистрированных Forge-сущностей.
     * Сохраняем ID существа и его характеристики, поэтому поддерживаются и модовые формы.
     */
    private static void applyAdaptiveForm(ServerPlayer player, net.minecraft.nbt.CompoundTag data) {
        String mobType = data.getString(MOB_TYPE_KEY);
        float speed = data.getFloat(SPEED_KEY);
        float health = data.getFloat(HEALTH_KEY);
        float damage = data.getFloat(DAMAGE_KEY);

        int speedLevel = speed >= 0.32F ? 2 : speed >= 0.22F ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, speedLevel));

        if (health >= 40.0F) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 1));
        } else if (health >= 20.0F) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));
        }

        if (damage >= 8.0F) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 1));
        } else if (damage > 0.0F) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
        }

        String id = data.getString(FORM_KEY).toLowerCase(java.util.Locale.ROOT);
        // Registry-name traits let forms from other mods receive a fitting
        // ability without linking against that mod's classes.
        if (id.contains("blaze") || id.contains("magma") || id.contains("fire")
                || id.contains("dragon")) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 240, 0));
        }
        if (id.contains("phantom") || id.contains("bat") || id.contains("bird")
                || id.contains("bee") || id.contains("elytra") || id.contains("flying")) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
        }
        if (id.contains("slime") || id.contains("rabbit") || id.contains("frog")) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 180, 1));
        }
        if (id.contains("guardian") || id.contains("golem") || id.contains("turtle")
                || id.contains("wither")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 180, 1));
        }
        if (id.contains("squid") || id.contains("fish") || id.contains("dolphin")
                || id.contains("axolotl") || id.contains("aquatic")) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 240, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 180, 0));
        }

        switch (mobType) {
            case "ARTHROPOD" -> {
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 160, 1));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1));
            }
            case "UNDEAD" -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
            case "WATER" -> {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 240, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 160, 0));
            }
            case "ILLAGER" -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 1));
            default -> {
                if (speed >= 0.30F) {
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 1));
                } else {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
                }
            }
        }
    }

    /**
     * Forge creates a fresh player entity on death. Copy only this mod's
     * mimic state so saved forms survive respawn without overwriting other mods' data.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            CompoundTag oldData = event.getOriginal().getPersistentData();
            CompoundTag newData = event.getEntity().getPersistentData();
            String[] keys = { FORM_KEY, NAME_KEY, COOLDOWN_KEY, MOB_TYPE_KEY,
                    SPEED_KEY, HEALTH_KEY, DAMAGE_KEY, FORMS_KEY, FORM_INDEX_KEY };
            for (String key : keys) {
                if (oldData.contains(key)) newData.put(key, oldData.get(key).copy());
            }
        }
    }

    @SubscribeEvent
    public static void onFormTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        Player player = event.player;
        if (player instanceof ServerPlayer serverPlayer) {
            tickTransformationSounds(serverPlayer);
        }
        if (player.tickCount % 40 != 0 || !hasFullSet(player)) return;
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
            default -> applyAdaptivePassive(player, player.getPersistentData());
        }
    }

    /**
     * Broadcasts a staged transformation sequence to nearby players.
     * The hurt grunts are deliberately spaced out and followed by magical
     * resonance, so it feels like a painful metamorphosis rather than one click.
     */
    private static void tickTransformationSounds(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        long startedAt = data.getLong(TRANSFORM_START_KEY);
        if (startedAt <= 0L) return;

        long elapsed = player.level().getGameTime() - startedAt;
        int stage = data.getInt(TRANSFORM_STAGE_KEY);
        SoundEvent sound = null;
        ParticleOptions particles = null;
        int particleCount = 0;
        float volume = 0.85F;
        float pitch = 1.0F;

        if (stage == 0 && elapsed >= 0L) {
            sound = SoundEvents.PLAYER_HURT;
            particles = ParticleTypes.CRIT;
            particleCount = 18;
            pitch = 0.78F;
        } else if (stage == 1 && elapsed >= 7L) {
            sound = SoundEvents.PLAYER_HURT;
            particles = ParticleTypes.DAMAGE_INDICATOR;
            particleCount = 12;
            volume = 0.72F;
            pitch = 0.62F;
        } else if (stage == 2 && elapsed >= 15L) {
            sound = SoundEvents.ZOMBIE_VILLAGER_CURE;
            particles = ParticleTypes.WITCH;
            particleCount = 28;
            volume = 0.9F;
            pitch = 0.72F;
        } else if (stage == 3 && elapsed >= 25L) {
            sound = SoundEvents.ENDERMAN_TELEPORT;
            particles = ParticleTypes.REVERSE_PORTAL;
            particleCount = 36;
            volume = 0.8F;
            pitch = 0.78F;
        } else if (stage == 4 && elapsed >= 38L) {
            sound = SoundEvents.PLAYER_LEVELUP;
            particles = ParticleTypes.SOUL;
            particleCount = 24;
            volume = 0.7F;
            pitch = 0.9F;
        }

        if (sound != null) {
            player.level().playSound(null, player.blockPosition(), sound,
                    SoundSource.PLAYERS, volume, pitch);
            if (particles != null && player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(particles, player.getX(), player.getY() + 0.9D,
                        player.getZ(), particleCount, 0.42D, 0.75D, 0.42D, 0.035D);
                serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(),
                        player.getY() + 0.9D, player.getZ(), 10, 0.35D, 0.65D,
                        0.35D, 0.6D);
            }
            data.putInt(TRANSFORM_STAGE_KEY, stage + 1);
            if (stage == 4) {
                data.remove(TRANSFORM_START_KEY);
                data.remove(TRANSFORM_STAGE_KEY);
            }
        } else if (elapsed > 80L) {
            data.remove(TRANSFORM_START_KEY);
            data.remove(TRANSFORM_STAGE_KEY);
        }
    }

    private static void applyAdaptivePassive(Player player, CompoundTag data) {
        String mobType = data.getString(MOB_TYPE_KEY);
        String id = data.getString(FORM_KEY).toLowerCase(java.util.Locale.ROOT);
        float speed = data.getFloat(SPEED_KEY);
        float health = data.getFloat(HEALTH_KEY);
        float damage = data.getFloat(DAMAGE_KEY);

        if (speed >= 0.30F) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0, true, false));
        }
        if (health >= 40.0F || damage >= 10.0F) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, true, false));
        }
        if ("WATER".equals(mobType) || id.contains("fish") || id.contains("squid")
                || id.contains("aquatic") || id.contains("dolphin") || id.contains("axolotl")) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 100, 0, true, false));
            if (id.contains("dolphin") || id.contains("guardian") || id.contains("axolotl")) {
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100, 0, true, false));
            }
        }
        if (id.contains("phantom") || id.contains("flying") || id.contains("bird")
                || id.contains("bat") || id.contains("bee") || id.contains("elytra")) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, true, false));
        }
        if (id.contains("blaze") || id.contains("magma") || id.contains("fire")
                || id.contains("dragon")) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false));
        }
        if (id.contains("slime") || id.contains("frog") || id.contains("rabbit")
                || id.contains("spider") || "ARTHROPOD".equals(mobType)) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 100, 0, true, false));
        }
        if (id.contains("cave") || id.contains("glow") || id.contains("warden")
                || id.contains("enderman") || id.contains("endermite")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 0, true, false));
        }
    }
}
