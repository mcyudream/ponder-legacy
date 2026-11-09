package net.createmod.ponder.gui;

import java.util.ArrayList;
import java.util.List;
import net.createmod.ponder.api.gui.GuiDemoLayout;
import net.createmod.ponder.api.gui.GuiTutorial.Rect;
import org.junit.Test;
import static org.junit.Assert.*;

public class GuiDemoLayoutTest {
    @Test public void nestedControlsAndHighlightsShareOneOrigin() {
        var b=GuiDemoLayout.builder(300,200);var panel=b.root().group(23,9).group(40,30);
        panel.node("input",new Rect(3,4,61,12),GuiDemoLayout.CONTROL,()->{});
        var layout=b.build();
        assertEquals(new Rect(66,43,61,12),layout.target("input"));
        assertEquals(layout.nodes().getFirst().bounds(),layout.target("input"));
    }
    @Test public void clippingFollowsNestedGroupsAndTargets() {
        var b=GuiDemoLayout.builder(100,100);
        b.root().group(10,20).clip(new Rect(0,0,40,30)).node("row",new Rect(30,20,30,20),0,()->{});
        var layout=b.build();assertEquals(new Rect(40,40,10,10),layout.target("row"));
        assertEquals(new Rect(10,20,40,30),layout.nodes().getFirst().clip());
    }
    @Test public void popupPaintsAfterForegroundRegardlessOfRegistrationOrder() {
        var b=GuiDemoLayout.builder(100,100);var order=new ArrayList<String>();var root=b.root();
        root.node("popup",new Rect(1,1,10,10),GuiDemoLayout.MODAL,()->order.add("popup"));
        root.node("label",new Rect(1,1,10,10),GuiDemoLayout.TEXT,()->order.add("label"));
        root.node("background",new Rect(1,1,10,10),GuiDemoLayout.BACKGROUND,()->order.add("background"));
        b.build().nodes().forEach(node->node.paint().run());assertEquals(List.of("background","label","popup"),order);
    }
    @Test public void hiddenAndClippedOutControlsNeverLeaveGhostHighlights() {
        var b=GuiDemoLayout.builder(100,100);var root=b.root();
        root.region("closedPopup",new Rect(1,1,10,10)).hideTarget("closedPopup");
        root.region("outside",new Rect(1,1,10,10)).node("outside",new Rect(200,200,10,10),0,()->{});
        var layout=b.build();assertNull(layout.target("closedPopup"));assertNull(layout.target("outside"));assertTrue(layout.nodes().isEmpty());
    }
    @Test public void actualControlReplacesAuthoredApproximation() {
        var b=GuiDemoLayout.builder(100,100);var root=b.root();
        root.region("field",new Rect(1,1,30,30));root.group(10,5).node("field",new Rect(2,3,61,12),0,()->{});
        assertEquals(new Rect(12,8,61,12),b.build().target("field"));
    }
}
