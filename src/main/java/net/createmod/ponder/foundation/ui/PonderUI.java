package net.createmod.ponder.foundation.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.api.subject.PonderSubjectResolvers;
import net.createmod.ponder.api.subject.ResolvedPonderSubject;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderTag;
import net.createmod.ponder.foundation.element.OverlayDataElement;
import net.createmod.ponder.render.PonderWorldRenderer;
import net.createmod.ponder.render.SceneProjection;
import net.createmod.ponder.render.SceneProjection.ScreenRay;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PonderUI extends AbstractPonderScreen {
    private static final int IDENTIFY_KEY = Keyboard.KEY_Q;

    private final List<PonderScene> scenes;
    private java.util.function.Supplier<net.minecraft.client.gui.GuiScreen> guideScreen;
    private net.minecraft.client.gui.GuiScreen returnScreen;
    public PonderUI withGuide(java.util.function.Supplier<net.minecraft.client.gui.GuiScreen> factory) { guideScreen = factory; return this; }
    public PonderUI returningTo(net.minecraft.client.gui.GuiScreen screen) { returnScreen = screen; return this; }
    private final PonderSceneMenu sceneMenu;
    private PonderButton sceneMenuButton;
    private final PonderWorldRenderer renderer = new PonderWorldRenderer();
    private final net.createmod.ponder.client.guitutorial.GuiAnimationRenderer guiAnimations = new net.createmod.ponder.client.guitutorial.GuiAnimationRenderer();
    private net.createmod.ponder.gui.GuiAnimationLayout guiLayout;
    private int sceneIndex;
    private float yaw = -35;
    private float pitch = 25;
    private float zoom = 1;
    private float lastPartialTicks = 1;
    private float frozenPartialTicks = 1;
    private boolean dragging;
    private boolean identifyMode;
    private int lastMouseX;
    private int lastMouseY;
    private PonderProgressBar progress;
    private ItemStack identifiedStack = ItemStack.EMPTY;
    private BlockPos identifiedBlock;

    public static PonderUI of(ResourceLocation component) {
        return new PonderUI(PonderIndex.getSceneAccess().compile(component));
    }

    public static PonderUI of(ItemStack stack) {
        ResolvedPonderSubject subject = PonderSubjectResolvers.resolve(stack);
        if (!subject.isHandled())
            throw new IllegalArgumentException("Item stack could not be resolved to a Ponder component");
        return of(subject.getComponent());
    }

    public static PonderUI of(ItemStack stack, PonderTag tag) {
        return of(stack);
    }

    public PonderUI(List<PonderScene> scenes) {
        if (scenes == null || scenes.isEmpty())
            throw new IllegalArgumentException("At least one compiled Ponder scene is required");
        this.scenes = new ArrayList<PonderScene>(scenes);
        sceneMenu = this.scenes.size() > 1 ? new PonderSceneMenu(this.scenes) : null;
        for (PonderScene scene : this.scenes) scene.begin();
    }

    @Override
    public void initGui() {
        int bottom = height - 29;
        buttonList.clear();
        boolean guideAvailable = guideScreen != null || net.createmod.ponder.client.guide.Tutorials.hasGuide(getActiveScene().getId());
        addButton(new PonderButton(10, 8, 7, 22, 20, PonderButton.Icon.GUIDE)
            .enabled(guideAvailable).withTooltip("ponder.guide.guide").withCallback(() ->
                mc.displayGuiScreen(guideScreen == null ? new net.createmod.ponder.client.guide.GuideScreen(
                    getActiveScene().getComponent(), getActiveScene().getId()) : guideScreen.get())));
        addButton(new PonderButton(11, 36, 7, 22, 20, PonderButton.Icon.STAR)
            .active(!net.createmod.ponder.client.guide.Tutorials.preferGuide())
            .withTooltip("ponder.guide.default_animation").withCallback(() -> {
                net.createmod.ponder.client.guide.Tutorials.setPreferGuide(false); initGui();
            }));
        if (!net.createmod.ponder.client.guitutorial.GuiTutorials.forComponent(getActiveScene().getComponent()).isEmpty())
            addButton(new PonderButton(14,64,7,22,20,PonderButton.Icon.GUI).withTooltip("ponder.gui.demo").withCallback(()->{
                var lesson=net.createmod.ponder.client.guitutorial.GuiTutorials.forComponent(getActiveScene().getComponent()).get(0);
                mc.displayGuiScreen(new net.createmod.ponder.client.guitutorial.GuiDemoScreen(lesson,()->mc.displayGuiScreen(this),false));
            }));
        addButton(new PonderButton(1, 8, bottom, 22, 20, PonderButton.Icon.INDEX)
            .withTooltip("ponder.ui.index_title")
            .withCallback(new Runnable() {
                @Override public void run() { ScreenOpener.open(new PonderIndexScreen()); }
            }));
        addButton(new PonderButton(2, width / 2 - 88, bottom, 22, 20, PonderButton.Icon.LEFT)
            .enabled(sceneIndex > 0)
            .withTooltip("ponder.ui.previous")
            .withCallback(new Runnable() {
                @Override public void run() { previousScene(); }
            }));
        addButton(new PonderButton(3, width / 2 - 62, bottom, 22, 20, PonderButton.Icon.REPLAY)
            .withTooltip("ponder.ui.replay")
            .withCallback(new Runnable() {
                @Override public void run() { replay(); }
            }));
        addButton(new PonderButton(4, width / 2 - 36, bottom, 22, 20,
            getActiveScene().isPaused() ? PonderButton.Icon.PLAY : PonderButton.Icon.PAUSE)
            .withTooltip(getActiveScene().isPaused() ? "ponder.ui.play" : "ponder.ui.pause")
            .withCallback(new Runnable() {
                @Override public void run() { togglePause(); }
            }));
        addButton(new PonderButton(5, width / 2 - 10, bottom, 22, 20, PonderButton.Icon.RIGHT)
            .enabled(sceneIndex + 1 < scenes.size())
            .withTooltip("ponder.ui.next")
            .withCallback(new Runnable() {
                @Override public void run() { nextScene(); }
            }));
        addButton(new PonderButton(8, width / 2 + 20, bottom, 22, 20, PonderButton.Icon.IDENTIFY)
            .active(identifyMode)
            .withTooltip("ponder.ui.identify")
            .withCallback(new Runnable() {
                @Override public void run() { toggleIdentify(); }
            }));
        addButton(new PonderButton(6, width - 30, bottom, 22, 20, PonderButton.Icon.CLOSE)
            .withTooltip("ponder.ui.close")
            .withCallback(new Runnable() {
                @Override public void run() { mc.displayGuiScreen(returnScreen); }
            }));
        addButton(new PonderButton(12,width-84,bottom,22,20,PonderButton.Icon.PROJECT).withTooltip("ponder.projection.project_tip")
            .withCallback(() -> net.createmod.ponder.client.projection.ProjectionService.project(getActiveScene(),this)));
        addButton(new PonderButton(13,width-58,bottom,22,20,PonderButton.Icon.MATERIALS).withTooltip("ponder.projection.pin_tip")
            .withCallback(() -> net.createmod.ponder.client.projection.ProjectionService.pin(getActiveScene(),this)));
        progress = addButton(new PonderProgressBar(7, this, 42, height - 17,
            Math.max(12, width / 2 - 138), 4));
        if (sceneMenu != null) {
            String counter = (sceneIndex + 1) + " / " + scenes.size();
            int menuWidth = fontRenderer.getStringWidth(counter) + 41;
            sceneMenuButton = addButton(new PonderButton(9, width - menuWidth - 8, 6, menuWidth, 22,
                PonderButton.Icon.SCENES).withLabel(counter).withTooltip("ponder.ui.scene_menu")
                .withCallback(() -> {
                    dragging = false;
                    sceneMenu.toggle(sceneIndex);
                }));
            sceneMenu.layout(width, height, sceneMenuButton);
        }
    }

    @Override
    public void updateScreen() {
        PonderScene scene = getActiveScene();
        if (!identifyMode && !scene.isPaused()) {
            scene.tick();
            renderer.tick(scene.getWorld());
        }
    }

    @Override
    protected void renderWindow(int mouseX, int mouseY, float partialTicks) {
        if (sceneMenu != null) {
            sceneMenu.updateHover(mouseX, mouseY, sceneIndex);
            sceneMenuButton.active(sceneMenu.isOpen());
        }
        PonderScene scene = getActiveScene();
        if (!identifyMode && !scene.isPaused()) lastPartialTicks = partialTicks;
        guiLayout = guiAnimations.prepare(scene, new net.createmod.ponder.api.gui.GuiTutorial.Rect(0,34,width,Math.max(1,height-72)));
        float scenePartialTicks = identifyMode || scene.isPaused() ? frozenPartialTicks : partialTicks;
        clearSceneDepth();
        beforeScenePassForTesting();
        renderScene(scene, mouseX, mouseY, scenePartialTicks);
        afterScenePassForTesting();
        if (!identifyMode) {
            for (PonderOverlayElement overlay : scene.getOverlayElements())
                if (overlay.isVisible() && !(overlay instanceof OverlayDataElement))
                    overlay.render(scene, mouseX, mouseY, scenePartialTicks);
            guiAnimations.render(scene);
        }
        Gui.drawRect(0, 0, width, 34, 0xdd171b20);
        Gui.drawRect(0, height - 38, width, height, 0xff171b20);
        int titleStart=buttonList.stream().anyMatch(button->button.id==14)?94:68;
        int titleWidth = (sceneMenuButton == null ? width - 56 : sceneMenuButton.x) - titleStart - 4;
        fontRenderer.drawString(fontRenderer.trimStringToWidth(scene.getTitle(), Math.max(0, titleWidth)), titleStart, 13, 0xffeef2f5);
        if (sceneMenu == null) fontRenderer.drawString("1 / 1", width - fontRenderer.getStringWidth("1 / 1") - 12, 13, 0xffaeb8c2);
    }

    private static void clearSceneDepth() {
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.depthMask(true);
            GlStateManager.clearDepth(1.0D);
            GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        }
    }

    // Package-private so the development harness can bracket the 3D pass without exposing test API.
    void beforeScenePassForTesting() {
    }

    void afterScenePassForTesting() {
    }

    private void renderScene(PonderScene scene, int mouseX, int mouseY, float partialTicks) {
        var viewport = guiLayout.panels().isEmpty() ? new net.createmod.ponder.api.gui.GuiTutorial.Rect(0,0,width,height) : guiLayout.world();
        BlockPos min = scene.getWorld().getBoundsMin();
        BlockPos max = scene.getWorld().getBoundsMax();
        Vec3d center = new Vec3d((min.getX() + max.getX() + 1) / 2d,
            (min.getY() + max.getY() + 1) / 2d, (min.getZ() + max.getZ() + 1) / 2d);
        double span = Math.max(2, Math.max(max.getX() - min.getX() + 1,
            Math.max(max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1)));
        double distance = span * 2.35 * zoom / scene.getScale() / Math.min(1,viewport.width()/(double)Math.max(1,viewport.height()));
        float viewPitch = pitch + scene.getCameraPitch();
        float viewYaw = yaw + scene.getCameraYaw();
        Vec3d cameraOffset = rotateX(new Vec3d(0, -scene.getYOffset(), distance), -viewPitch);
        cameraOffset = rotateY(cameraOffset, -viewYaw);
        Vec3d sortingCamera = center.add(cameraOffset);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        boolean projectionPushed = false;
        boolean modelViewPushed = false;
        java.nio.IntBuffer previousViewport = org.lwjgl.BufferUtils.createIntBuffer(16);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, previousViewport);
        try {
            GL11.glViewport(viewport.x()*mc.displayWidth/width,mc.displayHeight-(viewport.y()+viewport.height())*mc.displayHeight/height,
                Math.max(1,viewport.width()*mc.displayWidth/width),Math.max(1,viewport.height()*mc.displayHeight/height));
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            projectionPushed = true;
            GlStateManager.loadIdentity();
            Project.gluPerspective(45, viewport.width() / (float) Math.max(1, viewport.height()), .05f, 256);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            modelViewPushed = true;
            GlStateManager.loadIdentity();
            GlStateManager.translate(0, scene.getYOffset(), -distance);
            GlStateManager.rotate(viewPitch, 1, 0, 0);
            GlStateManager.rotate(viewYaw, 0, 1, 0);
            GlStateManager.translate(-center.x, -center.y, -center.z);
            SceneProjection.captureCurrent();
            updateIdentifiedItem(scene, mouseX, mouseY, partialTicks);
            renderer.renderScene(scene, Vec3d.ZERO, sortingCamera, viewYaw - 180, viewPitch, partialTicks);
        } finally {
            if (modelViewPushed) {
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
            }
            if (projectionPushed) {
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
            }
            GlStateManager.matrixMode(previousMatrixMode);
            GL11.glViewport(previousViewport.get(0),previousViewport.get(1),previousViewport.get(2),previousViewport.get(3));
        }
    }

    private void updateIdentifiedItem(PonderScene scene, int mouseX, int mouseY, float partialTicks) {
        identifiedStack = ItemStack.EMPTY;
        identifiedBlock = null;
        if (!identifyMode) return;
        if (mouseY <= 34 || mouseY >= height - 38 || guiAnimations.contains(mouseX,mouseY) || sceneMenu != null && sceneMenu.isOpen() && sceneMenu.contains(mouseX, mouseY)) {
            scene.deselect();
            return;
        }
        ScreenRay ray = SceneProjection.screenRay(mouseX, mouseY);
        if (ray == null) {
            scene.deselect();
            return;
        }
        Pair<ItemStack, BlockPos> result = scene.rayTraceScene(ray.near, ray.far, partialTicks);
        identifiedStack = result.getFirst() == null ? ItemStack.EMPTY : result.getFirst();
        identifiedBlock = result.getSecond();
    }

    @Override
    protected void renderWindowForeground(int mouseX, int mouseY, float partialTicks) {
        if (sceneMenu != null && sceneMenu.isOpen()) {
            sceneMenu.draw(fontRenderer, sceneIndex);
            return;
        }
        for (GuiButton button : buttonList) {
            if (!(button instanceof PonderButton)) continue;
            PonderButton ponderButton = (PonderButton) button;
            if (ponderButton.isHoveredButton() && ponderButton.getTooltipKey() != null) {
                drawHoveringText(Arrays.asList(I18n.format(ponderButton.getTooltipKey())),
                    mouseX, mouseY, fontRenderer);
                return;
            }
        }
        if (!identifyMode || mouseY <= 34 || mouseY >= height - 38 || guiAnimations.contains(mouseX,mouseY)) return;
        if (!identifiedStack.isEmpty()) {
            renderToolTip(identifiedStack, mouseX, mouseY);
            return;
        }
        if (identifiedBlock != null) {
            IBlockState state = getActiveScene().getWorld().getBlockState(identifiedBlock);
            String name = state.getBlock().getLocalizedName();
            drawHoveringText(Arrays.asList(name,
                TextFormatting.DARK_GRAY + identifiedBlock.toString()), mouseX, mouseY, fontRenderer);
            return;
        }
        String help = I18n.format("ponder.ui.identify_mode", Keyboard.getKeyName(IDENTIFY_KEY));
        List<String> lines = Arrays.asList((TextFormatting.GRAY + help).split("\\n"));
        drawHoveringText(lines, mouseX, mouseY, fontRenderer);
    }

    private static Vec3d rotateX(Vec3d value, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3d(value.x, value.y * cos - value.z * sin, value.y * sin + value.z * cos);
    }

    private static Vec3d rotateY(Vec3d value, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3d(value.x * cos + value.z * sin, value.y, -value.x * sin + value.z * cos);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (sceneMenu != null && sceneMenu.isOpen() && !sceneMenu.overAnchor(mouseX, mouseY)) {
            dragging = false;
            if (sceneMenu.contains(mouseX, mouseY)) {
                int selected = sceneMenu.rowAt(mouseX, mouseY);
                if (button == 0 && selected >= 0) selectScene(selected);
            } else sceneMenu.close();
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
        if (guiAnimations.contains(mouseX,mouseY)) return;
        if (button == 0 && !identifyMode && mouseY > 34 && mouseY < height - 38) {
            dragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        dragging = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long elapsed) {
        if (sceneMenu != null && sceneMenu.isOpen()) return;
        if (progress != null && progress.isMouseOver()) {
            progress.dragTo(mouseX);
            return;
        }
        if (!dragging) return;
        yaw += (mouseX - lastMouseX) * .5f;
        pitch = Math.max(-85, Math.min(85, pitch + (mouseY - lastMouseY) * .5f));
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        scroll(wheel, mouseX, mouseY);
    }

    private void scroll(int wheel, int mouseX, int mouseY) {
        if (sceneMenu != null && sceneMenu.isOpen()) {
            if (wheel != 0 && (sceneMenu.contains(mouseX, mouseY) || sceneMenu.overAnchor(mouseX, mouseY)))
                sceneMenu.scroll(wheel > 0 ? -1 : 1);
            return;
        }
        if (wheel != 0 && guiAnimations.scroll(wheel,mouseX,mouseY)) return;
        if (wheel != 0) zoom = Math.max(.35f, Math.min(3, zoom * (wheel > 0 ? .9f : 1.1f)));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE && returnScreen != null) { mc.displayGuiScreen(returnScreen); return; }
        if (sceneMenu != null) {
            if (keyCode == Keyboard.KEY_TAB) {
                dragging = false;
                sceneMenu.toggle(sceneIndex);
                return;
            }
            if (sceneMenu.isOpen()) {
                switch (keyCode) {
                    case Keyboard.KEY_ESCAPE:
                    case Keyboard.KEY_BACK: sceneMenu.close(); return;
                    case Keyboard.KEY_UP: sceneMenu.move(-1); return;
                    case Keyboard.KEY_DOWN: sceneMenu.move(1); return;
                    case Keyboard.KEY_PRIOR: sceneMenu.move(-sceneMenu.pageSize()); return;
                    case Keyboard.KEY_NEXT: sceneMenu.move(sceneMenu.pageSize()); return;
                    case Keyboard.KEY_HOME: sceneMenu.move(-scenes.size()); return;
                    case Keyboard.KEY_END: sceneMenu.move(scenes.size()); return;
                    case Keyboard.KEY_RETURN:
                    case Keyboard.KEY_NUMPADENTER: selectScene(sceneMenu.highlighted()); return;
                }
            }
        }
        if (net.createmod.ponder.client.editor.EditorClientEvents.isEditorKey(keyCode)) {
            var authored = PonderIndex.getTutorials().get(getActiveScene().getId());
            if (authored != null) net.createmod.ponder.client.guide.MarkdownEditor.open(authored, this);
            else net.createmod.ponder.client.editor.EditorWorkspace.openScene(getActiveScene().getId());
            return;
        }
        if (keyCode == IDENTIFY_KEY) {
            toggleIdentify();
            return;
        }
        if (keyCode == Keyboard.KEY_SPACE) {
            togglePause();
            return;
        }
        if (keyCode == Keyboard.KEY_R) {
            replay();
            return;
        }
        if (keyCode == Keyboard.KEY_LEFT) {
            previousScene();
            return;
        }
        if (keyCode == Keyboard.KEY_RIGHT) {
            nextScene();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void toggleIdentify() {
        identifyMode = !identifyMode;
        if (identifyMode) frozenPartialTicks = lastPartialTicks;
        else clearIdentification();
        initGui();
    }

    private void clearIdentification() {
        identifyMode = false;
        identifiedStack = ItemStack.EMPTY;
        identifiedBlock = null;
        getActiveScene().deselect();
    }

    private void togglePause() {
        if (identifyMode) {
            clearIdentification();
        } else {
            PonderScene scene = getActiveScene();
            if (!scene.isPaused()) frozenPartialTicks = lastPartialTicks;
            scene.setPaused(!scene.isPaused());
        }
        initGui();
    }

    private void replay() {
        clearIdentification();
        getActiveScene().restart();
        initGui();
    }

    private void previousScene() {
        selectScene(sceneIndex - 1);
    }

    private void nextScene() {
        selectScene(sceneIndex + 1);
    }

    public void selectScene(ResourceLocation id) {
        for (int i = 0; i < scenes.size(); i++) if (scenes.get(i).getId().equals(id)) { sceneIndex = i; return; }
    }

    private void selectScene(int index) {
        if (index < 0 || index >= scenes.size()) return;
        if (sceneMenu != null) sceneMenu.close();
        dragging = false;
        if (index == sceneIndex) return;
        clearIdentification();
        sceneIndex = index;
        getActiveScene().restart();
        initGui();
    }

    public PonderScene getActiveScene() {
        return scenes.get(sceneIndex);
    }

    public ItemStack getSubject() {
        ResourceLocation component = getActiveScene().getComponent();
        Item item = Item.REGISTRY.getObject(component);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public void seekToTime(int tick) {
        getActiveScene().seek(tick);
    }
}
