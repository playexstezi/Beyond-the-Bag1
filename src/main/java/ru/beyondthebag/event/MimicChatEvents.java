package ru.beyondthebag.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.beyondthebag.BeyondTheBag;
import ru.beyondthebag.entity.MimicEntity;

/** Lets a nearby mimic occasionally answer the player it is impersonating. */
@Mod.EventBusSubscriber(modid = BeyondTheBag.MOD_ID)
public final class MimicChatEvents {
    private MimicChatEvents() {}

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer speaker = event.getPlayer();
        if (speaker.level().isClientSide) return;
        String message = event.getMessage().getString();
        if (message.isBlank()) return;
        var area = speaker.getBoundingBox().inflate(32.0D);
        var mimics = speaker.serverLevel().getEntitiesOfClass(MimicEntity.class, area,
                mimic -> speaker.getUUID().equals(mimic.getCopiedPlayerId()));
        for (MimicEntity mimic : mimics) {
            if (speaker.getRandom().nextInt(3) == 0) {
                mimic.respondToChat(speaker, message);
                break;
            }
        }
    }
}
