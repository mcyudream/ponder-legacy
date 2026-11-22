package net.createmod.ponder.foundation.ui;

import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.createmod.ponder.api.theme.PonderTheme.Color;
import net.createmod.ponder.client.theme.PonderThemes;
import net.createmod.ponder.client.theme.PonderThemeRenderer;

public class PonderProgressBar extends AbstractSimiWidget {
    private final PonderUI ponder;

    public PonderProgressBar(int id, PonderUI ponder, int x, int y, int width, int height) {
        super(id, x, y, width, height, "");
        this.ponder = ponder;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        PonderScene scene = ponder.getActiveScene();
        if (PonderThemes.modern()) {
            hovered = containsModern(mouseX, mouseY);
            drawModern(mc, scene, mouseX);
            return;
        }
        hovered = mouseX >= x && mouseY >= y - 4 && mouseX < x + width && mouseY < y + height + 8;
        Gui.drawRect(x, y, x + width, y + height, PonderThemes.color(Color.PROGRESS_TRACK));
        int progress = Math.round(width * scene.getSceneProgress());
        Gui.drawRect(x, y, x + progress, y + height, PonderThemes.color(Color.PROGRESS_FILL));
        int total = Math.max(1, scene.getTotalTicks());
        for (Integer tick : scene.getKeyframes()) {
            int marker = x + Math.round(width * tick / (float) total);
            Gui.drawRect(marker - 1, y - 3, marker + 1, y + height + 3, PonderThemes.color(Color.KEYFRAME));
        }
    }

    private boolean containsModern(int mouseX, int mouseY) {
        return visible && mouseX >= x && mouseX < x + width + 4 && mouseY >= y - 3 && mouseY < y + height + 20;
    }

    private void drawModern(Minecraft mc, PonderScene scene, int mouseX) {
        PonderThemeRenderer.modernBox(x, y, width, height, PonderThemes.color(Color.PROGRESS_TRACK),
            PonderThemes.color(Color.PROGRESS_BORDER), PonderThemes.color(Color.PROGRESS_BORDER_BOTTOM));
        int fill = Math.round((width + 4) * scene.getSceneProgress());
        if (fill > 0) {
            Gui.drawRect(x - 2, y - 1, x - 2 + fill, y + 1, PonderThemes.color(Color.PROGRESS_FILL));
            Gui.drawRect(x - 2, y + 1, x - 2 + fill, y + 2, PonderThemes.color(Color.PROGRESS_FILL_BOTTOM));
        }
        int total = Math.max(1, scene.getTotalTicks()), selected = -2;
        var keyframes = scene.getKeyframes();
        if (hovered) {
            int time = Math.round((mouseX - x) * total / (float) (width + 4));
            selected = -1;
            for (int i = 0; i < keyframes.size(); i++) {
                if (keyframes.get(i) > time) break;
                selected = i;
            }
            if (!keyframes.isEmpty()) {
                int fromLast = time - keyframes.get(keyframes.size() - 1), toEnd = total - time;
                if (toEnd >= 0 && toEnd < fromLast / 2) selected = keyframes.size();
            }
        }
        if (selected == -1) marker(mc, scene, 0, x - 2, true);
        if (selected == keyframes.size()) marker(mc, scene, total, x + width + 2, true);
        for (int i = 0; i < keyframes.size(); i++) {
            int tick = keyframes.get(i), marker = x - 2 + (int) ((width + 2) * tick / (float) total);
            marker(mc, scene, tick, marker, i == selected);
        }
    }

    private void marker(Minecraft mc, PonderScene scene, int tick, int marker, boolean selected) {
        int color = PonderThemes.color(Color.KEYFRAME);
        if (selected) color = color & 0xffffff | 0xe0000000;
        Gui.drawRect(marker, y - 2, marker + 2, y + (selected ? 7 : 3), color);
        if (!selected) return;
        Gui.drawRect(marker, y + 7, marker + 2, y + 15, color);
        String arrow = scene.getCurrentTick() < tick ? "\u00a7l>" : "\u00a7l<";
        int offset = scene.getCurrentTick() < tick ? -2 - mc.fontRenderer.getStringWidth(arrow) : 4;
        mc.fontRenderer.drawString(arrow, marker + offset, y + 8, color);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (PonderThemes.modern() ? !enabled || !containsModern(mouseX, mouseY) : !super.mousePressed(mc, mouseX, mouseY))
            return false;
        seek(mouseX);
        return true;
    }

    public void dragTo(int mouseX) { seek(mouseX); }

    private void seek(int mouseX) {
        PonderScene scene = ponder.getActiveScene();
        float value = Math.max(0, Math.min(1, (mouseX - x) / (float) Math.max(1, width)));
        scene.seek(Math.round(scene.getTotalTicks() * value));
    }
}
