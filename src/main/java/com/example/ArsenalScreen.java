package com.example;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ArsenalScreen extends Screen {

    private static final int BG_PANEL = 0xEE0C1017;
    private static final int BG_CARD = 0xC0121823;
    private static final int BG_CARD_HOVER = 0xE01A2332;
    private static final int ACCENT_RED = 0xFFFF2A4D;
    private static final int ACCENT_DIM = 0x30FF2A4D;
    private static final int BORDER_COLOR = 0x25FFFFFF;
    private static final int TEXT_MAIN = 0xFFF0F4FC;
    private static final int TEXT_MUTED = 0xFF6A7890;

    private int winX, winY;
    private final int winW = 420;
    private final int winH = 310;

    private boolean isBindingKey = false;
    private boolean isDraggingSlider = false;

    public ArsenalScreen() {
        super(Component.literal("Arsenal GUI"));
    }

    @Override
    protected void init() {
        this.winX = (this.width - this.winW) / 2;
        this.winY = (this.height - this.winH) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, this.width, this.height, 0x60000000, 0x90000000);

        // Painel Principal
        graphics.fill(winX, winY, winX + winW, winY + winH, BG_PANEL);
        renderOutline(graphics, winX, winY, winW, winH, ACCENT_RED);

        // Header
        int headerH = 42;
        graphics.fill(winX, winY, winX + winW, winY + headerH, 0x40000000);
        graphics.fill(winX, winY + headerH, winX + winW, winY + headerH + 1, BORDER_COLOR);

        // Logo A
        int logoSize = 22;
        int logoX = winX + 12;
        int logoY = winY + 10;
        graphics.fill(logoX, logoY, logoX + logoSize, logoY + logoSize, ACCENT_RED);
        graphics.drawString(this.font, "A", logoX + 7, logoY + 7, 0xFF000000, false);
        graphics.drawString(this.font, "ARSENAL", logoX + logoSize + 8, logoY + 7, 0xFFFFFFFF, true);

        // Aba de Categoria
        int catY = winY + headerH + 8;
        int tabW = 70;
        int tabH = 18;
        int tabX = winX + 12;

        graphics.fill(tabX, catY, tabX + tabW, catY + tabH, ACCENT_DIM);
        renderOutline(graphics, tabX, catY, tabW, tabH, ACCENT_RED);
        graphics.drawString(this.font, "Combat", tabX + 10, catY + 5, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "1", tabX + tabW - 12, catY + 5, ACCENT_RED, false);

        // Card do Módulo Xbow Cart
        int cardX = winX + 12;
        int cardY = catY + tabH + 12;
        int cardW = winW - 24;
        int cardH = 180;

        boolean isHovered = mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH;
        int cardBg = ExampleMod.isEnabled ? (isHovered ? BG_CARD_HOVER : 0xD5161E2C) : (isHovered ? BG_CARD_HOVER : BG_CARD);
        graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, cardBg);
        renderOutline(graphics, cardX, cardY, cardW, cardH, ExampleMod.isEnabled ? ACCENT_RED : BORDER_COLOR);

        graphics.drawString(this.font, "Xbow Cart", cardX + 12, cardY + 10, TEXT_MAIN, true);

        // Keybind Pill
        int bindW = 44;
        int bindH = 14;
        int bindX = cardX + cardW - bindW - 12;
        int bindY = cardY + 8;
        String bindText = isBindingKey ? "..." : (ExampleMod.keybind != GLFW.GLFW_KEY_UNKNOWN && GLFW.glfwGetKeyName(ExampleMod.keybind, 0) != null) ? GLFW.glfwGetKeyName(ExampleMod.keybind, 0).toUpperCase() : "NONE";
        graphics.fill(bindX, bindY, bindX + bindW, bindY + bindH, 0x60000000);
        renderOutline(graphics, bindX, bindY, bindW, bindH, isBindingKey ? ACCENT_RED : BORDER_COLOR);
        graphics.drawCenteredString(this.font, bindText, bindX + (bindW / 2), bindY + 3, isBindingKey ? ACCENT_RED : TEXT_MUTED);

        // Toggle Switch
        int switchW = 32;
        int switchH = 14;
        int switchX = cardX + cardW - switchW - 12;
        int switchY = cardY + 28;
        graphics.fill(switchX, switchY, switchX + switchW, switchY + switchH, ExampleMod.isEnabled ? ACCENT_RED : 0x50000000);
        int toggleCircleX = ExampleMod.isEnabled ? (switchX + switchW - 12) : (switchX + 2);
        graphics.fill(toggleCircleX, switchY + 2, toggleCircleX + 10, switchY + 12, 0xFFFFFFFF);

        graphics.drawString(this.font, "Ativa o combo com mira S-Curve ao segurar o trilho.", cardX + 12, cardY + 28, TEXT_MUTED, false);
        graphics.fill(cardX + 10, cardY + 48, cardX + cardW - 10, cardY + 49, BORDER_COLOR);

        // Sliders e Checkboxes dentro do Card
        int optY = cardY + 58;
        renderCheckbox(graphics, cardX + 12, optY, "Smooth Aim (Cinematica)", ExampleMod.smoothAim);
        optY += 22;

        graphics.drawString(this.font, "Aim Speed: " + (int) ExampleMod.aimSpeed + " deg/tick", cardX + 12, optY, TEXT_MAIN, false);
        int sliderX = cardX + 160;
        int sliderW = 120;
        graphics.fill(sliderX, optY + 2, sliderX + sliderW, optY + 8, 0x60000000);
        int filledW = (int) (((ExampleMod.aimSpeed - 5) / 55.0) * sliderW);
        graphics.fill(sliderX, optY + 2, sliderX + filledW, optY + 8, ACCENT_RED);
        graphics.fill(sliderX + filledW - 2, optY, sliderX + filledW + 2, optY + 10, 0xFFFFFFFF);
        optY += 22;

        renderCheckbox(graphics, cardX + 12, optY, "Auto Shoot Crossbow", ExampleMod.autoShoot);
        optY += 22;

        renderCheckbox(graphics, cardX + 12, optY, "Swap Back to Previous Slot", ExampleMod.swapBack);

        // Footer Bar
        int footerH = 24;
        int footerY = winY + winH - footerH;
        graphics.fill(winX, footerY, winX + winW, winY + winH, 0x40000000);
        graphics.fill(winX, footerY, winX + winW, footerY + 1, BORDER_COLOR);

        int fps = this.minecraft != null ? this.minecraft.getFps() : 144;
        graphics.drawString(this.font, "FPS: ", winX + 14, footerY + 8, TEXT_MUTED, false);
        graphics.drawString(this.font, String.valueOf(fps), winX + 38, footerY + 8, ACCENT_RED, false);

        graphics.drawString(this.font, "THEME: ", winX + 100, footerY + 8, TEXT_MUTED, false);
        graphics.drawString(this.font, "ARSENAL RED", winX + 142, footerY + 8, ACCENT_RED, false);

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void renderCheckbox(GuiGraphics graphics, int x, int y, String label, boolean checked) {
        int boxSize = 10;
        graphics.fill(x, y, x + boxSize, y + boxSize, checked ? ACCENT_RED : 0x50000000);
        renderOutline(graphics, x, y, boxSize, boxSize, checked ? ACCENT_RED : BORDER_COLOR);
        graphics.drawString(this.font, label, x + boxSize + 8, y + 1, checked ? TEXT_MAIN : TEXT_MUTED, false);
    }

    private void renderOutline(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int cardX = winX + 12;
            int cardY = winY + 42 + 20 + 18;
            int cardW = winW - 24;

            int switchX = cardX + cardW - 32 - 12;
            int switchY = cardY + 28;
            if (mouseX >= switchX && mouseX <= switchX + 32 && mouseY >= switchY && mouseY <= switchY + 14) {
                ExampleMod.isEnabled = !ExampleMod.isEnabled;
                return true;
            }

            int bindX = cardX + cardW - 44 - 12;
            int bindY = cardY + 8;
            if (mouseX >= bindX && mouseX <= bindX + 44 && mouseY >= bindY && mouseY <= bindY + 14) {
                this.isBindingKey = !this.isBindingKey;
                return true;
            }

            int optY = cardY + 58;
            if (mouseX >= cardX + 12 && mouseX <= cardX + 160 && mouseY >= optY && mouseY <= optY + 12) {
                ExampleMod.smoothAim = !ExampleMod.smoothAim;
                return true;
            }

            optY += 22;
            int sliderX = cardX + 160;
            int sliderW = 120;
            if (mouseX >= sliderX && mouseX <= sliderX + sliderW && mouseY >= optY && mouseY <= optY + 12) {
                this.isDraggingSlider = true;
                updateSlider(mouseX, sliderX, sliderW);
                return true;
            }

            optY += 22;
            if (mouseX >= cardX + 12 && mouseX <= cardX + 160 && mouseY >= optY && mouseY <= optY + 12) {
                ExampleMod.autoShoot = !ExampleMod.autoShoot;
                return true;
            }

            optY += 22;
            if (mouseX >= cardX + 12 && mouseX <= cardX + 160 && mouseY >= optY && mouseY <= optY + 12) {
                ExampleMod.swapBack = !ExampleMod.swapBack;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isDraggingSlider = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingSlider) {
            int cardX = winX + 12;
            int cardY = winY + 42 + 20 + 18;
            int optY = cardY + 58 + 22;
            int sliderX = cardX + 160;
            int sliderW = 120;
            updateSlider(mouseX, sliderX, sliderW);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void updateSlider(double mouseX, int sliderX, int sliderW) {
        double pct = Math.max(0.0, Math.min(1.0, (mouseX - sliderX) / (double) sliderW));
        ExampleMod.aimSpeed = 5.0 + (pct * 55.0);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.isBindingKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                ExampleMod.keybind = GLFW.GLFW_KEY_UNKNOWN;
            } else {
                ExampleMod.keybind = keyCode;
            }
            this.isBindingKey = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
					  }
