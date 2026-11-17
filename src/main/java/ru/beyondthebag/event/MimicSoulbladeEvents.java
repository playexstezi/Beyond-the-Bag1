package ru.beyondthebag.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.network.MimicNetwork;
import ru.beyondthebag.registry.ModItems;

/**
 * Non-lethal soul capture: the victim remains in the world, frozen and
 * invulnerable for a short time, while the wielder temporarily wears its form.
 */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MimicSoulbladeEvents {
    private static final String BOUND_UNTIL = "beyondthebag_soul_bound_until";
    private static final String OWNER = "beyondthebag_soul_bound_owner";
    private static final String RETURN_AT = "beyondthebag_soul_form_return";
    private MimicSoulbladeEvents() {}

    @SubscribeEvent
    public static void onLethalHit(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || victim == player || victim instanceof Player || !victim.isAlive()) return;
        boolean holding = player.getMainHandItem().is(ModItems.MIMIC_SOULBLADE.get())
                || player.getOffhandItem().is(ModItems.MIMIC_SOULBLADE.get());
        if (!holding || event.getAmount() < victim.getHealth()) return;

        CompoundTag victimData = victim.getPersistentData();
        if (victimData.getLong(BOUND_UNTIL) > victim.level().getGameTime()) return;

        event.setAmount(0.0F);
        victim.setHealth(Math.max(1.0F, Math.min(victim.getHealth(), 1.0F)));
        victim.setInvulnerable(true);
        if (victim instanceof Mob mob) mob.setNoAi(true);
        victimData.putLong(BOUND_UNTIL, victim.level().getGameTime() + 20L * 30L);
        victimData.putUUID(OWNER, player.getUUID());

        String id = String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(victim.getType()));
        CompoundTag data = player.getPersistentData();
        if (!data.contains(RETURN_AT)) {
            data.putString(RETURN_AT + "_form", data.getString("beyondthebag_mimic_form"));
            data.putBoolean(RETURN_AT + "_active", data.getBoolean("beyondthebag_mimic_disguise_active"));
        }
        data.putLong(RETURN_AT, player.level().getGameTime() + 20L * 30L);
        data.putString("beyondthebag_mimic_form", id);
        data.putString("beyondthebag_mimic_form_name", victim.getType().getDescription().getString());
        data.putFloat("beyondthebag_mimic_speed", (float) victim.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
        data.putFloat("beyondthebag_mimic_health", victim.getMaxHealth());
        data.putFloat("beyondthebag_mimic_damage", (float) victim.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        data.putBoolean("beyondthebag_mimic_disguise_active", true);
        MimicNetwork.broadcastForm(player.getUUID(), id, true);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§5Душа связана: §f" + victim.getType().getDescription().getString()
                        + " §7(30 сек.)"), true);
    }

    @SubscribeEvent
    public static void releaseBoundSoul(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        CompoundTag data = entity.getPersistentData();
        long until = data.getLong(BOUND_UNTIL);
        if (until == 0L || entity.level().getGameTime() < until) return;
        entity.setInvulnerable(false);
        if (entity instanceof Mob mob) mob.setNoAi(false);
        data.remove(BOUND_UNTIL);
        data.remove(OWNER);
    }

    @SubscribeEvent
    public static void restoreWielder(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData();
        long until = data.getLong(RETURN_AT);
        if (until == 0L || player.level().getGameTime() < until) return;
        String oldForm = data.getString(RETURN_AT + "_form");
        boolean wasActive = data.getBoolean(RETURN_AT + "_active");
        data.putString("beyondthebag_mimic_form", oldForm);
        data.putBoolean("beyondthebag_mimic_disguise_active", wasActive);
        MimicNetwork.broadcastForm(player.getUUID(), oldForm, wasActive);
        data.remove(RETURN_AT);
        data.remove(RETURN_AT + "_form");
        data.remove(RETURN_AT + "_active");
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§bДуша вернулась к владельцу."), true);
    }
}
