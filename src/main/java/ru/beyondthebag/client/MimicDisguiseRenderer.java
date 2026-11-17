package ru.beyondthebag.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Client-only disguise renderer. Cached entity copies are ticked normally so
 * species-specific limb/head animation state can advance between rendered frames.
 */
@Mod.EventBusSubscriber(modid = "beyondthebag", value = Dist.CLIENT)
public final class MimicDisguiseRenderer {
    private static final Map<UUID, String> ACTIVE_FORMS = new ConcurrentHashMap<>();
    private static final Map<UUID, LivingEntity> RENDER_ENTITIES = new ConcurrentHashMap<>();
    private MimicDisguiseRenderer() {}

    public static void setForm(UUID playerId, String formId, boolean active) {
        if (active && formId != null && !formId.isBlank()) {
            ACTIVE_FORMS.put(playerId, formId);
            RENDER_ENTITIES.remove(playerId);
        } else {
            ACTIVE_FORMS.remove(playerId);
            RENDER_ENTITIES.remove(playerId);
        }
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE_FORMS.clear();
        RENDER_ENTITIES.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            RENDER_ENTITIES.clear();
            return;
        }
        RENDER_ENTITIES.keySet().removeIf(id -> mc.level.getPlayerByUUID(id) == null
                || !ACTIVE_FORMS.containsKey(id));
        for (Map.Entry<UUID, String> entry : ACTIVE_FORMS.entrySet()) {
            var player = mc.level.getPlayerByUUID(entry.getKey());
            if (player == null) continue;
            LivingEntity disguise = getOrCreate(entry.getKey(), entry.getValue());
            if (disguise == null) continue;
            syncPose(disguise, player);
            // Tick the render-only copy once per client tick. This advances
            // wolf tails/legs, villager head/arms and other vanilla model states.
            disguise.tick();
            syncPose(disguise, player);
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
        if (disguise instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
        RENDER_ENTITIES.put(playerId, disguise);
        return disguise;
    }

    private static void syncPose(LivingEntity disguise, LivingEntity player) {
        disguise.setPos(player.getX(), player.getY(), player.getZ());
        disguise.setYRot(player.getYRot());
        disguise.setXRot(player.getXRot());
        disguise.setYBodyRot(player.yBodyRot);
        disguise.setYHeadRot(player.getYHeadRot());
        disguise.yRotO = player.yRotO;
        disguise.xRotO = player.xRotO;
        disguise.yBodyRotO = player.yBodyRotO;
        disguise.yHeadRotO = player.yHeadRotO;
        disguise.tickCount = player.tickCount;
        disguise.setDeltaMovement(player.getDeltaMovement());
        disguise.setOnGround(player.onGround());
        disguise.setSprinting(player.isSprinting());
        disguise.setShiftKeyDown(player.isShiftKeyDown());
        disguise.setSwimming(player.isSwimming());
        disguise.setPose(player.getPose());
        disguise.walkAnimation.update(player.walkAnimation.position(), player.walkAnimation.speed());
    }

    @SubscribeEvent
    public static void renderDisguise(RenderPlayerEvent.Pre event) {
        UUID id = event.getEntity().getUUID();
        String form = ACTIVE_FORMS.get(id);
        if (form == null || form.equals("minecraft:player")) return;
        LivingEntity disguise = getOrCreate(id, form);
        if (disguise == null) return;
        syncPose(disguise, event.getEntity());
        PoseStack pose = event.getPoseStack();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        event.setCanceled(true);
        dispatcher.render(disguise, 0.0D, 0.0D, 0.0D, event.getEntity().getYRot(),
                event.getPartialTick(), pose, event.getMultiBufferSource(), event.getPackedLight());
    }
}
