package net.fretux.mindmotion.network;

import net.fretux.mindmotion.client.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncIdentityBlindS2CPacket {

    private final boolean blind;

    public SyncIdentityBlindS2CPacket(boolean blind) {
        this.blind = blind;
    }

    public static void encode(SyncIdentityBlindS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.blind);
    }

    public static SyncIdentityBlindS2CPacket decode(FriendlyByteBuf buf) {
        return new SyncIdentityBlindS2CPacket(buf.readBoolean());
    }

    public static void handle(SyncIdentityBlindS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientData.IDENTITY_BLIND = msg.blind;
        });
        ctx.get().setPacketHandled(true);
    }
}
