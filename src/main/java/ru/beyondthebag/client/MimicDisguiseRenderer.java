package ru.beyondthebag.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Client-only disguise renderer. A render-only copy of the selected living
 * entity is kept per player. The copy receives the player's pose and equipment
 * while retaining its own renderer/model, so vanilla species animations run.
 */
@Mod.EventBusSubscriber(modid = "beyondthebag", value = Dist.CLIENT)
public final class MimicDisguiseRenderer {
    private static final Map<UUID, String> ACTIVE_FORMS = new ConcurrentHashMap<>();
    private static final Map<UUID, LivingEntity> RENDER_ENTITIES = new ConcurrentHashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel cachedLevel;

    private MimicDisguiseRenderer() {}

    public static void setForm(UUID playerId, String formId, boolean active) {
        if (active && formId != null && !formId.isBlank()) {
            ACTIVE_FORMS.put(playerId, formId);
        } else {
            ACTIVE_FORMS.remove(playerId);
        }
        RENDER_ENTITIES.remove(playerId);
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE_FORMS.clear();
        RENDER_ENTITIES.clear();
        cachedLevel = null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != cachedLevel) {
            RENDER_ENTITIES.clear();
            cachedLevel = mc.level;
        }
        if (mc.level == null) return;

        RENDER_ENTITIES.keySet().removeIf(id -> mc.level.getPlayerByUUID(id) == null
                || !ACTIVE_FORMS.containsKey(id));
        for (Map.Entry<UUID, String> entry : ACTIVE_FORMS.entrySet()) {
            Player player = mc.level.getPlayerByUUID(entry.getKey());
            if (player == null) continue;
            LivingEntity disguise = getOrCreate(entry.getKey(), entry.getValue());
            if (disguise == null) continue;

            // Copy before and after ticking: tick() advances species-specific
            // animation state, while the second sync prevents its AI/physics
            // from drifting away from the real player's position.
            syncFromPlayer(disguise, player);
            disguise.tick();
            syncFromPlayer(disguise, player);
        }
    }

    private static LivingEntity getOrCreate(UUID playerId, String id) {
        LivingEntity cached = RENDER_ENTITIES.get(playerId);
        if (cached != null) return cached;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return null;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(key);
        if (type == null) return null;
        Entity raw = type.create(mc.level);
        if (!(raw instanceof LivingEntity disguise)) return null;
        if (disguise instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
        RENDER_ENTITIES.put(playerId, disguise);
        return disguise;
    }

    private static void syncFromPlayer(LivingEntity disguise, Player player) {
        disguise.setPos(player.getX(), player.getY(), player.getZ());
        disguise.setYRot(player.getYRot());
        disguise.setXRot(player.getXRot());
        disguise.setYBodyRot(player.yBodyRot);
        disguise.setYHeadRot(player.getYHeadRot());
        disguise.yRotO = player.yRotO;
        disguise.xRotO = player.xRotO;
        disguise.yBodyRotO = player.yBodyRotO;
        disguise.yHeadRotO = player.yHeadRotO;
        disguise.setDeltaMovement(player.getDeltaMovement());
        disguise.setOnGround(player.onGround());
        disguise.setSprinting(player.isSprinting());
        disguise.setShiftKeyDown(player.isShiftKeyDown());
        disguise.setSwimming(player.isSwimming());
        disguise.setPose(player.getPose());
        // Copy the player's animation clock directly. The render-only entity has
        // no authoritative movement simulation, so relying on its own tick would
        // leave walk cycles stuck or out of phase with the real player.
        disguise.walkAnimation.update(player.walkAnimation.position(), player.walkAnimation.speed());
        disguise.walkAnimation.update(player.walkAnimation.position(), player.walkAnimation.speed());

        // A disguise is meant to conceal the player, not display their gear.
        // In particular, never copy armour into the render-only creature:
        // creature renderers have different armour layers and can distort the
        // player's custom armour. The real player keeps vanilla armour rendering
        // whenever no disguise is active. Clear any stale armour from cached
        // render entities as well.
        disguise.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY);
        disguise.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
        disguise.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.item.ItemStack.EMPTY);
        disguise.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.item.ItemStack.EMPTY);

        // Held items are not armour and may still be part of the disguise.
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] {
                net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                net.minecraft.world.entity.EquipmentSlot.OFFHAND
        }) {
            disguise.setItemSlot(slot, player.getItemBySlot(slot).copy());
        }
    }

    @SubscribeEvent
    public static void renderDisguise(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        UUID id = player.getUUID();
        String form = ACTIVE_FORMS.get(id);
        if (form == null || form.equals("minecraft:player")) return;

        LivingEntity disguise = getOrCreate(id, form);
        if (disguise == null) return;
        syncFromPlayer(disguise, player);

        PoseStack pose = event.getPoseStack();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        event.setCanceled(true);
        dispatcher.render(disguise, 0.0D, 0.0D, 0.0D, player.getYRot(),
                event.getPartialTick(), pose, event.getMultiBufferSource(), event.getPackedLight());
    }
}
