package net.fretux.mindmotion.client.effects;

import net.fretux.mindmotion.ConfigMM;
import net.fretux.mindmotion.client.ClientData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = "mindmotion", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FakeChatMessageHandler {
    private static final Random RANDOM = new Random();
    private static final int MIN_MESSAGE_DELAY = 3600;
    private static final int RANDOM_MESSAGE_DELAY = 3600;
    private static final int MAX_RECENT_NAMES = 12;
    private static final int FAKE_LEAVE_DELAY = 60;
    private static final int FAKE_JOIN_TO_MESSAGE_DELAY = 120;
    private static final int POST_FAKE_LEAVE_COOLDOWN = 4800;
    private static final int POST_FAKE_LEAVE_RANDOM_COOLDOWN = 2400;
    private static final double WHISPER_CHANCE = 0.35;

    private static final String[] FALLBACK_NAMES = {
            "Barney2011",
            "leeharker",
            "Vionics",
            "iqman1000",
            "gianimon",
            "Riccardo085"
    };

    private static final String[] FAKE_MESSAGE_TEMPLATES = {
            "<%s> behind you",
            "<%s> did you hear that",
            "<%s> stop looking at me",
            "<%s> this is not real",
            "<%s> closer",
            "<%s> I can see you",
            "<%s> your bed is too far away",
            "<%s> you are no longer alone",
            "<%s> footsteps approach from below",
            "<%s> something moved in the dark",
            "<%s> t̴̢̡̨͙̱̘̭̩͓̳͔̱̫͔̀̍̓̇h̷̯̳͖̙̞͎̬̳̣͈̜̻̞̍ȇ̴͔̟͈̮͕̖̤̞̗̟̫̳̥͛̀͊̎͂͠ŕ̷͕͓͍̯̹́̔́̏̈̍͗̊͘è̸͓̬̜ ̸̨͖̖͈̦̥͍͙͚̋̈́͛̌ͅï̸̧͕̪̰̺̤̪̓͛̚s̷̼̖̝̱̹̭͇̤͇̦̀́̅̾̾̊̈́͂͗͗̐̾̊͘ ̴̨͇̤̤͔̮̩͕̥̥̖͙̲̠̊͊͊͜n̵̢̨̨̤͚͎̺͚̘̼̗̅̾̉ơ̴̼̻͉͓̦̠͉̄͌̒̋̚͝ ̶̧̢̲̜̺̯̬͒̈͋̍̍͝e̴̯̬͈̮͊́̓͗͗̔̆́͜ͅs̶̢̭̣̩̘͎̤̱̳̺̺̞͂̽͜ͅç̵̻͍̥̝̙̌̿͗̀͌̽̿̈́̾̚̕á̸̧̰̬͓̭̒̏̒̈́̓͊̏̈́̐̄́͆̒̕͜p̶̪̠͓̻͎̦̺̑͆̎͘͠͝ë̷̛̪̜̘͙͚̩̩̫̯̓̈́̾̒̽̀̀̾̇͊̕͠"
    };

    private static final List<String> recentPlayerNames = new ArrayList<>();
    private static final List<PendingFakeMessage> pendingFakeMessages = new ArrayList<>();
    private static final List<PendingLeaveMessage> pendingLeaveMessages = new ArrayList<>();
    private static int messageCooldown = MIN_MESSAGE_DELAY;
    private static int lastTemplateIndex = -1;
    private static String lastPickedName = "";
    private static boolean wasInsane;

    private FakeChatMessageHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || ConfigMM.CLIENT.DISABLE_SHADERS.get() || !ConfigMM.COMMON.ENABLE_SANITY.get() || !ClientData.SANITY_ENABLED) {
            reset();
            return;
        }

        boolean insane = ClientData.SANITY <= 0f;
        if (!insane) {
            reset();
            return;
        }

        updateRecentPlayerNames(minecraft);
        tickPendingFakeMessages(minecraft);
        tickPendingLeaveMessages(minecraft);

        if (!wasInsane) {
            messageCooldown = MIN_MESSAGE_DELAY + RANDOM.nextInt(RANDOM_MESSAGE_DELAY + 1);
            wasInsane = true;
            return;
        }

        if (!pendingFakeMessages.isEmpty() || !pendingLeaveMessages.isEmpty()) {
            return;
        }

        if (--messageCooldown <= 0) {
            sendFakeMessage(minecraft);
            if (pendingFakeMessages.isEmpty() && pendingLeaveMessages.isEmpty()) {
                messageCooldown = MIN_MESSAGE_DELAY + RANDOM.nextInt(RANDOM_MESSAGE_DELAY + 1);
            }
        }
    }

    private static void sendFakeMessage(Minecraft minecraft) {
        PickedName pickedName = pickPlayerName();
        String template = pickMessageTemplate();
        boolean whisper = RANDOM.nextDouble() < WHISPER_CHANCE;
        if (pickedName.fallback()) {
            minecraft.gui.getChat().addMessage(joinLeaveMessage(pickedName.name(), "joined"));
            pendingFakeMessages.add(new PendingFakeMessage(pickedName.name(), template, whisper, FAKE_JOIN_TO_MESSAGE_DELAY));
            return;
        }

        deliverFakeMessage(minecraft, pickedName.name(), template, whisper);
    }

    private static void tickPendingFakeMessages(Minecraft minecraft) {
        for (int i = pendingFakeMessages.size() - 1; i >= 0; i--) {
            PendingFakeMessage pending = pendingFakeMessages.get(i);
            pending.ticks--;
            if (pending.ticks <= 0) {
                deliverFakeMessage(minecraft, pending.name, pending.template, pending.whisper);
                pendingLeaveMessages.add(new PendingLeaveMessage(pending.name, FAKE_LEAVE_DELAY));
                pendingFakeMessages.remove(i);
            }
        }
    }

    private static void deliverFakeMessage(Minecraft minecraft, String name, String template, boolean whisper) {
        String normalMessage = String.format(template, name);
        minecraft.gui.getChat().addMessage(whisper ? whisperMessage(name, normalMessage) : Component.literal(normalMessage));

        if (minecraft.player != null && minecraft.level != null) {
            minecraft.level.playLocalSound(
                    minecraft.player.getX(),
                    minecraft.player.getY(),
                    minecraft.player.getZ(),
                    SoundEvents.AMBIENT_CAVE.value(),
                    SoundSource.AMBIENT,
                    0.65f,
                    0.85f + RANDOM.nextFloat() * 0.3f,
                    false
            );
        }
    }

    private static Component whisperMessage(String name, String normalMessage) {
        String prefix = "<" + name + "> ";
        String body = normalMessage.startsWith(prefix) ? normalMessage.substring(prefix.length()) : normalMessage;
        return Component.literal(name + " whispers to you: " + body).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }

    private static void tickPendingLeaveMessages(Minecraft minecraft) {
        for (int i = pendingLeaveMessages.size() - 1; i >= 0; i--) {
            PendingLeaveMessage pending = pendingLeaveMessages.get(i);
            pending.ticks--;
            if (pending.ticks <= 0) {
                minecraft.gui.getChat().addMessage(joinLeaveMessage(pending.name, "left"));
                pendingLeaveMessages.remove(i);
                messageCooldown = POST_FAKE_LEAVE_COOLDOWN + RANDOM.nextInt(POST_FAKE_LEAVE_RANDOM_COOLDOWN + 1);
            }
        }
    }

    private static void updateRecentPlayerNames(Minecraft minecraft) {
        String localName = minecraft.player != null ? minecraft.player.getGameProfile().getName() : "";

        if (minecraft.level != null) {
            minecraft.level.players().forEach(player -> rememberName(player.getGameProfile().getName(), localName));
        }

        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            for (PlayerInfo playerInfo : connection.getOnlinePlayers()) {
                rememberName(playerInfo.getProfile().getName(), localName);
            }
        }
    }

    private static void rememberName(String name, String localName) {
        if (name == null || name.isBlank() || name.equals(localName)) return;

        recentPlayerNames.remove(name);
        recentPlayerNames.add(0, name);
        while (recentPlayerNames.size() > MAX_RECENT_NAMES) {
            recentPlayerNames.remove(recentPlayerNames.size() - 1);
        }
    }

    private static PickedName pickPlayerName() {
        if (!recentPlayerNames.isEmpty()) {
            String name = pickNameFrom(recentPlayerNames);
            lastPickedName = name;
            return new PickedName(name, false);
        }

        String name = pickNameFrom(FALLBACK_NAMES);
        lastPickedName = name;
        return new PickedName(name, true);
    }

    private static String pickMessageTemplate() {
        int index = RANDOM.nextInt(FAKE_MESSAGE_TEMPLATES.length);
        if (FAKE_MESSAGE_TEMPLATES.length > 1) {
            for (int attempts = 0; attempts < 6 && index == lastTemplateIndex; attempts++) {
                index = RANDOM.nextInt(FAKE_MESSAGE_TEMPLATES.length);
            }
        }
        lastTemplateIndex = index;
        return FAKE_MESSAGE_TEMPLATES[index];
    }

    private static String pickNameFrom(List<String> names) {
        String name = names.get(RANDOM.nextInt(names.size()));
        if (names.size() > 1) {
            for (int attempts = 0; attempts < 6 && name.equals(lastPickedName); attempts++) {
                name = names.get(RANDOM.nextInt(names.size()));
            }
        }
        return name;
    }

    private static String pickNameFrom(String[] names) {
        String name = names[RANDOM.nextInt(names.length)];
        if (names.length > 1) {
            for (int attempts = 0; attempts < 6 && name.equals(lastPickedName); attempts++) {
                name = names[RANDOM.nextInt(names.length)];
            }
        }
        return name;
    }

    private static Component joinLeaveMessage(String name, String action) {
        return Component.literal(name + " " + action + " the game").withStyle(ChatFormatting.YELLOW);
    }

    private static void reset() {
        messageCooldown = MIN_MESSAGE_DELAY;
        lastTemplateIndex = -1;
        lastPickedName = "";
        wasInsane = false;
        pendingFakeMessages.clear();
        pendingLeaveMessages.clear();
    }

    private record PickedName(String name, boolean fallback) {
    }

    private static final class PendingFakeMessage {
        private final String name;
        private final String template;
        private final boolean whisper;
        private int ticks;

        private PendingFakeMessage(String name, String template, boolean whisper, int ticks) {
            this.name = name;
            this.template = template;
            this.whisper = whisper;
            this.ticks = ticks;
        }
    }

    private static final class PendingLeaveMessage {
        private final String name;
        private int ticks;

        private PendingLeaveMessage(String name, int ticks) {
            this.name = name;
            this.ticks = ticks;
        }
    }
}
