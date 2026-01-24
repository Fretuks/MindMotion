package net.fretux.mindmotion;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = AscendMindMotion.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigMM {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();

        CLIENT = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();

        COMMON = new Common(commonBuilder);
        COMMON_SPEC = commonBuilder.build();
    }

    public static class Client {
        public final ForgeConfigSpec.BooleanValue DISABLE_SHADERS;
        public final ForgeConfigSpec.DoubleValue FLASH_STRENGTH_MULTIPLIER;
        public final ForgeConfigSpec.DoubleValue SANITY_DESAT_MULTIPLIER;
        public final ForgeConfigSpec.IntValue BAR_WIDTH;
        public final ForgeConfigSpec.IntValue BAR_HEIGHT;
        public final ForgeConfigSpec.IntValue TEMPO_BAR_X_OFFSET;
        public final ForgeConfigSpec.IntValue TEMPO_BAR_Y_OFFSET;
        public final ForgeConfigSpec.IntValue SANITY_BAR_X_OFFSET;
        public final ForgeConfigSpec.IntValue SANITY_BAR_Y_OFFSET;

        Client(ForgeConfigSpec.Builder b) {
            b.push("visual");

            DISABLE_SHADERS = b
                    .comment("Disable all screen-space effects for accessibility / epilepsy.")
                    .define("disableShaders", false);

            FLASH_STRENGTH_MULTIPLIER = b
                    .comment("Global multiplier for white/black insanity flashes (0 = off).")
                    .defineInRange("flashStrengthMultiplier", 0.8, 0.0, 2.0);

            SANITY_DESAT_MULTIPLIER = b
                    .comment("Intensity of desaturation from low sanity.")
                    .defineInRange("sanityDesaturationMultiplier", 1.0, 0.0, 3.0);

            b.pop().push("hud");

            BAR_WIDTH = b
                    .comment("Width of sanity/tempo bars in pixels.")
                    .defineInRange("barWidth", 102, 40, 240);

            BAR_HEIGHT = b
                    .comment("Height of sanity/tempo bars in pixels.")
                    .defineInRange("barHeight", 9, 4, 30);

            TEMPO_BAR_X_OFFSET = b
                    .comment("Horizontal offset for the tempo bar relative to its default position.")
                    .defineInRange("tempoBarXOffset", 0, -300, 300);

            TEMPO_BAR_Y_OFFSET = b
                    .comment("Vertical offset for the tempo bar relative to its default position.")
                    .defineInRange("tempoBarYOffset", 0, -300, 300);

            SANITY_BAR_X_OFFSET = b
                    .comment("Horizontal offset for the sanity bar relative to its default position.")
                    .defineInRange("sanityBarXOffset", 0, -300, 300);

            SANITY_BAR_Y_OFFSET = b
                    .comment("Vertical offset for the sanity bar relative to its default position.")
                    .defineInRange("sanityBarYOffset", 0, -300, 300);

            b.pop();
        }
    }

    public static class Common {
        public final ForgeConfigSpec.DoubleValue BASE_SANITY_REGEN_LIGHT;
        public final ForgeConfigSpec.DoubleValue BASE_SANITY_DECAY_DARK;
        public final ForgeConfigSpec.BooleanValue ENABLE_LOW_SANITY_SOUNDS;
        public final ForgeConfigSpec.DoubleValue SCRATCHING_CHANCE;
        public final ForgeConfigSpec.DoubleValue PANIC_CHANCE;
        public final ForgeConfigSpec.IntValue TEMPO_DECAY_DELAY;
        public final ForgeConfigSpec.IntValue VENT_COST;
        public final ForgeConfigSpec.IntValue TEMPO_PER_MANA_REGEN_PERCENT;

        Common(ForgeConfigSpec.Builder b) {
            b.push("sanity");

            BASE_SANITY_REGEN_LIGHT = b
                    .comment("Sanity gained per tick in bright light.")
                    .defineInRange("sanityRegenLight", 0.03, 0.0, 1.0);

            BASE_SANITY_DECAY_DARK = b
                    .comment("Sanity lost per tick in darkness.")
                    .defineInRange("sanityDecayDark", 0.02, 0.0, 1.0);

            ENABLE_LOW_SANITY_SOUNDS = b
                    .comment("Enable creepy noises when sanity is low.")
                    .define("enableLowSanitySounds", true);

            b.pop().push("insanity");

            SCRATCHING_CHANCE = b
                    .comment("Base chance per sanity tick to trigger 'scratching' at low sanity.")
                    .defineInRange("scratchingChance", 0.02, 0.0, 1.0);

            PANIC_CHANCE = b
                    .comment("Base chance per sanity tick to trigger panic at low sanity.")
                    .defineInRange("panicChance", 0.015, 0.0, 1.0);

            b.pop().push("tempo");

            TEMPO_DECAY_DELAY = b
                    .comment("Ticks without combat before tempo begins to decay.")
                    .defineInRange("tempoDecayDelay", 100, 0, 20000);

            VENT_COST = b
                    .comment("Tempo required to use the Vent ability.")
                    .defineInRange("ventCost", 40, 1, 1000);

            TEMPO_PER_MANA_REGEN_PERCENT = b
                    .comment("How many tempo = 1% bonus mana regen with Iron's Spellbooks.")
                    .defineInRange("tempoPerManaRegenPercent", 5, 1, 100);

            b.pop();
        }
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading e) {
        System.out.println("MindMotion config loaded: " + e.getConfig().getModId());
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading e) {
    }
}
