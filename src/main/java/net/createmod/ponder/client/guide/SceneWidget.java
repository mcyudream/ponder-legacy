package net.createmod.ponder.client.guide;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.api.guide.Tutorial.Snapshot;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.OverlayDataElement;
import net.createmod.ponder.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.*;
import net.minecraftforge.fml.relauncher.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

@SideOnly(Side.CLIENT)
public final class SceneWidget {
    public final PonderScene scene;
    public final boolean interactive;
    public float yaw,pitch,zoom;
    private final PonderWorldRenderer renderer=new PonderWorldRenderer();
    private final net.createmod.ponder.client.guitutorial.GuiAnimationRenderer guiAnimations=new net.createmod.ponder.client.guitutorial.GuiAnimationRenderer();
    private final boolean annotations;
    public SceneWidget(PonderScene scene,Snapshot snapshot) {
        this.scene=scene;interactive=snapshot.interactive();annotations=snapshot.definition()!=null;
        yaw=snapshot.yaw();pitch=snapshot.pitch();zoom=snapshot.zoom();
        scene.seekToTime(Math.min(snapshot.tick(),scene.getTotalTicks()));scene.setPaused(true);
    }
    public ItemStack render(int x,int y,int w,int h,int guiW,int guiH,int top,int bottom,int mx,int my,float partial) {
        Minecraft mc=Minecraft.getMinecraft();
        int clipTop=Math.max(y,top),clipBottom=Math.min(y+h,bottom);
        if(clipBottom<=clipTop||w<4||h<4)return ItemStack.EMPTY;
        int stageX=x,stageW=w;
        var guiLayout=guiAnimations.prepare(scene,new net.createmod.ponder.api.gui.GuiTutorial.Rect(x,y,w,h));
        x=guiLayout.world().x();w=guiLayout.world().width();
        BlockPos min=scene.getWorld().getBoundsMin(),max=scene.getWorld().getBoundsMax();
        Vec3d center=new Vec3d((min.getX()+max.getX()+1)/2d,(min.getY()+max.getY()+1)/2d,(min.getZ()+max.getZ()+1)/2d);
        double span=Math.max(2,Math.max(max.getX()-min.getX()+1,Math.max(max.getY()-min.getY()+1,max.getZ()-min.getZ()+1)));
        double distance=span*1.75/zoom/scene.getScale();
        float p=pitch+scene.getCameraPitch(),a=yaw+scene.getCameraYaw();
        Vec3d camera=center.add(rotateY(rotateX(new Vec3d(0,-scene.getYOffset(),distance),-p),-a));
        int px=x*mc.displayWidth/guiW,py=mc.displayHeight-(y+h)*mc.displayHeight/guiH;
        int pw=w*mc.displayWidth/guiW,ph=h*mc.displayHeight/guiH;
        int sy=mc.displayHeight-clipBottom*mc.displayHeight/guiH,sh=(clipBottom-clipTop)*mc.displayHeight/guiH;
        ItemStack hover=ItemStack.EMPTY;
        try(GlStateGuard ignored=GlStateGuard.capture()) {
            GL11.glViewport(px,py,pw,ph);GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(px,sy,pw,sh);
            GlStateManager.depthMask(true);GlStateManager.clearDepth(1);GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.loadIdentity();Project.gluPerspective(45,w/(float)h,.05f,512);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.loadIdentity();
            GlStateManager.translate(0,scene.getYOffset(),-distance);GlStateManager.rotate(p,1,0,0);GlStateManager.rotate(a,0,1,0);
            GlStateManager.translate(-center.x,-center.y,-center.z);SceneProjection.captureCurrent();
            renderer.renderScene(scene,Vec3d.ZERO,camera,a-180,p,1);
            if(mx>=x&&mx<x+w&&my>=clipTop&&my<clipBottom) {
                SceneProjection.ScreenRay ray=SceneProjection.screenRay(mx,my);
                if(ray!=null) {Pair<ItemStack,BlockPos> hit=scene.rayTraceScene(ray.near,ray.far,1);hover=hit.getFirst();}
            }
        }
        if(annotations)try(GlStateGuard ignored=GlStateGuard.capture()) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(px,sy,pw,sh);
            for(var overlay:scene.getOverlayElements())if(overlay.isVisible()&&!(overlay instanceof OverlayDataElement))overlay.render(scene,mx,my,1);
        }
        try(GlStateGuard ignored=GlStateGuard.capture()){
            GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(stageX*mc.displayWidth/guiW,sy,stageW*mc.displayWidth/guiW,sh);
            guiAnimations.render(scene);
        }
        return hover;
    }
    private static Vec3d rotateX(Vec3d p,double angle){double a=Math.toRadians(angle),c=Math.cos(a),s=Math.sin(a);return new Vec3d(p.x,p.y*c-p.z*s,p.y*s+p.z*c);}
    private static Vec3d rotateY(Vec3d p,double angle){double a=Math.toRadians(angle),c=Math.cos(a),s=Math.sin(a);return new Vec3d(p.x*c+p.z*s,p.y,-p.x*s+p.z*c);}
}
