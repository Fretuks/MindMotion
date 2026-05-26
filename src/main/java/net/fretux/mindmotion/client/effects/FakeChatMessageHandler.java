package net.fretux.mindmotion.client.effects;

import net.fretux.mindmotion.ConfigMM;
import net.fretux.mindmotion.client.ClientData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = "mindmotion", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FakeChatMessageHandler {
    private static final Random RANDOM = new Random();
    private static final int MIN_MESSAGE_DELAY = 600;
    private static final int RANDOM_MESSAGE_DELAY = 900;
    private static final String[] FAKE_MESSAGES = {
            "<...> behind you",
            "<Steve> did you hear that",
            "<Alex> stop looking at me",
            "<...> this is not real",
            "<...> closer",
            "<Herobrine> I can see you",
            "Your bed is too far away",
            "You are no longer alone",
            "Footsteps approach from below",
            "Something moved in the dark"
    };

    private static int messageCooldown = MIN_MESSAGE_DELAY;
    private static boolean wasInsane;

    private FakeChatMessageHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || ConfigMM.CLIENT.DISABLE_SHADERS.get()) {
            reset();
            return;
        }

        boolean insane = ClientData.SANITY <= 0f;
        if (!insane) {
            reset();
            return;
        }

        if (!wasInsane) {
            messageCooldown = MIN_MESSAGE_DELAY + RANDOM.nextInt(RANDOM_MESSAGE_DELAY + 1);
            wasInsane = true;
            return;
        }

        if (--messageCooldown <= 0) {
            sendFakeMessage(minecraft);
            messageCooldown = MIN_MESSAGE_DELAY + RANDOM.nextInt(RANDOM_MESSAGE_DELAY + 1);
        }
    }

    private static void sendFakeMessage(Minecraft minecraft) {
        String message = FAKE_MESSAGES[RANDOM.nextInt(FAKE_MESSAGES.length)];
        Component component = Component.literal(message).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        minecraft.gui.getChat().addMessage(component);
    }

    private static void reset() {
        messageCooldown = MIN_MESSAGE_DELAY;
        wasInsane = false;
    }
}
