package net.fretux.mindmotion.client;

import net.fretux.mindmotion.network.ModMessages;
import net.fretux.mindmotion.network.VentPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mindmotion", value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (Keybinds.VENT_KEY.isDown()) {
            ModMessages.CHANNEL.sendToServer(new VentPacket());
        }
    }
}
