package net.createmod.ponder.api.gui;

/** Read-only playback data supplied to complete GUI appearances. Present in the standalone API jar. */
public interface GuiDemoState {
    GuiTutorial tutorial();
    int stepIndex();
    GuiTutorial.Step step();
    int tick();
    GuiTutorial.Stack demoSlot(String target);
    String demoValue(String key);
}
