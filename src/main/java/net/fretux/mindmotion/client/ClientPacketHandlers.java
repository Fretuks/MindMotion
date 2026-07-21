package net.fretux.mindmotion.client;

import net.fretux.mindmotion.ConfigMM;
import net.fretux.mindmotion.client.shader.VentShaderHandler;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void handleVentEffect() {
        if (!ConfigMM.COMMON.ENABLE_TEMPO.get() || !ClientData.TEMPO_ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.swingTime = 10;
        VentShaderHandler.triggerVentShockwave();
    }
}
