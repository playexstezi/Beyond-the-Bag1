package ru.beyondthebag.entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A player mimic that shadows one explorer, helps in fights, and imitates building/mining. */
public final class MimicEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> COPIED_PLAYER =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.STRING);
    private final Deque<CopiedBlockAction> copiedActions = new ArrayDeque<>();
    private int messageCooldown, stalkTicks, attackCooldown, retreatTicks, gestureCooldown, blockActionCooldown;
    private record CopiedBlockAction(BlockPos pos, BlockState state, boolean place) {}

    public MimicEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(COPIED_PLAYER, "");
    }

    public void copyPlayer(ServerPlayer player) {
        entityData.set(COPIED_PLAYER, player.getUUID().toString());
        setCustomName(Component.literal(player.getGameProfile().getName()));
        setCustomNameVisible(true);
    }

    public UUID getCopiedPlayerId() {
        try {
            String value = entityData.get(COPIED_PLAYER);
            return value.isEmpty() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) { return null; }
    }

    public void imitateBlockAction(BlockPos pos, BlockState state, boolean place) {
        if (copiedActions.size() < 8 && !state.hasBlockEntity())
            copiedActions.addLast(new CopiedBlockAction(pos.immutable(), state, place));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D).add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, false));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.72D));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel server)) return;
        if (messageCooldown > 0) messageCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (retreatTicks > 0) retreatTicks--;
        if (gestureCooldown > 0) gestureCooldown--;
        if (blockActionCooldown > 0) blockActionCooldown--;

        UUID copiedId = getCopiedPlayerId();
        Player onlineTarget = copiedId == null ? null : server.getPlayerByUUID(copiedId);
        ServerPlayer target = onlineTarget instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (target == null || !target.isAlive() || target.isSpectator()) {
            setTarget(null); getNavigation().stop(); copiedActions.clear(); return;
        }
        performCopiedBlockAction(server);

        AABB playerArea = target.getBoundingBox().inflate(24.0D);
        List<ServerPlayer> nearbyPlayers = server.getEntitiesOfClass(ServerPlayer.class,
                playerArea, p -> p.isAlive() && !p.isSpectator());
        if (nearbyPlayers.size() >= 2) {
            stalkTicks = 0; setTarget(null); retreatTicks = Math.max(retreatTicks, 100);
            getNavigation().stop();
            Vec3 away = position().subtract(target.position()).normalize();
            if (away.lengthSqr() < 0.01D) away = new Vec3(1, 0, 0);
            getNavigation().moveTo(getX() + away.x * 24.0D, getY(), getZ() + away.z * 24.0D, 1.2D);
            return;
        }

        LivingEntity attacker = target.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && attacker.distanceTo(target) < 14.0D) {
            setTarget(attacker);
            if (distanceToSqr(attacker) > 4.0D) getNavigation().moveTo(attacker, 1.12D);
            return;
        }
        if (getTarget() != null) setTarget(null);
        if (retreatTicks > 0) { getNavigation().stop(); return; }

        stalkTicks++;
        setCustomName(Component.literal(target.getGameProfile().getName()));
        setShiftKeyDown(target.isCrouching());
        setSprinting(target.isSprinting());
        double distance = distanceToSqr(target);
        if (distance > 20.25D) {
            Vec3 behind = Vec3.directionFromRotation(0.0F, target.getYRot()).scale(-2.8D);
            getNavigation().moveTo(target.getX() + behind.x, target.getY(), target.getZ() + behind.z,
                    target.isSprinting() ? 1.18D : 0.98D);
        } else if (distance < 4.0D || random.nextInt(45) == 0) getNavigation().stop();

        setYRot(target.getYRot()); setYHeadRot(target.getYHeadRot()); yBodyRot = target.yBodyRot;
        if (target.getDeltaMovement().y > 0.18D && onGround() && random.nextInt(3) == 0) jumpFromGround();
        if (gestureCooldown == 0 && target.isUsingItem()) {
            swing(InteractionHand.MAIN_HAND); gestureCooldown = 35;
        }
        if (messageCooldown == 0 && random.nextInt(1800) == 0) {
            String[] lines = {"Давно здесь ходишь?", "Кажется, я видел тебя раньше.",
                    "Тут сегодня как-то тихо.", "Ты тоже кого-то заметил?"};
            target.sendSystemMessage(Component.literal("§8[Мимик] §7" + lines[random.nextInt(lines.length)]));
            messageCooldown = 1200;
        }
        if (stalkTicks > 2400 && distance < 9.0D && attackCooldown == 0 && nearbyPlayers.size() == 1) {
            target.hurt(server.damageSources().mobAttack(this), 5.0F);
            attackCooldown = 100; stalkTicks = 0; retreatTicks = 80; getNavigation().stop();
        }
    }

    private void performCopiedBlockAction(ServerLevel server) {
        if (blockActionCooldown > 0 || copiedActions.isEmpty()) return;
        CopiedBlockAction action = copiedActions.removeFirst();
        blockActionCooldown = 8;
        if (distanceToSqr(Vec3.atCenterOf(action.pos())) > 64.0D || !server.hasChunkAt(action.pos())) return;
        BlockState current = server.getBlockState(action.pos());
        if (action.place()) {
            if (current.isAir() && !action.state().hasBlockEntity() && action.state().canSurvive(server, action.pos()))
                server.setBlock(action.pos(), action.state(), 3);
        } else if (!current.isAir() && !current.hasBlockEntity() && current.is(action.state().getBlock())) {
            server.destroyBlock(action.pos(), true, this);
        }
    }
}