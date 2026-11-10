package net.createmod.ponder.foundation.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.block.Block;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

public class PonderIndexScreen extends PonderCatalogScreen {
    private static final String UNCATEGORIZED = "#uncategorized";
    private final PonderScrollPane categoriesPane = new PonderScrollPane();
    private final List<Category> categories = new ArrayList<>();
    private final Map<ResourceLocation, String> names = new LinkedHashMap<>();
    private final Map<String, String> modNames = new LinkedHashMap<>();
    private PonderCatalog catalog;
    private GuiTextField search;
    private boolean byTheme;
    private String selected = "";
    private String language = "";

    @Override
    public void initGui() {
        String query = search == null ? "" : search.getText();
        buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        language = mc.getLanguageManager().getCurrentLanguage().getLanguageCode();
        catalog = PonderCatalog.current();
        names.clear(); modNames.clear();
        for (PonderCatalog.ItemEntry item : catalog.items) {
            String namespace = item.id.getNamespace();
            modNames.computeIfAbsent(namespace, PonderModNames::localized);
            names.put(item.id, stackFor(item.id).getDisplayName() + "\n" + modNames.get(namespace)
                + "\n" + PonderModNames.original(namespace) + "\n" + PonderModNames.owner(namespace));
        }
        int sideWidth = Math.max(112, Math.min(160, width / 4));
        categoriesPane.layout(12, 68, 12 + sideWidth, height - 44, categories.size(), 22);
        int contentLeft = categoriesPane.right + 14;
        search = new GuiTextField(10, fontRenderer, contentLeft, 40, width - contentLeft - 12, 18);
        search.setMaxStringLength(128);
        search.setText(query);
        int tabWidth = (sideWidth - 4) / 2;
        addButton(new PonderButton(3, 12, 40, tabWidth, 20, null).withLabel(I18n.format("ponder.ui.catalog.mods"))
            .active(!byTheme).withCallback(() -> changeMode(false)));
        addButton(new PonderButton(4, 16 + tabWidth, 40, tabWidth, 20, null).withLabel(I18n.format("ponder.ui.catalog.themes"))
            .active(byTheme).withCallback(() -> changeMode(true)));
        addButton(new PonderButton(1, 8, height - 29, 22, 20, PonderButton.Icon.TAGS)
            .withTooltip("ponder.ui.catalog.theme_index").withCallback(() -> ScreenOpener.open(new PonderTagIndexScreen())));
        addCloseButton();
        rebuildCategories();
        rebuildContent(false);
    }

    private void changeMode(boolean themes) {
        byTheme = themes; selected = "";
        categoriesPane.setFirstRow(0);
        for (net.minecraft.client.gui.GuiButton button : buttonList)
            if (button.id == 3 || button.id == 4) ((PonderButton)button).active((button.id == 4) == byTheme);
        rebuildCategories();
        rebuildContent(true);
    }

    private List<PonderCatalog.ItemEntry> matching(String selection) {
        return catalog.filter(!byTheme && !selection.isEmpty() ? selection : null,
            byTheme && !selection.isEmpty() && !UNCATEGORIZED.equals(selection) ? new ResourceLocation(selection) : null,
            byTheme && UNCATEGORIZED.equals(selection), search.getText(), names::get);
    }

    private void rebuildCategories() {
        categories.clear();
        categories.add(new Category("", I18n.format("ponder.ui.catalog.all"), matching("").size()));
        List<Category> sorted = new ArrayList<>();
        if (byTheme) {
            for (PonderTag tag : catalog.themes.values())
                sorted.add(new Category(tag.getId().toString(), tag.getTitle(), matching(tag.getId().toString()).size()));
        } else {
            for (String namespace : new TreeSet<>(modNames.keySet()))
                sorted.add(new Category(namespace, modNames.get(namespace), matching(namespace).size()));
        }
        sorted.sort(Comparator.comparing(category -> category.title, String.CASE_INSENSITIVE_ORDER));
        categories.addAll(sorted);
        if (byTheme) categories.add(new Category(UNCATEGORIZED, I18n.format("ponder.ui.catalog.uncategorized"), matching(UNCATEGORIZED).size()));
        if (categories.stream().noneMatch(category -> category.key.equals(selected))) selected = "";
        categoriesPane.layout(categoriesPane.left, categoriesPane.top, categoriesPane.right, categoriesPane.bottom, categories.size(), 22);
    }

    private void rebuildContent(boolean resetScroll) {
        entries.clear();
        addItems(matching(selected));
        if (resetScroll) grid.setFirstRow(0);
        layoutGrid(categoriesPane.right + 14, 82, width - 12, height - 44, 36, 28);
    }

