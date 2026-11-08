package net.createmod.ponder.client.editor;

import java.util.List;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.api.editor.CuboidSelection;
import net.createmod.ponder.api.editor.SceneDocument;
import net.createmod.ponder.api.editor.ScenePackIO;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderStoryBoardEntry;
import net.createmod.ponder.foundation.element.OverlayDataElement;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.render.PonderWorldRenderer;
import net.createmod.ponder.render.SceneProjection;
import net.createmod.ponder.script.ScriptSceneDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class EditorPreview {
    private final PonderWorldRenderer renderer = new PonderWorldRenderer();
    private final net.createmod.ponder.client.guitutorial.GuiAnimationRenderer guiAnimations=new net.createmod.ponder.client.guitutorial.GuiAnimationRenderer();
    private PonderScene scene;
    private net.createmod.ponder.api.gui.GuiTutorial.Rect worldArea;
    net.createmod.ponder.api.gui.GuiTutorial.Rect worldArea(){return worldArea;}
    boolean guiContains(int x,int y){return guiAnimations.contains(x,y);}
    boolean scrollGui(int wheel,int x,int y){return guiAnimations.scroll(wheel,x,y);}
    float yaw = -35, pitch = 25, zoom = 1;
    boolean playing;

    void compile(SceneDocument document) {
        List<ScriptSceneDefinition> definitions = ScenePackIO.validate(document.snapshot());
        ScriptSceneDefinition definition = definitions.get(document.sceneIndex());
        try { new PonderStructureLoader().load(definition.getStructure()); }
        catch (java.io.IOException missing) { throw new IllegalArgumentException(missing.getMessage(), missing); }
        PonderLocalization localization = new PonderLocalization();
        JsonObject pack = document.snapshot();
        if (pack.has("sharedText")) {
            for (java.util.Map.Entry<String, JsonElement> entry : pack.getAsJsonObject("sharedText").entrySet())
                localization.registerShared(new ResourceLocation(entry.getKey()), entry.getValue().getAsString());
        }
        PonderStoryBoardEntry entry = new PonderStoryBoardEntry(definition.asStoryBoard(),
            definition.getSceneId().getNamespace(), definition.getStructure(), definition.getComponent());
        entry.identifiedBy(definition.getSceneId());
        PonderScene replacement = new PonderSceneRegistry(localization).compileEntry(entry);
        scene = replacement;
        playing = false;
    }

    PonderScene scene() { return scene; }
    void invalidate() { scene = null; playing = false; }
    int tick() { return scene == null ? 0 : scene.getCurrentTick(); }
    int total() { return scene == null ? 1 : Math.max(1, scene.getTotalTicks()); }
    void seek(int time) { if (scene != null) scene.seekToTime(Math.max(0, Math.min(time, total()))); }
    void tickPreview() {
        if (scene == null || !playing) return;
        if (scene.isFinished()) { playing = false; return; }
        scene.tick(); renderer.tick(scene.getWorld());
    }

    void render(int x, int y, int width, int height, int guiWidth, int guiHeight,
                int mouseX, int mouseY, float partialTicks, CuboidSelection selection) {
        if (scene == null || width < 4 || height < 4) return;
        Minecraft mc = Minecraft.getMinecraft();
        var guiLayout=guiAnimations.prepare(scene,new net.createmod.ponder.api.gui.GuiTutorial.Rect(x,y,width,height));
        worldArea=guiLayout.world();
        int stageX=x,stageY=y,stageWidth=width,stageHeight=height;
        x=guiLayout.world().x();y=guiLayout.world().y();width=guiLayout.world().width();height=guiLayout.world().height();
        int px = x * mc.displayWidth / guiWidth;
        int py = mc.displayHeight - (y + height) * mc.displayHeight / guiHeight;
        int pw = width * mc.displayWidth / guiWidth;
        int ph = height * mc.displayHeight / guiHeight;
        BlockPos min = scene.getWorld().getBoundsMin(), max = scene.getWorld().getBoundsMax();
        Vec3d center = new Vec3d((min.getX() + max.getX() + 1) / 2d,
            (min.getY() + max.getY() + 1) / 2d, (min.getZ() + max.getZ() + 1) / 2d);
        double span = Math.max(2, Math.max(max.getX() - min.getX() + 1,
            Math.max(max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1)));
        double distance = span * (guiLayout.panels().isEmpty()?1.75:2.35) * zoom / scene.getScale()
            / Math.min(1,width/(double)Math.max(1,height));
        float viewPitch = pitch + scene.getCameraPitch(), viewYaw = yaw + scene.getCameraYaw();
        Vec3d sortingCamera = center.add(rotateY(rotateX(new Vec3d(0, -scene.getYOffset(), distance), -viewPitch), -viewYaw));
        float partial = playing ? partialTicks : 1;
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GL11.glViewport(px, py, pw, ph);
            GL11.glEnable(GL11.GL_SCISSOR_TEST); GL11.glScissor(px, py, pw, ph);
            GlStateManager.depthMask(true); GlStateManager.clearDepth(1); GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
            GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.loadIdentity();
            Project.gluPerspective(45, width / (float) height, .05f, 512);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.loadIdentity();
            GlStateManager.translate(0, scene.getYOffset(), -distance);
            GlStateManager.rotate(viewPitch, 1, 0, 0); GlStateManager.rotate(viewYaw, 0, 1, 0);
            GlStateManager.translate(-center.x, -center.y, -center.z);
            SceneProjection.captureCurrent();
            renderer.renderScene(scene, Vec3d.ZERO, sortingCamera, viewYaw - 180, viewPitch, partial);
            if (selection != null) {
                AABBOutline outline = new AABBOutline(new AxisAlignedBB(selection.min(), selection.max().add(1, 1, 1)));
                outline.getParams().colored(0x72e3bd).lineWidth(.035f);
                outline.render(Vec3d.ZERO, partial);
            }
        }
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST); GL11.glScissor(px, py, pw, ph);
            for (PonderOverlayElement overlay : scene.getOverlayElements())
                if (overlay.isVisible() && !(overlay instanceof OverlayDataElement))
                    overlay.render(scene, mouseX, mouseY, partial);
        }
        try(GlStateGuard ignored=GlStateGuard.capture()){
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(stageX*mc.displayWidth/guiWidth,mc.displayHeight-(stageY+stageHeight)*mc.displayHeight/guiHeight,stageWidth*mc.displayWidth/guiWidth,stageHeight*mc.displayHeight/guiHeight);
            guiAnimations.render(scene);
        }
    }

    BlockPos pick(int mouseX, int mouseY) {
        if (scene == null || guiAnimations.contains(mouseX,mouseY)) return null;
        SceneProjection.ScreenRay ray = SceneProjection.screenRay(mouseX, mouseY);
        if (ray == null) return null;
        Pair<ItemStack, BlockPos> result = scene.rayTraceScene(ray.near, ray.far, playing ? 0 : 1);
        return result.getSecond();
    }

    private static Vec3d rotateX(Vec3d p, double angle) {
        double a = Math.toRadians(angle), c = Math.cos(a), s = Math.sin(a);
        return new Vec3d(p.x, p.y * c - p.z * s, p.y * s + p.z * c);
    }
    private static Vec3d rotateY(Vec3d p, double angle) {
        double a = Math.toRadians(angle), c = Math.cos(a), s = Math.sin(a);
        return new Vec3d(p.x * c + p.z * s, p.y, -p.x * s + p.z * c);
    }
}
