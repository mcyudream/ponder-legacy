import net.createmod.ponder.api.gui.GuiDemoLayout;
import net.createmod.ponder.api.gui.GuiDemoState;
import net.createmod.ponder.api.gui.GuiTutorial.Rect;
public final class LayoutSdkProbe {
    public GuiDemoLayout build(GuiDemoState state) {
        var builder=GuiDemoLayout.builder(200,160);
        builder.root().group(12,8).node("field",new Rect(8,8,61,12),GuiDemoLayout.CONTROL,()->state.demoValue("field"));
        return builder.build();
    }
}
