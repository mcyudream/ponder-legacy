package net.createmod.ponder.foundation.ui;

import javax.annotation.Nullable;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.foundation.PonderTag;
import net.createmod.ponder.api.theme.PonderTheme.Color;
import net.createmod.ponder.client.theme.PonderThemes;
import net.createmod.ponder.client.theme.PonderThemeRenderer;
import net.createmod.ponder.enums.PonderGuiTextures;
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
        THEME("   #####   ", " ######### ", "##  ##  ###", "##  ##  ###", "###########",
            "###  ###  #", "###  ###  #", "######    #", " #####   ##", "  ###   ## ", "   #####   "),
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
    private int shortcut = Integer.MIN_VALUE;

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
    public PonderButton withShortcut(int value) { shortcut = value; return this; }
    public ItemStack getItem() { return item.copy(); }
    @Nullable public PonderTag getTag() { return tag; }
    @Nullable public String getTooltipKey() { return tooltipKey; }
    public boolean isHoveredButton() { return hovered; }
    public boolean containsPointer(int px, int py) {
        int pad = PonderThemes.modern() ? 4 : 0;
        return visible && px >= x - pad && py >= y - pad && px < x + width + pad && py < y + height + pad;
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        boolean pressed = enabled && containsPointer(mouseX, mouseY);
        if (pressed && callback != null) callback.run();
        return pressed;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        hovered = containsPointer(mouseX, mouseY);
        int background = PonderThemes.color(!enabled ? Color.BUTTON_DISABLED : active ? Color.BUTTON_ACTIVE : hovered ? Color.BUTTON_HOVER : Color.BUTTON);
        int border = PonderThemes.color(!enabled ? Color.BORDER_DISABLED : active ? Color.BORDER_ACTIVE : hovered ? Color.BORDER_HOVER : Color.BORDER);
        int bottom = PonderThemes.color(!enabled ? Color.BORDER_DISABLED_BOTTOM : active ? Color.BORDER_ACTIVE_BOTTOM : hovered ? Color.BORDER_HOVER_BOTTOM : Color.BORDER_BOTTOM);
        if (PonderThemes.modern()) PonderThemeRenderer.modernBox(x, y, width, height, background, border, bottom);
        else PonderThemeRenderer.box(x, y, width, height, background, border, bottom);
        if (!item.isEmpty()) {
            GuiGameElement.of(item).scale(PonderThemes.modern() ? 1.5f : 1)
                .render(x + width / 2, y + height / 2, enabled ? 1 : .35f);
            return;
        }
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.disableCull();
            int color = PonderThemes.color(enabled ? Color.ICON : Color.ICON_DISABLED);
            boolean labelled = !displayString.isEmpty();
            PonderGuiTextures nativeIcon = PonderThemes.modern() && !labelled ? nativeIcon(icon) : null;
            if (nativeIcon != null) {
                GlStateManager.enableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.color((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f,
                    (color & 255) / 255f, (color >>> 24) / 255f);
                nativeIcon.render(x + (width - 16) / 2, y + (height - 16) / 2);
            } else drawIcon(icon, labelled ? x + 11 : x + width / 2, y + height / 2,
                Math.min(labelled ? 13 : 16, Math.min(width - 4, height - 4)), color);
            if (labelled) {
                int reserved = icon == null ? 8 : PonderThemes.modern() && icon == Icon.SCENES ? 35 : 38;
                String label = mc.fontRenderer.trimStringToWidth(displayString, Math.max(0, width - reserved));
                mc.fontRenderer.drawString(label, icon == null
                    ? x + (width - mc.fontRenderer.getStringWidth(label)) / 2 : x + 22, y + (height - 8) / 2, color);
                if (icon == Icon.SCENES) {
                    Gui.drawRect(x + width - 11, y + height / 2 - 1, x + width - 6, y + height / 2, color);
                    Gui.drawRect(x + width - 10, y + height / 2, x + width - 7, y + height / 2 + 1, color);
                    Gui.drawRect(x + width - 9, y + height / 2 + 1, x + width - 8, y + height / 2 + 2, color);
                }
            }
            if (PonderThemes.modern() && shortcut != Integer.MIN_VALUE) {
                String key = net.minecraft.client.settings.GameSettings.getKeyDisplayString(shortcut)
                    .toLowerCase(java.util.Locale.ROOT);
                key = mc.fontRenderer.trimStringToWidth(key, 16);
                mc.fontRenderer.drawString(key, x + width / 2 + 8 - mc.fontRenderer.getStringWidth(key) / 2,
                    y + height - 6, PonderThemes.color(Color.MUTED));
            }
        }
    }

    private static PonderGuiTextures nativeIcon(Icon icon) {
        if (icon == null) return null;
        return switch (icon) {
            case LEFT -> PonderGuiTextures.ICON_PONDER_LEFT;
            case RIGHT -> PonderGuiTextures.ICON_PONDER_RIGHT;
            case REPLAY -> PonderGuiTextures.ICON_PONDER_REPLAY;
            case CLOSE -> PonderGuiTextures.ICON_PONDER_CLOSE;
            case IDENTIFY -> PonderGuiTextures.ICON_PONDER_IDENTIFY;
            default -> null;
        };
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
