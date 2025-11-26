package net.fretux.mindmotion.network;

import net.fretux.mindmotion.AscendMindMotion;
import net.fretux.mindmotion.client.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientSyncStatsPayload(float sanity,
                                     float insanity,
                                     int tempo,
                                     int ventCooldown,
                                     float maxSanity,
                                     int maxTempo) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientSyncStatsPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(AscendMindMotion.MODID, "client_sync_stats_payload")
    );

    public static final StreamCodec<FriendlyByteBuf, ClientSyncStatsPayload> CODEC = StreamCodec.of(
            ClientSyncStatsPayload::write,
            ClientSyncStatsPayload::read
    );

    public static void write(FriendlyByteBuf buffer, ClientSyncStatsPayload payload) {
        buffer.writeFloat(payload.sanity);
        buffer.writeFloat(payload.insanity);
        buffer.writeInt(payload.tempo);
        buffer.writeInt(payload.ventCooldown);
        buffer.writeFloat(payload.maxSanity);
        buffer.writeInt(payload.maxTempo);
    }

    public static ClientSyncStatsPayload read(FriendlyByteBuf buffer) {
        float sanity = buffer.readFloat();
        float insanity = buffer.readFloat();
        int tempo = buffer.readInt();
        int ventCooldown = buffer.readInt();
        float maxSanity = buffer.readFloat();
        int maxTempo = buffer.readInt();
        return new ClientSyncStatsPayload(sanity, insanity, tempo, ventCooldown, maxSanity, maxTempo);
    }

    public static void handleClient(ClientSyncStatsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientData.SANITY = payload.sanity;
            ClientData.INSANITY = payload.insanity;
            ClientData.TEMPO = payload.tempo;
            ClientData.VENT_COOLDOWN = payload.ventCooldown;
            ClientData.MAX_SANITY = payload.maxSanity;
            ClientData.MAX_TEMPO = payload.maxTempo;
        });

        System.out.println(
                "[Client] Sanity: " + payload.sanity +
                        ", Insanity: " + payload.insanity +
                        ", Tempo: " + payload.tempo +
                        ", VentCD: " + payload.ventCooldown +
                        ", MaxSanity: " + payload.maxSanity +
                        ", MaxTempo: " + payload.maxTempo
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
