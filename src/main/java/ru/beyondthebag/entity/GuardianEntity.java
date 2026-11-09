package ru.beyondthebag.entity;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Guardian of the final chamber. Expedition bosses have a boss bar, two combat
 * phases, a charged shockwave and a one-time call for reinforcements.
 */
public final class GuardianEntity extends Monster {
    private static final String BOSS_TAG = "BeyondTheBagExpeditionBoss";
    private static final int SHOCKWAVE_INTERVAL = 200;
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("Хранитель измерения"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean expeditionBoss;
    private boolean enraged;
    private boolean calledReinforcements;
    private int abilityCooldown = 100;

    public GuardianEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 80;
    }

    public void setExpeditionBoss(boolean boss) {
        expeditionBoss = boss;
        if (boss) {
            setCustomName(Component.literal("Хранитель измерения"));
            setCustomNameVisible(true);
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(180.0D);
            setHealth(180.0F);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(13.0D);
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22D);
            getAttribute(Attributes.ARMOR).setBaseValue(12.0D);
            bossEvent.setName(getDisplayName());
        }
    }

    public boolean isExpeditionBoss() {
        return expeditionBoss;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 72.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.19D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.92D, true));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 22.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!expeditionBoss || level().isClientSide || !(level() instanceof ServerLevel server)) return;

        List<ServerPlayer> nearby = server.getEntitiesOfClass(ServerPlayer.class,
                getBoundingBox().inflate(64.0D), p -> p.isAlive() && !p.isSpectator());
        for (ServerPlayer player : nearby) bossEvent.addPlayer(player);
        for (ServerPlayer player : List.copyOf(bossEvent.getPlayers())) {
            if (!nearby.contains(player)) bossEvent.removePlayer(player);
        }
        bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));

        if (!enraged && getHealth() <= getMaxHealth() * 0.5F) {
            enraged = true;
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.30D);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(16.0D);
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60, 0));
            server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0D, getZ(),
                    45, 1.8D, 1.0D, 1.8D, 0.08D);
            nearby.forEach(p -> p.displayClientMessage(
                    Component.literal("§5Хранитель входит в фазу ярости!"), true));
        }

        if (!calledReinforcements && getHealth() <= getMaxHealth() * 0.30F) {
            calledReinforcements = true;
            for (int i = 0; i < 2; i++) {
                var minion = ru.beyondthebag.registry.ModEntities.DISTORTED.get().create(server);
                if (minion != null) {
                    double angle = Math.PI * i;
                    minion.moveTo(getX() + Math.cos(angle) * 3.0D, getY(),
                            getZ() + Math.sin(angle) * 3.0D, random.nextFloat() * 360.0F, 0.0F);
                    minion.setPersistenceRequired();
                    server.addFreshEntity(minion);
                }
            }
            nearby.forEach(p -> p.displayClientMessage(
                    Component.literal("§cХранитель призывает искажённых стражей!"), true));
        }

        if (--abilityCooldown <= 0) {
            abilityCooldown = enraged ? 150 : SHOCKWAVE_INTERVAL;
            performShockwave(server, nearby);
        }
    }

    private void performShockwave(ServerLevel server, List<ServerPlayer> nearby) {
        server.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 0.8D, getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.2D, getZ(),
                32, 2.5D, 0.25D, 2.5D, 0.06D);
        server.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 1.1F, 0.65F);
        AABB impact = getBoundingBox().inflate(enraged ? 7.0D : 5.0D, 2.0D, enraged ? 7.0D : 5.0D);
        for (ServerPlayer player : nearby) {
            if (!impact.intersects(player.getBoundingBox())) continue;
            player.hurt(server.damageSources().mobAttack(this), enraged ? 10.0F : 7.0F);
            double dx = player.getX() - getX();
            double dz = player.getZ() - getZ();
            player.knockback(enraged ? 1.7D : 1.2D, dx, dz);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(BOSS_TAG, expeditionBoss);
        tag.putBoolean("BeyondTheBagEnraged", enraged);
        tag.putBoolean("BeyondTheBagReinforcements", calledReinforcements);
        tag.putInt("BeyondTheBagAbilityCooldown", abilityCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setExpeditionBoss(tag.getBoolean(BOSS_TAG));
        enraged = tag.getBoolean("BeyondTheBagEnraged");
        calledReinforcements = tag.getBoolean("BeyondTheBagReinforcements");
        abilityCooldown = Math.max(1, tag.getInt("BeyondTheBagAbilityCooldown"));
        if (enraged) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.30D);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(16.0D);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        bossEvent.removeAllPlayers();
        super.remove(reason);
    }
}
