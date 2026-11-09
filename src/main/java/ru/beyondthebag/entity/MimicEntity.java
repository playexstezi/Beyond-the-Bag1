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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import ru.beyondthebag.registry.ModItems;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A player mimic that shadows one explorer, helps in fights, and imitates building/mining. */
public final class MimicEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> COPIED_PLAYER =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> ACTION_ANIMATION =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_ANIMATION_TICKS =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.INT);
    private final Deque<CopiedBlockAction> copiedActions = new ArrayDeque<>();
    private int messageCooldown, stalkTicks, attackCooldown, retreatTicks, gestureCooldown, blockActionCooldown;
    private int targetRecheckTicks, behaviourTicks, nextRustleTicks = 500, observePauseTicks, freezeTicks;
    private record CopiedBlockAction(BlockPos relativePos, BlockState state, boolean place, int delay) {}

    public MimicEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(COPIED_PLAYER, "");
        entityData.define(ACTION_ANIMATION, 0);
        entityData.define(ACTION_ANIMATION_TICKS, 0);
    }

    public void copyPlayer(ServerPlayer player) {
        entityData.set(COPIED_PLAYER, player.getUUID().toString());
        setCustomName(Component.literal(player.getGameProfile().getName()));
        setCustomNameVisible(true);
    }

    public int getActionAnimation() { return entityData.get(ACTION_ANIMATION); }
    public int getActionAnimationTicks() { return entityData.get(ACTION_ANIMATION_TICKS); }

    public void animateAction(int action) {
        if (!level().isClientSide) {
            entityData.set(ACTION_ANIMATION, action);
            entityData.set(ACTION_ANIMATION_TICKS, 10);
        }
    }

    public UUID getCopiedPlayerId() {
        try {
            String value = entityData.get(COPIED_PLAYER);
            return value.isEmpty() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) { return null; }
    }

    public void imitateBlockAction(BlockPos relativePos, BlockState state, boolean place) {
        if (copiedActions.size() < 8 && !state.hasBlockEntity()
                && !state.isAir() && !state.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
            copiedActions.addLast(new CopiedBlockAction(relativePos.immutable(), state, place,
                    30 + random.nextInt(71)));
        }
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

    @Override public int getExperienceReward() { return 8 + random.nextInt(8); }

    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (!level().isClientSide && random.nextFloat() < 0.75F) {
            ItemStack fragment = new ItemStack(ModItems.MIMIC_FRAGMENT.get(), 1 + random.nextInt(2));
            ItemEntity drop = new ItemEntity(level(), getX(), getY() + 0.25D, getZ(), fragment);
            level().addFreshEntity(drop);
        }
    }


    public void respondToChat(ServerPlayer speaker, String message) {
        if (level().isClientSide || !(level() instanceof ServerLevel server) || messageCooldown > 0
                || !speaker.getUUID().equals(getCopiedPlayerId()) || distanceToSqr(speaker) > 1024.0D) return;
        String text = message.toLowerCase(java.util.Locale.ROOT);
        String reply;
        if (text.contains("привет") || text.contains("здравств")) {
            reply = "Привет! Давно тебя не видел.";
        } else if (text.contains("спасибо")) {
            reply = "Всегда пожалуйста.";
        } else if (text.contains("?")) {
            String[] answers = {"Не знаю точно, но я бы не стал торопиться.",
                    "Хороший вопрос. Сам над этим думаю.",
                    "Может быть. А ты как считаешь?",
                    "Пока не уверен. Посмотрим, что будет дальше."};
            reply = answers[random.nextInt(answers.length)];
        } else {
            String[] replies = {"Да, понимаю.", "Согласен, здесь странное место.",
                    "Тоже об этом подумал.", "Посмотрим, что из этого выйдет.",
                    "Кстати, я заметил кое-что неподалёку.", "Возможно, ты прав.",
                    "Не уверен. Тут всё какое-то непривычное."};
            reply = replies[random.nextInt(replies.length)];
        }
        String copiedName = speaker.getGameProfile().getName();
        Component line = Component.literal("<" + copiedName + "> " + reply);
        for (ServerPlayer recipient : server.players()) recipient.sendSystemMessage(line);
        messageCooldown = 240 + random.nextInt(241);
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel server)) return;
        if (messageCooldown > 0) messageCooldown--;
        if (getActionAnimationTicks() > 0) entityData.set(ACTION_ANIMATION_TICKS, getActionAnimationTicks() - 1);
        if (attackCooldown > 0) attackCooldown--;
        if (retreatTicks > 0) retreatTicks--;
        if (gestureCooldown > 0) gestureCooldown--;
        if (blockActionCooldown > 0) blockActionCooldown--;
        if (nextRustleTicks > 0) nextRustleTicks--;
        if (observePauseTicks > 0) observePauseTicks--;
        if (freezeTicks > 0) freezeTicks--;

        if (targetRecheckTicks-- <= 0) { targetRecheckTicks = 160 + random.nextInt(121); chooseObservedPlayer(server); }
        UUID copiedId = getCopiedPlayerId();
        Player onlineTarget = copiedId == null ? null : server.getPlayerByUUID(copiedId);
        ServerPlayer target = onlineTarget instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (target == null || !target.isAlive() || target.isSpectator()) {
            setTarget(null); getNavigation().stop(); copiedActions.clear(); return;
        }
        performCopiedBlockAction(server);
        syncCopiedPlayerScale(target);
        playAmbientRustle(server, target);

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
        double distance = distanceToSqr(target);
        behaviourTicks++;
        if (behaviourTicks % 40 == 0) {
            // Keep a believable distance instead of constantly walking at the player's shoulder.
            if (distance > 576.0D) {
                Vec3 offset = Vec3.directionFromRotation(0.0F, random.nextFloat() * 360.0F)
                        .scale(9.0D + random.nextDouble() * 6.0D);
                getNavigation().moveTo(target.getX() + offset.x, target.getY(), target.getZ() + offset.z, 0.92D);
            } else if (distance > 100.0D) {
                if (observePauseTicks == 0 && random.nextInt(3) == 0) {
                    observePauseTicks = 40 + random.nextInt(81);
                    getNavigation().stop();
                    getLookControl().setLookAt(target, 25.0F, 25.0F);
                } else if (observePauseTicks == 0) {
                    Vec3 flank = Vec3.directionFromRotation(0.0F, random.nextFloat() * 360.0F)
                            .scale(8.0D + random.nextDouble() * 6.0D);
                    getNavigation().moveTo(target.getX() + flank.x, target.getY(), target.getZ() + flank.z, 0.68D);
                } else {
                    getNavigation().stop();
                    getLookControl().setLookAt(target, 20.0F, 20.0F);
                }
            } else if (distance > 25.0D && distance < 256.0D) {
                Vec3 toMimic = position().subtract(target.position()).normalize();
                boolean playerWatching = target.hasLineOfSight(this)
                        && target.getViewVector(1.0F).dot(toMimic) > 0.72D;
                if (playerWatching) {
                    // When noticed, act like an ordinary player who has paused.
                    getNavigation().stop();
                    getLookControl().setLookAt(target, 15.0F, 15.0F);
                    if (freezeTicks == 0 && random.nextInt(5) == 0) freezeTicks = 30 + random.nextInt(51);
                } else if (freezeTicks > 0) {
                    getNavigation().stop();
                } else {
                    // Advance only while outside the player's view, choosing an offset rather than a direct chase.
                    Vec3 flank = Vec3.directionFromRotation(0.0F, random.nextFloat() * 360.0F)
                            .scale(3.0D + random.nextDouble() * 4.0D);
                    getNavigation().moveTo(target.getX() + flank.x, target.getY(), target.getZ() + flank.z, 0.68D);
                }
            } else if (distance >= 256.0D) {
                Vec3 flank = Vec3.directionFromRotation(0.0F, random.nextFloat() * 360.0F)
                        .scale(5.0D + random.nextDouble() * 5.0D);
                getNavigation().moveTo(target.getX() + flank.x, target.getY(), target.getZ() + flank.z, 0.72D);
            } else {
                getNavigation().stop();
                getLookControl().setLookAt(target, 20.0F, 20.0F);
            }
        }

        if (gestureCooldown == 0 && target.isUsingItem()) {
            swing(InteractionHand.MAIN_HAND); gestureCooldown = 35;
        }

        if (stalkTicks > 2400 && distance < 9.0D && attackCooldown == 0 && nearbyPlayers.size() == 1) {
            target.hurt(server.damageSources().mobAttack(this), 5.0F);
            attackCooldown = 100; stalkTicks = 0; retreatTicks = 80; getNavigation().stop();
        }
    }


    /**
     * Pehkui is optional. Read the player's BASE scale and apply it to the mimic
     * through reflection so the mod still loads when Pehkui is not installed.
     */
    private void syncCopiedPlayerScale(ServerPlayer player) {
        if (tickCount % 40 != 0) return;
        try {
            Class<?> scaleTypes = Class.forName("virtuoel.pehkui.api.ScaleTypes");
            Object baseType = scaleTypes.getField("BASE").get(null);
            Object playerData = baseType.getClass().getMethod("getScaleData", net.minecraft.world.entity.Entity.class)
                    .invoke(baseType, player);
            float scale = ((Number) playerData.getClass().getMethod("getScale").invoke(playerData)).floatValue();
            Object mimicData = baseType.getClass().getMethod("getScaleData", net.minecraft.world.entity.Entity.class)
                    .invoke(baseType, this);
            mimicData.getClass().getMethod("setScale", float.class).invoke(mimicData, scale);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Vanilla-sized mimic when Pehkui is absent or its API differs.
        }
    }

    /**
     * Occasionally creates a small environmental sound away from both entities.
     * The sound is anchored to nearby foliage/wood, so it feels like something
     * moved through the surroundings rather than coming directly from the mimic.
     */
    private void playAmbientRustle(ServerLevel server, ServerPlayer target) {
        if (nextRustleTicks > 0 || distanceToSqr(target) < 9.0D || distanceToSqr(target) > 576.0D) return;
        nextRustleTicks = 420 + random.nextInt(901);

        BlockPos origin = target.blockPosition();
        BlockPos soundPos = null;
        for (int attempt = 0; attempt < 14; attempt++) {
            int dx = random.nextInt(17) - 8;
            int dz = random.nextInt(17) - 8;
            if (dx * dx + dz * dz < 16) continue;
            BlockPos candidate = server.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    origin.offset(dx, 0, dz));
            if (!server.hasChunkAt(candidate) || distanceToSqr(Vec3.atCenterOf(candidate)) < 25.0D) continue;
            BlockState state = server.getBlockState(candidate.below());
            if (state.is(net.minecraft.tags.BlockTags.LEAVES)
                    || state.is(net.minecraft.tags.BlockTags.LOGS)
                    || state.getSoundType().getVolume() > 0.8F) {
                soundPos = candidate;
                break;
            }
        }
        if (soundPos == null) return;

        if (random.nextInt(3) == 0) {
            server.playSound(null, soundPos, SoundEvents.AZALEA_LEAVES_BREAK,
                    SoundSource.AMBIENT, 0.32F, 0.75F + random.nextFloat() * 0.45F);
        } else {
            server.playSound(null, soundPos, SoundEvents.WOOD_BREAK,
                    SoundSource.AMBIENT, 0.24F, 0.70F + random.nextFloat() * 0.40F);
        }
    }

    private void chooseObservedPlayer(ServerLevel server) {
        List<ServerPlayer> candidates = server.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48.0D), p -> p.isAlive() && !p.isSpectator());
        if (candidates.isEmpty()) return;
        UUID currentId = getCopiedPlayerId();
        Player onlineCurrent = currentId == null ? null : server.getPlayerByUUID(currentId);
        ServerPlayer current = onlineCurrent instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        ServerPlayer best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (ServerPlayer candidate : candidates) {
            int nearby = server.getEntitiesOfClass(ServerPlayer.class, candidate.getBoundingBox().inflate(20.0D), p -> p.isAlive() && !p.isSpectator()).size();
            double score = (nearby <= 1 ? 2.5D : nearby == 2 ? 0.8D : -1.5D) - Math.sqrt(distanceToSqr(candidate)) * 0.035D;
            if (candidate.getDeltaMovement().horizontalDistanceSqr() > 0.01D) score += 0.35D;
            if (candidate == current) score += 1.25D;
            score += random.nextDouble() * 0.2D;
            if (score > bestScore) { bestScore = score; best = candidate; }
        }
        if (best != null && (current == null || best == current || bestScore > 1.0D)) {
            if (!best.getUUID().equals(getCopiedPlayerId())) { copyPlayer(best); stalkTicks = 0; copiedActions.clear(); }
        }
    }
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) animateAction(1);
        return hit;
    }

    private void performCopiedBlockAction(ServerLevel server) {
        if (copiedActions.isEmpty()) return;
        CopiedBlockAction action = copiedActions.removeFirst();
        if (action.delay() > 0) {
            copiedActions.addLast(new CopiedBlockAction(action.relativePos(), action.state(),
                    action.place(), action.delay() - 1));
            return;
        }
        if (blockActionCooldown > 0) {
            copiedActions.addFirst(action);
            return;
        }
        blockActionCooldown = 12 + random.nextInt(13);
        BlockPos destination = blockPosition().offset(action.relativePos());
        if (Math.abs(action.relativePos().getX()) > 4 || Math.abs(action.relativePos().getY()) > 3
                || Math.abs(action.relativePos().getZ()) > 4 || !server.hasChunkAt(destination)) return;
        BlockState current = server.getBlockState(destination);
        if (action.place()) {
            if (current.canBeReplaced() && server.getFluidState(destination).isEmpty()
                    && !action.state().hasBlockEntity() && action.state().canSurvive(server, destination)
                    && server.setBlock(destination, action.state(), 3)) animateAction(3);
        } else if (!current.isAir() && !current.hasBlockEntity()
                && current.is(action.state().getBlock())) {
            // No drops: the mimic must not duplicate the player's mined resources.
            if (server.destroyBlock(destination, false, this)) animateAction(2);
        }
    }
}