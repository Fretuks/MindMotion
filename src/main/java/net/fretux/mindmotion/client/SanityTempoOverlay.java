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
        float sanity = ClientData.SANITY;
        float insanity = ClientData.INSANITY;
        float tempo = ClientData.TEMPO;
        float sanityPercent = sanity / ClientData.MAX_SANITY;
        float insanityPercent = insanity / ClientData.MAX_SANITY;
        float tempoPercent = (float) tempo / (float) ClientData.MAX_TEMPO;
        int sanityFill = (int) (barWidth * sanityPercent);
        int tempoFill = (int) (barWidth * tempoPercent);
        int insanityFill = (int) (barWidth * insanityPercent);
        RenderSystem.enableBlend();
        drawRoundedBar(gui, xTempo, yTempo, barWidth, barHeight, 0xFF33AFFF, tempoFill);
        gui.drawString(mc.font, Component.literal("Tempo"), xTempo, yTempo - labelOffset, 0x66CCFF, false);
        if (ClientData.VENT_COOLDOWN > 0) {
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
        boolean insaneMode = sanity <= 0;
        if (!insaneMode) {
            drawRoundedBar(gui, xSanity, ySanity, barWidth, barHeight, 0xFFFFFFFF, sanityFill);
            int sanityLabelWidth = mc.font.width("Sanity");
            int sanityLabelX = xSanity + barWidth - sanityLabelWidth;
            gui.drawString(mc.font, Component.literal("Sanity"), sanityLabelX, ySanity - labelOffset, 0xFFFFFF, false);
        } else {
            int purple = 0xFFAA33FF;
            int darkPurple = 0xFF6611AA;
            drawRoundedRect(gui, xSanity, ySanity, barWidth, barHeight, 0xDD000000);
            drawRoundedRect(gui, xSanity, ySanity, insanityFill, barHeight, purple, darkPurple);
            int insanityLabelWidth = mc.font.width("Insanity");
            int insanityLabelX = xSanity + barWidth - insanityLabelWidth;
            gui.drawString(mc.font, Component.literal("Insanity"), insanityLabelX, ySanity - labelOffset, 0xCC66FF, false);
        }
        double mouseX = mc.mouseHandler.xpos() * width / (double) event.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * height / (double) event.getWindow().getScreenHeight();
        if (isMouseOver(mouseX, mouseY, xTempo, yTempo, barWidth, barHeight)) {
            String text = String.format("%.0f%%", tempoPercent * 100f);
            gui.drawCenteredString(mc.font, text, xTempo + barWidth / 2, yTempo - (labelOffset + 2), 0x66CCFF);
        }
        if (isMouseOver(mouseX, mouseY, xSanity, ySanity, barWidth, barHeight)) {
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
