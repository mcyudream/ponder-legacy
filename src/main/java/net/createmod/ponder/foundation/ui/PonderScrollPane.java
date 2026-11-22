package net.createmod.ponder.foundation.ui;

import net.createmod.ponder.client.theme.PonderThemes;

import net.minecraft.client.gui.Gui;

/** Scrolls complete rows so rendered entries and their hit boxes always stay inside the pane. */
final class PonderScrollPane {
    int left, top, right, bottom;
    private int firstRow, rows, pageRows = 1;
    private boolean dragging;
    private int grabOffset;

    void layout(int left, int top, int right, int bottom, int rows, int rowHeight) {
        this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        this.rows = rows;
        pageRows = Math.max(1, (bottom - top) / rowHeight);
        setFirstRow(firstRow);
    }

    int firstRow() { return firstRow; }
    int pageRows() { return pageRows; }
    int maxFirstRow() { return Math.max(0, rows - pageRows); }
    boolean canScroll() { return maxFirstRow() > 0; }
    boolean contains(int x, int y) { return x >= left && x < right && y >= top && y < bottom; }
    void setFirstRow(int row) { firstRow = Math.max(0, Math.min(maxFirstRow(), row)); }
    void scroll(int delta) { setFirstRow(firstRow + delta); }
    void release() { dragging = false; }

    int thumbHeight() { return Math.min(bottom - top, Math.max(18, (bottom - top) * pageRows / Math.max(1, rows))); }
    int thumbTop() {
        return top + (canScroll() ? (bottom - top - thumbHeight()) * firstRow / maxFirstRow() : 0);
    }

    boolean click(int x, int y) {
        if (!canScroll() || !contains(x, y) || x < right - 10) return false;
        int thumb = thumbTop();
        grabOffset = y >= thumb && y < thumb + thumbHeight() ? y - thumb : thumbHeight() / 2;
        dragging = true;
        drag(y);
        return true;
    }

    boolean drag(int y) {
        if (!dragging) return false;
        int travel = bottom - top - thumbHeight();
        setFirstRow(travel <= 0 ? 0 : Math.round((y - top - grabOffset) * maxFirstRow() / (float) travel));
        return true;
    }

    void draw() {
        if (!canScroll()) return;
        Gui.drawRect(right - 7, top, right - 3, bottom, PonderThemes.color(0xff232c35));
        Gui.drawRect(right - 8, thumbTop(), right - 2, thumbTop() + thumbHeight(), dragging ? PonderThemes.color(0xffb8e3df) : PonderThemes.color(0xff78949f));
    }
}
