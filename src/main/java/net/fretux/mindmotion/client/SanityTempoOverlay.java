package net.fretux.mindmotion.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fretux.mindmotion.ConfigMM;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mindmotion", value = Dist.CLIENT)
public class SanityTempoOverlay {

    private static final int DEFAULT_LABEL_OFFSET = 10;
    private static final int DEFAULT_VENT_OFFSET = 22;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        boolean tempoEnabled = ConfigMM.COMMON.ENABLE_TEMPO.get() && ClientData.TEMPO_ENABLED;
        boolean sanityEnabled = ConfigMM.COMMON.ENABLE_SANITY.get() && ClientData.SANITY_ENABLED;
        boolean madnessEnabled = sanityEnabled && ConfigMM.CLIENT.ENABLE_MADNESS_BAR.get();
        if (!tempoEnabled && !sanityEnabled) return;
        ClientData.updateInterpolatedValues();
        GuiGraphics gui = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int barWidth = ConfigMM.CLIENT.BAR_WIDTH.get();
        int barHeight = ConfigMM.CLIENT.BAR_HEIGHT.get();
        int labelOffset = Math.max(DEFAULT_LABEL_OFFSET, barHeight + 1);
        int ventOffset = Math.max(DEFAULT_VENT_OFFSET, labelOffset + 12);
        int baseY = height - 48;
        int baseTempoX = 8;
        int baseSanityX = width - barWidth - 8;
        int xTempo = baseTempoX + ConfigMM.CLIENT.TEMPO_BAR_X_OFFSET.get();
        int yTempo = baseY + ConfigMM.CLIENT.TEMPO_BAR_Y_OFFSET.get();
        int xSanity = baseSanityX + ConfigMM.CLIENT.SANITY_BAR_X_OFFSET.get();
        int ySanity = baseY + ConfigMM.CLIENT.SANITY_BAR_Y_OFFSET.get();
        float sanity = ClientData.getDisplayedSanity();
        float insanity = ClientData.getDisplayedInsanity();
        float madness = ClientData.getDisplayedMadness();
        float tempo = ClientData.getDisplayedTempo();
        float insanityBlend = ClientData.getDisplayedInsanityBlend();
        float sanityPercent = BarRenderer.clamp01(sanity / ClientData.MAX_SANITY);
        float insanityPercent = BarRenderer.clamp01(insanity / ClientData.MAX_SANITY);
        float tempoPercent = BarRenderer.clamp01(tempo / ClientData.MAX_TEMPO);
        float mentalPercent = sanityPercent + (insanityPercent - sanityPercent) * insanityBlend;
        double animationTime = System.nanoTime() / 1_000_000_000.0;
        double mouseX = mc.mouseHandler.xpos() * width / (double) event.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * height / (double) event.getWindow().getScreenHeight();
        RenderSystem.enableBlend();
        if (tempoEnabled) {
            BarRenderer.drawRoundedBar(gui, xTempo, yTempo, barWidth, barHeight, tempoPercent,
                    HudTheme.TEMPO_TOP, HudTheme.TEMPO_BOTTOM);
            gui.drawString(mc.font, Component.literal("Tempo"), xTempo, yTempo - labelOffset,
                    HudTheme.TEMPO_LABEL, false);
        }
        if (tempoEnabled && ClientData.VENT_COOLDOWN > 0) {
            int seconds = ClientData.VENT_COOLDOWN / 20;
            String ventText = "Vent: " + seconds + "s";
            gui.drawString(
                    mc.font,
                    Component.literal(ventText),
                    xTempo,
                    yTempo - ventOffset,
                    HudTheme.VENT_LABEL,
                    false
            );
        }
        boolean showMadness = ClientData.MADNESS > 0f;
        if (sanityEnabled) {
            float insanityPulse = 0.92f + 0.08f
                    * (0.5f + 0.5f * (float) Math.sin(animationTime * 3.5));
            int pulsingInsanityTop = BarRenderer.scaleColor(HudTheme.INSANITY_TOP, insanityPulse);
            int pulsingInsanityBottom = BarRenderer.scaleColor(HudTheme.INSANITY_BOTTOM, insanityPulse);
            int mentalTop = BarRenderer.mixColor(HudTheme.SANITY_TOP, pulsingInsanityTop, insanityBlend);
            int mentalBottom = BarRenderer.mixColor(HudTheme.SANITY_BOTTOM, pulsingInsanityBottom, insanityBlend);
            BarRenderer.drawRoundedBar(gui, xSanity, ySanity, barWidth, barHeight, mentalPercent,
                    mentalTop, mentalBottom);

            float sanityLabelAlpha = 1f - insanityBlend;
            if (sanityLabelAlpha > 0.01f) {
                int sanityLabelWidth = mc.font.width("Sanity");
                int sanityLabelX = xSanity + barWidth - sanityLabelWidth;
                gui.drawString(mc.font, Component.literal("Sanity"), sanityLabelX, ySanity - labelOffset,
                        BarRenderer.withAlpha(HudTheme.SANITY_LABEL, sanityLabelAlpha), false);
            }
            if (insanityBlend > 0.01f) {
                int insanityLabelWidth = mc.font.width("Insanity");
                int insanityLabelX = xSanity + barWidth - insanityLabelWidth;
                gui.drawString(mc.font, Component.literal("Insanity"), insanityLabelX, ySanity - labelOffset,
                        BarRenderer.withAlpha(HudTheme.INSANITY_LABEL, insanityBlend), false);
            }
        }
        if (madnessEnabled && showMadness) {
            int madnessBarWidth = 182;
            int madnessBarHeight = 7;
            int madnessX = (width - madnessBarWidth) / 2 + ConfigMM.CLIENT.MADNESS_BAR_X_OFFSET.get();
            int madnessY = 13 + ConfigMM.CLIENT.MADNESS_BAR_Y_OFFSET.get();
            float madnessPercent = ClientData.MAX_MADNESS <= 0f
                    ? 0f
                    : BarRenderer.clamp01(madness / ClientData.MAX_MADNESS);
            int shakeX = madnessPercent >= HudTheme.MADNESS_URGENT_THRESHOLD
                    ? BarRenderer.urgentShake(animationTime)
                    : 0;
            int renderedMadnessX = madnessX + shakeX;
            BarRenderer.drawBossStyleBar(gui, renderedMadnessX, madnessY, madnessBarWidth, madnessBarHeight,
                    madnessPercent, animationTime);
            gui.drawCenteredString(mc.font, Component.literal("Madness"),
                    madnessX + madnessBarWidth / 2, madnessY - 11, HudTheme.MADNESS_LABEL);

            if (isMouseOver(mouseX, mouseY, renderedMadnessX, madnessY, madnessBarWidth, madnessBarHeight)) {
                String text = String.format("%.0f%%", madnessPercent * 100f);
                gui.drawCenteredString(mc.font, text, madnessX + madnessBarWidth / 2,
                        madnessY + madnessBarHeight + 3, HudTheme.MADNESS_TOOLTIP);
            }
        }
        if (tempoEnabled && isMouseOver(mouseX, mouseY, xTempo, yTempo, barWidth, barHeight)) {
            String text = String.format("%.0f%%", tempoPercent * 100f);
            gui.drawCenteredString(mc.font, text, xTempo + barWidth / 2, yTempo - (labelOffset + 2),
                    HudTheme.TEMPO_LABEL);
        }
        if (sanityEnabled && isMouseOver(mouseX, mouseY, xSanity, ySanity, barWidth, barHeight)) {
            String text = String.format("%.0f%%", mentalPercent * 100f);
            int tooltipColor = BarRenderer.mixColor(
                    HudTheme.SANITY_LABEL, HudTheme.INSANITY_LABEL, insanityBlend);
            gui.drawCenteredString(mc.font, text, xSanity + barWidth / 2,
                    ySanity - (labelOffset + 2), tooltipColor);
        }
    }

    private static boolean isMouseOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
