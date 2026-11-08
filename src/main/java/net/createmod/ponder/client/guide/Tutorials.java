package net.createmod.ponder.client.guide;

import java.util.*;
import net.createmod.catnip.config.*;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.guide.Tutorial;
import net.createmod.ponder.api.guide.Tutorial.*;
import net.createmod.ponder.config.CClient;
import net.createmod.ponder.foundation.*;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.createmod.ponder.foundation.registration.*;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.*;

@SideOnly(Side.CLIENT)
public final class Tutorials {
    private Tutorials() {}
    public static boolean exists(ResourceLocation component) {
        return PonderIndex.getSceneAccess().doScenesExistForId(component) || PonderIndex.getTutorials().containsComponent(component)
            || !net.createmod.ponder.client.guitutorial.GuiTutorials.forComponent(component).isEmpty();
    }
    public static boolean hasGuide(ResourceLocation id) {
        Tutorial page = PonderIndex.getTutorials().get(id);
        return page == null || page.presentation() != Presentation.ANIMATION;
    }
    public static boolean preferGuide() {
        CClient config=(CClient)ConfigRegistry.get(Ponder.MOD_ID,ConfigType.CLIENT);
        return config != null && config.preferredPresentation.get() == CClient.TutorialPresentation.GUIDE;
    }
    public static void setPreferGuide(boolean guide) {
        CClient config=(CClient)ConfigRegistry.get(Ponder.MOD_ID,ConfigType.CLIENT);
        if(config!=null)config.preferredPresentation.set(guide?CClient.TutorialPresentation.GUIDE:CClient.TutorialPresentation.ANIMATION);
    }
    public static GuiScreen open(ResourceLocation component) { return open(component, null, preferGuide()); }
    public static GuiScreen open(ResourceLocation component, ResourceLocation selected, boolean guide) {
        List<PonderScene> scenes=PonderIndex.getSceneAccess().compile(component);
        if (scenes.isEmpty() && pages(component).isEmpty()) {
            var gui=net.createmod.ponder.client.guitutorial.GuiTutorials.forComponent(component);
            if (!gui.isEmpty()) return new net.createmod.ponder.client.guitutorial.GuiDemoScreen(gui.get(0),()->{
                if (!net.createmod.catnip.gui.ScreenOpener.openPreviousScreen()) net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(null);
            },false);
        }
        if (!scenes.isEmpty() && (!guide || pages(component).isEmpty())) {
            PonderUI ui=new PonderUI(scenes);if(selected!=null)ui.selectScene(selected);return ui;
        }
        return new GuideScreen(component, selected);
    }
    public static List<Tutorial> pages(ResourceLocation component) {
        List<Tutorial> result = new ArrayList<>(PonderIndex.getTutorials().forComponent(component).stream()
            .filter(t -> t.presentation()!=Presentation.ANIMATION).toList());
        Set<ResourceLocation> represented = new HashSet<>();
        for(Tutorial page:result) { represented.add(page.id());if(page.animation()!=null)represented.add(page.animation()); }
        for(PonderScene scene:PonderIndex.getSceneAccess().compile(component)) {
            if(!represented.add(scene.getId()))continue;
            Tutorial authored=PonderIndex.getTutorials().get(scene.getId());
            if(authored!=null&&authored.presentation()==Presentation.ANIMATION)continue;
            result.add(fromScene(scene));
        }
        return result;
    }
    public static Tutorial fromScene(PonderScene scene) {
        List<Block> blocks=new ArrayList<>();blocks.add(Block.heading(scene.getTitle(),1));
        StringBuilder source=new StringBuilder("---\nid: "+scene.getId()+"\ntitle: "+new com.google.gson.Gson().toJson(scene.getTitle())+
            "\nitem_ids: ["+scene.getComponent()+"]\nscene: "+scene.getId()+"\npresentation: both\n---\n\n# "+scene.getTitle()+"\n");
        int step=0;
        for(PonderScene.Narration narration:scene.getNarration()) {
            String text=narration.text().get();int tick=Math.min(scene.getTotalTicks(),narration.tick()+Math.min(30,narration.duration()));
            blocks.add(Block.heading(Integer.toString(++step),2));blocks.add(Block.text(Kind.TEXT,text));
            blocks.add(Block.scene(new Snapshot(scene.getId(),tick,null,-35,25,1,true)));
            source.append("\n## ").append(step).append("\n\n").append(text).append("\n\n<GameScene scene=\"").append(scene.getId())
                .append("\" tick={").append(tick).append("} />\n");
        }
        if(step==0)blocks.add(Block.scene(new Snapshot(scene.getId(),Math.max(0,scene.getTotalTicks()-1),null,-35,25,1,true)));
        return new Tutorial(scene.getId(),scene.getTitle(),List.of(scene.getComponent()),List.of(),"", "animation",source.toString(),
            Presentation.BOTH,scene.getId(),blocks,List.of());
    }
    static PonderScene compile(Snapshot snapshot, ResourceLocation component) {
        if(snapshot.definition()!=null)return compile(snapshot.definition());
        var matches=PonderIndex.getSceneAccess().getRegisteredEntries().stream()
            .filter(e -> e.getKey().equals(component) && snapshot.scene().equals(e.getValue().getDeclaredSceneId())).map(Map.Entry::getValue).toList();
        List<PonderScene> scenes=matches.isEmpty()?PonderIndex.getSceneAccess().compile(component):PonderIndex.getSceneAccess().compile(matches);
        return scenes.stream().filter(s->s.getId().equals(snapshot.scene())).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Scene not registered: "+snapshot.scene()));
    }
    public static PonderScene compile(net.createmod.ponder.script.ScriptSceneDefinition definition) {
        PonderStoryBoardEntry entry=new PonderStoryBoardEntry(definition.asStoryBoard(),definition.getSceneId().getNamespace(),definition.getStructure(),definition.getComponent());
        entry.identifiedBy(definition.getSceneId());
        return new PonderSceneRegistry(new PonderLocalization()).compileEntry(entry);
    }
}
