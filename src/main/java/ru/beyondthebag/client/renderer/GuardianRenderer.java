package ru.beyondthebag.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.client.model.GuardianModel;
import ru.beyondthebag.entity.GuardianEntity;

public final class GuardianRenderer extends MobRenderer<GuardianEntity, GuardianModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BeyondTheBag.MOD_ID, "textures/entity/guardian.png");

    public GuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new GuardianModel(context.bakeLayer(GuardianModel.LAYER)), 0.85F);
    }

    @Override
    public ResourceLocation getTextureLocation(GuardianEntity entity) {
        return TEXTURE;
    }
}
