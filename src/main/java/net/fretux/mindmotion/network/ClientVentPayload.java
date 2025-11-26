package net.fretux.mindmotion.network;

import net.fretux.mindmotion.AscendMindMotion;
import net.fretux.mindmotion.client.shader.VentShaderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientVentPayload(String string) implements CustomPacketPayload {
    public static final Type<ClientVentPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AscendMindMotion.MODID, "client_vent_payload")
    );

    public static final StreamCodec<FriendlyByteBuf, ClientVentPayload> CODEC = StreamCodec.of(
            ClientVentPayload::write,
            ClientVentPayload::read
    );

    public static void write(FriendlyByteBuf buffer, ClientVentPayload payload) {buffer.writeUtf(payload.string);}

    public static ClientVentPayload read(FriendlyByteBuf buffer) {
        return new ClientVentPayload(buffer.readUtf());
    }

    public static void handleClient(ClientVentPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            mc.player.swingTime = 10;
            VentShaderHandler.triggerVentShockwave();
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
