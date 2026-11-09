package net.createmod.ponder.api.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Optional client integration. Reads widget geometry and synchronized state; never performs GUI actions. */
@SideOnly(Side.CLIENT)
public interface GuiTutorialAdapter {
    boolean matches(GuiScreen screen,GuiTutorial tutorial);
    GuiTutorial.Rect window(GuiScreen screen,GuiTutorial tutorial);
    GuiTutorial.Rect target(GuiScreen screen,GuiTutorial tutorial,String key);
    /** null denotes an unresolved target. */
    GuiTutorial.Stack slot(GuiScreen screen,GuiTutorial tutorial,String key);
    /** Whether an observed click can complete this target; custom widgets need not use numeric button IDs. */
    default boolean enabled(GuiScreen screen,GuiTutorial tutorial,String key){return true;}
    default String value(GuiScreen screen,String key){return "";}
    default boolean textFocused(GuiScreen screen){return false;}
    /** Complete passive appearance; Ponder paints its layers and resolves highlights from the same tree. */
    default GuiDemoLayout demoLayout(net.createmod.ponder.gui.GuiTutorialSession session){return null;}
}
