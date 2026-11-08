package net.createmod.ponder.client.guitutorial;

import java.io.IOException;
import java.util.*;
import net.createmod.catnip.platform.CatnipClientServices;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.gui.*;
import net.createmod.ponder.api.gui.GuiTutorial.*;
import net.createmod.ponder.api.gui.GuiTutorial.Stack;
import net.createmod.ponder.enums.PonderKeybinds;
import net.createmod.ponder.foundation.ui.PonderButton;
import net.createmod.ponder.gui.GuiTutorialSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.*;
import org.lwjgl.input.*;

/** Native GUI overlay: live input passes through; demonstration/authoring inputs stay local. */
public final class GuiTutorialEvents {
    public static final GuiTutorialEvents INSTANCE=new GuiTutorialEvents();
    private static GuiScreen bound,holdingScreen;
    private static GuiTutorialSession live;
    private static GuiDemoScreen demo;
    private static GuiTutorialEditorScreen editor;
    private static final Set<String> clicks=new HashSet<>();
    private static float progress;
    private static boolean latched,dragging;
    private static int stable,scroll,lastWidth,lastHeight,dragX,dragY;
    private static Rect panel;
    private GuiTutorialEvents(){}
    public static GuiTutorialSession liveSession(){return live;}
    public static boolean active(){return live!=null||demo!=null||editor!=null;}
    public static boolean handles(GuiScreen gui){return gui!=null&&(gui==bound&&active()||!GuiTutorials.forScreen(gui).isEmpty());}
    public static void stop(){bound=null;live=null;demo=null;editor=null;clicks.clear();progress=0;panel=null;dragging=false;}
    public static void start(GuiScreen gui,GuiTutorial tutorial){
        GuiTutorialAdapter adapter=GuiTutorials.adapter(tutorial);
        if(adapter==null||!adapter.matches(gui,tutorial))throw new IllegalArgumentException("GUI tutorial does not match the open machine");
        bound=gui;live=new GuiTutorialSession(tutorial);live.playing(false);demo=null;editor=null;clicks.clear();stable=scroll=0;panel=null;latched=true;progress=0;
    }
    public static void preview(GuiTutorial tutorial){
        if(bound==null)return;
        demo=new GuiDemoScreen(tutorial,()->{demo=null;latched=true;},true);
        demo.setWorldAndResolution(Minecraft.getMinecraft(),bound.width,bound.height);
    }
    public static void capture(GuiContainer gui){
        bound=gui;demo=null;live=null;panel=null;latched=true;
        editor=new GuiTutorialEditorScreen(GuiTutorialEditorScreen.capture(gui),null,gui,()->{stop();latched=true;});
        editor.setWorldAndResolution(Minecraft.getMinecraft(),gui.width,gui.height);
    }
    public static void tick(){tick(CatnipClientServices.CLIENT_HOOKS.isKeyPressed(PonderKeybinds.PONDER.getMapping()));}
    /** The hold gesture has one controller, also used by installation-jar input acceptance. */
    static void tick(boolean held){
        Minecraft mc=Minecraft.getMinecraft();GuiScreen gui=mc.currentScreen;
        if(bound!=null&&bound!=gui)stop();
        if(gui==null){progress=0;holdingScreen=null;latched=false;return;}
        if(holdingScreen!=gui){holdingScreen=gui;progress=0;latched=held;}
        if(!held)latched=false;
        try{
            if(demo!=null){resizeEmbedded(demo,gui);demo.updateScreen();return;}
            if(editor!=null){resizeEmbedded(editor,gui);editor.updateScreen();return;}
            if(live!=null){
                if(live.step().condition().type()!=Wait.NEXT&&live.complete(observation())){if(++stable>=6){if(live.stepIndex()+1<live.tutorial().steps().size())advance(1);else stable=6;}}else stable=0;
                return;
            }
            List<GuiTutorial> candidates=GuiTutorials.forScreen(gui);
            if(candidates.isEmpty()){progress=0;return;}
            GuiTutorialAdapter adapter=GuiTutorials.adapter(candidates.get(0));
            boolean typing=adapter.textFocused(gui)||GuiScreen.isCtrlKeyDown()||GuiScreen.isAltKeyDown();
            progress=Math.max(0,Math.min(1,progress+(held&&!latched&&!typing?.065f:-.15f)));
            if(progress>=1)start(gui,candidates.get(0));
        }catch(RuntimeException e){Ponder.LOGGER.warn("GUI tutorial stopped: {}",e.toString());stop();}
    }
    private static void resizeEmbedded(GuiScreen overlay,GuiScreen host){if(overlay.width!=host.width||overlay.height!=host.height)overlay.setWorldAndResolution(Minecraft.getMinecraft(),host.width,host.height);}
    private static GuiTutorialSession.Observation observation(){
        GuiTutorial t=live.tutorial();GuiTutorialAdapter adapter=GuiTutorials.adapter(t);
        return new GuiTutorialSession.Observation(){
            public Stack slot(String key){return adapter.slot(bound,t,key);}
            public String value(String key){return adapter.value(bound,key);}
            public boolean clicked(String key){return clicks.contains(key);}
        };
    }
    private static void advance(int direction){if(direction>0)live.next();else live.previous();clicks.clear();stable=scroll=0;}
    private static void layout(GuiScreen gui){
        if(panel!=null&&lastWidth==gui.width&&lastHeight==gui.height)return;
        lastWidth=gui.width;lastHeight=gui.height;Rect window=GuiTutorials.adapter(live.tutorial()).window(gui,live.tutorial());
        int right=gui.width-window.x()-window.width()-12,left=window.x()-12,w=Math.min(220,Math.max(left,right)),h=Math.min(204,gui.height-12);
        w=Math.max(116,Math.min(w,gui.width-16));int x=right>=w?window.x()+window.width()+8:left>=w?window.x()-w-8:gui.width-w-8;
        int y=Math.max(6,Math.min(gui.height-h-6,window.y()));panel=new Rect(Math.max(6,Math.min(gui.width-w-6,x)),y,w,h);
    }
    private static Rect control(int index){int w=(panel.width()-19)/4;return new Rect(panel.x()+6+(index%4)*(w+2),panel.y()+panel.height()-48+(index/4)*23,w,20);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void draw(GuiScreenEvent.DrawScreenEvent.Post event){
        GuiScreen gui=event.getGui();
        try(GlStateGuard ignored=GlStateGuard.capture()){
            GlStateManager.disableDepth();GlStateManager.disableLighting();
            if(gui==bound&&demo!=null){demo.drawScreen(event.getMouseX(),event.getMouseY(),event.getRenderPartialTicks());return;}
            if(gui==bound&&editor!=null){editor.drawScreen(event.getMouseX(),event.getMouseY(),event.getRenderPartialTicks());return;}
            if(gui==bound&&live!=null){drawLive(gui,event.getMouseX(),event.getMouseY());return;}
            if(!GuiTutorials.forScreen(gui).isEmpty()){
                var font=Minecraft.getMinecraft().fontRenderer;int width=Math.min(230,gui.width-16),x=gui.width-width-8;
                Gui.drawRect(x,5,x+width,23,0xe51b2b34);
                font.drawString(font.trimStringToWidth(GuiTutorialDrawing.tr("hold",PonderKeybinds.PONDER.message().getUnformattedText()),width-8),x+4,8,0xffb8e3db);
                Gui.drawRect(x,21,x+Math.round(width*progress),24,GuiTutorialDrawing.ACCENT);
            }
        }
    }
    private static void drawLive(GuiScreen gui,int mx,int my){
        layout(gui);var font=Minecraft.getMinecraft().fontRenderer;GuiTutorial t=live.tutorial();
        Rect target=GuiTutorials.adapter(t).target(gui,t,live.step().target());GuiTutorialDrawing.box(target,GuiTutorialDrawing.ACCENT);
        Gui.drawRect(panel.x(),panel.y(),panel.x()+panel.width(),panel.y()+panel.height(),0xee1a2b35);
        font.drawString(font.trimStringToWidth(GuiTutorialDrawing.text(t.title()),panel.width()-12),panel.x()+6,panel.y()+6,GuiTutorialDrawing.ACCENT);
        font.drawString((live.stepIndex()+1)+" / "+t.steps().size()+" · "+GuiTutorialDrawing.tr("live"),panel.x()+6,panel.y()+20,0xff9bb5be);
        GuiTutorialDrawing.body(GuiTutorialDrawing.text(live.step().title())+"\n\n"+GuiTutorialDrawing.text(live.step().text()),panel.x()+6,panel.y()+36,panel.width()-12,panel.height()-105,scroll);
        String status=target==null?"target_missing":live.step().condition().type()==Wait.NEXT?"next_hint":live.complete(observation())?"complete":"waiting";
        font.drawString(font.trimStringToWidth(GuiTutorialDrawing.tr(status),panel.width()-12),panel.x()+6,panel.y()+panel.height()-61,0xffe1cb8c);
        var icons=new PonderButton.Icon[]{PonderButton.Icon.LEFT,PonderButton.Icon.RIGHT,PonderButton.Icon.PLAY,PonderButton.Icon.CLOSE,PonderButton.Icon.GUIDE,PonderButton.Icon.SCENES};
        String[] keys={"previous","next","demo","exit","edit","switch_lesson"};
        for(int i=0;i<icons.length;i++){
            if(i==5&&GuiTutorials.forScreen(gui).size()<2)continue;
            Rect b=control(i);Gui.drawRect(b.x(),b.y(),b.x()+b.width(),b.y()+b.height(),b.contains(mx,my)?0xff41606c:0xff2d444f);
            PonderButton.drawIcon(icons[i],b.x()+b.width()/2,b.y()+10,15,i==1&&!live.complete(observation())?0xff65777d:0xffdaeee9);
            if(b.contains(mx,my))gui.drawHoveringText(GuiTutorialDrawing.tr(keys[i]),mx,my);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void mouse(GuiScreenEvent.MouseInputEvent.Pre event){
        if(event.getGui()!=bound||!active())return;
        Minecraft mc=Minecraft.getMinecraft();int x=Mouse.getEventX()*bound.width/mc.displayWidth,y=bound.height-Mouse.getEventY()*bound.height/mc.displayHeight-1;
        int button=Mouse.getEventButton(),wheel=Mouse.getEventDWheel();boolean down=Mouse.getEventButtonState();
        try{
            if(demo!=null){event.setCanceled(true);demo.mouse(x,y,button,down,wheel);return;}
            if(editor!=null){event.setCanceled(true);editor.mouse(x,y,button,down);return;}
            layout(bound);
            if(dragging){event.setCanceled(true);if(button==0&&!down){dragging=false;return;}panel=new Rect(Math.max(4,Math.min(bound.width-panel.width()-4,x-dragX)),Math.max(4,Math.min(bound.height-panel.height()-4,y-dragY)),panel.width(),panel.height());return;}
            if(!panel.contains(x,y))return;event.setCanceled(true);
            if(wheel!=0){scroll=Math.max(0,scroll+(wheel>0?-2:2));return;}
            if(button!=0||!down)return;
            if(y<panel.y()+18){dragging=true;dragX=x-panel.x();dragY=y-panel.y();return;}
            for(int i=0;i<6;i++)if(control(i).contains(x,y)){
                switch(i){
                    case 0 -> advance(-1);
                    case 1 -> {if(live.complete(observation())||GuiScreen.isShiftKeyDown()){if(live.stepIndex()==live.tutorial().steps().size()-1)stop();else advance(1);}}
                    case 2 -> preview(live.tutorial());
                    case 3 -> stop();
                    case 4 -> {GuiTutorial t=live.tutorial();editor=new GuiTutorialEditorScreen(t,null,bound,()->{editor=null;latched=true;});editor.setWorldAndResolution(mc,bound.width,bound.height);}
                    case 5 -> {var choices=GuiTutorials.forScreen(bound);if(choices.size()>1){int n=choices.indexOf(live.tutorial());start(bound,choices.get((n+1)%choices.size()));}}
                }return;
            }
        }catch(IOException|RuntimeException e){Ponder.LOGGER.warn("GUI tutorial input failed",e);stop();}
    }
    @SubscribeEvent public void clicked(GuiScreenEvent.MouseInputEvent.Post event){
        if(event.getGui()!=bound||live==null||demo!=null||editor!=null||Mouse.getEventButton()<0||!Mouse.getEventButtonState())return;
        Minecraft mc=Minecraft.getMinecraft();int x=Mouse.getEventX()*bound.width/mc.displayWidth,y=bound.height-Mouse.getEventY()*bound.height/mc.displayHeight-1;
        for(var entry:live.tutorial().targets().entrySet()){
            Rect r=GuiTutorials.adapter(live.tutorial()).target(bound,live.tutorial(),entry.getKey());
            boolean enabled=GuiTutorials.adapter(live.tutorial()).enabled(bound,live.tutorial(),entry.getKey());
            if(enabled&&r!=null&&r.contains(x,y))clicks.add(entry.getKey());
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void keyboard(GuiScreenEvent.KeyboardInputEvent.Pre event){
        GuiScreen gui=event.getGui();int key=Keyboard.getEventKey();boolean down=Keyboard.getEventKeyState();
        try{
            if(gui==bound&&demo!=null){event.setCanceled(true);if(down)demo.key(Keyboard.getEventCharacter(),key);return;}
            if(gui==bound&&editor!=null){event.setCanceled(true);if(down)editor.key(Keyboard.getEventCharacter(),key);return;}
            if(down&&key==Keyboard.KEY_F7&&GuiScreen.isCtrlKeyDown()&&gui instanceof GuiContainer container){event.setCanceled(true);capture(container);return;}
            if(gui==bound&&live!=null&&key==Keyboard.KEY_ESCAPE&&down){event.setCanceled(true);stop();latched=true;return;}
            if(key==PonderKeybinds.PONDER.getMapping().getKeyCode()&&handles(gui)){
                var candidates=GuiTutorials.forScreen(gui);if(!candidates.isEmpty()&&!GuiTutorials.adapter(candidates.get(0)).textFocused(gui))event.setCanceled(true);
            }
        }catch(IOException|RuntimeException e){Ponder.LOGGER.warn("GUI tutorial keyboard failed",e);stop();}
    }
}
