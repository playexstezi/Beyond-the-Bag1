package ru.beyondthebag.network;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import ru.beyondthebag.BeyondTheBag;

public final class MimicNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BeyondTheBag.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int nextId;

    private MimicNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(FormStatePacket.class, nextId++)
                .encoder(FormStatePacket::encode)
                .decoder(FormStatePacket::decode)
                .consumerMainThread(FormStatePacket::handle)
                .add();
    }

    public static void broadcastForm(UUID playerId, String formId, boolean active) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new FormStatePacket(playerId, formId, active));
    }

    public record FormStatePacket(UUID playerId, String formId, boolean active) {
        static void encode(FormStatePacket packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.playerId);
            buffer.writeUtf(packet.formId, 256);
            buffer.writeBoolean(packet.active);
        }

        static FormStatePacket decode(FriendlyByteBuf buffer) {
            return new FormStatePacket(buffer.readUUID(), buffer.readUtf(256), buffer.readBoolean());
        }

        static void handle(FormStatePacket packet, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> ru.beyondthebag.client.MimicDisguiseRenderer.setForm(
                    packet.playerId, packet.formId, packet.active));
            context.get().setPacketHandled(true);
        }
    }
}
