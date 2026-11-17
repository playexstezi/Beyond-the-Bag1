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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Client-side visual disguise. The server sends the selected registry ID to
 * every client; each client creates a render-only copy of that entity type.
 * The real player entity remains authoritative for movement, inventory and combat.
 */
@Mod.EventBusSubscriber(modid = "beyondthebag", value = Dist.CLIENT)
public final class MimicDisguiseRenderer {
    private static final Map<UUID, String> ACTIVE_FORMS = new ConcurrentHashMap<>();
    private MimicDisguiseRenderer() {}

    public static void setForm(UUID playerId, String formId, boolean active) {
        if (active && formId != null && !formId.isBlank()) ACTIVE_FORMS.put(playerId, formId);
        else ACTIVE_FORMS.remove(playerId);
    }

    @SubscribeEvent
    public static void renderDisguise(RenderPlayerEvent.Pre event) {
        String id = ACTIVE_FORMS.get(event.getEntity().getUUID());
        if (id == null || id.equals("minecraft:player")) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(key);
        if (type == null) return;

        Entity raw = type.create(minecraft.level);
        if (!(raw instanceof LivingEntity disguise)) return;

        var player = event.getEntity();
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

        PoseStack pose = event.getPoseStack();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        event.setCanceled(true);
        dispatcher.render(disguise, 0.0D, 0.0D, 0.0D, player.getYRot(),
                event.getPartialTick(), pose, event.getMultiBufferSource(), event.getPackedLight());
    }
}
