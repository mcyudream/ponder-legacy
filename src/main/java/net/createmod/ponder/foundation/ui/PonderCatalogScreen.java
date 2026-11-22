package net.createmod.ponder.foundation.ui;

import net.createmod.ponder.client.theme.PonderThemes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Shared viewport and input handling for the item index, theme index and theme contents. */
abstract class PonderCatalogScreen extends AbstractPonderScreen {
    protected final PonderScrollPane grid = new PonderScrollPane();
    protected final List<GridEntry> entries = new ArrayList<>();
    final List<PonderButton> visibleEntries = new ArrayList<>();
    private int columns = 1, columnWidth = 36, rowHeight = 36, buttonSize = 28;

    protected void layoutGrid(int left, int top, int right, int bottom, int pitch, int buttonSize) {
        layoutGrid(left, top, right, bottom, pitch, pitch, buttonSize);
    }

    protected void layoutGrid(int left, int top, int right, int bottom, int columnWidth, int rowHeight, int buttonSize) {
        this.columnWidth = columnWidth; this.rowHeight = rowHeight; this.buttonSize = PonderThemes.modern() ? Math.min(20, buttonSize) : buttonSize;
        columns = Math.max(1, Math.min(12, (right - left - 14) / columnWidth));
        grid.layout(left, top, right, bottom, (entries.size() + columns - 1) / columns, rowHeight);
        rebuildGrid();
    }

    protected void rebuildGrid() {
        visibleEntries.clear();
        int startX = grid.left + Math.max(0, (grid.right - grid.left - 14 - columns * columnWidth) / 2);
        int first = grid.firstRow() * columns;
        int end = Math.min(entries.size(), first + grid.pageRows() * columns);
        for (int i = first; i < end; i++) {
            GridEntry entry = entries.get(i);
            boolean card = entry.caption != null;
            int x = startX + (i % columns) * columnWidth + (card ? 4 : (columnWidth - buttonSize) / 2);
            int y = grid.top + ((i - first) / columns) * rowHeight + (card ? 4 : (rowHeight - buttonSize) / 2);
            int height = card ? rowHeight - 8 : buttonSize;
            if (y + height > grid.bottom) break;
            visibleEntries.add(new CatalogButton(100 + i, x, y, card ? columnWidth - 8 : buttonSize, height, entry));
        }
    }

    protected void addCloseButton() {
        addThemeButton(20, width - 30, 7);
        addButton(new PonderButton(2, width - 30, height - 29, 22, 20, PonderButton.Icon.CLOSE)
            .withTooltip("ponder.ui.close").withCallback(() -> mc.displayGuiScreen(null)));
    }

    protected void addItems(List<PonderCatalog.ItemEntry> items) {
        for (PonderCatalog.ItemEntry item : items) {
            ItemStack stack = PonderIndexScreen.stackFor(item.id);
            entries.add(new GridEntry(item.id, stack, Arrays.asList(stack.getDisplayName(),
                "\u00a78" + item.id, "\u00a77" + I18n.format("ponder.ui.catalog.scenes", item.sceneCount)),
                () -> ScreenOpener.open(net.createmod.ponder.client.guide.Tutorials.open(item.id))));
        }
    }

    @Override
    protected final void renderWindow(int mouseX, int mouseY, float partialTicks) {
        renderCatalogHeader(mouseX, mouseY, partialTicks);
        for (PonderButton button : visibleEntries) button.drawButton(mc, mouseX, mouseY, partialTicks);
        grid.draw();
        if (entries.isEmpty()) {
            String empty = I18n.format("ponder.ui.catalog.empty");
            fontRenderer.drawString(empty, grid.left + (grid.right - grid.left - fontRenderer.getStringWidth(empty)) / 2,
                grid.top + 16, PonderThemes.color(0xffaeb8c2));
        }
        String help = I18n.format("ponder.ui.catalog.scroll_help");
        fontRenderer.drawString(fontRenderer.trimStringToWidth(help, Math.max(0, width - 84)), 40, height - 22, PonderThemes.color(0xff91a0ac));
    }

