package ru.beyondthebag.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.client.model.StriderModel;
import ru.beyondthebag.entity.StriderEntity;

public class StriderRenderer extends MobRenderer<StriderEntity, StriderModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BeyondTheBag.MOD_ID, "textures/entity/strider_anomaly.png");

    public StriderRenderer(EntityRendererProvider.Context context) {
        super(context, new StriderModel(context.bakeLayer(StriderModel.LAYER_LOCATION)), 0.55F);
    }

    @Override
    public ResourceLocation getTextureLocation(StriderEntity entity) {
        return TEXTURE;
    }
}
