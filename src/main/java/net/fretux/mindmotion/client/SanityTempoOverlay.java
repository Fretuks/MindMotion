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
        float sanityPercent = clamp01(sanity / ClientData.MAX_SANITY);
        float insanityPercent = clamp01(insanity / ClientData.MAX_SANITY);
        float tempoPercent = clamp01(tempo / ClientData.MAX_TEMPO);
        int sanityFill = Math.round(barWidth * sanityPercent);
        int tempoFill = Math.round(barWidth * tempoPercent);
        int insanityFill = Math.round(barWidth * insanityPercent);
        double animationTime = System.nanoTime() / 1_000_000_000.0;
        double mouseX = mc.mouseHandler.xpos() * width / (double) event.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * height / (double) event.getWindow().getScreenHeight();
        RenderSystem.enableBlend();
        if (tempoEnabled) {
            drawRoundedBar(gui, xTempo, yTempo, barWidth, barHeight, 0xFF33AFFF, tempoFill);
            gui.drawString(mc.font, Component.literal("Tempo"), xTempo, yTempo - labelOffset, 0x66CCFF, false);
        }
        if (tempoEnabled && ClientData.VENT_COOLDOWN > 0) {
            int seconds = ClientData.VENT_COOLDOWN / 20;
            String ventText = "Vent: " + seconds + "s";
            gui.drawString(
                    mc.font,
                    Component.literal(ventText),
                    xTempo,
                    yTempo - ventOffset,
                    0x33AFFF,
                    false
            );
        }
        boolean insaneMode = ClientData.SANITY <= 0f;
        boolean showMadness = ClientData.MADNESS > 0f;
        if (sanityEnabled) {
            if (!insaneMode) {
                drawRoundedBar(gui, xSanity, ySanity, barWidth, barHeight, 0xFFE8E8E8, sanityFill);
                int sanityLabelWidth = mc.font.width("Sanity");
                int sanityLabelX = xSanity + barWidth - sanityLabelWidth;
                gui.drawString(mc.font, Component.literal("Sanity"), sanityLabelX, ySanity - labelOffset, 0xFFFFFF, false);
            } else {
                float insanityPulse = 0.92f + 0.08f * (0.5f + 0.5f * (float) Math.sin(animationTime * 3.5));
                int purple = scaleColor(0xFFAA33FF, insanityPulse);
                int darkPurple = scaleColor(0xFF6611AA, insanityPulse);
                drawRoundedRect(gui, xSanity, ySanity, barWidth, barHeight, 0xDD000000);
                drawRoundedRect(gui, xSanity, ySanity, insanityFill, barHeight, purple, darkPurple);
                drawTopHighlight(gui, xSanity, ySanity, insanityFill, lightenColor(purple, 0.18f));
                int insanityLabelWidth = mc.font.width("Insanity");
                int insanityLabelX = xSanity + barWidth - insanityLabelWidth;
                gui.drawString(mc.font, Component.literal("Insanity"), insanityLabelX, ySanity - labelOffset, 0xCC66FF, false);
            }
        }
        if (madnessEnabled && showMadness) {
            int madnessBarWidth = 182;
            int madnessBarHeight = 7;
            int madnessX = (width - madnessBarWidth) / 2 + ConfigMM.CLIENT.MADNESS_BAR_X_OFFSET.get();
            int madnessY = 13 + ConfigMM.CLIENT.MADNESS_BAR_Y_OFFSET.get();
            float madnessPercent = ClientData.MAX_MADNESS <= 0f
                    ? 0f
                    : clamp01(madness / ClientData.MAX_MADNESS);
            int shakeX = madnessPercent >= 0.85f ? urgentShake(animationTime) : 0;
            int renderedMadnessX = madnessX + shakeX;
            drawBossStyleBar(gui, renderedMadnessX, madnessY, madnessBarWidth, madnessBarHeight,
                    madnessPercent, animationTime);
            gui.drawCenteredString(mc.font, Component.literal("Madness"),
                    madnessX + madnessBarWidth / 2, madnessY - 11, 0xFFFFFF55);

            if (isMouseOver(mouseX, mouseY, renderedMadnessX, madnessY, madnessBarWidth, madnessBarHeight)) {
                String text = String.format("%.0f%%", madnessPercent * 100f);
                gui.drawCenteredString(mc.font, text, madnessX + madnessBarWidth / 2,
                        madnessY + madnessBarHeight + 3, 0xFFFFFF88);
            }
        }
        if (tempoEnabled && isMouseOver(mouseX, mouseY, xTempo, yTempo, barWidth, barHeight)) {
            String text = String.format("%.0f%%", tempoPercent * 100f);
            gui.drawCenteredString(mc.font, text, xTempo + barWidth / 2, yTempo - (labelOffset + 2), 0x66CCFF);
        }
        if (sanityEnabled && isMouseOver(mouseX, mouseY, xSanity, ySanity, barWidth, barHeight)) {
            if (!insaneMode) {
                String text = String.format("%.0f%%", sanityPercent * 100f);
                gui.drawCenteredString(mc.font, text, xSanity + barWidth / 2, ySanity - (labelOffset + 2), 0xFFFFFF);
            } else {
                String text = String.format("%.0f%%", insanityPercent * 100f);
                gui.drawCenteredString(mc.font, text, xSanity + barWidth / 2, ySanity - (labelOffset + 2), 0xCC66FF);
            }
        }
    }

    private static void drawRoundedBar(GuiGraphics gui, int x, int y, int width, int height, int color, int fillWidth) {
        int bg = 0xCC000000;
        drawRoundedRect(gui, x, y, width, height, bg);
        if (fillWidth <= 0) return;

        int clampedFill = Math.min(width, fillWidth);
        drawRoundedRect(gui, x, y, clampedFill, height, color, scaleColor(color, 0.78f));
        drawTopHighlight(gui, x, y, clampedFill, lightenColor(color, 0.18f));
    }

    private static void drawBossStyleBar(GuiGraphics gui, int x, int y, int width, int height,
                                         float progress, double animationTime) {
        gui.fill(x, y, x + width, y + height, 0xFF000000);
        gui.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF4A4000);

        int innerWidth = width - 2;
        int fillWidth = Math.round(innerWidth * progress);
        if (fillWidth > 0) {
            int fillColor = mixColor(0xFFAD8D2C, 0xFFFFF4B0, progress);
            if (progress >= 0.85f) {
                float urgentPulse = 1f + 0.16f
                        * (0.5f + 0.5f * (float) Math.sin(animationTime * 13.0));
                fillColor = scaleColor(fillColor, urgentPulse);
            }
            gui.fillGradient(x + 1, y + 1, x + 1 + fillWidth, y + height - 1,
                    lightenColor(fillColor, 0.16f), scaleColor(fillColor, 0.78f));
        }
    }

    private static void drawTopHighlight(GuiGraphics gui, int x, int y, int fillWidth, int color) {
        if (fillWidth <= 2) return;
        gui.fill(x + 1, y, x + fillWidth - 1, y + 1, color);
    }

    private static int urgentShake(double animationTime) {
        long step = (long) (animationTime * 24.0);
        return (int) Math.floorMod(step * 31L + 17L, 3L) - 1;
    }

    private static int mixColor(int from, int to, float amount) {
        float t = clamp01(amount);
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int scaleColor(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = Math.min(255, Math.max(0, Math.round(((color >>> 16) & 0xFF) * factor)));
        int g = Math.min(255, Math.max(0, Math.round(((color >>> 8) & 0xFF) * factor)));
        int b = Math.min(255, Math.max(0, Math.round((color & 0xFF) * factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lightenColor(int color, float amount) {
        return mixColor(color, (color & 0xFF000000) | 0x00FFFFFF, amount);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static void drawRoundedRect(GuiGraphics gui, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        if (width <= 2 || height <= 2) {
            gui.fill(x, y, x + width, y + height, color);
            return;
        }
        gui.fill(x + 1, y, x + width - 1, y + height, color); // center
        gui.fill(x, y + 1, x + width, y + height - 1, color); // sides
        gui.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
    }

    private static void drawRoundedRect(GuiGraphics gui, int x, int y, int width, int height, int topColor, int bottomColor) {
        if (width <= 0 || height <= 0) return;
        if (width <= 2 || height <= 2) {
            gui.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
            return;
        }
        gui.fillGradient(x + 1, y, x + width - 1, y + height, topColor, bottomColor);
        gui.fillGradient(x, y + 1, x + width, y + height - 1, topColor, bottomColor);
    }

    private static boolean isMouseOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
