package net.fretux.mindmotion.client;

import java.util.function.IntConsumer;
import net.fretux.mindmotion.ConfigMM;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
    private static final int MIN_BAR_WIDTH = 40;
    private static final int MAX_BAR_WIDTH = 240;
    private static final int MIN_BAR_HEIGHT = 4;
    private static final int MAX_BAR_HEIGHT = 30;
    private static final int MIN_OFFSET = -300;
    private static final int MAX_OFFSET = 300;
    private static final int DEFAULT_BAR_WIDTH = 102;
    private static final int DEFAULT_BAR_HEIGHT = 9;
    private static final int DEFAULT_OFFSET = 0;
    private static final int BASE_TEMPO_X = 8;
    private static final int BASE_Y_OFFSET = 48;

    private final Screen previous;

    private int barWidth;
    private int barHeight;
    private int tempoOffsetX;
    private int tempoOffsetY;
    private int sanityOffsetX;
    private int sanityOffsetY;

    private boolean draggingTempo;
    private boolean draggingSanity;
    private double dragStartX;
    private double dragStartY;
    private int dragStartTempoX;
    private int dragStartTempoY;
    private int dragStartSanityX;
    private int dragStartSanityY;

    private IntSlider barWidthSlider;
    private IntSlider barHeightSlider;
    private IntSlider tempoXSlider;
    private IntSlider tempoYSlider;
    private IntSlider sanityXSlider;
    private IntSlider sanityYSlider;

    public HudEditorScreen(Screen previous) {
        super(Component.translatable("screen.mindmotion.hud_editor.title"));
        this.previous = previous;
        this.barWidth = ConfigMM.CLIENT.BAR_WIDTH.get();
        this.barHeight = ConfigMM.CLIENT.BAR_HEIGHT.get();
        this.tempoOffsetX = ConfigMM.CLIENT.TEMPO_BAR_X_OFFSET.get();
        this.tempoOffsetY = ConfigMM.CLIENT.TEMPO_BAR_Y_OFFSET.get();
        this.sanityOffsetX = ConfigMM.CLIENT.SANITY_BAR_X_OFFSET.get();
        this.sanityOffsetY = ConfigMM.CLIENT.SANITY_BAR_Y_OFFSET.get();
    }

    @Override
    protected void init() {
        int leftX = this.width / 2 - 155;
        int rightX = this.width / 2 + 5;
        int sliderWidth = 150;
        int y = 36;
        int step = 24;

        barWidthSlider = addRenderableWidget(new IntSlider(
                leftX,
                y,
                sliderWidth,
                20,
                "screen.mindmotion.hud_editor.bar_width",
                MIN_BAR_WIDTH,
                MAX_BAR_WIDTH,
                barWidth,
                value -> setBarWidth(value, false)
        ));
        barHeightSlider = addRenderableWidget(new IntSlider(
                rightX,
                y,
                sliderWidth,
                20,
                "screen.mindmotion.hud_editor.bar_height",
                MIN_BAR_HEIGHT,
                MAX_BAR_HEIGHT,
                barHeight,
                value -> setBarHeight(value, false)
        ));

        y += step;
        tempoXSlider = addRenderableWidget(new IntSlider(
                leftX,
                y,
                sliderWidth,
                20,
                "screen.mindmotion.hud_editor.tempo_x",
                MIN_OFFSET,
                MAX_OFFSET,
                tempoOffsetX,
                value -> setTempoOffsetX(value, false)
        ));
        tempoYSlider = addRenderableWidget(new IntSlider(
                rightX,
                y,
                sliderWidth,
                20,
                "screen.mindmotion.hud_editor.tempo_y",
                MIN_OFFSET,
                MAX_OFFSET,
                tempoOffsetY,
                value -> setTempoOffsetY(value, false)
        ));

        y += step;
        sanityXSlider = addRenderableWidget(new IntSlider(
                leftX,
                y,
                sliderWidth,
                20,
                "screen.mindmotion.hud_editor.sanity_x",
                MIN_OFFSET,
                MAX_OFFSET,
                sanityOffsetX,
                value -> setSanityOffsetX(value, false)
        ));
        sanityYSlider = addRenderableWidget(new IntSlider(
                rightX,
                y,
                sliderWidth,
                20,
                "screen.mindmotion.hud_editor.sanity_y",
                MIN_OFFSET,
                MAX_OFFSET,
                sanityOffsetY,
                value -> setSanityOffsetY(value, false)
        ));

        int buttonY = this.height - 28;
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.mindmotion.hud_editor.reset"),
                        button -> resetDefaults())
                .bounds(leftX, buttonY, 150, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        button -> onClose())
                .bounds(rightX, buttonY, 150, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        gui.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        gui.drawCenteredString(
                this.font,
                Component.translatable("screen.mindmotion.hud_editor.instructions"),
                this.width / 2,
                22,
                0xA0A0A0
        );
        renderPreview(gui);
        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            BarPosition tempoBar = getTempoBarPosition();
            if (isMouseOver(mouseX, mouseY, tempoBar)) {
                draggingTempo = true;
                dragStartX = mouseX;
                dragStartY = mouseY;
                dragStartTempoX = tempoOffsetX;
                dragStartTempoY = tempoOffsetY;
                return true;
            }
            BarPosition sanityBar = getSanityBarPosition();
            if (isMouseOver(mouseX, mouseY, sanityBar)) {
                draggingSanity = true;
                dragStartX = mouseX;
                dragStartY = mouseY;
                dragStartSanityX = sanityOffsetX;
                dragStartSanityY = sanityOffsetY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            if (draggingTempo) {
                int newX = clamp(dragStartTempoX + (int) Math.round(mouseX - dragStartX), MIN_OFFSET, MAX_OFFSET);
                int newY = clamp(dragStartTempoY + (int) Math.round(mouseY - dragStartY), MIN_OFFSET, MAX_OFFSET);
                setTempoOffsetX(newX, true);
                setTempoOffsetY(newY, true);
                return true;
            }
            if (draggingSanity) {
                int newX = clamp(dragStartSanityX + (int) Math.round(mouseX - dragStartX), MIN_OFFSET, MAX_OFFSET);
                int newY = clamp(dragStartSanityY + (int) Math.round(mouseY - dragStartY), MIN_OFFSET, MAX_OFFSET);
                setSanityOffsetX(newX, true);
                setSanityOffsetY(newY, true);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingTempo = false;
            draggingSanity = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        saveConfig();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(previous);
    }

    private void renderPreview(GuiGraphics gui) {
        BarPosition tempoBar = getTempoBarPosition();
        BarPosition sanityBar = getSanityBarPosition();
        drawRoundedBar(gui, tempoBar, 0xFF33AFFF, (int) (barWidth * 0.75f));
        gui.drawString(this.font, Component.literal("Tempo"), tempoBar.x, tempoBar.y - 12, 0x66CCFF, false);
        drawRoundedBar(gui, sanityBar, 0xFFFFFFFF, (int) (barWidth * 0.6f));
        int sanityLabelWidth = this.font.width("Sanity");
        gui.drawString(
                this.font,
                Component.literal("Sanity"),
                sanityBar.x + barWidth - sanityLabelWidth,
                sanityBar.y - 12,
                0xFFFFFF,
                false
        );
    }

    private BarPosition getTempoBarPosition() {
        int baseY = this.height - BASE_Y_OFFSET;
        return new BarPosition(
                BASE_TEMPO_X + tempoOffsetX,
                baseY + tempoOffsetY,
                barWidth,
                barHeight
        );
    }

    private BarPosition getSanityBarPosition() {
        int baseY = this.height - BASE_Y_OFFSET;
        int baseSanityX = this.width - barWidth - 8;
        return new BarPosition(
                baseSanityX + sanityOffsetX,
                baseY + sanityOffsetY,
                barWidth,
                barHeight
        );
    }

    private void setBarWidth(int value, boolean updateSlider) {
        barWidth = clamp(value, MIN_BAR_WIDTH, MAX_BAR_WIDTH);
        ConfigMM.CLIENT.BAR_WIDTH.set(barWidth);
        if (updateSlider && barWidthSlider != null) {
            barWidthSlider.setIntValue(barWidth);
        }
    }

    private void setBarHeight(int value, boolean updateSlider) {
        barHeight = clamp(value, MIN_BAR_HEIGHT, MAX_BAR_HEIGHT);
        ConfigMM.CLIENT.BAR_HEIGHT.set(barHeight);
        if (updateSlider && barHeightSlider != null) {
            barHeightSlider.setIntValue(barHeight);
        }
    }

    private void setTempoOffsetX(int value, boolean updateSlider) {
        tempoOffsetX = clamp(value, MIN_OFFSET, MAX_OFFSET);
        ConfigMM.CLIENT.TEMPO_BAR_X_OFFSET.set(tempoOffsetX);
        if (updateSlider && tempoXSlider != null) {
            tempoXSlider.setIntValue(tempoOffsetX);
        }
    }

    private void setTempoOffsetY(int value, boolean updateSlider) {
        tempoOffsetY = clamp(value, MIN_OFFSET, MAX_OFFSET);
        ConfigMM.CLIENT.TEMPO_BAR_Y_OFFSET.set(tempoOffsetY);
        if (updateSlider && tempoYSlider != null) {
            tempoYSlider.setIntValue(tempoOffsetY);
        }
    }

    private void setSanityOffsetX(int value, boolean updateSlider) {
        sanityOffsetX = clamp(value, MIN_OFFSET, MAX_OFFSET);
        ConfigMM.CLIENT.SANITY_BAR_X_OFFSET.set(sanityOffsetX);
        if (updateSlider && sanityXSlider != null) {
            sanityXSlider.setIntValue(sanityOffsetX);
        }
    }

    private void setSanityOffsetY(int value, boolean updateSlider) {
        sanityOffsetY = clamp(value, MIN_OFFSET, MAX_OFFSET);
        ConfigMM.CLIENT.SANITY_BAR_Y_OFFSET.set(sanityOffsetY);
        if (updateSlider && sanityYSlider != null) {
            sanityYSlider.setIntValue(sanityOffsetY);
        }
    }

    private void resetDefaults() {
        setBarWidth(DEFAULT_BAR_WIDTH, true);
        setBarHeight(DEFAULT_BAR_HEIGHT, true);
        setTempoOffsetX(DEFAULT_OFFSET, true);
        setTempoOffsetY(DEFAULT_OFFSET, true);
        setSanityOffsetX(DEFAULT_OFFSET, true);
        setSanityOffsetY(DEFAULT_OFFSET, true);
    }

    private void saveConfig() {
        ConfigMM.CLIENT_SPEC.save();
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private void drawRoundedBar(GuiGraphics gui, BarPosition pos, int color, int fillWidth) {
        int bg = 0xCC000000;
        drawRoundedRect(gui, pos.x, pos.y, pos.width, pos.height, bg);
        int top = color;
        int bottom = (color & 0x00FFFFFF) | 0xFF000000;
        drawRoundedRect(gui, pos.x, pos.y, fillWidth, pos.height, top, bottom);
    }

    private void drawRoundedRect(GuiGraphics gui, int x, int y, int width, int height, int color) {
        gui.fill(x + 1, y, x + width - 1, y + height, color);
        gui.fill(x, y + 1, x + width, y + height - 1, color);
        gui.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
    }

    private void drawRoundedRect(GuiGraphics gui, int x, int y, int width, int height, int topColor, int bottomColor) {
        gui.fillGradient(x + 1, y, x + width - 1, y + height, topColor, bottomColor);
        gui.fillGradient(x, y + 1, x + width, y + height - 1, topColor, bottomColor);
    }

    private static boolean isMouseOver(double mouseX, double mouseY, BarPosition pos) {
        return mouseX >= pos.x
                && mouseX <= pos.x + pos.width
                && mouseY >= pos.y
                && mouseY <= pos.y + pos.height;
    }

    private static class BarPosition {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private BarPosition(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final String translationKey;
        private final IntConsumer onChange;

        private IntSlider(
                int x,
                int y,
                int width,
                int height,
                String translationKey,
                int min,
                int max,
                int value,
                IntConsumer onChange
        ) {
            super(x, y, width, height, Component.translatable(translationKey, value), toSliderValue(value, min, max));
            this.min = min;
            this.max = max;
            this.translationKey = translationKey;
            this.onChange = onChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(translationKey, getIntValue()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(getIntValue());
        }

        private int getIntValue() {
            double valueRange = max - min;
            return (int) Math.round(min + (valueRange * this.value));
        }

        private void setIntValue(int value) {
            this.value = toSliderValue(value, min, max);
            updateMessage();
        }

        private static double toSliderValue(int value, int min, int max) {
            if (max == min) {
                return 0.0;
            }
            return (double) (value - min) / (double) (max - min);
        }
    }
}
