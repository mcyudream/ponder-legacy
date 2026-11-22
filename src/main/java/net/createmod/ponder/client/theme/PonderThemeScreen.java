package net.createmod.ponder.client.theme;

import java.io.IOException;
import java.util.List;
import net.createmod.ponder.api.theme.PonderTheme;
import net.createmod.ponder.api.theme.PonderTheme.Color;
import net.createmod.ponder.enums.PonderConfig;
import net.createmod.ponder.foundation.ui.AbstractPonderScreen;
import net.createmod.ponder.foundation.ui.PonderButton;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Live selection. The previous viewer is retained, including its paused time and camera. */
public final class PonderThemeScreen extends AbstractPonderScreen {
    private final GuiScreen back;
    private List<PonderTheme> themes = List.of();
    private int first, focus = -1, left, top, cardWidth, rows, pitch;
    private long revision;

    public PonderThemeScreen(GuiScreen back) { this.back = back; }

    @Override public void initGui() {
        themes = PonderThemes.all();
        revision = PonderThemes.revision();
        left = Math.max(10, (width - 500) / 2);
        top = 60;
        cardWidth = width - left * 2;
        pitch = PonderThemes.modern() ? 72 : 66;
        rows = Math.max(1, 1 + (height - 42 - top - (PonderThemes.modern() ? 64 : 60)) / pitch);
        if (focus < 0) {
            focus = Math.max(0, themes.stream().map(PonderTheme::id).toList().indexOf(PonderThemes.current().id()));
            first = Math.max(0, focus - rows / 2);
        }
        first = Math.max(0, Math.min(first, Math.max(0, themes.size() - rows)));
        focus = Math.min(focus, Math.max(0, themes.size() - 1));
        buttonList.clear();
        addButton(new PonderButton(1, width - 32, height - 29, 22, 20, PonderButton.Icon.CLOSE)
            .withTooltip("ponder.ui.close").withCallback(() -> mc.displayGuiScreen(back)));
        addButton(new PonderButton(2, left, height - 29, Math.min(190, cardWidth - 32), 20, null)
            .withLabel(I18n.format("ponder.theme.automatic"))
            .active("auto".equals(PonderConfig.client().getTheme()))
            .withCallback(() -> { PonderThemes.selectAutomatic(); initGui(); }));
        for (int row = 0; row < rows && first + row < themes.size(); row++) {
            int index = first + row;
            PonderTheme theme = themes.get(index);
            addButton(new PonderButton(100 + index, left, top + row * pitch, cardWidth, 60, null)
                .active(theme.id().equals(PonderThemes.current().id()))
                .withCallback(() -> { focus = index; PonderThemes.select(theme.id()); initGui(); }));
        }
    }

    @Override public void updateScreen() { if (revision != PonderThemes.revision()) initGui(); }

    @Override protected void renderWindow(int mx, int my, float partial) {
        String title = I18n.format("ponder.theme.title");
        fontRenderer.drawString(title, left, 13, PonderThemes.color(Color.TEXT));
        fontRenderer.drawString(fontRenderer.trimStringToWidth(I18n.format("ponder.theme.hint"), cardWidth),
            left, 41, PonderThemes.color(Color.MUTED));
    }

    @Override protected void renderWindowForeground(int mx, int my, float partial) {
        for (var button : buttonList) {
            if (button.id < 100) continue;
            PonderTheme theme = themes.get(button.id - 100);
            int x = button.x + 9, y = button.y + 8;
            int textWidth = Math.max(10, button.width - 120);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(localize(theme.name()), textWidth),
                x, y, PonderThemes.color(Color.TEXT));
            fontRenderer.drawString(fontRenderer.trimStringToWidth(theme.id().toString(), textWidth),
                x, y + 13, PonderThemes.color(Color.SUBTLE));
            fontRenderer.drawString(fontRenderer.trimStringToWidth(localize(theme.description()), textWidth),
                x, y + 29, PonderThemes.color(Color.MUTED));
            int px = button.x + button.width - 99, py = button.y + 8;
            PonderThemeRenderer.gradient(px, py, px + 88, py + 44,
                theme.color(Color.BACKGROUND_TOP), theme.color(Color.BACKGROUND_BOTTOM));
            PonderThemeRenderer.box(px + 6, py + 7, 27, 28, theme.color(Color.BUTTON),
                theme.color(Color.BORDER), theme.color(Color.BORDER_BOTTOM));
            PonderButton.drawIcon(PonderButton.Icon.PLAY, px + 19, py + 20, 11, theme.color(Color.ICON));
            Gui.drawRect(px + 42, py + 14, px + 80, py + 16, theme.color(Color.TEXT));
            Gui.drawRect(px + 42, py + 22, px + 68, py + 23, theme.color(Color.MUTED));
            Gui.drawRect(px + 42, py + 32, px + 80, py + 34, theme.color(Color.PROGRESS_TRACK));
            Gui.drawRect(px + 42, py + 32, px + 65, py + 34, theme.color(Color.PROGRESS_FILL));
            if (button.id - 100 == focus)
                Gui.drawRect(button.x + 3, button.y + 5, button.x + 4, button.y + button.height - 5, PonderThemes.color(Color.ACCENT));
        }
        if (themes.size() > rows)
            fontRenderer.drawString((first + 1) + "–" + Math.min(themes.size(), first + rows) + " / " + themes.size(),
                width - left - 80, 41, PonderThemes.color(Color.MUTED));
        for (var button : buttonList)
            if (button instanceof PonderButton b && b.isHoveredButton() && b.getTooltipKey() != null)
                drawHoveringText(I18n.format(b.getTooltipKey()), mx, my);
    }

    private static String localize(String text) { return I18n.hasKey(text) ? I18n.format(text) : text; }

    private void move(int amount) {
        focus = Math.max(0, Math.min(themes.size() - 1, focus + amount));
        if (focus < first) first = focus;
        if (focus >= first + rows) first = focus - rows + 1;
        initGui();
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) { first += wheel > 0 ? -1 : 1; initGui(); }
    }

    @Override protected void keyTyped(char c, int key) throws IOException {
        switch (key) {
            case Keyboard.KEY_ESCAPE -> mc.displayGuiScreen(back);
            case Keyboard.KEY_UP -> move(-1);
            case Keyboard.KEY_DOWN -> move(1);
            case Keyboard.KEY_TAB -> move(isShiftKeyDown() ? -1 : 1);
            case Keyboard.KEY_HOME -> move(-themes.size());
            case Keyboard.KEY_END -> move(themes.size());
            case Keyboard.KEY_PRIOR -> move(-rows);
            case Keyboard.KEY_NEXT -> move(rows);
            case Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> {
                if (!themes.isEmpty()) { PonderThemes.select(themes.get(focus).id()); initGui(); }
            }
            default -> super.keyTyped(c, key);
        }
    }
}
