package net.createmod.ponder.client.guitutorial;

import java.util.IdentityHashMap;
import java.util.Map;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.gui.GuiTutorial.Rect;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.GuiAnimationElement;
import net.createmod.ponder.gui.GuiAnimationLayout;
import net.createmod.ponder.gui.GuiTutorialSession;
import net.createmod.ponder.render.SceneProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/** Renders scene clips in the existing stage. It never opens a screen or advances the scene clock. */
public final class GuiAnimationRenderer {
    private final Map<GuiAnimationElement,Playback> cache=new IdentityHashMap<>();
    private GuiAnimationLayout layout;
    private long revision=-1;
    private Object pages;
    private static final class Playback {
        GuiTutorialSession session;
        String error;
        int scroll,lastStep=-1;
    }
    public GuiAnimationLayout prepare(PonderScene scene,Rect stage){
        if(revision!=GuiTutorials.revision()||pages!=PonderIndex.getTutorials()){
            cache.clear();revision=GuiTutorials.revision();pages=PonderIndex.getTutorials();
        }
        return layout=GuiAnimationLayout.arrange(stage,scene.getActiveGuiAnimations());
    }
    private Playback playback(GuiAnimationElement clip){
        return cache.computeIfAbsent(clip,c->{
            Playback p=new Playback();
            try{p.session=new GuiTutorialSession(c.definition()!=null?c.definition():GuiTutorials.resolve(c.tutorial().toString()));p.session.playing(false);}
            catch(RuntimeException failure){p.error=failure.getMessage();Ponder.LOGGER.warn("GUI animation {}: {}",c.tutorial(),p.error);}
            return p;
        });
    }
    public boolean contains(int x,int y){return layout!=null&&layout.panels().stream().anyMatch(p->p.bounds().contains(x,y));}
    public boolean scroll(int wheel,int x,int y){
        if(layout==null)return false;
        for(var panel:layout.panels())if(panel.bounds().contains(x,y)){
            Playback p=playback(panel.animation());p.scroll=Math.max(0,p.scroll+(wheel>0?-1:1));return true;
        }
        return false;
    }
    public void render(PonderScene scene){
        if(layout==null)return;
        for(var panel:layout.panels())render(scene,panel);
    }
    private void render(PonderScene scene,GuiAnimationLayout.Panel panel){
        Rect b=panel.bounds();if(b.width()<32||b.height()<36)return;
        Minecraft mc=Minecraft.getMinecraft();var font=mc.fontRenderer;var clip=panel.animation();Playback p=playback(clip);
        try(GlStateGuard ignored=GlStateGuard.capture()){
            GlStateManager.disableDepth();GlStateManager.disableLighting();
            Gui.drawRect(b.x(),b.y(),b.x()+b.width(),b.y()+b.height(),0xe51a252f);
            Gui.drawRect(b.x(),b.y(),b.x()+2,b.y()+b.height(),GuiTutorialDrawing.ACCENT);
            if(p.session==null){
                font.drawString(GuiTutorialDrawing.tr("animation_missing"),b.x()+8,b.y()+8,0xffedbb8f);
                GuiTutorialDrawing.body(clip.tutorial()+"\n"+p.error,b.x()+8,b.y()+24,b.width()-16,b.height()-30,p.scroll);return;
            }
            var session=p.session;session.seekAbsolute(clip.tutorialTick(scene,session.lastTick()));
            if(p.lastStep!=session.stepIndex()){p.scroll=0;p.lastStep=session.stepIndex();}
            String title=GuiTutorialDrawing.text(session.tutorial().title());
            font.drawString(font.trimStringToWidth(title,b.width()-16),b.x()+8,b.y()+7,GuiTutorialDrawing.ACCENT);
            String step=(session.stepIndex()+1)+"/"+session.tutorial().steps().size()+"  "+GuiTutorialDrawing.text(session.step().title());
            font.drawString(font.trimStringToWidth(step,b.width()-16),b.x()+8,b.y()+20,0xffe6edf1);
            int description=Math.min(66,Math.max(28,b.height()/4));
            float scale=Math.max(.01f,Math.min(clip.scale(),Math.min((b.width()-22)/(float)session.tutorial().width(),(b.height()-description-42)/(float)session.tutorial().height())));
            int modelX=b.x()+(b.width()-Math.round(session.tutorial().width()*scale))/2,modelY=b.y()+35;
            GuiTutorialDrawing.demo(session,modelX,modelY,scale);
            int textY=b.y()+b.height()-description;
            Gui.drawRect(b.x()+8,textY-5,b.x()+b.width()-8,textY-4,0xff36505b);
            int max=GuiTutorialDrawing.body(session.step().text(),b.x()+8,textY,b.width()-16,description-6,p.scroll);
            p.scroll=Math.min(p.scroll,max);
            if(clip.target()!=null){
                var point=SceneProjection.project(clip.target());
                if(point.visible){float edge=point.x<b.x()?b.x():b.x()+b.width();line(point.x,point.y,edge,b.y()+b.height()/2f);}
            }
        }
    }
    private static void line(float x1,float y1,float x2,float y2){
        GlStateManager.disableTexture2D();GlStateManager.color(.62f,.87f,.83f,1);GL11.glLineWidth(1);
        GL11.glBegin(GL11.GL_LINES);GL11.glVertex2f(x1,y1);GL11.glVertex2f(x2,y2);GL11.glEnd();GlStateManager.enableTexture2D();
    }
}
