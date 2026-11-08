package net.createmod.ponder.gui;

import com.google.gson.*;
import java.util.List;
import net.createmod.ponder.api.editor.EditorActions;
import net.createmod.ponder.api.gui.GuiTutorial.Rect;
import net.createmod.ponder.foundation.*;
import net.createmod.ponder.foundation.element.GuiAnimationElement;
import net.createmod.ponder.foundation.registration.*;
import net.createmod.ponder.guide.MarkdownTutorialCompiler;
import net.createmod.ponder.script.*;
import net.minecraft.util.ResourceLocation;
import org.junit.Test;
import static org.junit.Assert.*;

public class GuiAnimationTest {
    private PonderScene scene(){return new PonderScene(null,new PonderLocalization(),"test",new ResourceLocation("minecraft:furnace"),List.of(),List.of());}
    private PonderScene compile(ScriptSceneDefinition definition){
        var entry=new PonderStoryBoardEntry(definition.asStoryBoard(),"test",new ResourceLocation("ponder:empty"),new ResourceLocation("minecraft:furnace"));
        return PonderSceneRegistry.compileScene(new PonderLocalization(),entry,null);
    }
    private String example()throws Exception{
        try(var in=getClass().getResourceAsStream("/assets/ponder/ponder/gui/examples/furnace.json")){return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
    }
    @Test public void sceneClockControlsClipsAcrossPauseSeekSnapshotAndReplay(){
        var s=scene();var b=s.builder();b.idle(10);b.overlay().showGui(5,new ResourceLocation("test:machine")).frames(40,80);
        b.idle(2);b.addKeyframe();b.idle(10);s.begin();
        s.seek(10);assertTrue(s.getActiveGuiAnimations().isEmpty());s.tick();var clip=s.getActiveGuiAnimations().get(0);assertEquals(40,clip.tutorialTick(s,100));
        s.setPaused(true);s.tick();assertEquals(11,s.getCurrentTick());s.seek(13);assertTrue(s.isPaused());assertEquals(60,clip.tutorialTick(s,100));
        s.seek(15);assertEquals(80,clip.tutorialTick(s,100));s.seek(16);assertTrue(s.getActiveGuiAnimations().isEmpty());
        s.seek(13);assertSame(clip,s.getActiveGuiAnimations().get(0));assertEquals(60,clip.tutorialTick(s,100));
        s.restart();assertTrue(s.getActiveGuiAnimations().isEmpty());s.seek(11);assertEquals(40,clip.tutorialTick(s,100));
    }
    @Test public void aOneTickClipIsVisibleForOneFrameAndDoesNotRequireClientResources(){
        var s=scene();s.builder().overlay().showGui(1,new ResourceLocation("absent:machine")).holdFrame(30);s.begin();
        assertEquals(2,s.getTotalTicks());s.tick();assertEquals(1,s.getActiveGuiAnimations().size());assertEquals(30,s.getActiveGuiAnimations().get(0).tutorialTick(s,100));
        assertFalse(s.isPaused());s.tick();assertTrue(s.getActiveGuiAnimations().isEmpty());assertTrue(s.isFinished());
    }
    @Test public void independentGuiFramesReconstructAllEarlierActions()throws Exception{
        var session=new GuiTutorialSession(GuiTutorialJson.parse(example()));
        session.seekAbsolute(81);assertEquals(1,session.stepIndex());assertEquals(0,session.tick());assertEquals("minecraft:iron_ore",session.demoSlot("input").item());
        session.seekAbsolute(session.lastTick());assertEquals(4,session.stepIndex());assertEquals(0,session.demoSlot("output").count());
        session.seekAbsolute(20);assertEquals(0,session.stepIndex());assertEquals("minecraft:iron_ore",session.demoSlot("input").item());assertEquals(0,session.demoSlot("output").count());
    }
    @Test public void sidePanelsReserveSpaceAndCanCoexistAtSmallResolutions(){
        var left=new GuiAnimationElement(new ResourceLocation("test:left"),null,0,40);left.onLeft();
        var right=new GuiAnimationElement(new ResourceLocation("test:right"),null,0,40);
        for(var clips:List.of(List.of(right),List.of(left,right),List.of(right,new GuiAnimationElement(new ResourceLocation("test:second"),null,0,40)))){
            var stage=new Rect(0,34,320,168);var layout=GuiAnimationLayout.arrange(stage,clips);assertTrue(layout.world().width()>100);
            for(var panel:layout.panels()){
                Rect p=panel.bounds();assertTrue(p.x()>=0&&p.y()>=34&&p.x()+p.width()<=320&&p.y()+p.height()<=202);
                assertTrue(p.x()+p.width()<=layout.world().x()||p.x()>=layout.world().x()+layout.world().width());
            }
        }
    }
    @Test public void markdownEmbedsGuiIntoAnimationAndHonorsPresentationSpecificContent()throws Exception{
        String header="---\nitem_ids: [minecraft:furnace]\nstructure: ponder:empty\nstep_ticks: 40\n---\n";
        var page=MarkdownTutorialCompiler.compile(new ResourceLocation("test:machine"),header+"## GUI\n\n```ponder-gui\n"+example()+"\n```\n\n## Continue\n\nContinue building.\n","gui.md");
        var s=compile(page.scenes().get(0));assertEquals(1,s.getGuiAnimations().size());assertNotNull(s.getGuiAnimations().get(0).definition());
        s.seek(30);assertEquals(1,s.getActiveGuiAnimations().size());assertFalse(s.isPaused());s.seek(s.getTotalTicks());assertTrue(s.getActiveGuiAnimations().isEmpty());
        var reference=MarkdownTutorialCompiler.compile(new ResourceLocation("test:ref"),header+"<GuiTutorial id=\"test:gui\" duration={60} side=\"left\" from={30} to={30} />","ref.md");
        var ref=compile(reference.scenes().get(0));assertEquals(GuiAnimationElement.Placement.LEFT,ref.getGuiAnimations().get(0).placement());ref.seek(21);assertEquals(30,ref.getGuiAnimations().get(0).tutorialTick(ref,100));
        var guideOnly=MarkdownTutorialCompiler.compile(new ResourceLocation("test:guide"),header+"<GuideOnly>\n\n<GuiTutorial id=\"test:gui\" />\n\n</GuideOnly>","guide.md");assertTrue(compile(guideOnly.scenes().get(0)).getGuiAnimations().isEmpty());
    }
    @Test public void jsonAndEditorUseTheSameValidatedClipOperation(){
        var action=EditorActions.all(null).stream().filter(a->a.id().equals("overlay.show_gui")).findFirst().orElseThrow().template();
        var root=new JsonParser().parse("{\"format\":1,\"id\":\"test:pack\",\"scenes\":[{\"id\":\"test:machine\",\"component\":\"minecraft:furnace\",\"title\":\"GUI\",\"structure\":\"ponder:empty\",\"instructions\":[]}]}").getAsJsonObject();
        root.getAsJsonArray("scenes").get(0).getAsJsonObject().getAsJsonArray("instructions").add(action);
        var s=compile(PonderJsonLoader.parseScenes(root,"gui.json").get(0));assertEquals(160,s.getGuiAnimations().get(0).duration());
        action.addProperty("side","fullscreen");assertThrows(IllegalArgumentException.class,()->PonderJsonLoader.parseScenes(root,"gui.json"));action.addProperty("side","left");
        action.addProperty("from",40);action.addProperty("to",20);assertThrows(IllegalArgumentException.class,()->PonderJsonLoader.parseScenes(root,"gui.json"));action.remove("from");action.remove("to");
        action.addProperty("x",.5);assertThrows(IllegalArgumentException.class,()->PonderJsonLoader.parseScenes(root,"gui.json"));action.addProperty("y",.25);PonderJsonLoader.parseScenes(root,"gui.json");
    }
    @Test public void editorInsertsACompleteSegmentBeforeFinishAndUndoIsAtomic(){
        var document=net.createmod.ponder.api.editor.SceneDocument.create("test:gui_editor","minecraft:furnace","GUI","ponder:demo/basics");
        var original=document.snapshot();var clip=EditorActions.all(null).stream().filter(a->a.id().equals("overlay.show_gui")).findFirst().orElseThrow().template();
        int index=net.createmod.ponder.api.editor.EditorAnimations.insertGuiClip(document,document.steps().size(),clip);
        assertEquals(2,index);assertEquals("overlay.show_gui",document.steps().get(2).getAsJsonObject().get("op").getAsString());
        assertEquals(160,document.steps().get(3).getAsJsonObject().get("ticks").getAsInt());assertEquals(180,document.stepStartTick(4));
        document.undo();assertEquals(original,document.snapshot());document.redo();assertEquals(5,document.steps().size());
        clip.addProperty("duration",0);var good=document.snapshot();assertThrows(IllegalArgumentException.class,()->net.createmod.ponder.api.editor.EditorAnimations.insertGuiClip(document,2,clip));assertEquals(good,document.snapshot());
    }
    @Test public void publishedJsonAndMarkdownExamplesCompile()throws Exception{
        var pack=new JsonParser().parse(java.nio.file.Files.readString(java.nio.file.Path.of("examples/gui/furnace.ponder.json"))).getAsJsonObject();
        var scenes=PonderJsonLoader.parseScenes(pack,"furnace.ponder.json");assertEquals(1,scenes.size());assertTrue(scenes.get(0).getInstructions().stream().anyMatch(i->i.getOperation().equals("show_gui")));
        assertEquals("set_blocks",scenes.get(0).getInstructions().get(0).getOperation());
        var page=MarkdownTutorialCompiler.compile(new ResourceLocation("example:furnace_gui_markdown"),java.nio.file.Files.readString(java.nio.file.Path.of("examples/gui/furnace-animation.md")),"furnace-animation.md");
        assertEquals(2,page.scenes().get(0).getInstructions().stream().filter(i->i.getOperation().equals("show_gui")).count());
    }
}
