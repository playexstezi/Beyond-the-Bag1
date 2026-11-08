package ru.beyondthebag.entity;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A player-skin mimic that shadows isolated players and avoids groups. */
public final class MimicEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> COPIED_PLAYER =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.STRING);
    private int messageCooldown;
    private int stalkTicks;
    private int attackCooldown;
    private int retreatTicks;

    public MimicEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setCustomName(Component.literal("Мимик"));
        setCustomNameVisible(false);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(COPIED_PLAYER, "");
    }

    public void copyPlayer(ServerPlayer player) {
        entityData.set(COPIED_PLAYER, player.getUUID().toString());
    }

    public UUID getCopiedPlayerId() {
        try {
            String value = entityData.get(COPIED_PLAYER);
            return value.isEmpty() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.75D));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel server)) return;
        if (messageCooldown > 0) messageCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (retreatTicks > 0) retreatTicks--;

        List<ServerPlayer> nearby = server.getEntitiesOfClass(ServerPlayer.class,
                getBoundingBox().inflate(24.0D), p -> p.isAlive() && !p.isSpectator());
        if (nearby.isEmpty()) {
            getNavigation().stop();
            return;
        }

        ServerPlayer target = nearby.stream()
                .min((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b)))
                .orElse(null);
        if (target == null) return;

        // Absolute safety rule: never approach or attack while two or more players are nearby.
        if (nearby.size() >= 2) {
            stalkTicks = 0;
            retreatTicks = Math.max(retreatTicks, 100);
            getNavigation().stop();
            Vec3 away = position().subtract(target.position()).normalize();
            if (away.lengthSqr() < 0.01D) away = new Vec3(1, 0, 0);
            getNavigation().moveTo(getX() + away.x * 28.0D, getY(), getZ() + away.z * 28.0D, 1.25D);
            return;
        }

        if (retreatTicks > 0) {
            getNavigation().stop();
            return;
        }

        stalkTicks++;
        // Follow at a believable distance and imitate direction, pauses and jumps.
        double distance = distanceToSqr(target);
        if (distance > 25.0D) {
            Vec3 behind = Vec3.directionFromRotation(0.0F, target.getYRot()).scale(-4.0D);
            getNavigation().moveTo(target.getX() + behind.x, target.getY(),
                    target.getZ() + behind.z, 0.92D);
        } else if (random.nextInt(35) == 0) {
            getNavigation().stop();
        }
        setYRot(target.getYRot());
        yBodyRot = target.yBodyRot;
        if (target.getDeltaMovement().y > 0.18D && onGround() && random.nextInt(3) == 0) jumpFromGround();

        if (messageCooldown == 0 && random.nextInt(1800) == 0) {
            String[] lines = {
                    "Давно здесь ходишь?",
                    "Кажется, я видел тебя раньше.",
                    "Тут сегодня как-то тихо.",
                    "Ты тоже кого-то заметил?"
            };
            target.sendSystemMessage(Component.literal("§8[Мимик] §7" + lines[random.nextInt(lines.length)]));
            messageCooldown = 1200;
        }

        // It watches for a long time before making a move; it does not attack on sight.
        if (stalkTicks > 2400 && distance < 9.0D && attackCooldown == 0) {
            if (nearby.size() == 1 && target.isAlive()) {
                target.hurt(server.damageSources().mobAttack(this), 5.0F);
                attackCooldown = 100;
                stalkTicks = 0;
                retreatTicks = 80;
                getNavigation().stop();
            }
        }
    }
}
