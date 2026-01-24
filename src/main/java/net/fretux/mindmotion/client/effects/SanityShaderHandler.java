package net.fretux.mindmotion.client.effects;

import com.mojang.logging.LogUtils;
import net.fretux.mindmotion.ConfigMM;
import net.fretux.mindmotion.client.ClientData;
import net.fretux.mindmotion.client.shader.VentShaderHandler;
import net.fretux.mindmotion.mixin.PostChainAccessor;
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
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.List;

@Mod.EventBusSubscriber(modid = "mindmotion", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SanityShaderHandler {

    private static final ResourceLocation DESAT_EFFECT =
            ResourceLocation.fromNamespaceAndPath("mindmotion", "shaders/post/desaturate.json");

    private static float currentStrength = 0f;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Field PASSES_FIELD =
            ObfuscationReflectionHelper.findField(PostChain.class, "f_110009_");

    @SuppressWarnings("unchecked")
    private static List<PostPass> safeGetPasses(PostChain chain) {
        try {
            return (List<PostPass>) PASSES_FIELD.get(chain);
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to access PostChain.passes", e);
            return List.of();
        }
    }


    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (ConfigMM.CLIENT.DISABLE_SHADERS.get()) {
            if (mc.gameRenderer.currentEffect() != null)
                mc.gameRenderer.shutdownEffect();
            return;
        }
        float flashMultiplier = ConfigMM.CLIENT.FLASH_STRENGTH_MULTIPLIER.get().floatValue();
        float desatMultiplier = ConfigMM.CLIENT.SANITY_DESAT_MULTIPLIER.get().floatValue();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (mc.player == null || mc.level == null) return;
        float sanity = ClientData.SANITY;
        float insanity = ClientData.INSANITY;
        float maxSanity = ClientData.MAX_SANITY;
        float sanityPct = sanity / maxSanity;
        float insanityPct = insanity / maxSanity;
        float baseDesat = 0f;
        if (sanityPct < 0.70f) {
            baseDesat = 1f - (sanityPct / 0.70f);
        }
        float insanityBoost = 0.75f * insanityPct;
        float target = Math.max(baseDesat, insanityBoost);
        if (sanity <= 0f) target = 1f;
        currentStrength += (target - currentStrength) * 0.08f;
        if (currentStrength < 0.005f) {
            if (mc.gameRenderer.currentEffect() != null) {
                mc.gameRenderer.shutdownEffect();
            }
            currentStrength = 0f;
            return;
        }
        if (mc.gameRenderer.currentEffect() == null) {
            try {
                mc.gameRenderer.loadEffect(DESAT_EFFECT);
                PostChain chain = mc.gameRenderer.currentEffect();
                LOGGER.info("Loaded post chain = {}", chain != null);
                if (chain != null) {
                    try {
                        for (PostPass pass : safeGetPasses(chain)) {
                            EffectInstance fx = pass.getEffect();
                            LOGGER.info("pass fx={}", fx != null ? fx.getName() : "null");
                            if (fx != null) {
                                LOGGER.info("  Desat uniform={}", fx.getUniform("Desat") != null);
                            }
                        }
                    } catch (Throwable t) {
                        LOGGER.error("Failed to read PostChain passes via reflection", t);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read PostChain passes via reflection", e);
                return;
            }
        }
        PostChain chain = mc.gameRenderer.currentEffect();
        if (chain == null) return;
        try {
            for (PostPass pass : safeGetPasses(chain)) {
                EffectInstance effect = pass.getEffect();
                if (effect == null) continue;
                var uDesat = effect.getUniform("Desat");
                if (uDesat != null) uDesat.set(currentStrength * desatMultiplier);
                float blur = 0f;
                float vig = 0f;
                float fog = 0f;
                float flash = 0f;
                if (sanityPct < 0.5f) {
                    blur = (0.5f - sanityPct) * 0.6f;
                    vig = (0.5f - sanityPct) * 0.4f;
                    fog = (0.5f - sanityPct) * 0.25f;
                }
                if (insanityPct >= 0.8f) {
                    float level = (insanityPct - 0.8f) / 0.2f;
                    flash = 0.5f * level;
                    if (mc.player.getRandom().nextFloat() < 0.01f) {
                        flash += 0.4f;
                    }
                    flash = Math.min(flash, 1f);
                }
                var uFlash = effect.getUniform("FlashStrength");
                flash *= flashMultiplier;
                if (uFlash != null) uFlash.set(flash);
                blur += insanityPct * 0.25f;
                vig += insanityPct * 0.35f;
                fog += insanityPct * 0.35f;
                blur = Math.min(blur, 1f);
                vig = Math.min(vig, 1f);
                fog = Math.min(fog, 1f);
                var uBlur = effect.getUniform("BlurAmount");
                if (uBlur != null) uBlur.set(blur);
                var uVig = effect.getUniform("Vignette");
                if (uVig != null) uVig.set(vig);
                var uFog = effect.getUniform("FogStrength");
                if (uFog != null) uFog.set(fog);
                float shake = 0f;
                float pulse = 0f;
                if (sanityPct <= 0.125f) {
                    float lowSan = (0.125f - sanityPct) / 0.125f;
                    shake = lowSan * 0.010f;
                    pulse = lowSan * 0.6f;
                }
                shake += insanityPct * 0.012f;
                pulse += insanityPct * 0.8f;
                shake = Math.min(shake, 0.025f);
                pulse = Math.min(pulse, 1f);
                var uShake = effect.getUniform("ShakeStrength");
                if (uShake != null) uShake.set(shake);
                var uPulse = effect.getUniform("PulseStrength");
                if (uPulse != null) uPulse.set(pulse);
                float time = (mc.level.getGameTime() % 200000) / 20f;
                var uTime = effect.getUniform("Time");
                if (uTime != null) uTime.set(time);
                if (VentShaderHandler.isActive()) {
                    float strength = VentShaderHandler.getStrength();
                    var uShock = effect.getUniform("ShockwaveStrength");
                    if (uShock != null) uShock.set(strength);
                } else {
                    var uShock = effect.getUniform("ShockwaveStrength");
                    if (uShock != null) uShock.set(0f);
                }
            }
        } catch (Exception e) {
            LOGGER.info("Error loading post chain{}", e.getMessage());
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