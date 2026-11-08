package ru.beyondthebag.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.client.model.DistortedModel;
import ru.beyondthebag.entity.DistortedEntity;

public final class DistortedRenderer extends MobRenderer<DistortedEntity, DistortedModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BeyondTheBag.MOD_ID, "textures/entity/distorted.png");

    public DistortedRenderer(EntityRendererProvider.Context context) {
        super(context, new DistortedModel(context.bakeLayer(DistortedModel.LAYER)), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(DistortedEntity entity) {
        return TEXTURE;
    }
}
