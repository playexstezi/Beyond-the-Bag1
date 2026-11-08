package ru.beyondthebag.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.client.model.WatcherModel;
import ru.beyondthebag.entity.WatcherEntity;

public final class WatcherRenderer extends MobRenderer<WatcherEntity, WatcherModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BeyondTheBag.MOD_ID, "textures/entity/watcher.png");

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new WatcherModel(context.bakeLayer(WatcherModel.LAYER)), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(WatcherEntity entity) {
        return TEXTURE;
    }
}
