package net.createmod.ponder.foundation.ui;

import net.createmod.ponder.client.theme.PonderThemes;

import java.util.List;

import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;

/** A hoverable popup whose input is consumed before the world preview receives it. */
final class PonderSceneMenu {
    private static final int HEADER = 25;
    private static final int ROW = 28;
    private static final int FOOTER = 20;
    private final List<PonderScene> scenes;
    private GuiButton anchor;
    private int x, y, width, height, visibleRows;
    private int firstVisible, highlighted;
    private int lastMouseX = Integer.MIN_VALUE, lastMouseY = Integer.MIN_VALUE;
    private boolean open, pinned, hoverSuppressed;
    private long lastInside;

    PonderSceneMenu(List<PonderScene> scenes) { this.scenes = scenes; }

    void layout(int screenWidth, int screenHeight, GuiButton anchor) {
        boolean suppressHover = hoverSuppressed;
        this.anchor = anchor;
        width = Math.min(300, screenWidth - 16);
        x = Math.max(8, Math.min(screenWidth - width - 8, anchor.x + anchor.width - width));
        y = anchor.y + anchor.height + 4;
        visibleRows = Math.max(1, Math.min(scenes.size(), (screenHeight - (PonderThemes.modern() ? 80 : 42) - y - HEADER - FOOTER) / ROW));
        height = HEADER + visibleRows * ROW + FOOTER;
        close();
        hoverSuppressed = suppressHover;
    }

    boolean isOpen() { return open; }
    int highlighted() { return highlighted; }
    int pageSize() { return visibleRows; }

    boolean overAnchor(int mouseX, int mouseY) {
        if (anchor instanceof PonderButton button) return button.containsPointer(mouseX, mouseY);
        return anchor != null && inside(mouseX, mouseY, anchor.x, anchor.y, anchor.width, anchor.height);
    }

    boolean contains(int mouseX, int mouseY) { return inside(mouseX, mouseY, x, y, width, height); }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    void updateHover(int mouseX, int mouseY, int selected) {
        long now = System.currentTimeMillis();
        boolean overAnchor = overAnchor(mouseX, mouseY);
        if (!overAnchor) hoverSuppressed = false;
        if (overAnchor && !open && !hoverSuppressed) open(selected, false);
        // Include the four-pixel bridge so entering the popup never closes it.
        boolean inside = overAnchor || inside(mouseX, mouseY, x, y - 4, width, height + 4);
        if (inside) lastInside = now;
        else if (open && !pinned && now - lastInside > 220) close();
        if (open && (mouseX != lastMouseX || mouseY != lastMouseY)) {
            int row = rowAt(mouseX, mouseY);
            if (row >= 0) highlighted = row;
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    void toggle(int selected) {
        if (open && pinned) close();
        else open(selected, true);
    }

    private void open(int selected, boolean pin) {
        open = true;
        pinned = pin;
        highlighted = selected;
        firstVisible = Math.max(0, Math.min(selected - visibleRows / 2, scenes.size() - visibleRows));
        lastInside = System.currentTimeMillis();
    }

    void close() { open = false; pinned = false; hoverSuppressed = true; }

    int rowAt(int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, x + 4, y + HEADER, width - 12, visibleRows * ROW)) return -1;
        int index = firstVisible + (mouseY - y - HEADER) / ROW;
        return index < scenes.size() ? index : -1;
    }

    void scroll(int direction) {
        firstVisible = Math.max(0, Math.min(firstVisible + direction, scenes.size() - visibleRows));
        highlighted = Math.max(firstVisible, Math.min(highlighted, firstVisible + visibleRows - 1));
    }

    void move(int amount) {
        pinned = true;
        highlighted = Math.max(0, Math.min(scenes.size() - 1, highlighted + amount));
        if (highlighted < firstVisible) firstVisible = highlighted;
        if (highlighted >= firstVisible + visibleRows) firstVisible = highlighted - visibleRows + 1;
    }

    void draw(FontRenderer font, int selected) {
        if (!open) return;
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            Gui.drawRect(x + 3, y + 3, x + width + 3, y + height + 3, PonderThemes.color(0x77000000));
            Gui.drawRect(x, y, x + width, y + height, PonderThemes.color(0xff171e25));
            Gui.drawRect(x, y, x + width, y + 1, PonderThemes.color(0xff9fd6d2));
            Gui.drawRect(x, y, x + 1, y + height, PonderThemes.color(0xff586b79));
            Gui.drawRect(x + width - 1, y, x + width, y + height, PonderThemes.color(0xff586b79));
            Gui.drawRect(x, y + height - 1, x + width, y + height, PonderThemes.color(0xff586b79));
            font.drawString(I18n.format("ponder.ui.scene_menu_title"), x + 9, y + 9, PonderThemes.color(0xffe3e8ed));
            String count = (selected + 1) + " / " + scenes.size();
            font.drawString(count, x + width - 10 - font.getStringWidth(count), y + 9, PonderThemes.color(0xff9fd6d2));
            for (int row = 0; row < visibleRows; row++) {
                int index = firstVisible + row;
                int top = y + HEADER + row * ROW;
                boolean current = index == selected;
                if (index == highlighted || current)
                    Gui.drawRect(x + 4, top, x + width - 9, top + ROW - 1,
                        index == highlighted ? PonderThemes.color(0xff354752) : PonderThemes.color(0xff24393f));
                if (current) Gui.drawRect(x + 4, top + 3, x + 6, top + ROW - 4, PonderThemes.color(0xff9fd6d2));
                font.drawString(Integer.toString(index + 1), x + 11, top + 5, current ? PonderThemes.color(0xff9fd6d2) : PonderThemes.color(0xff8d9eab));
                PonderScene scene = scenes.get(index);
                int numberWidth = font.getStringWidth(Integer.toString(scenes.size()));
                int textX = x + 18 + numberWidth;
                int textWidth = x + width - 18 - textX;
                font.drawString(ellipsize(font, scene.getTitle(), textWidth), textX, top + 4,
                    current ? PonderThemes.color(0xffbce8df) : PonderThemes.color(0xffeef2f5));
                font.drawString(ellipsize(font, scene.getId().toString(), textWidth), textX, top + 16, PonderThemes.color(0xff8d9eab));
            }
            if (scenes.size() > visibleRows) {
                int track = visibleRows * ROW - 4;
                int thumb = Math.max(12, track * visibleRows / scenes.size());
                int offset = (track - thumb) * firstVisible / (scenes.size() - visibleRows);
                Gui.drawRect(x + width - 6, y + HEADER + 2, x + width - 4, y + HEADER + 2 + track, PonderThemes.color(0xff303f4a));
                Gui.drawRect(x + width - 6, y + HEADER + 2 + offset, x + width - 4, y + HEADER + 2 + offset + thumb, PonderThemes.color(0xff9fb5c9));
            }
            font.drawString(ellipsize(font, I18n.format("ponder.ui.scene_menu_help"), width - 18),
                x + 9, y + height - 13, PonderThemes.color(0xff8d9eab));
        }
    }

    private static String ellipsize(FontRenderer font, String text, int width) {
        return font.getStringWidth(text) <= width ? text : font.trimStringToWidth(text, Math.max(0, width - font.getStringWidth("..."))) + "...";
    }
}
