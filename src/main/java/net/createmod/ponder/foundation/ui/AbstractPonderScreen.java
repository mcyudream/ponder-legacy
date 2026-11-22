package net.createmod.ponder.foundation.ui;

import net.createmod.catnip.gui.NavigatableSimiScreen;
import net.createmod.ponder.client.theme.PonderThemeRenderer;
import net.createmod.ponder.client.theme.PonderThemes;
import net.createmod.ponder.client.theme.PonderThemeScreen;

public abstract class AbstractPonderScreen extends NavigatableSimiScreen {
    private long themeRevision = PonderThemes.revision();
    protected void addThemeButton(int id, int x, int y) {
        addButton(new PonderButton(id, x, y, 22, 20, PonderButton.Icon.THEME)
            .withTooltip("ponder.theme.title").withCallback(() -> mc.displayGuiScreen(new PonderThemeScreen(this))));
    }

    @Override
    protected void renderWindowBackground(int mouseX,int mouseY,float partialTicks) {
        if (themeRevision != PonderThemes.revision()) { themeRevision = PonderThemes.revision(); initGui(); }
        PonderThemeRenderer.background(width, height);
    }
}
