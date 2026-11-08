package ru.beyondthebag.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.client.model.MimicModel;
import ru.beyondthebag.entity.MimicEntity;

public final class MimicRenderer extends MobRenderer<MimicEntity, MimicModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BeyondTheBag.MOD_ID, "textures/entity/mimic.png");
    public MimicRenderer(EntityRendererProvider.Context context) {
        super(context, new MimicModel(context.bakeLayer(MimicModel.LAYER)), 0.35F);
    }
    @Override public ResourceLocation getTextureLocation(MimicEntity entity) { return TEXTURE; }
}
