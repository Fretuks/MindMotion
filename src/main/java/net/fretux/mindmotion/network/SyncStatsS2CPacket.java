package net.fretux.mindmotion.network;

import net.fretux.mindmotion.client.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncStatsS2CPacket {
    private final float sanity;
    private final float insanity;
    private final int tempo;
    private final int ventCooldown;
    private final float maxSanity;
    private final int maxTempo;
    private final boolean sanityEnabled;
    private final boolean tempoEnabled;

    public SyncStatsS2CPacket(float sanity,
                              float insanity,
                              int tempo,
                              int ventCooldown,
                              float maxSanity,
                              int maxTempo,
                              boolean sanityEnabled,
                              boolean tempoEnabled) {
        this.sanity = sanity;
        this.insanity = insanity;
        this.tempo = tempo;
        this.ventCooldown = ventCooldown;
        this.maxSanity = maxSanity;
        this.maxTempo = maxTempo;
        this.sanityEnabled = sanityEnabled;
        this.tempoEnabled = tempoEnabled;
    }

    public static void encode(SyncStatsS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.sanity);
        buf.writeFloat(msg.insanity);
        buf.writeInt(msg.tempo);
        buf.writeInt(msg.ventCooldown);
        buf.writeFloat(msg.maxSanity);
        buf.writeInt(msg.maxTempo);
        buf.writeBoolean(msg.sanityEnabled);
        buf.writeBoolean(msg.tempoEnabled);
    }

    public static SyncStatsS2CPacket decode(FriendlyByteBuf buf) {
        float sanity = buf.readFloat();
        float insanity = buf.readFloat();
        int tempo = buf.readInt();
        int ventCooldown = buf.readInt();
        float maxSanity = buf.readFloat();
        int maxTempo = buf.readInt();
        boolean sanityEnabled = buf.readBoolean();
        boolean tempoEnabled = buf.readBoolean();
        return new SyncStatsS2CPacket(sanity, insanity, tempo, ventCooldown, maxSanity, maxTempo, sanityEnabled, tempoEnabled);
    }

    public static void handle(SyncStatsS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientData.SANITY = msg.sanity;
            ClientData.INSANITY = msg.insanity;
            ClientData.TEMPO = msg.tempo;
            ClientData.VENT_COOLDOWN = msg.ventCooldown;
            ClientData.MAX_SANITY = msg.maxSanity;
            ClientData.MAX_TEMPO = msg.maxTempo;
            ClientData.SANITY_ENABLED = msg.sanityEnabled;
            ClientData.TEMPO_ENABLED = msg.tempoEnabled;
        });
        ctx.get().setPacketHandled(true);

        System.out.println(
                "[Client] Sanity: " + msg.sanity +
                        ", Insanity: " + msg.insanity +
                        ", Tempo: " + msg.tempo +
                        ", VentCD: " + msg.ventCooldown +
                        ", MaxSanity: " + msg.maxSanity +
                        ", MaxTempo: " + msg.maxTempo +
                        ", SanityEnabled: " + msg.sanityEnabled +
                        ", TempoEnabled: " + msg.tempoEnabled
        );
    }
}
