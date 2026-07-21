package net.fretux.mindmotion.client;

import net.fretux.mindmotion.ConfigMM;
import net.fretux.mindmotion.network.ModMessages;
import net.fretux.mindmotion.network.VentPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mindmotion", value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        boolean tempoEnabled = ConfigMM.COMMON.ENABLE_TEMPO.get() && ClientData.TEMPO_ENABLED;
        boolean sanityEnabled = ConfigMM.COMMON.ENABLE_SANITY.get() && ClientData.SANITY_ENABLED;
        if (tempoEnabled && Keybinds.VENT_KEY.isDown()) {
            ModMessages.CHANNEL.sendToServer(new VentPacket());
        }
        if (Keybinds.HUD_EDITOR_KEY.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen == null && (tempoEnabled || sanityEnabled)) {
                minecraft.setScreen(new HudEditorScreen(null));
            }
        }
    }
}
