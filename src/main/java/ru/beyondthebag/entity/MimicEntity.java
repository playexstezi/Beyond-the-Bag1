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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import ru.beyondthebag.registry.ModItems;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A player mimic that shadows one explorer, helps in fights, and imitates building/mining. */
public final class MimicEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> COPIED_PLAYER =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.STRING);
    private final Deque<CopiedBlockAction> copiedActions = new ArrayDeque<>();
    private int messageCooldown, stalkTicks, attackCooldown, retreatTicks, gestureCooldown, blockActionCooldown;
    private int targetRecheckTicks, behaviourTicks;
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

    @Override protected int getExperienceReward() { return 8 + random.nextInt(8); }

    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (random.nextFloat() < 0.85F) {
            int count = 1 + random.nextInt(2);
            spawnAtLocation(new ItemStack(ModItems.MIMIC_SHARD.get(), count));
        }
    }

    public void respondToChat(ServerPlayer speaker, String message) {
        if (level().isClientSide || !(level() instanceof ServerLevel server) || messageCooldown > 0
                || !speaker.getUUID().equals(getCopiedPlayerId()) || distanceToSqr(speaker) > 1024.0D) return;
        String text = message.toLowerCase(java.util.Locale.ROOT);
        String reply;
        if (text.contains("мим") || text.contains("кто ты") || text.contains("кто-нибудь")) {
            reply = "Не понимаю, о чём ты. Я просто иду своей дорогой.";
        } else if (text.contains("привет") || text.contains("здравств")) {
            reply = "О, привет. Давно тебя не видел.";
        } else if (text.contains("куда") || text.contains("ид")) {
            reply = "Пока сам не решил. Здесь постоянно что-то меняется.";
        } else if (text.contains("опас") || text.contains("страш") || text.contains("монстр")) {
            reply = "Да, лучше держаться настороже. Я тоже кое-что заметил.";
        } else if (text.contains("спасибо")) {
            reply = "Да не за что.";
        } else {
            String[] replies = {"Понимаю тебя.", "Хм, может быть.", "Да, я тоже об этом думал.",
                    "Здесь иногда лучше не торопиться.", "Интересно. Продолжай, я слушаю.",
                    "Ага, заметил такое.", "Не уверен, но звучит правдоподобно."};
            reply = replies[random.nextInt(replies.length)];
        }
        String copiedName = speaker.getGameProfile().getName();
        Component line = Component.literal("<" + copiedName + "> " + reply);
        for (ServerPlayer recipient : server.players()) recipient.sendSystemMessage(line);
        messageCooldown = 1200;
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel server)) return;
        if (messageCooldown > 0) messageCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (retreatTicks > 0) retreatTicks--;
        if (gestureCooldown > 0) gestureCooldown--;
        if (blockActionCooldown > 0) blockActionCooldown--;

        if (targetRecheckTicks-- <= 0) { targetRecheckTicks = 160 + random.nextInt(121); chooseObservedPlayer(server); }
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
        double distance = distanceToSqr(target);
        behaviourTicks++;
        if (behaviourTicks % 40 == 0) {
            if (distance > 100.0D) {
                Vec3 offset = Vec3.directionFromRotation(0.0F, random.nextFloat() * 360.0F).scale(2.0D + random.nextDouble() * 3.0D);
                getNavigation().moveTo(target.getX() + offset.x, target.getY(), target.getZ() + offset.z, 0.85D);
            } else if (random.nextInt(4) == 0) getNavigation().stop();
            else {
                Vec3 wander = Vec3.directionFromRotation(0.0F, random.nextFloat() * 360.0F).scale(2.0D + random.nextDouble() * 5.0D);
                getNavigation().moveTo(getX() + wander.x, getY(), getZ() + wander.z, 0.72D);
            }
        }

        if (gestureCooldown == 0 && target.isUsingItem()) {
            swing(InteractionHand.MAIN_HAND); gestureCooldown = 35;
        }
        if (messageCooldown == 0 && random.nextInt(1800) == 0) {
            String[] lines = {"Кажется, здесь что-то изменилось.", "Я немного осмотрюсь и догоню тебя.",
                    "Странное сегодня место.", "Надо бы передохнуть.", "Я слышал что-то неподалёку.",
                    "Похоже, нам сюда.", "Иногда тишина здесь подозрительнее шума.",
                    "Я уже начинаю привыкать к этому месту."};
            String copiedName = target.getGameProfile().getName();
            Component disguisedMessage = Component.literal("<" + copiedName + "> " + lines[random.nextInt(lines.length)]);
            // Broadcast as an ordinary-looking chat line, without forging a signed player message.
            for (ServerPlayer recipient : server.players()) {
                recipient.sendSystemMessage(disguisedMessage);
            }
            messageCooldown = 1200;
        }
        if (stalkTicks > 2400 && distance < 9.0D && attackCooldown == 0 && nearbyPlayers.size() == 1) {
            target.hurt(server.damageSources().mobAttack(this), 5.0F);
            attackCooldown = 100; stalkTicks = 0; retreatTicks = 80; getNavigation().stop();
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