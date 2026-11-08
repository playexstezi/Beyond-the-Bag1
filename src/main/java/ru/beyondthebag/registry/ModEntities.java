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
import ru.beyondthebag.entity.DistortedEntity;
import ru.beyondthebag.entity.GuardianEntity;
import ru.beyondthebag.entity.MimicEntity;
import ru.beyondthebag.entity.StriderEntity;
import ru.beyondthebag.entity.WatcherEntity;

@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BeyondTheBag.MOD_ID);

    public static final RegistryObject<EntityType<StriderEntity>> STRIDER = ENTITY_TYPES.register(
            "strider_anomaly", () -> EntityType.Builder.of(StriderEntity::new, MobCategory.MONSTER)
                    .sized(0.72F, 2.25F).clientTrackingRange(10).updateInterval(2)
                    .build("strider_anomaly"));

    public static final RegistryObject<EntityType<WatcherEntity>> WATCHER = ENTITY_TYPES.register(
            "watcher", () -> EntityType.Builder.of(WatcherEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.35F).clientTrackingRange(10).updateInterval(2)
                    .build("watcher"));

    public static final RegistryObject<EntityType<DistortedEntity>> DISTORTED = ENTITY_TYPES.register(
            "distorted", () -> EntityType.Builder.of(DistortedEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.0F).clientTrackingRange(10).updateInterval(2)
                    .build("distorted"));

    public static final RegistryObject<EntityType<GuardianEntity>> GUARDIAN = ENTITY_TYPES.register(
            "guardian", () -> EntityType.Builder.of(GuardianEntity::new, MobCategory.MONSTER)
                    .sized(1.35F, 2.8F).clientTrackingRange(10).updateInterval(2)
                    .build("guardian"));

    public static final RegistryObject<EntityType<MimicEntity>> MIMIC = ENTITY_TYPES.register(
            "mimic", () -> EntityType.Builder.of(MimicEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.0F).clientTrackingRange(10).updateInterval(2)
                    .build("mimic"));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(STRIDER.get(), StriderEntity.createAttributes().build());
        event.put(WATCHER.get(), WatcherEntity.createAttributes().build());
        event.put(DISTORTED.get(), DistortedEntity.createAttributes().build());
        event.put(GUARDIAN.get(), GuardianEntity.createAttributes().build());
        event.put(MIMIC.get(), MimicEntity.createAttributes().build());
    }
}