    protected abstract void renderCatalogHeader(int mouseX, int mouseY, float partialTicks);

    @Override
    protected void renderWindowForeground(int mouseX, int mouseY, float partialTicks) {
        for (PonderButton button : visibleEntries) {
            if (button.isHoveredButton()) {
                drawHoveringText(entries.get(button.id - 100).tooltip, mouseX, mouseY);
                return;
            }
        }
        for (GuiButton button : buttonList) {
            if (button instanceof PonderButton && ((PonderButton)button).isHoveredButton()) {
                String key = ((PonderButton)button).getTooltipKey();
                if (key != null) drawHoveringText(I18n.format(key), mouseX, mouseY);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scrollAt(wheel, Mouse.getEventX() * width / mc.displayWidth,
            height - Mouse.getEventY() * height / mc.displayHeight - 1);
    }

    void scrollAt(int wheel, int x, int y) {
        if (grid.contains(x, y)) {
            grid.scroll(wheel > 0 ? -1 : 1);
            rebuildGrid();
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        if (button == 0) {
            if (grid.click(x, y)) { rebuildGrid(); return; }
            for (PonderButton entry : visibleEntries) {
                if (entry.mousePressed(mc, x, y)) { entry.playPressSound(mc.getSoundHandler()); return; }
            }
        }
        super.mouseClicked(x, y, button);
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long elapsed) {
        if (button == 0 && grid.drag(y)) { rebuildGrid(); return; }
        super.mouseClickMove(x, y, button, elapsed);
    }

    @Override
    protected void mouseReleased(int x, int y, int button) {
        grid.release();
        super.mouseReleased(x, y, button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        switch (keyCode) {
            case Keyboard.KEY_HOME: grid.setFirstRow(0); break;
            case Keyboard.KEY_END: grid.setFirstRow(grid.maxFirstRow()); break;
            case Keyboard.KEY_PRIOR: grid.scroll(-grid.pageRows()); break;
            case Keyboard.KEY_NEXT: grid.scroll(grid.pageRows()); break;
            case Keyboard.KEY_UP: grid.scroll(-1); break;
            case Keyboard.KEY_DOWN: grid.scroll(1); break;
            default: super.keyTyped(typedChar, keyCode); return;
        }
        rebuildGrid();
    }

    protected void drawTitle(String title) {
        String fitted = fontRenderer.trimStringToWidth(title, Math.max(0, width - 80));
        fontRenderer.drawString(fitted, (width - fontRenderer.getStringWidth(fitted)) / 2, 13, PonderThemes.color(0xffeef2f5));
    }

    private static final class CatalogButton extends PonderButton {
        private final GridEntry entry;
        CatalogButton(int id, int x, int y, int width, int height, GridEntry entry) {
            super(id, x, y, width, height, null);
            this.entry = entry;
            showing(entry.caption == null ? entry.stack : ItemStack.EMPTY);
            withCallback(entry.open);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            super.drawButton(mc, mouseX, mouseY, partialTicks);
            if (entry.caption == null) return;
            GuiGameElement.of(entry.stack).render(x + 20, y + height / 2, 1);
            String title = mc.fontRenderer.trimStringToWidth(entry.caption, width - 48);
            mc.fontRenderer.drawString(title, x + 40, y + 9, PonderThemes.color(0xffd8e3e8));
            String count = TextFormatting.getTextWithoutFormattingCodes(entry.tooltip.get(entry.tooltip.size() - 1));
            mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(count, width - 48), x + 40, y + 25, PonderThemes.color(0xff91a0ac));
        }
    }

    static final class GridEntry {
        final ResourceLocation id;
        final ItemStack stack;
        final List<String> tooltip;
        final String caption;
        final Runnable open;
        GridEntry(ResourceLocation id, ItemStack stack, List<String> tooltip, Runnable open) {
            this(id, stack, tooltip, null, open);
        }
        GridEntry(ResourceLocation id, ItemStack stack, List<String> tooltip, String caption, Runnable open) {
            this.id = id; this.stack = stack; this.tooltip = tooltip; this.caption = caption; this.open = open;
        }
    }
}
