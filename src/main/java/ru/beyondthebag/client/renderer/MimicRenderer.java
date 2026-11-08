package ru.beyondthebag.client.renderer;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import ru.beyondthebag.entity.MimicEntity;

/** Renders the mimic with the copied online player's actual skin texture. */
public final class MimicRenderer extends MobRenderer<MimicEntity, PlayerModel<MimicEntity>> {
    public MimicRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(MimicEntity entity) {
        UUID copied = entity.getCopiedPlayerId();
        if (copied != null && Minecraft.getInstance().getConnection() != null) {
            var info = Minecraft.getInstance().getConnection().getPlayerInfo(copied);
            if (info != null) return info.getSkinLocation();
        }
        return DefaultPlayerSkin.getDefaultSkin(entity.getUUID());
    }
}
