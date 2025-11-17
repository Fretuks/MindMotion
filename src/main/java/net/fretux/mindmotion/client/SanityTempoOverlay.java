package net.fretux.mindmotion.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mindmotion", value = Dist.CLIENT)
public class SanityTempoOverlay {

    private static final int BAR_WIDTH = 102;
    private static final int BAR_HEIGHT = 9;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        GuiGraphics gui = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int y = height - 48;
        int xTempo = 8;
        int xSanity = width - BAR_WIDTH - 8;
        float sanity = ClientData.SANITY;
        float insanity = ClientData.INSANITY;
        float tempo = ClientData.TEMPO;
        float sanityPercent = sanity / ClientData.MAX_SANITY;
        float insanityPercent = insanity / ClientData.MAX_SANITY;
        float tempoPercent = (float) tempo / (float) ClientData.MAX_TEMPO;
        int sanityFill = (int) (BAR_WIDTH * sanityPercent);
        int tempoFill = (int) (BAR_WIDTH * tempoPercent);
        int insanityFill = (int) (BAR_WIDTH * insanityPercent);
        RenderSystem.enableBlend();
        drawRoundedBar(gui, xTempo, y, BAR_WIDTH, BAR_HEIGHT, 0xFF33AFFF, tempoFill);
        gui.drawString(mc.font, Component.literal("Tempo"), xTempo, y - 10, 0x66CCFF, false);
        if (ClientData.VENT_COOLDOWN > 0) {
            int seconds = ClientData.VENT_COOLDOWN / 20;
            String ventText = "Vent: " + seconds + "s";
            gui.drawString(
                    mc.font,
                    Component.literal(ventText),
                    xTempo,
                    y - 22,
                    0x33AFFF,
                    false
            );
        }
        boolean insaneMode = sanity <= 0;
        if (!insaneMode) {
            drawRoundedBar(gui, xSanity, y, BAR_WIDTH, BAR_HEIGHT, 0xFFFFFFFF, sanityFill);
            gui.drawString(mc.font, Component.literal("Sanity"), xSanity + BAR_WIDTH - 30, y - 10, 0xFFFFFF, false);
        } else {
            int purple = 0xFFAA33FF;
            int darkPurple = 0xFF6611AA;
            drawRoundedRect(gui, xSanity, y, BAR_WIDTH, BAR_HEIGHT, 0xDD000000);
            drawRoundedRect(gui, xSanity, y, insanityFill, BAR_HEIGHT, purple, darkPurple);
            gui.drawString(mc.font, Component.literal("Insanity"), xSanity + BAR_WIDTH - 42, y - 10, 0xCC66FF, false);
        }
        double mouseX = mc.mouseHandler.xpos() * width / (double) event.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * height / (double) event.getWindow().getScreenHeight();
        if (isMouseOver(mouseX, mouseY, xTempo, y, BAR_WIDTH, BAR_HEIGHT)) {
            String text = String.format("%.0f%%", tempoPercent * 100f);
            gui.drawCenteredString(mc.font, text, xTempo + BAR_WIDTH / 2, y - 12, 0x66CCFF);
        }
        if (isMouseOver(mouseX, mouseY, xSanity, y, BAR_WIDTH, BAR_HEIGHT)) {
            if (!insaneMode) {
                String text = String.format("%.0f%%", sanityPercent * 100f);
                gui.drawCenteredString(mc.font, text, xSanity + BAR_WIDTH / 2, y - 12, 0xFFFFFF);
            } else {
                String text = String.format("%.0f%%", insanityPercent * 100f);
                gui.drawCenteredString(mc.font, text, xSanity + BAR_WIDTH / 2, y - 12, 0xCC66FF);
            }
        }
    }

    private static void drawRoundedBar(GuiGraphics gui, int x, int y, int width, int height, int color, int fillWidth) {
        int bg = 0xCC000000;
        drawRoundedRect(gui, x, y, width, height, bg);
        int top = color;
        int bottom = (color & 0x00FFFFFF) | 0xFF000000;
        drawRoundedRect(gui, x, y, fillWidth, height, top, bottom);
    }

    private static void drawRoundedRect(GuiGraphics gui, int x, int y, int width, int height, int color) {
        gui.fill(x + 1, y, x + width - 1, y + height, color); // center
        gui.fill(x, y + 1, x + width, y + height - 1, color); // sides
        gui.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
    }

    private static void drawRoundedRect(GuiGraphics gui, int x, int y, int width, int height, int topColor, int bottomColor) {
        gui.fillGradient(x + 1, y, x + width - 1, y + height, topColor, bottomColor);
        gui.fillGradient(x, y + 1, x + width, y + height - 1, topColor, bottomColor);
    }

    private static boolean isMouseOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}