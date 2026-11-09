package net.createmod.ponder.client.guitutorial;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import net.createmod.ponder.api.gui.GuiTutorial;
import net.createmod.ponder.api.gui.GuiTutorial.Rect;
import net.createmod.ponder.api.guide.Tutorial;
import net.createmod.ponder.client.guide.*;
import net.createmod.ponder.gui.*;
import net.createmod.ponder.guide.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.init.*;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.*;

/** Actual furnace/container synchronization, overlay input isolation and framebuffer acceptance. */
public final class GuiTutorialSmoke {
    private final Minecraft mc;
    private final BiConsumer<Boolean,String> check;
    private final Consumer<String> screenshot;
    private GuiTutorial lesson;
    private GuiFurnace furnace;
    private GuiDemoScreen demo;
    private GuiStage stage;
    private String sceneInventory;
    private CompletableFuture<Void> pending;
    private int phase,wait,window;
    private String inventory;
    private Object beforeDisconnectScenes;
    private final BlockPos pos=new BlockPos(1,65,0);
    public GuiTutorialSmoke(Minecraft mc,BiConsumer<Boolean,String> check,Consumer<String> screenshot){this.mc=mc;this.check=check;this.screenshot=screenshot;}
    public boolean advance()throws Exception{
        if(wait-->0)return false;
        if(pending!=null){if(!pending.isDone())return false;pending.join();pending=null;}
        switch(phase){
            case 0 -> {
                language("zh_cn");mc.displayGuiScreen(null);
                var source=GuiTutorials.furnaceExample();lesson=new GuiTutorial(new ResourceLocation("smoke:furnace_gui"),source.title(),source.components(),source.screen(),source.adapter(),source.width(),source.height(),source.texture(),source.targets(),source.steps());
                GuiTutorials.save(lesson);check.accept(GuiTutorials.forComponent(new ResourceLocation("minecraft:furnace")).contains(lesson),"saved GUI lesson is discoverable from its machine item");
                server(()->{
                    var world=mc.getIntegratedServer().getWorld(0);for(BlockPos floor:BlockPos.getAllInBox(pos.add(-3,-1,-3),pos.add(3,-1,4)))world.setBlockState(floor,Blocks.STONE.getDefaultState());world.setBlockState(pos,Blocks.FURNACE.getDefaultState());
                    var player=mc.getIntegratedServer().getPlayerList().getPlayers().get(0);player.setPositionAndUpdate(1.5,66,2.5);
                    player.capabilities.isFlying=true;player.sendPlayerAbilities();
                    player.inventory.setInventorySlotContents(0,new ItemStack(Blocks.IRON_ORE));player.inventory.setInventorySlotContents(1,new ItemStack(Items.COAL));
                    player.displayGUIChest((TileEntityFurnace)world.getTileEntity(pos));
                });phase++;
            }
            case 1 -> {
                if(!(mc.currentScreen instanceof GuiFurnace))return false;furnace=(GuiFurnace)mc.currentScreen;window=mc.player.openContainer.windowId;
                check.accept(window>0,"real server opens the native furnace container");
                Rect r=new StandardGuiAdapter().target(furnace,lesson,"input"),w=new StandardGuiAdapter().window(furnace,lesson);
                check.accept(r.x()==w.x()+56&&r.y()==w.y()+17,"live targets use the native slot geometry rather than screen pixels");
                GuiTutorialEvents.stop();GuiTutorialEvents.tick(false);for(int i=0;i<18;i++)GuiTutorialEvents.tick(true);
                check.accept(GuiTutorialEvents.liveSession()!=null&&mc.currentScreen==furnace,"holding the configured Ponder key activates a live overlay on the same screen");
                check.accept(mc.player.openContainer.windowId==window,"starting live guidance keeps the native server container open");
                screenshot.accept("gui-live-furnace-zh.png");phase++;
            }
            case 2 -> {
                inventory=fingerprint();GuiTutorialEvents.preview(lesson);demo=field("demo");demo.session().playing(false);demo.session().seek(3,80);
                check.accept(demo.session().demoSlot("output").item().equals("minecraft:iron_ingot")&&mc.player.openContainer.getSlot(2).getStack().isEmpty(),"demo output exists only in independent demonstration state");
                var mouse=new GuiScreenEvent.MouseInputEvent.Pre(furnace);GuiTutorialEvents.INSTANCE.mouse(mouse);
                var keyboard=new GuiScreenEvent.KeyboardInputEvent.Pre(furnace);GuiTutorialEvents.INSTANCE.keyboard(keyboard);
                check.accept(mouse.isCanceled()&&keyboard.isCanceled(),"embedded demonstration intercepts native mouse and keyboard input");
                screenshot.accept("gui-demo-embedded-zh.png");phase++;
            }
            case 3 -> {
                demo.key('\0',Keyboard.KEY_ESCAPE);
                check.accept(mc.currentScreen==furnace&&mc.player.openContainer.windowId==window,"exiting the embedded demo returns to the open machine without reopening it; screen="+(mc.currentScreen==null?"none":mc.currentScreen.getClass().getSimpleName())+", window="+mc.player.openContainer.windowId+", expected="+window);
                check.accept(inventory.equals(fingerprint()),"demo playback and exit preserve all native slots and carried items");
                check.accept(GuiTutorialEvents.liveSession().stepIndex()==0,"viewing a demo does not complete the actual live operation");
                mc.playerController.windowClick(window,30,0,ClickType.PICKUP,mc.player);mc.playerController.windowClick(window,0,0,ClickType.PICKUP,mc.player);
                wait=10;phase++;
            }
            case 4 -> {
                if(GuiTutorialEvents.liveSession().stepIndex()<1)return false;
                check.accept(GuiTutorialEvents.liveSession().stepIndex()==1,"live guide advances after the real iron-ore slot synchronizes");
                mc.playerController.windowClick(window,31,0,ClickType.PICKUP,mc.player);mc.playerController.windowClick(window,1,0,ClickType.PICKUP,mc.player);
                phase++;
            }
            case 5 -> {
                if(GuiTutorialEvents.liveSession().stepIndex()<3)return false;
                check.accept(!new StandardGuiAdapter().value(furnace,"inventory_field.0").equals("0"),"fuel completion observes real burn time even after the fuel item is consumed");
                screenshot.accept("gui-live-wait-output-zh.png");phase++;
            }
            case 6 -> {
                if(GuiTutorialEvents.liveSession().stepIndex()<4)return false;
                check.accept(mc.player.openContainer.getSlot(2).getStack().getItem()==Items.IRON_INGOT,"guide waits for the actual furnace recipe output");
                mc.playerController.windowClick(window,2,0,ClickType.QUICK_MOVE,mc.player);wait=10;phase++;
            }
            case 7 -> {
                check.accept(mc.player.openContainer.getSlot(2).getStack().isEmpty(),"real native output collection clears the synchronized output slot");
                check.accept(GuiTutorialEvents.liveSession().complete(new GuiTutorialSession.Observation(){public GuiTutorial.Stack slot(String target){return new StandardGuiAdapter().slot(furnace,lesson,target);}public String value(String key){return "";}public boolean clicked(String key){return false;}}),"final live completion is based on the observed empty slot");
                GuiTutorialEvents.capture(furnace);GuiTutorialEditorScreen editor=field("editor");
                Field id=GuiTutorialEditorScreen.class.getDeclaredField("id");id.setAccessible(true);((GuiTextField)id.get(editor)).setText("smoke:captured_gui");
                inventory=fingerprint();var mouse=new GuiScreenEvent.MouseInputEvent.Pre(furnace);GuiTutorialEvents.INSTANCE.mouse(mouse);
                check.accept(mouse.isCanceled(),"GUI capture mode intercepts container clicks while authoring");
                Rect input=new StandardGuiAdapter().target(furnace,lesson,"input");editor.mouse(input.x()+3,input.y()+3,0,true);editor.mouse(input.x()+3,input.y()+3,0,false);
                Rect progress=new StandardGuiAdapter().target(furnace,lesson,"progress");editor.mouse(progress.x(),progress.y(),1,true);editor.mouse(progress.x()+progress.width(),progress.y()+progress.height(),1,false);editor.save();
                check.accept(inventory.equals(fingerprint()),"selecting controls, marking a region and saving do not change the machine inventory");
                GuiTutorial saved=GuiTutorials.resolve("smoke:captured_gui");check.accept(saved.targets().values().stream().anyMatch(t->t.type()==GuiTutorial.TargetType.REGION),"captured regions survive validated local save and reload");
                screenshot.accept("gui-capture-editor-zh.png");phase++;
            }
            case 8 -> {
                GuiTutorialEditorScreen editor=field("editor");editor.key('\0',Keyboard.KEY_ESCAPE);
                check.accept(mc.currentScreen==furnace&&mc.player.openContainer.windowId==window,"closing capture retains the same native machine container");
                Display.setDisplayMode(new DisplayMode(854,480));mc.resize(854,480);GuiTutorialEvents.start(furnace,lesson);
                Rect r=new StandardGuiAdapter().target(furnace,lesson,"input"),w=new StandardGuiAdapter().window(furnace,lesson);
                check.accept(r.x()==w.x()+56&&r.y()==w.y()+17,"machine highlighting follows the resized GUI origin");
                screenshot.accept("gui-live-small-zh.png");phase++;
            }
            case 9 -> {
                GuiTutorialEvents.stop();language("en_us");
                demo=new GuiDemoScreen(lesson,()->mc.displayGuiScreen(null),false);mc.displayGuiScreen(demo);demo.session().playing(false);demo.session().seek(2,70);
                check.accept(!demo.session().playing()&&demo.session().demoValue("inventory_field.2").equals("200"),"standalone demo supports deterministic pause and seeking");
                screenshot.accept("gui-demo-en.png");phase++;
            }
            case 10 -> {
                Display.setDisplayMode(new DisplayMode(320,240));mc.resize(320,240);
                Rect bounds=demo.machineBounds();check.accept(bounds.x()>=0&&bounds.y()>=34&&bounds.x()+bounds.width()<demo.width&&bounds.y()+bounds.height()<=demo.height-52,"small-screen demonstration keeps the machine above the operation hint and playback controls");
                screenshot.accept("gui-demo-small-en.png");phase++;
            }
            case 11 -> {
                String source="---\nid: smoke:gui_markdown\nitem_ids: [minecraft:furnace]\npresentation: guide\n---\n# GUI\n\n```ponder-gui\n"+GuiTutorialJson.write(lesson)+"\n```\n";
                Tutorial page=MarkdownTutorialCompiler.compile(new ResourceLocation("smoke:gui_markdown"),source,"gui.md");
                check.accept(page.blocks().stream().anyMatch(b->b.kind()==Tutorial.Kind.GUI_TUTORIAL),"Markdown compilation includes an interactive GUI demonstration entry");
                mc.displayGuiScreen(new GuideScreen(new ResourceLocation("minecraft:furnace"),page.id(),List.of(page),true));
                screenshot.accept("gui-markdown-entry-en.png");phase++;
            }
            case 12 -> {
                Display.setDisplayMode(new DisplayMode(1280,720));mc.resize(1280,720);language("zh_cn");
                stage=new GuiStage(animation(false));mc.displayGuiScreen(stage);sceneInventory=playerInventory();
                stage.seekToTime(30);check.accept(stage.getActiveScene().getActiveGuiAnimations().isEmpty(),"machine scene starts without a GUI clip");
                wait=16;phase++;
            }
            case 13 -> {
                var scene=stage.getActiveScene();
                check.accept(mc.currentScreen==stage&&!scene.isPaused()&&scene.getActiveGuiAnimations().size()==1,"normal 3D scene playback automatically displays its GUI clip without replacing the screen or pausing");
                scene.setPaused(true);int at=scene.getCurrentTick();stage.updateScreen();check.accept(at==scene.getCurrentTick(),"one pause control freezes both the machine and GUI animation");
                screenshot.accept("gui-animation-stage-zh.png");phase++;
            }
            case 14 -> {
                stage.seekToTime(200);var scene=stage.getActiveScene();var clip=scene.getActiveGuiAnimations().get(0);var sampled=new GuiTutorialSession(lesson);sampled.seekAbsolute(clip.tutorialTick(scene,sampled.lastTick()));
                check.accept(sampled.demoSlot("output").item().equals("minecraft:iron_ingot"),"scrubbing the scene reconstructs the GUI output frame from the same timeline");
                screenshot.accept("gui-animation-output-zh.png");phase++;
            }
            case 15 -> {
                stage.seekToTime(242);check.accept(stage.getActiveScene().getActiveGuiAnimations().isEmpty()&&mc.currentScreen==stage,"GUI clip retracts after its duration while remaining in the same machine scene");
                stage.getActiveScene().setPaused(false);int before=stage.getActiveScene().getCurrentTick();stage.updateScreen();check.accept(stage.getActiveScene().getCurrentTick()==before+1,"3D animation continues after the GUI clip");
                stage.key(Keyboard.KEY_R);check.accept(stage.getActiveScene().getCurrentTick()==0,"replay resets the shared scene clock");
                stage.seekToTime(100);stage.getActiveScene().setPaused(true);check.accept(stage.getActiveScene().getActiveGuiAnimations().size()==1,"replay and backward seeking show the same GUI segment again");
                screenshot.accept("gui-animation-replay-zh.png");phase++;
            }
            case 16 -> {
                Display.setDisplayMode(new DisplayMode(854,480));mc.resize(854,480);
                screenshot.accept("gui-animation-small-zh.png");phase++;
            }
            case 17 -> {
                Display.setDisplayMode(new DisplayMode(320,240));mc.resize(320,240);
                var scene=stage.getActiveScene();var layout=GuiAnimationLayout.arrange(new Rect(0,34,stage.width,stage.height-72),scene.getActiveGuiAnimations());
                Rect panel=layout.panels().get(0).bounds();check.accept(panel.y()+panel.height()<stage.height-38&&panel.x()>=layout.world().x()+layout.world().width(),"320x240 stage reserves blank space beside the machine and above playback controls");
                check.accept(sceneInventory.equals(playerInventory()),"scene GUI frames never mutate real player inventories or server containers");
                screenshot.accept("gui-animation-tiny-zh.png");phase++;
            }
            case 18 -> {
                Display.setDisplayMode(new DisplayMode(1280,720));mc.resize(1280,720);language("en_us");
                stage=new GuiStage(animation(true));mc.displayGuiScreen(stage);stage.seekToTime(100);stage.getActiveScene().setPaused(true);
                check.accept(stage.getActiveScene().getActiveGuiAnimations().size()==1&&mc.currentScreen==stage,"a missing optional GUI lesson leaves a recoverable in-scene placeholder");
                screenshot.accept("gui-animation-missing-en.png");phase++;
            }
            case 19 -> {
                stage.getActiveScene().setPaused(false);stage.seekToTime(250);check.accept(stage.getActiveScene().getActiveGuiAnimations().isEmpty()&&!stage.getActiveScene().isPaused(),"missing GUI assets cannot block or stall the timeline");
                var document=new net.createmod.ponder.api.editor.SceneDocument(animationPack(false));
                var screen=new net.createmod.ponder.client.editor.EditorScreen(document,null);mc.displayGuiScreen(screen);
                Field f=screen.getClass().getDeclaredField("preview");f.setAccessible(true);Object preview=f.get(screen);var seek=preview.getClass().getDeclaredMethod("seek",int.class);seek.setAccessible(true);seek.invoke(preview,100);
                screenshot.accept("gui-animation-editor-en.png");phase++;
            }
            case 20 -> {
                mc.displayGuiScreen(furnace);GuiTutorialEvents.start(furnace,lesson);
                beforeDisconnectScenes=net.createmod.ponder.foundation.PonderIndex.getSceneAccess();
                Thread network=new Thread(()->new net.createmod.ponder.client.PonderClientEvents().onDisconnect(null),"Ponder disconnect fixture");
                network.start();network.join();
                check.accept(GuiTutorialEvents.liveSession()!=null&&beforeDisconnectScenes==net.createmod.ponder.foundation.PonderIndex.getSceneAccess(),"network disconnect defers GUI cleanup and addon reload until the client thread runs");
                phase++;
            }
            case 21 -> {
                check.accept(GuiTutorialEvents.liveSession()==null&&beforeDisconnectScenes!=net.createmod.ponder.foundation.PonderIndex.getSceneAccess(),"client task clears live GUI state and rebuilds the scene index after network disconnect");
                phase++;
            }
            default -> {GuiTutorialEvents.stop();return true;}
        }
        return false;
    }
    private com.google.gson.JsonObject animationPack(boolean missing){
        var root=new com.google.gson.JsonParser().parse("{\"format\":1,\"id\":\"smoke:gui_animation\",\"scenes\":[{\"id\":\"smoke:gui_machine\",\"component\":\"minecraft:furnace\",\"title\":\"Machine and GUI animation\",\"structure\":\"ponder:demo/smelting\",\"instructions\":[]}]}").getAsJsonObject();
        var instructions=root.getAsJsonArray("scenes").get(0).getAsJsonObject().getAsJsonArray("instructions");
        var upper=new com.google.gson.JsonObject();upper.addProperty("type","layers_from");upper.addProperty("y",1);
        instructions.add(MarkdownTutorialCompiler.op("world.set_blocks","selection",upper,"state","minecraft:air","particles",false));
        instructions.add(MarkdownTutorialCompiler.op("world.set_block","x",2,"y",1,"z",2,"state","minecraft:furnace","particles",false));
        var all=new com.google.gson.JsonObject();all.addProperty("type","everywhere");
        instructions.add(MarkdownTutorialCompiler.op("world.show_section","selection",all,"direction","down"));
        instructions.add(MarkdownTutorialCompiler.op("scene.idle","ticks",40));
        instructions.add(MarkdownTutorialCompiler.op("overlay.show_gui","duration",200,"tutorial",missing?"absent:machine_gui":lesson.id().toString(),"pointX",2.5,"pointY",1.5,"pointZ",2.5));
        instructions.add(MarkdownTutorialCompiler.op("scene.idle","ticks",200));
        instructions.add(MarkdownTutorialCompiler.op("scene.rotate_camera","degrees",45));
        instructions.add(MarkdownTutorialCompiler.op("scene.idle","ticks",80));
        instructions.add(MarkdownTutorialCompiler.op("scene.finish"));return root;
    }
    private net.createmod.ponder.foundation.PonderScene animation(boolean missing){return Tutorials.compile(net.createmod.ponder.script.PonderJsonLoader.parseScenes(animationPack(missing),"gui-animation.json").get(0));}
    private String playerInventory(){StringBuilder s=new StringBuilder();mc.player.inventory.mainInventory.forEach(item->s.append(item.serializeNBT()));return s.toString();}
    public void verifyFrame(String name){
        if(!name.startsWith("gui-animation-")||name.contains("missing")||name.contains("editor"))return;
        var image=net.minecraft.util.ScreenShotHelper.createScreenshot(mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
        var layout=GuiAnimationLayout.arrange(new Rect(0,34,stage.width,stage.height-72),stage.getActiveScene().getActiveGuiAnimations());
        Rect panel=layout.panels().get(0).bounds();int scale=new ScaledResolution(mc).getScaleFactor(),pixels=0;
        for(int x=panel.x()*scale;x<(panel.x()+panel.width())*scale;x++)for(int y=panel.y()*scale;y<(panel.y()+panel.height())*scale;y++)if((image.getRGB(x,y)&0xffffff)==0xc6c6c6)pixels++;
        check.accept(pixels>100,"framebuffer contains the actual GUI texture beside the 3D machine: "+name);
    }
    private static final class GuiStage extends net.createmod.ponder.foundation.ui.PonderUI {
        GuiStage(net.createmod.ponder.foundation.PonderScene scene){super(List.of(scene));}
        void key(int code)throws java.io.IOException{keyTyped('\0',code);}
    }
    private String fingerprint(){StringBuilder s=new StringBuilder();for(var slot:mc.player.openContainer.inventorySlots)s.append(slot.getStack().serializeNBT());return s.append(mc.player.inventory.getItemStack().serializeNBT()).toString();}
    @SuppressWarnings("unchecked") private static <T>T field(String name)throws Exception{Field f=GuiTutorialEvents.class.getDeclaredField(name);f.setAccessible(true);return (T)f.get(null);}
    private void server(Runnable action){pending=new CompletableFuture<>();mc.getIntegratedServer().addScheduledTask(()->{try{action.run();pending.complete(null);}catch(Throwable e){pending.completeExceptionally(e);}});}
    private void language(String code){mc.gameSettings.language=code;mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguage(code));mc.refreshResources();}
}
