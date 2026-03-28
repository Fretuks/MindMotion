package net.fretux.mindmotion.network;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VentClientEffectPacket {

    public VentClientEffectPacket() {}
    public VentClientEffectPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    net.fretux.mindmotion.client.ClientPacketHandlers.handleVentEffect()
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
