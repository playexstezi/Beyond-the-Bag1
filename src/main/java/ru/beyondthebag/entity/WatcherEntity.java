package ru.beyondthebag.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The Watcher observes explorers and only advances while it is out of sight. */
public final class WatcherEntity extends Monster {
    private int stalkingCooldown;

    public WatcherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel server)) return;
        if (stalkingCooldown > 0) stalkingCooldown--;
        if (stalkingCooldown > 0) return;
        stalkingCooldown = 10;

        ServerPlayer target = server.getNearestPlayer(this, 24.0D);
        if (target == null || !target.isAlive() || target.isSpectator()) {
            getNavigation().stop();
            return;
        }

        // The Watcher freezes whenever the player can see it.
        if (hasLineOfSight(target)) {
            getNavigation().stop();
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            return;
        }

        double distance = distanceTo(target);
        if (distance > 11.0D && distance < 24.0D) {
            Vec3 directionAway = position().subtract(target.position()).normalize();
            if (directionAway.lengthSqr() < 0.001D) directionAway = new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 shadowPoint = target.position().add(directionAway.scale(8.0D));
            getNavigation().moveTo(shadowPoint.x, target.getY(), shadowPoint.z, 0.78D);
        } else {
            getNavigation().stop();
        }
    }

    @Override
    public boolean isWithinRestriction() {
        return true;
    }
}
