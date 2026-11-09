package net.createmod.ponder.client.guitutorial;

import java.io.IOException;
import net.createmod.ponder.api.gui.GuiTutorial;
import net.createmod.ponder.api.gui.GuiTutorial.Rect;
import net.createmod.ponder.foundation.ui.*;
import net.createmod.ponder.gui.GuiTutorialSession;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Can be embedded above an open machine without closing its server container. */
public final class GuiDemoScreen extends AbstractPonderScreen {
    private final GuiTutorialSession session;
    private final Runnable close;
    private final boolean embedded;
    private int x,y,panelX,panelWidth,scroll;
    private float scale;
    private boolean seeking;
    public GuiDemoScreen(GuiTutorial tutorial,Runnable close,boolean embedded){this.session=new GuiTutorialSession(tutorial);this.close=close;this.embedded=embedded;}
    public GuiTutorialSession session(){return session;}
    public Rect machineBounds(){return new Rect(x,y,Math.round(session.tutorial().width()*scale),Math.round(session.tutorial().height()*scale));}
    @Override public void initGui(){
        buttonList.clear();panelWidth=Math.min(240,Math.max(128,width/3));panelX=width-panelWidth-10;
        int availableHeight=Math.max(1,height-92); // Below the header, above the operation hint and playback controls.
        scale=Math.max(.05f,Math.min(1.5f,Math.min((panelX-20)/(float)session.tutorial().width(),availableHeight/(float)session.tutorial().height())));
        x=Math.max(6,(panelX-Math.round(session.tutorial().width()*scale))/2);y=40+Math.max(0,(availableHeight-Math.round(session.tutorial().height()*scale))/2);
        addButton(new PonderButton(1,8,height-29,22,20,PonderButton.Icon.LEFT).withTooltip("ponder.gui.previous").withCallback(()->{session.previous();scroll=0;}));
        addButton(new PonderButton(2,34,height-29,22,20,PonderButton.Icon.REPLAY).withTooltip("ponder.gui.replay").withCallback(()->{session.seek(session.stepIndex(),0);session.playing(true);}));
        addButton(new PonderButton(3,60,height-29,22,20,PonderButton.Icon.PAUSE).withTooltip("ponder.gui.pause").withCallback(()->session.playing(!session.playing())));
        addButton(new PonderButton(4,86,height-29,22,20,PonderButton.Icon.RIGHT).withTooltip("ponder.gui.next").withCallback(()->{session.next();scroll=0;}));
        addButton(new PonderButton(5,width-30,7,22,20,PonderButton.Icon.CLOSE).withTooltip("ponder.ui.close").withCallback(close));
        if(!embedded)addButton(new PonderButton(6,width-76,height-29,68,20,null).withLabel(GuiTutorialDrawing.tr("edit")).withCallback(()->mc.displayGuiScreen(new GuiTutorialEditorScreen(session.tutorial(),this))));
    }
    @Override public void updateScreen(){session.advanceDemo();for(var button:buttonList)if(button.id==3&&button instanceof PonderButton p)p.withIcon(session.playing()?PonderButton.Icon.PAUSE:PonderButton.Icon.PLAY).withTooltip(session.playing()?"ponder.gui.pause":"ponder.ui.play");}
    @Override protected void renderWindow(int mx,int my,float partial){
        GuiTutorial t=session.tutorial();fontRenderer.drawString(fontRenderer.trimStringToWidth(GuiTutorialDrawing.text(t.title()),width-60),10,11,0xffe0f2ef);
        GuiTutorialDrawing.demo(session,x,y,scale);
        Gui.drawRect(panelX-6,40,width-6,height-44,0xff1c2a33);
        fontRenderer.drawString(GuiTutorialDrawing.tr("demo"),panelX,47,0xff9fded4);
        fontRenderer.drawString((session.stepIndex()+1)+" / "+t.steps().size(),panelX,62,0xff819ba8);
        GuiTutorialDrawing.body(GuiTutorialDrawing.text(session.step().title())+"\n\n"+GuiTutorialDrawing.text(session.step().text()),panelX,81,panelWidth-6,Math.max(36,height-145),scroll);
        int left=116,right=Math.max(left+10,width-(embedded?12:84)),top=height-24;
        Gui.drawRect(left,top,right,top+9,0xff3b4e5a);Gui.drawRect(left,top,left+(right-left)*session.tick()/session.step().duration(),top+9,0xff9fded4);
        fontRenderer.drawString(GuiTutorialDrawing.tr("demo_hint"),8,height-46,0xff849aa5);
    }
    @Override protected void renderWindowForeground(int mx,int my,float partial){
        for(var b:buttonList)if(b instanceof PonderButton p&&p.isHoveredButton()&&p.getTooltipKey()!=null)drawHoveringText(net.minecraft.client.resources.I18n.format(p.getTooltipKey()),mx,my);
    }
    private void seek(int mx){int right=Math.max(126,width-(embedded?12:84));session.seek(session.stepIndex(),Math.round((mx-116)/(float)(right-116)*session.step().duration()));}
    @Override protected void mouseClicked(int mx,int my,int button)throws IOException{
        if(button==0&&my>=height-27&&my<height-12&&mx>=116&&mx<width-(embedded?12:84)){seeking=true;seek(mx);return;}super.mouseClicked(mx,my,button);
    }
    @Override protected void mouseClickMove(int mx,int my,int button,long time){if(seeking)seek(mx);else super.mouseClickMove(mx,my,button,time);}
    @Override protected void mouseReleased(int mx,int my,int button){seeking=false;super.mouseReleased(mx,my,button);}
    @Override public void handleMouseInput()throws IOException{super.handleMouseInput();if(Mouse.getEventDWheel()!=0)scroll=Math.max(0,scroll+(Mouse.getEventDWheel()>0?-2:2));}
    @Override protected void keyTyped(char c,int key)throws IOException{
        if(key==Keyboard.KEY_ESCAPE){close.run();return;}if(key==Keyboard.KEY_SPACE){session.playing(!session.playing());return;}
        if(key==Keyboard.KEY_LEFT){session.previous();scroll=0;return;}if(key==Keyboard.KEY_RIGHT){session.next();scroll=0;return;}super.keyTyped(c,key);
    }
    public void mouse(int mx,int my,int button,boolean down,int wheel)throws IOException{if(wheel!=0)scroll=Math.max(0,scroll+(wheel>0?-2:2));if(button>=0){if(down)mouseClicked(mx,my,button);else mouseReleased(mx,my,button);}else if(seeking)seek(mx);}
    public void key(char c,int key)throws IOException{keyTyped(c,key);}
    @Override public boolean doesGuiPauseGame(){return true;}
}
