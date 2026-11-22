package net.createmod.ponder.client.theme;

import net.createmod.ponder.api.theme.PonderTheme.Color;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Shared fixed-pipeline chrome; no modern Minecraft renderer is required. */
@SideOnly(Side.CLIENT)
public final class PonderThemeRenderer extends Gui {
    private static final PonderThemeRenderer DRAW = new PonderThemeRenderer();
    private PonderThemeRenderer() {}

    public static void gradient(int x, int y, int right, int bottom, int topColor, int bottomColor) {
        if (right <= x || bottom <= y) return;
        if (topColor == bottomColor) Gui.drawRect(x, y, right, bottom, topColor);
        else DRAW.drawGradientRect(x, y, right, bottom, topColor, bottomColor);
    }

    public static void background(int width, int height) {
        int top = PonderThemes.color(Color.BACKGROUND_TOP), bottom = PonderThemes.color(Color.BACKGROUND_BOTTOM);
        if (PonderThemes.modern() && !PonderThemeBackdrop.blur()) { top |= 0xff000000; bottom |= 0xff000000; }
        gradient(0, 0, width, height, top, bottom);
        Gui.drawRect(0, 0, width, 34, PonderThemes.color(Color.HEADER));
        Gui.drawRect(0, height - 38, width, height, PonderThemes.color(Color.FOOTER));
    }

    public static void box(int x, int y, int width, int height, int fill, int top, int bottom) {
        Gui.drawRect(x, y, x + width, y + height, fill);
        Gui.drawRect(x, y, x + width, y + 1, top);
        Gui.drawRect(x, y + height - 1, x + width, y + height, bottom);
        gradient(x, y + 1, x + 1, y + height - 1, top, bottom);
        gradient(x + width - 1, y + 1, x + width, y + height - 1, top, bottom);
    }

    /** Matches Catnip BoxElement's two-pixel padding, gradient inner edge and black outer edge. */
    public static void modernBox(int x, int y, int width, int height, int fill, int top, int bottom) {
        Gui.drawRect(x - 3, y - 4, x + width + 3, y - 3, fill);
        Gui.drawRect(x - 4, y - 3, x - 3, y + height + 3, fill);
        Gui.drawRect(x - 3, y + height + 3, x + width + 3, y + height + 4, fill);
        Gui.drawRect(x + width + 3, y - 3, x + width + 4, y + height + 3, fill);
        Gui.drawRect(x - 3, y - 3, x + width + 3, y + height + 3, fill);
        Gui.drawRect(x - 3, y - 3, x + width + 3, y - 2, top);
        gradient(x - 3, y - 2, x - 2, y + height + 2, top, bottom);
        Gui.drawRect(x - 3, y + height + 2, x + width + 3, y + height + 3, bottom);
        gradient(x + width + 2, y - 2, x + width + 3, y + height + 2, top, bottom);
    }

    /** Horizontal three-stop fading streak; the upstream title also has a short mirrored tail. */
    public static void streak(int x, int y, int width, int height) {
        int color = PonderThemes.color(Color.STREAK);
        int alpha = color >>> 24;
        for (int column = -30; column < width; column++) {
            float t = column < 0 ? -column / 30f : column / (float) Math.max(1, width);
            float fade = t < .5f ? .625f - t * .25f
                : t < .75f ? .5f - (t - .5f) * 1.75f : .0625f * (1 - t) * 4;
            Gui.drawRect(x + column, y, x + column + 1, y + height,
                Math.round(alpha * fade) << 24 | color & 0xffffff);
        }
        GlStateManager.color(1, 1, 1, 1);
    }
}
