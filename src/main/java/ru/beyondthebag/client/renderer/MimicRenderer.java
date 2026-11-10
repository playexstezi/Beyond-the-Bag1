package ru.beyondthebag.client.renderer;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.entity.MimicEntity;

/**
 * Renders a mimic with the copied player's actual client skin. The vanilla
 * player model also supplies the hat, jacket, sleeves and trouser outer layers.
 * If the copied player is not in this client's player list yet, use our fallback.
 */
public final class MimicRenderer extends MobRenderer<MimicEntity, PlayerModel<MimicEntity>> {
    private static final ResourceLocation FALLBACK =
            new ResourceLocation(BeyondTheBag.MOD_ID, "textures/entity/mimic.png");

    public MimicRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(MimicEntity entity) {
        UUID copiedPlayer = entity.getCopiedPlayerId();
        if (copiedPlayer != null) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null) {
                PlayerInfo info = minecraft.getConnection().getPlayerInfo(copiedPlayer);
                if (info != null) {
                    return info.getSkinLocation();
                }
            }
        }
        return FALLBACK;
    }
}
