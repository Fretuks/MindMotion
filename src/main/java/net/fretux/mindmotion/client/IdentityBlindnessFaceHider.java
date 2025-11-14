package net.fretux.mindmotion.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mindmotion", value = Dist.CLIENT)
public class IdentityBlindnessFaceHider {

    @SubscribeEvent
    public static void hideFaces(RenderLivingEvent.Pre<?, ?> event) {
        if (!ClientData.IDENTITY_BLIND) return;
        if (event.getRenderer().getModel() instanceof HumanoidModel<?> model) {
            model.setAllVisible(false);
        }
    }

    @SubscribeEvent
    public static void restoreFaces(RenderLivingEvent.Post<?, ?> event) {
        if (event.getRenderer().getModel() instanceof HumanoidModel<?> model) {
            model.head.visible = true;
            model.hat.visible = true;
        }
    }
}