package net.fretux.mindmotion.client;

import net.fretux.mindmotion.client.shader.VentShaderHandler;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void handleVentEffect() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.swingTime = 10;
        VentShaderHandler.triggerVentShockwave();
    }
}
