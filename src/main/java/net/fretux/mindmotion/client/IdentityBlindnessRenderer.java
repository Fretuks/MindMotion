package net.fretux.mindmotion.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mindmotion", value = Dist.CLIENT)
public class IdentityBlindnessRenderer {

    @SubscribeEvent
    public static void hideNameTags(RenderNameTagEvent event) {
        if (ClientData.IDENTITY_BLIND) {
            event.setResult(RenderNameTagEvent.Result.DENY);
        }
    }
}
