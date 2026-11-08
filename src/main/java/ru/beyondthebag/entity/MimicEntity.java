package ru.beyondthebag.entity;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A cautious mimic that shadows isolated players and retreats from groups. */
public final class MimicEntity extends Mob {
    private int messageCooldown = 0;

    public MimicEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("Мимик"));
        this.setCustomNameVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel server)) return;
        if (messageCooldown > 0) messageCooldown--;

        List<ServerPlayer> nearby = server.getEntitiesOfClass(ServerPlayer.class,
                getBoundingBox().inflate(18.0D), p -> p.isAlive() && !p.isSpectator());
        if (nearby.isEmpty()) {
            getNavigation().stop();
            return;
        }

        ServerPlayer nearest = nearby.stream()
                .min((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b)))
                .orElse(null);
        if (nearest == null) return;

        if (nearby.size() >= 2) {
            Vec3 away = position().subtract(nearest.position()).normalize();
            if (away.lengthSqr() < 0.01D) away = new Vec3(1, 0, 0);
            getNavigation().moveTo(getX() + away.x * 20.0D, getY(), getZ() + away.z * 20.0D, 1.15D);
            return;
        }

        // Stay a few blocks behind, loosely copying the player's direction and pauses.
        if (distanceToSqr(nearest) > 36.0D) {
            Vec3 behind = Vec3.directionFromRotation(0.0F, nearest.getYRot()).scale(-4.5D);
            getNavigation().moveTo(nearest.getX() + behind.x, nearest.getY(),
                    nearest.getZ() + behind.z, 0.95D);
        } else if (random.nextInt(45) == 0) {
            getNavigation().stop();
        }
        setYRot(nearest.getYRot());
        yBodyRot = nearest.yBodyRot;
        if (nearest.getDeltaMovement().y > 0.18D && onGround() && random.nextInt(3) == 0) jumpFromGround();

        if (messageCooldown == 0 && random.nextInt(1600) == 0) {
            String[] lines = {
                    "Я тоже здесь.",
                    "Ты давно идёшь один?",
                    "Не оборачивайся слишком часто.",
                    "Кажется, мы уже встречались."
            };
            server.players().stream().filter(p -> p.distanceToSqr(this) < 32.0D * 32.0D)
                    .forEach(p -> p.sendSystemMessage(Component.literal("§8[Мимик] §7" + lines[random.nextInt(lines.length)])));
            messageCooldown = 1200;
        }
    }
}
