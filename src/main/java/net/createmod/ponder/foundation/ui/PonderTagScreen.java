package net.createmod.ponder.foundation.ui;

import net.createmod.ponder.client.theme.PonderThemes;

import java.util.List;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.util.ResourceLocation;

public class PonderTagScreen extends PonderCatalogScreen {
    private final PonderTag tag;
    private List<String> description;
    private int shownLines;

    public PonderTagScreen(ResourceLocation id) { this(PonderIndex.getTagAccess().getRegisteredTag(id)); }
    public PonderTagScreen(PonderTag tag) { this.tag = tag; }

    @Override
    public void initGui() {
        buttonList.clear(); entries.clear();
        PonderCatalog catalog = PonderCatalog.current();
        addItems(catalog.filter(null, tag.getId(), false, "", id -> ""));
        description = fontRenderer.listFormattedStringToWidth(tag.getDescription(), Math.min(440, width - 40));
        shownLines = Math.min(3, description.size());
        layoutGrid(12, 60 + shownLines * 10, width - 12, height - 44, 36, 28);
        addButton(new PonderButton(1, 8, height - 29, 22, 20, PonderButton.Icon.TAGS)
            .withTooltip("ponder.ui.catalog.theme_index").withCallback(() -> ScreenOpener.open(new PonderTagIndexScreen())));
        addCloseButton();
    }

    @Override
    protected void renderCatalogHeader(int mouseX, int mouseY, float partialTicks) {
        drawTitle(tag.getTitle() + "  (" + entries.size() + ")");
        for (int i = 0; i < shownLines; i++) {
            String line = description.get(i);
            if (i == shownLines - 1 && description.size() > shownLines)
                line = fontRenderer.trimStringToWidth(line, Math.min(440, width - 40) - 12) + "...";
            fontRenderer.drawString(line, (width - fontRenderer.getStringWidth(line)) / 2, 42 + i * 10, PonderThemes.color(0xffaeb8c2));
        }
    }

    @Override
    protected void renderWindowForeground(int mouseX, int mouseY, float partialTicks) {
        if (description.size() > shownLines && mouseY >= 40 && mouseY < 42 + shownLines * 10)
            drawHoveringText(description, mouseX, mouseY);
        else super.renderWindowForeground(mouseX, mouseY, partialTicks);
    }
}
