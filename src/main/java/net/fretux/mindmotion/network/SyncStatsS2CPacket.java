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
    private final float madness;
    private final float maxMadness;
    private final float madnessDecayPerTick;
    private final int madnessDecayDelayTicks;

    public SyncStatsS2CPacket(float sanity,
                              float insanity,
                              int tempo,
                              int ventCooldown,
                              float maxSanity,
                              int maxTempo,
                              boolean sanityEnabled,
                              boolean tempoEnabled,
                              float madness,
                              float maxMadness,
                              float madnessDecayPerTick,
                              int madnessDecayDelayTicks) {
        this.sanity = sanity;
        this.insanity = insanity;
        this.tempo = tempo;
        this.ventCooldown = ventCooldown;
        this.maxSanity = maxSanity;
        this.maxTempo = maxTempo;
        this.sanityEnabled = sanityEnabled;
        this.tempoEnabled = tempoEnabled;
        this.madness = madness;
        this.maxMadness = maxMadness;
        this.madnessDecayPerTick = madnessDecayPerTick;
        this.madnessDecayDelayTicks = madnessDecayDelayTicks;
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
        buf.writeFloat(msg.madness);
        buf.writeFloat(msg.maxMadness);
        buf.writeFloat(msg.madnessDecayPerTick);
        buf.writeInt(msg.madnessDecayDelayTicks);
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
        float madness = buf.readFloat();
        float maxMadness = buf.readFloat();
        float madnessDecayPerTick = buf.readFloat();
        int madnessDecayDelayTicks = buf.readInt();
        return new SyncStatsS2CPacket(sanity, insanity, tempo, ventCooldown, maxSanity, maxTempo,
                sanityEnabled, tempoEnabled, madness, maxMadness, madnessDecayPerTick, madnessDecayDelayTicks);
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
            ClientData.setMadness(msg.madness);
            ClientData.MAX_MADNESS = msg.maxMadness;
            ClientData.MADNESS_DECAY_PER_TICK = msg.madnessDecayPerTick;
            ClientData.MADNESS_DECAY_DELAY_TICKS = msg.madnessDecayDelayTicks;
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
                        ", TempoEnabled: " + msg.tempoEnabled +
                        ", Madness: " + msg.madness +
                        ", MaxMadness: " + msg.maxMadness
                        + ", MadnessDecayPerTick: " + msg.madnessDecayPerTick +
                        ", MadnessDecayDelayTicks: " + msg.madnessDecayDelayTicks
        );
    }
}
