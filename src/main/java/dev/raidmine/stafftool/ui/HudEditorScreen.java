package dev.raidmine.stafftool.ui;

import dev.raidmine.stafftool.RaidMineStaffMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class HudEditorScreen extends Screen {
    private DragMode dragMode = DragMode.NONE;
    private double dragOffsetX;
    private double dragOffsetY;
    private int resizeStartX;
    private int resizeAnchorX;
    private int resizeAnchorY;
    private float resizeStartScale;
    private long centeredAt;

    public HudEditorScreen() {
        super(Text.literal("RM Tools HUD editor"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, UiTheme.argb(100, 3, 5, 10));
        drawGuides(context);
        HudOverlay.renderEditable(context);

        String title = "Настройка панели RM Tools";
        int titleW = UiTheme.textWidth(title, UiTheme.FONT_TITLE, true);
        int boxW = Math.max(420, titleW + 36);
        int x = (width - boxW) / 2;
        int y = height - 62;
        UiTheme.shadow(context, x, y, boxW, 46, 13);
        UiTheme.roundedRect(context, x, y, boxW, 46, 13, UiTheme.argb(235, 18, 22, 32));
        UiTheme.textTitle(context, textRenderer, title, (width - titleW) / 2, y + 7, UiTheme.TEXT);
        String hint = "Перетаскивание • угол — размер • колесо — масштаб • Right Shift — центр • R — сброс • Esc — сохранить";
        int hintW = UiTheme.textWidth(hint, UiTheme.FONT_SMALL, false);
        UiTheme.text(context, textRenderer, hint, (width - hintW) / 2, y + 27,
                UiTheme.FONT_SMALL, UiTheme.MUTED, false);

        if (System.currentTimeMillis() - centeredAt < 1300L) {
            String centered = "Панель отцентрирована";
            int w = UiTheme.textWidth(centered, 10F, true) + 34;
            int tx = (width - w) / 2;
            UiTheme.glow(context, tx, 42, w, 30, 10, UiTheme.accent());
            UiTheme.roundedRect(context, tx, 42, w, 30, 10, UiTheme.PANEL_2);
            UiTheme.icon(context, UiIcon.CENTER, tx + 9, 50, 14, UiTheme.accent());
            UiTheme.text(context, textRenderer, centered, tx + 28, 51, 10F, UiTheme.TEXT, true);
        }
    }

    private void drawGuides(DrawContext context) {
        int centerX = width / 2;
        int centerY = height / 2;
        context.fill(centerX, 0, centerX + 1, height, UiTheme.argb(34, 255, 163, 26));
        context.fill(0, centerY, width, centerY + 1, UiTheme.argb(18, 255, 163, 26));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        HudOverlay.Layout layout = HudOverlay.layout(width, height);
        if (layout.resizeHandle().contains(click.x(), click.y())) {
            dragMode = DragMode.RESIZE;
            resizeStartX = (int) click.x();
            resizeAnchorX = layout.x();
            resizeAnchorY = layout.y();
            resizeStartScale = RaidMineStaffMod.config().hudScale;
            return true;
        }
        if (layout.contains(click.x(), click.y())) {
            dragMode = DragMode.MOVE;
            dragOffsetX = click.x() - layout.x();
            dragOffsetY = click.y() - layout.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragMode == DragMode.MOVE) {
            HudOverlay.setPosition(width, height,
                    (int) Math.round(click.x() - dragOffsetX),
                    (int) Math.round(click.y() - dragOffsetY));
            return true;
        }
        if (dragMode == DragMode.RESIZE) {
            float deltaScale = ((float) click.x() - resizeStartX) / HudOverlay.BASE_WIDTH;
            HudOverlay.setScale(resizeStartScale + deltaScale);
            HudOverlay.setPosition(width, height, resizeAnchorX, resizeAnchorY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragMode != DragMode.NONE) {
            dragMode = DragMode.NONE;
            RaidMineStaffMod.config().save();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        HudOverlay.Layout layout = HudOverlay.layout(width, height);
        if (!layout.contains(mouseX, mouseY)) return false;
        HudOverlay.setScale(RaidMineStaffMod.config().hudScale + (verticalAmount > 0 ? 0.05F : -0.05F));
        RaidMineStaffMod.config().save();
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_R) {
            HudOverlay.reset();
            centeredAt = System.currentTimeMillis();
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_RIGHT_SHIFT || input.key() == GLFW.GLFW_KEY_C) {
            HudOverlay.centerTop();
            centeredAt = System.currentTimeMillis();
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        RaidMineStaffMod.config().save();
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private enum DragMode { NONE, MOVE, RESIZE }
}