    @Override
    protected void renderCatalogHeader(int mouseX, int mouseY, float partialTicks) {
        drawTitle(I18n.format("ponder.ui.index_title"));
        search.drawTextBox();
        if (search.getText().isEmpty() && !search.isFocused())
            fontRenderer.drawString(fontRenderer.trimStringToWidth(I18n.format("ponder.ui.catalog.search"), search.width - 8),
                search.x + 4, search.y + 5, 0xff7c8a95);
        Category active = categories.stream().filter(category -> category.key.equals(selected)).findFirst().orElse(categories.get(0));
        String heading = active.title + "  (" + entries.size() + ")";
        fontRenderer.drawString(fontRenderer.trimStringToWidth(heading, grid.right - grid.left - 4), grid.left, 66, 0xffc9d6dc);
        Gui.drawRect(categoriesPane.left - 2, categoriesPane.top - 2, categoriesPane.right, categoriesPane.bottom, 0xff161b21);
        int end = Math.min(categories.size(), categoriesPane.firstRow() + categoriesPane.pageRows());
        for (int i = categoriesPane.firstRow(); i < end; i++) {
            Category category = categories.get(i);
            int y = categoriesPane.top + (i - categoriesPane.firstRow()) * 22;
            boolean activeRow = selected.equals(category.key);
            if (activeRow || categoryAt(mouseX, mouseY) == i)
                Gui.drawRect(categoriesPane.left, y, categoriesPane.right - 12, y + 20, activeRow ? 0xff31525a : 0xff29343e);
            String count = Integer.toString(category.count);
            int countX = categoriesPane.right - 17 - fontRenderer.getStringWidth(count);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(category.title, Math.max(0, countX - categoriesPane.left - 10)),
                categoriesPane.left + 4, y + 6, activeRow ? 0xffc9efeb : 0xffb5c2cd);
            fontRenderer.drawString(count, countX, y + 6, 0xff82939e);
        }
        categoriesPane.draw();
    }

    private int categoryAt(int x, int y) {
        if (!categoriesPane.contains(x, y) || x >= categoriesPane.right - 12) return -1;
        int row = (y - categoriesPane.top) / 22;
        int index = categoriesPane.firstRow() + row;
        return row < categoriesPane.pageRows() && index < categories.size() ? index : -1;
    }

    @Override
    protected void renderWindowForeground(int mouseX, int mouseY, float partialTicks) {
        int index = categoryAt(mouseX, mouseY);
        if (index >= 0) {
            Category category = categories.get(index);
            drawHoveringText(Arrays.asList(category.title, "\u00a77" + I18n.format("ponder.ui.catalog.items", category.count)), mouseX, mouseY);
            return;
        }
        super.renderWindowForeground(mouseX, mouseY, partialTicks);
    }

    @Override
    void scrollAt(int wheel, int x, int y) {
        if (categoriesPane.contains(x, y)) categoriesPane.scroll(wheel > 0 ? -1 : 1);
        else super.scrollAt(wheel, x, y);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        search.mouseClicked(x, y, button);
        if (button == 0) {
            if (categoriesPane.click(x, y)) return;
            int index = categoryAt(x, y);
            if (index >= 0) { selected = categories.get(index).key; rebuildContent(true); return; }
        }
        super.mouseClicked(x, y, button);
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long elapsed) {
        if (button == 0 && categoriesPane.drag(y)) return;
        super.mouseClickMove(x, y, button, elapsed);
    }

    @Override
    protected void mouseReleased(int x, int y, int button) {
        categoriesPane.release();
        super.mouseReleased(x, y, button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_F && isCtrlKeyDown()) { search.setFocused(true); return; }
        if (search.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) { search.setFocused(false); return; }
            String previous = search.getText();
            search.textboxKeyTyped(typedChar, keyCode);
            if (!previous.equals(search.getText())) { rebuildCategories(); rebuildContent(true); }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override public void updateScreen() {
        if (!language.equals(mc.getLanguageManager().getCurrentLanguage().getLanguageCode())) initGui();
        search.updateCursorCounter();
    }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }

    static ItemStack stackFor(ResourceLocation id) {
        if (Item.REGISTRY.containsKey(id)) return new ItemStack(Item.REGISTRY.getObject(id));
        Block block = Block.REGISTRY.containsKey(id) ? Block.REGISTRY.getObject(id) : null;
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    private static final class Category {
        final String key, title;
        final int count;
        Category(String key, String title, int count) { this.key = key; this.title = title; this.count = count; }
    }
}
