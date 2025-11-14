package net.fretux.mindmotion.network;

import net.fretux.mindmotion.client.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncStatsS2CPacket {
    private final float sanity;
    private final float insanity;
    private final int tempo;

    public SyncStatsS2CPacket(float sanity, float insanity, int tempo) {
        this.sanity = sanity;
        this.insanity = insanity;
        this.tempo = tempo;
    }

    public static void encode(SyncStatsS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.sanity);
        buf.writeFloat(msg.insanity);
        buf.writeInt(msg.tempo);
    }

    public static SyncStatsS2CPacket decode(FriendlyByteBuf buf) {
        return new SyncStatsS2CPacket(buf.readFloat(), buf.readFloat(), buf.readInt());
    }

    public static void handle(SyncStatsS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientData.SANITY = msg.sanity;
            ClientData.INSANITY = msg.insanity;
            ClientData.TEMPO = msg.tempo;
        });
        ctx.get().setPacketHandled(true);
        System.out.println("[Client] Sanity: " + msg.sanity + ", Insanity: " + msg.insanity + ", Tempo: " + msg.tempo);
    }
}