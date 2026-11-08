package net.createmod.ponder.foundation.ui;

import javax.annotation.Nullable;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

public class PonderButton extends GuiButton {
    /** Pixel silhouettes stay sharp at every GUI scale and have no triangle winding dependency. */
    public enum Icon {
        LEFT("##        #", "##       ##", "##     ####", "##    #####", "##  #######",
            "## ########", "##  #######", "##    #####", "##     ####", "##       ##", "##        #"),
        RIGHT("#        ##", "##       ##", "####     ##", "#####    ##", "#######  ##",
            "######## ##", "#######  ##", "#####    ##", "####     ##", "##       ##", "#        ##"),
        PAUSE("###     ###", "###     ###", "###     ###", "###     ###", "###     ###",
            "###     ###", "###     ###", "###     ###", "###     ###", "###     ###", "###     ###"),
        PLAY("##         ", "####       ", "######     ", "########   ", "########## ",
            "###########", "########## ", "########   ", "######     ", "####       ", "##         "),
        REPLAY("   ######  ", "  ######## ", " ##     ###", "##       ##", "##         ",
            "##         ", "##  ##     ", "######   ##", " #####  ###", "  ######## ", "   ######  "),
        CLOSE("##       ##", "###     ###", " ###   ### ", "  ### ###  ", "   #####   ",
            "    ###    ", "   #####   ", "  ### ###  ", " ###   ### ", "###     ###", "##       ##"),
        INDEX("##  #######", "##  #######", "           ", "           ", "##  #######",
            "##  #######", "           ", "           ", "##  #######", "##  #######", "           "),
        TAGS(" #######   ", "#########  ", "##     ### ", "## ###  ###", "## ###  ###",
            "##      ###", "##     ### ", "#########  ", " #######   ", "           ", "           "),
        IDENTIFY("  #####    ", " ##   ##   ", "##     ##  ", "##     ##  ", "##     ##  ",
            " ##   ##   ", "  #####    ", "      ###  ", "       ### ", "        ###", "         ##"),
        GUIDE("##### #####", "#   # #   #", "# # # # # #", "# # # # # #", "#   # #   #",
            "# # # # # #", "# # # # # #", "#   # #   #", "##### #####", "    ###    ", "     #     "),
        GUI("###########", "#         #", "###########", "#         #", "# ### ### #",
            "# # # # # #", "# ### ### #", "#         #", "# ####### #", "#         #", "###########"),
        STAR("     #     ", "    ###    ", "    ###    ", "###########", " ######### ",
            "  #######  ", "  #######  ", "  ### ###  ", " ###   ### ", " ##     ## ", " #       # "),
        PROJECT("    ###    ", "  #######  ", "#### # ####", "##   #   ##", "#### # ####", "## ##### ##", "##   #   ##", "#### # ####", "  #######  ", "    ###    ", "###########"),
        MATERIALS("  #######  ", "  #     #  ", "###########", "#         #", "# ## #### #", "#         #", "# ## #### #", "#         #", "# ## #### #", "#         #", "###########"),
        SCENES("  #########", "  #       #", "######### #", "#       # #", "# ##### # #",
            "#       # #", "# ##### ###", "#       #  ", "# ##### #  ", "#       #  ", "#########  ");

        private final String[] pixels;
        Icon(String... pixels) { this.pixels = pixels; }
    }

    private Icon icon;
    private ItemStack item = ItemStack.EMPTY;
    private PonderTag tag;
    private Runnable callback;
    private boolean active;
    private String tooltipKey;

    public PonderButton(int id, int x, int y, int width, int height, Icon icon) {
        super(id, x, y, width, height, "");
        this.icon = icon;
    }

    public PonderButton showing(ItemStack stack) { item = stack == null ? ItemStack.EMPTY : stack.copy(); tag = null; return this; }
    public PonderButton showing(PonderTag value) { tag = value; item = value == null ? ItemStack.EMPTY : value.getItemIcon(); return this; }
    public PonderButton withCallback(Runnable value) { callback = value; return this; }
    public PonderButton active(boolean value) { active = value; return this; }
    public PonderButton enabled(boolean value) { enabled = value; return this; }
    public PonderButton withTooltip(String value) { tooltipKey = value; return this; }
    public PonderButton withLabel(String value) { displayString = value; return this; }
    public PonderButton withIcon(Icon value) { icon = value; return this; }
    public ItemStack getItem() { return item.copy(); }
    @Nullable public PonderTag getTag() { return tag; }
    @Nullable public String getTooltipKey() { return tooltipKey; }
    public boolean isHoveredButton() { return hovered; }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        boolean pressed = super.mousePressed(mc, mouseX, mouseY);
        if (pressed && callback != null) callback.run();
        return pressed;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        int background = !enabled ? 0xff20242a : active ? 0xff31525a : hovered ? 0xff3b4652 : 0xff292f36;
        int border = !enabled ? 0xff3d444c : active ? 0xff9fd6d2 : hovered ? 0xff9fb5c9 : 0xff58636e;
        Gui.drawRect(x, y, x + width, y + height, background);
        Gui.drawRect(x, y, x + width, y + 1, border);
        Gui.drawRect(x, y + height - 1, x + width, y + height, border);
        Gui.drawRect(x, y, x + 1, y + height, border);
        Gui.drawRect(x + width - 1, y, x + width, y + height, border);
        if (!item.isEmpty()) {
            GuiGameElement.of(item).render(x + width / 2, y + height / 2, enabled ? 1 : .35f);
            return;
        }
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.disableCull();
            int color = enabled ? 0xffe3e8ed : 0xff68727e;
            boolean labelled = !displayString.isEmpty();
            drawIcon(icon, labelled ? x + 11 : x + width / 2, y + height / 2,
                Math.min(labelled ? 13 : 16, Math.min(width - 4, height - 4)), color);
            if (labelled) {
                String label = mc.fontRenderer.trimStringToWidth(displayString, Math.max(0, width - (icon == null ? 8 : 38)));
                mc.fontRenderer.drawString(label, icon == null
                    ? x + (width - mc.fontRenderer.getStringWidth(label)) / 2 : x + 22, y + (height - 8) / 2, color);
                if (icon == Icon.SCENES) {
                    Gui.drawRect(x + width - 11, y + height / 2 - 1, x + width - 6, y + height / 2, color);
                    Gui.drawRect(x + width - 10, y + height / 2, x + width - 7, y + height / 2 + 1, color);
                    Gui.drawRect(x + width - 9, y + height / 2 + 1, x + width - 8, y + height / 2 + 2, color);
                }
            }
        }
    }

    public static void drawIcon(Icon icon, int cx, int cy, int size, int color) {
        if (icon == null) return;
        int left = cx - size / 2, top = cy - size / 2;
        int grid = icon.pixels.length;
        for (int row = 0; row < icon.pixels.length; row++) {
            String pixels = icon.pixels[row];
            for (int column = 0; column < pixels.length();) {
                if (pixels.charAt(column) == ' ') { column++; continue; }
                int start = column++;
                while (column < pixels.length() && pixels.charAt(column) != ' ') column++;
                Gui.drawRect(left + start * size / grid, top + row * size / grid,
                    left + column * size / grid, top + (row + 1) * size / grid, color);
            }
        }
    }
}
