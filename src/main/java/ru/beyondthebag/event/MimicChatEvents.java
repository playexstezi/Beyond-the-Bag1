package ru.beyondthebag.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.entity.MimicEntity;

/** Lets a nearby mimic occasionally respond to the player it is imitating. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MimicChatEvents {
    private MimicChatEvents() {}

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer speaker = event.getPlayer();
        for (MimicEntity mimic : speaker.serverLevel().getEntitiesOfClass(
                MimicEntity.class, new AABB(speaker.blockPosition()).inflate(32.0D),
                entity -> speaker.getUUID().equals(entity.getCopiedPlayerId()))) {
            mimic.respondToChat(speaker, event.getMessage().getString());
        }
    }
}
