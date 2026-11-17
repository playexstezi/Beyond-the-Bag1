package ru.beyondthebag.client.renderer;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.entity.MimicEntity;
import ru.beyondthebag.client.model.MimicPlayerModel;

/**
 * Renders the mimic with the copied player's skin, armour and held items.
 * The vanilla player model preserves skin UVs and all outer skin layers.
 */
public final class MimicRenderer extends MobRenderer<MimicEntity, MimicPlayerModel> {
    private static final ResourceLocation FALLBACK =
            new ResourceLocation(BeyondTheBag.MOD_ID, "textures/entity/mimic.png");

    public MimicRenderer(EntityRendererProvider.Context context) {
        super(context, new MimicPlayerModel(context.bakeLayer(ModelLayers.PLAYER)), 0.35F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
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
