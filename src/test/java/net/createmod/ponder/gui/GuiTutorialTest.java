package net.createmod.ponder.gui;

import com.google.gson.*;
import java.nio.file.*;
import net.createmod.ponder.api.gui.GuiTutorial;
import net.createmod.ponder.api.gui.GuiTutorial.Stack;
import net.createmod.ponder.api.guide.Tutorial;
import net.createmod.ponder.guide.MarkdownTutorialCompiler;
import net.minecraft.util.ResourceLocation;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class GuiTutorialTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private GuiTutorial furnace()throws Exception{
        try(var stream=getClass().getResourceAsStream("/assets/ponder/ponder/gui/examples/furnace.json")){
            assertNotNull(stream);return GuiTutorialJson.parse(new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    @Test public void exampleRoundTripsWithoutChangingTargetsOrCompletions()throws Exception{
        GuiTutorial original=furnace();assertEquals(original,GuiTutorialJson.parse(GuiTutorialJson.write(original)));
        assertEquals(5,original.steps().size());assertEquals("input",original.targets().keySet().iterator().next());
    }
    @Test public void seekingAndReplayingReconstructsIndependentSlotState()throws Exception{
        var session=new GuiTutorialSession(furnace());assertEquals(Stack.EMPTY,session.demoSlot("input"));
        session.seek(3,80);assertEquals(new Stack("minecraft:iron_ingot",1),session.demoSlot("output"));
        session.seek(0,0);assertEquals(Stack.EMPTY,session.demoSlot("output"));assertEquals(Stack.EMPTY,session.demoSlot("input"));
        session.seek(0,20);assertTrue(session.complete(session.demo()));
        session.seek(4,80);assertTrue(session.complete(session.demo()));assertEquals(Stack.EMPTY,session.demoSlot("output"));
    }
    @Test public void pauseStopsTimeAndCompletionUsesObservedMachineState()throws Exception{
        var session=new GuiTutorialSession(furnace());session.playing(false);session.advanceDemo();assertEquals(0,session.tick());
        GuiTutorialSession.Observation missing=new GuiTutorialSession.Observation(){public Stack slot(String target){return null;}public String value(String key){return "";}public boolean clicked(String target){return false;}};
        assertFalse(session.complete(missing));session.seek(4,0);assertFalse("missing is not an empty slot",session.complete(missing));
        session.seek(1,80);assertTrue(session.complete(session.demo()));
        session.seek(1,0);assertFalse("a fuel click without burning is not completion",session.complete(session.demo()));
    }
    @Test public void invalidTargetsOperationsFieldsAndTimingAreRejected()throws Exception{
        String good=GuiTutorialJson.write(furnace());
        assertThrows(IllegalArgumentException.class,()->GuiTutorialJson.parse(good.replace("\"target\": \"input\"","\"target\": \"missing\"")));
        assertThrows(IllegalArgumentException.class,()->GuiTutorialJson.parse(good.replace("\"set_slot\"","\"send_packet\"")));
        assertThrows(IllegalArgumentException.class,()->GuiTutorialJson.parse(good.replace("\"at\": 20","\"at\": 9999")));
        assertThrows(IllegalArgumentException.class,()->GuiTutorialJson.parse(good.replace("\"format\": 1","\"unexpected\": true, \"format\": 1")));
        assertThrows(IllegalArgumentException.class,()->GuiTutorialJson.parse(good.replace("\"value\": \"1\"","\"value\": \"NaN\"")));
    }
    @Test public void actionsAreAppliedInTimeOrderRatherThanJsonArrayOrder()throws Exception{
        JsonObject root=GuiTutorialJson.tree(furnace());JsonObject step=root.getAsJsonArray("steps").get(0).getAsJsonObject();
        JsonArray actions=step.getAsJsonArray("demo");JsonObject late=actions.get(0).getAsJsonObject().deepCopy();late.addProperty("at",70);late.addProperty("item","minecraft:diamond");
        JsonArray reverse=new JsonArray();reverse.add(late);reverse.add(actions.get(0));step.add("demo",reverse);
        var session=new GuiTutorialSession(GuiTutorialJson.parse(root.toString()));session.seek(0,75);assertEquals("minecraft:diamond",session.demoSlot("input").item());
    }
    @Test public void markdownCanEmbedAndReferenceTheSameValidatedGuiDefinition()throws Exception{
        String source="# Machine\n\n```ponder-gui\n"+GuiTutorialJson.write(furnace())+"\n```\n\n<GuiTutorial id=\"ponder:examples/furnace\" />\n";
        var page=MarkdownTutorialCompiler.compile(new ResourceLocation("test:machine"),source,"test.md");
        assertEquals(2,page.blocks().stream().filter(b->b.kind()==Tutorial.Kind.GUI_TUTORIAL).count());
        String embedded=page.blocks().stream().filter(b->b.kind()==Tutorial.Kind.GUI_TUTORIAL).findFirst().orElseThrow().target();
        assertEquals(furnace(),GuiTutorialJson.parse(embedded));
    }
    @Test public void savesAreValidatedBeforeReplacingTheExistingFile()throws Exception{
        var root=temporary.newFolder().toPath();var tutorial=furnace();GuiTutorialFiles.save(root,tutorial);
        Path target=GuiTutorialFiles.path(root,tutorial.id());assertEquals(tutorial,GuiTutorialJson.parse(GuiTutorialFiles.read(target)));
        assertThrows(IllegalArgumentException.class,()->GuiTutorialFiles.path(root,new ResourceLocation("test:../outside")));
        Path outside=temporary.newFolder().toPath();Path link=root.resolve("linked");Files.createSymbolicLink(link,outside);
        assertThrows(java.io.IOException.class,()->GuiTutorialFiles.path(root,new ResourceLocation("linked:tutorial")));
    }
}
