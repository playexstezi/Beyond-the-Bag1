package ru.beyondthebag.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.network.MimicNetwork;
import ru.beyondthebag.registry.ModItems;

/** Binds a creature's essence for 30 seconds instead of killing it. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MimicSoulbladeEvents {
    private static final String BOUND_UNTIL = "beyondthebag_soul_bound_until";
    private static final String OWNER = "beyondthebag_soul_bound_owner";
    private static final String WAS_INVULNERABLE = "beyondthebag_soul_was_invulnerable";
    private static final String WAS_NO_AI = "beyondthebag_soul_was_no_ai";
    private static final String RETURN_AT = "beyondthebag_soul_form_return";
    private static final String PREFIX = "beyondthebag_soul_previous_";
    private static final String[] FORM_KEYS = {
            "beyondthebag_mimic_form", "beyondthebag_mimic_form_name",
            "beyondthebag_mimic_mob_type", "beyondthebag_mimic_speed",
            "beyondthebag_mimic_health", "beyondthebag_mimic_damage",
            "beyondthebag_mimic_disguise_active"
    };

    private MimicSoulbladeEvents() {}

    @SubscribeEvent
    public static void onLethalHit(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || victim == player || victim instanceof Player || !victim.isAlive()) return;

        boolean holdingBlade = player.getMainHandItem().is(ModItems.MIMIC_SOULBLADE.get())
                || player.getOffhandItem().is(ModItems.MIMIC_SOULBLADE.get());
        if (!holdingBlade || event.getAmount() < victim.getHealth()) return;

        CompoundTag victimData = victim.getPersistentData();
        long now = victim.level().getGameTime();
        // A creature already bound by this blade must not take the otherwise-lethal hit.
        if (victimData.getLong(BOUND_UNTIL) > now) {
            event.setAmount(0.0F);
            return;
        }

        event.setAmount(0.0F);
        victimData.putBoolean(WAS_INVULNERABLE, victim.isInvulnerable());
        victim.setInvulnerable(true);
        if (victim instanceof Mob mob) {
            victimData.putBoolean(WAS_NO_AI, mob.isNoAi());
            mob.setNoAi(true);
        } else {
            victimData.putBoolean(WAS_NO_AI, false);
        }
        victimData.putLong(BOUND_UNTIL, now + 20L * 30L);
        victimData.putUUID(OWNER, player.getUUID());

        String id = String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(victim.getType()));
        CompoundTag playerData = player.getPersistentData();
        if (!playerData.contains(RETURN_AT)) {
            for (String key : FORM_KEYS) {
                if (playerData.contains(key)) {
                    playerData.put(PREFIX + key, playerData.get(key).copy());
                } else {
                    playerData.remove(PREFIX + key);
                }
            }
        }
        playerData.putLong(RETURN_AT, player.level().getGameTime() + 20L * 30L);
        playerData.putString("beyondthebag_mimic_form", id);
        playerData.putString("beyondthebag_mimic_form_name", victim.getType().getDescription().getString());
        String mobType = victim instanceof net.minecraft.world.entity.monster.AbstractIllager ? "ILLAGER"
                : victim.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD ? "ARTHROPOD"
                : victim.getMobType() == net.minecraft.world.entity.MobType.UNDEAD ? "UNDEAD"
                : victim.getMobType() == net.minecraft.world.entity.MobType.WATER ? "WATER" : "OTHER";
        playerData.putString("beyondthebag_mimic_mob_type", mobType);
        var speed = victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        var damage = victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        playerData.putFloat("beyondthebag_mimic_speed", speed == null ? 0.0F : (float) speed.getValue());
        playerData.putFloat("beyondthebag_mimic_health", victim.getMaxHealth());
        playerData.putFloat("beyondthebag_mimic_damage", damage == null ? 0.0F : (float) damage.getValue());
        playerData.putBoolean("beyondthebag_mimic_disguise_active", true);

        MimicNetwork.broadcastForm(player.getUUID(), id, true);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§5Душа связана: §f" + victim.getType().getDescription().getString() + " §7(30 сек.)"), true);
    }

    @SubscribeEvent
    public static void releaseBoundSoul(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        CompoundTag data = entity.getPersistentData();
        long until = data.getLong(BOUND_UNTIL);
        if (until == 0L || entity.level().getGameTime() < until) return;

        entity.setInvulnerable(data.getBoolean(WAS_INVULNERABLE));
        if (entity instanceof Mob mob) mob.setNoAi(data.getBoolean(WAS_NO_AI));
        data.remove(BOUND_UNTIL);
        data.remove(OWNER);
        data.remove(WAS_INVULNERABLE);
        data.remove(WAS_NO_AI);
    }

    /** Ends the temporary soulblade form immediately and restores the saved mimic selection. */
    public static boolean restoreTemporaryForm(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(RETURN_AT)) return false;

        for (String key : FORM_KEYS) {
            String previousKey = PREFIX + key;
            if (data.contains(previousKey)) data.put(key, data.get(previousKey).copy());
            else data.remove(key);
            data.remove(previousKey);
        }
        data.remove(RETURN_AT);
        data.putBoolean("beyondthebag_mimic_disguise_active", false);
        return true;
    }
}
