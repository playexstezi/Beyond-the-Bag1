package ru.beyondthebag.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.client.model.*;
import ru.beyondthebag.client.renderer.*;
import ru.beyondthebag.registry.ModEntities;

@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StriderModel.LAYER_LOCATION, StriderModel::createBodyLayer);
        event.registerLayerDefinition(WatcherModel.LAYER, WatcherModel::createBodyLayer);
        event.registerLayerDefinition(DistortedModel.LAYER, DistortedModel::createBodyLayer);
        event.registerLayerDefinition(GuardianModel.LAYER, GuardianModel::createBodyLayer);
        event.registerLayerDefinition(MimicModel.LAYER, MimicModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.STRIDER.get(), StriderRenderer::new);
        event.registerEntityRenderer(ModEntities.WATCHER.get(), WatcherRenderer::new);
        event.registerEntityRenderer(ModEntities.DISTORTED.get(), DistortedRenderer::new);
        event.registerEntityRenderer(ModEntities.GUARDIAN.get(), GuardianRenderer::new);
        event.registerEntityRenderer(ModEntities.MIMIC.get(), MimicRenderer::new);
    }
}
