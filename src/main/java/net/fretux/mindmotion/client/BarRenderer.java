package net.fretux.mindmotion.client;

import net.minecraft.client.gui.GuiGraphics;

public final class BarRenderer {
    private static final float CORNER_ALPHA = 0.42f;

    private BarRenderer() {
    }

    public static void drawRoundedBar(GuiGraphics gui, int x, int y, int width, int height,
                                      float progress, int topColor, int bottomColor) {
        drawTrackAndFill(gui, x, y, width, height, progress,
                HudTheme.BAR_BACKGROUND, topColor, bottomColor);
    }

    public static void drawBossStyleBar(GuiGraphics gui, int x, int y, int width, int height,
                                        float progress, double animationTime) {
        int fillColor = mixColor(
                HudTheme.MADNESS_GRADIENT_START,
                HudTheme.MADNESS_GRADIENT_END,
                progress
        );
        if (progress >= HudTheme.MADNESS_URGENT_THRESHOLD) {
            float pulse = 1f + 0.16f
                    * (0.5f + 0.5f * (float) Math.sin(animationTime * 13.0));
            fillColor = scaleColor(fillColor, pulse);
        }
        drawTrackAndFill(gui, x, y, width, height, progress, HudTheme.MADNESS_TRACK,
                lightenColor(fillColor, 0.16f), scaleColor(fillColor, 0.78f));
    }

    private static void drawTrackAndFill(GuiGraphics gui, int x, int y, int width, int height,
                                         float progress, int trackColor, int topColor, int bottomColor) {
        if (width <= 0 || height <= 0) return;

        drawRoundedRect(gui, x, y, width, height, HudTheme.BORDER);
        if (width <= 2 || height <= 2) return;

        int innerX = x + 1;
        int innerY = y + 1;
        int innerWidth = width - 2;
        int innerHeight = height - 2;
        drawRoundedRect(gui, innerX, innerY, innerWidth, innerHeight, trackColor);

        int fillWidth = Math.round(innerWidth * clamp01(progress));
        if (fillWidth > 0) {
            drawRoundedRect(gui, innerX, innerY, fillWidth, innerHeight, topColor, bottomColor);
        }
    }

    public static void drawRoundedRect(GuiGraphics gui, int x, int y, int width, int height, int color) {
        drawRoundedRect(gui, x, y, width, height, color, color);
    }

    public static void drawRoundedRect(GuiGraphics gui, int x, int y, int width, int height,
                                       int topColor, int bottomColor) {
        if (width <= 0 || height <= 0) return;
        if (width <= 2 || height <= 2) {
            gui.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
            return;
        }

        gui.fillGradient(x + 1, y, x + width - 1, y + height, topColor, bottomColor);
        gui.fillGradient(x, y + 1, x + width, y + height - 1, topColor, bottomColor);

        int topCorner = scaleAlpha(topColor, CORNER_ALPHA);
        int bottomCorner = scaleAlpha(bottomColor, CORNER_ALPHA);
        gui.fill(x, y, x + 1, y + 1, topCorner);
        gui.fill(x + width - 1, y, x + width, y + 1, topCorner);
        gui.fill(x, y + height - 1, x + 1, y + height, bottomCorner);
        gui.fill(x + width - 1, y + height - 1, x + width, y + height, bottomCorner);
    }

    public static int mixColor(int from, int to, float amount) {
        float t = clamp01(amount);
        int a = mixChannel((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, t);
        int r = mixChannel((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, t);
        int g = mixChannel((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, t);
        int b = mixChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int scaleColor(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = clampChannel(Math.round(((color >>> 16) & 0xFF) * factor));
        int g = clampChannel(Math.round(((color >>> 8) & 0xFF) * factor));
        int b = clampChannel(Math.round((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lightenColor(int color, float amount) {
        return mixColor(color, (color & 0xFF000000) | 0x00FFFFFF, amount);
    }

    public static int withAlpha(int color, float alpha) {
        int a = clampChannel(Math.round(255f * clamp01(alpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static int urgentShake(double animationTime) {
        long step = (long) (animationTime * 24.0);
        return (int) Math.floorMod(step * 31L + 17L, 3L) - 1;
    }

    private static int scaleAlpha(int color, float factor) {
        int alpha = clampChannel(Math.round(((color >>> 24) & 0xFF) * factor));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static int mixChannel(int from, int to, float amount) {
        return Math.round(from + (to - from) * amount);
    }

    private static int clampChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
