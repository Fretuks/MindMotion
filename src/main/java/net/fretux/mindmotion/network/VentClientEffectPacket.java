package net.fretux.mindmotion.network;

import net.fretux.mindmotion.client.shader.VentShaderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VentClientEffectPacket {

    public VentClientEffectPacket() {}
    public VentClientEffectPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            mc.player.swingTime = 10;
            VentShaderHandler.triggerVentShockwave();
        });
        return true;
    }
}
