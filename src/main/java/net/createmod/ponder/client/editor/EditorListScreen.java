package net.createmod.ponder.client.editor;

import net.createmod.ponder.client.theme.PonderThemes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import static net.createmod.ponder.client.editor.EditorWidgets.*;

/** Searchable picker for packs, scenes and action templates. */
final class EditorListScreen<T> extends GuiScreen {
    static final class Entry<T> {
        final String label, detail;
        final T value;
        Entry(String label, String detail, T value) { this.label = label; this.detail = detail; this.value = value; }
    }
    private final GuiScreen parent;
    private final String title;
    private final List<Entry<T>> entries;
    private final Consumer<T> accept;
    private final List<Entry<T>> filtered = new ArrayList<>();
    private GuiTextField search;
    private int scroll;
    private String error = "", query = "";

    EditorListScreen(GuiScreen parent, String title, List<Entry<T>> entries, Consumer<T> accept) {
        this.parent = parent; this.title = title; this.entries = entries; this.accept = accept;
    }

    @Override public void initGui() {
        buttonList.clear();
        search = new GuiTextField(0, fontRenderer, 12, 33, width - 24, 20);
        search.setMaxStringLength(512); search.setText(query); search.setFocused(true);
        addButton(new Button(1, width - 86, height - 28, 74, tr("cancel"), () -> mc.displayGuiScreen(parent)));
        Keyboard.enableRepeatEvents(true); filter();
    }
    private int rows() { return Math.max(1, (height - 98) / 29); }
    private void filter() {
        query = search.getText(); filtered.clear();
        String match = query.toLowerCase(Locale.ROOT);
        for (Entry<T> entry : entries)
            if ((entry.label + " " + entry.detail).toLowerCase(Locale.ROOT).contains(match)) filtered.add(entry);
        scroll = Math.max(0, Math.min(scroll, filtered.size() - rows()));
    }
    @Override public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, PonderThemes.color(BACKGROUND));
        fontRenderer.drawString(title, 12, 14, PonderThemes.color(TEXT)); search.drawTextBox();
        for (int i = scroll; i < Math.min(filtered.size(), scroll + rows()); i++) {
            int y = 61 + (i - scroll) * 29;
            Entry<T> entry = filtered.get(i);
            drawRect(12, y, width - 12, y + 27, my >= y && my < y + 27 ? PonderThemes.color(0xff304638) : PonderThemes.color(PANEL));
            fontRenderer.drawString(fontRenderer.trimStringToWidth(entry.label, width - 42), 20, y + 4, PonderThemes.color(TEXT));
            fontRenderer.drawString(fontRenderer.trimStringToWidth(entry.detail, width - 42), 20, y + 15, PonderThemes.color(MUTED));
        }
        fontRenderer.drawString(fontRenderer.trimStringToWidth(error.isEmpty() ? tr("search_hint") : error, width - 110),
            12, height - 23, error.isEmpty() ? PonderThemes.color(MUTED) : PonderThemes.color(0xffff9c9c));
        super.drawScreen(mx, my, pt);
    }
    @Override protected void mouseClicked(int mx, int my, int button) throws IOException {
        search.mouseClicked(mx, my, button);
        if (button == 0 && mx >= 12 && mx < width - 12 && my >= 61 && my < 61 + rows() * 29) {
            int index = scroll + (my - 61) / 29;
            if (index < filtered.size()) {
                try { accept.accept(filtered.get(index).value); }
                catch (RuntimeException failure) { error = failure.getMessage(); }
                return;
            }
        }
        super.mouseClicked(mx, my, button);
    }
    @Override protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        if (search.textboxKeyTyped(c, key)) { scroll = 0; filter(); }
    }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput(); int delta = Mouse.getEventDWheel();
        if (delta != 0) scroll = Math.max(0, Math.min(scroll - Integer.signum(delta), Math.max(0, filtered.size() - rows())));
    }
    @Override protected void actionPerformed(GuiButton button) { if (button instanceof Button) ((Button) button).activate(); }
    @Override public void updateScreen() { search.updateCursorCounter(); }
    @Override public void onGuiClosed() { query = search.getText(); Keyboard.enableRepeatEvents(false); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
