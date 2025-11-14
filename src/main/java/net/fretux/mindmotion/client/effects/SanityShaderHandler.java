package net.fretux.mindmotion.client.effects;

import net.fretux.mindmotion.client.ClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.List;

@Mod.EventBusSubscriber(modid = "mindmotion", value = Dist.CLIENT)
public class SanityShaderHandler {

    private static final ResourceLocation DESAT_EFFECT =
            new ResourceLocation("mindmotion", "shaders/post/desaturate.json");

    private static float currentStrength = 0f;

    // cache reflection so we don't look it up every frame
    private static Field passesField;

    private static List<PostPass> getPasses(PostChain chain) throws IllegalAccessException, NoSuchFieldException {
        if (passesField == null) {
            passesField = PostChain.class.getDeclaredField("passes");
            passesField.setAccessible(true);
        }
        @SuppressWarnings("unchecked")
        List<PostPass> passes = (List<PostPass>) passesField.get(chain);
        return passes;
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        // AFTER_LEVEL is the point where the full world image is ready
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        float sanity = ClientData.SANITY;
        float insanity = ClientData.INSANITY;
        float maxSanity = 80f;

        float sanityPct = sanity / maxSanity;
        float insanityPct = insanity / maxSanity;

        float baseDesat = 0f;
        if (sanityPct < 0.70f) {
            baseDesat = 1f - (sanityPct / 0.70f);
        }

        float insanityBoost = 0.75f * insanityPct;
        float target = Math.max(baseDesat, insanityBoost);
        if (sanity <= 0f) target = 1f;

        // smooth to avoid popping
        currentStrength += (target - currentStrength) * 0.08f;

        // if essentially zero, shut the effect off completely
        if (currentStrength < 0.005f) {
            if (mc.gameRenderer.currentEffect() != null) {
                mc.gameRenderer.shutdownEffect();
            }
            currentStrength = 0f;
            return;
        }

        // ensure our effect is loaded
        if (mc.gameRenderer.currentEffect() == null) {
            try {
                mc.gameRenderer.loadEffect(DESAT_EFFECT);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        PostChain chain = mc.gameRenderer.currentEffect();
        if (chain == null) return; // failed to load

        // update the Desat uniform on all passes
        try {
            for (PostPass pass : getPasses(chain)) {
                EffectInstance effect = pass.getEffect();
                if (effect == null) continue;
                var uniform = effect.getUniform("Desat");
                if (uniform != null) {
                    uniform.set(currentStrength);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.getEntity().level().isClientSide()) return;

        Minecraft mc = Minecraft.getInstance();
        currentStrength = 0f;

        mc.execute(() -> {
            if (mc.gameRenderer.currentEffect() != null) {
                mc.gameRenderer.shutdownEffect();
            }
        });
    }
}