package ru.beyondthebag.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.entity.StriderEntity;

@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BeyondTheBag.MOD_ID);

    public static final RegistryObject<EntityType<StriderEntity>> STRIDER = ENTITY_TYPES.register(
            "strider_anomaly", () -> EntityType.Builder.of(StriderEntity::new, MobCategory.MONSTER)
                    .sized(0.72F, 2.25F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("strider_anomaly"));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(STRIDER.get(), StriderEntity.createAttributes().build());
    }
}
