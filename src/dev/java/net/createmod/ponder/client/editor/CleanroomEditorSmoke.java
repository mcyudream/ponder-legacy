package net.createmod.ponder.client.editor;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.editor.SceneDocument;
import net.createmod.ponder.api.editor.ScenePackIO;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.createmod.ponder.script.PonderJsonLoader;
import net.createmod.ponder.script.ScriptSceneRegistry;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/** Only installed by -PponderSmoke. Never shipped in the release jar. */
@Mod(modid = "ponder_editor_harness", name = "Ponder editor verification", version = "1",
    dependencies = "required-after:ponder-legacy", clientSideOnly = true, acceptableRemoteVersions = "*")
public final class CleanroomEditorSmoke {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final JsonArray checks = new JsonArray();
    private Path output;
    private EditorScreen editor;
    private net.createmod.ponder.foundation.ui.PonderViewerSmoke viewerSmoke;
    private net.createmod.ponder.foundation.ui.PonderCatalogSmoke catalogSmoke;
    private boolean viewerComplete, catalogComplete, guideComplete, mediaComplete;
    private net.createmod.ponder.client.guide.GuideSmoke guideSmoke;
    private net.createmod.ponder.client.guide.MarkdownMediaSmoke mediaSmoke;
    private net.createmod.ponder.client.projection.ProjectionPlacementSmoke placementSmoke;
    private net.createmod.ponder.client.guitutorial.GuiTutorialSmoke guiTutorialSmoke;
    private int phase, ticks, waitUntil, exitAt = Integer.MAX_VALUE;
    private String screenshot;
    private CompletableFuture<Void> worldReady;
    private final BlockPos origin = new BlockPos(-3, 64, -5);

    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        net.createmod.ponder.verification.optional.CraftTweakerChecks.prepare();
    }

    @Mod.EventHandler public void init(FMLInitializationEvent event) throws Exception {
        if (!Boolean.getBoolean("ponder.dev.smoke")) return;
        output = mc.gameDir.toPath().resolve("ponder-smoke");
        Files.createDirectories(output);
        net.createmod.ponder.client.guide.Tutorials.setPreferGuide(false);
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.guiScale = 2;
        mc.gameSettings.renderDistanceChunks = 2;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        mc.getToastGui().clear();
        MinecraftForge.EVENT_BUS.register(this);
        report("STARTING", null);
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        if (ticks >= exitAt) { mc.shutdown(); return; }
        if (exitAt != Integer.MAX_VALUE || ticks < waitUntil || screenshot != null) return;
        try {
            if (ticks > 2400) throw new IllegalStateException("Smoke timeout at phase " + phase);
            if (phase>=2 && Boolean.getBoolean("ponder.dev.guiTutorialOnly")) {
                if (worldReady!=null&&!worldReady.isDone()) return;
                if (guiTutorialSmoke==null) guiTutorialSmoke=new net.createmod.ponder.client.guitutorial.GuiTutorialSmoke(mc,this::require,this::requestScreenshot);
                if (guiTutorialSmoke.advance()) {report("PASS",null);exitAt=ticks+10;}
                return;
            }
            if (phase>=2 && Boolean.getBoolean("ponder.dev.catalogOnly")) {
                if (catalogSmoke==null) catalogSmoke=new net.createmod.ponder.foundation.ui.PonderCatalogSmoke(mc,this::require,this::requestScreenshot);
                if (catalogSmoke.advance()) { report("PASS",null);exitAt=ticks+10; }
                return;
            }
            if (phase>=2 && Boolean.getBoolean("ponder.dev.mediaEditorOnly")) {
                if (mediaSmoke==null) {
                    require(true,"focused Markdown media editor acceptance; legacy JSON editor scenario is separate");
                    mediaSmoke=new net.createmod.ponder.client.guide.MarkdownMediaSmoke(mc,this::require,this::requestScreenshot);
                }
                if (mediaSmoke.advance()) { report("PASS",null);exitAt=ticks+10; }
                return;
            }
            switch (phase) {
                case 0:
                    if (!(mc.currentScreen instanceof GuiMainMenu)) return;
                    if (Boolean.getBoolean("ponder.dev.traceGl")) {
                        var messages=new java.util.concurrent.atomic.AtomicInteger();
                        org.lwjgl.opengl.GL43.glDebugMessageCallback((source,type,id,severity,length,message,user)->{
                            if (type==org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_ERROR && messages.getAndIncrement()<12)
                                Ponder.LOGGER.error("GL driver: "+org.lwjgl.system.MemoryUtil.memUTF8(message,length),new Throwable("OpenGL call site"));
                        },0);
                        GL11.glEnable(org.lwjgl.opengl.GL43.GL_DEBUG_OUTPUT);
                        GL11.glEnable(org.lwjgl.opengl.GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
                    }
                    require(Loader.isModLoaded(Ponder.MOD_ID), "normal FML discovery");
                    require(Loader.isModLoaded("cleanroom"), "Cleanroom loaded");
                    require(net.createmod.ponder.foundation.PonderIndex.getSceneAccess().getRegisteredEntries().stream()
                        .noneMatch(entry -> {
                            ResourceLocation id = entry.getValue().getDeclaredSceneId();
                            return id != null && (id.getNamespace().equals("ponder") || id.getNamespace().equals("ponder_editor"));
                        }), "normal startup registers no bundled example scenes");
                    require(net.createmod.ponder.foundation.PonderIndex.getTagAccess().getListedTags().stream()
                        .noneMatch(tag -> tag.getId().getNamespace().equals("ponder")), "default example themes are absent");
                    require(!Files.exists(mc.gameDir.toPath().resolve("scripts/ponder/builtin/basics.zs"))
                        && !Files.exists(mc.gameDir.toPath().resolve("scripts/ponder/packs/ponder_editor/examples.ponder.json")),
                        "old unmodified examples are retired before script discovery and stay absent");
                    WorldSettings settings = new WorldSettings(1729, GameType.CREATIVE, false, false, WorldType.FLAT);
                    settings.setGeneratorOptions("3;minecraft:air;1;");
                    settings.enableCommands();
                    mc.launchIntegratedServer("ponder-editor-smoke", "Ponder editor smoke", settings);
                    phase++; break;
                case 1:
                    if (mc.world == null || mc.player == null || mc.getIntegratedServer() == null) return;
                    worldReady = new CompletableFuture<>();
                    mc.getIntegratedServer().addScheduledTask(() -> {
                        try {
                            net.minecraft.world.WorldServer world = mc.getIntegratedServer().getWorld(0);
                            for (BlockPos p : BlockPos.getAllInBox(origin, origin.add(4, 0, 4)))
                                world.setBlockState(p, Blocks.STONEBRICK.getDefaultState(), 2);
                            world.setBlockState(origin.add(2, 1, 2), Blocks.PISTON.getDefaultState()
                                .withProperty(BlockPistonBase.FACING, EnumFacing.EAST), 2);
                            BlockPos chestPos = origin.add(1, 1, 2);
                            world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
                            TileEntityChest chest = (TileEntityChest) world.getTileEntity(chestPos);
                            chest.setCustomName("Captured chest");
                            chest.setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 3));
                            net.minecraft.entity.player.EntityPlayerMP player = mc.getIntegratedServer().getPlayerList().getPlayers().get(0);
                            player.setGameType(GameType.CREATIVE);
                            player.setPositionAndUpdate(0, 66, 0);
                            worldReady.complete(null);
                        } catch (Throwable failure) { worldReady.completeExceptionally(failure); }
                    });
                    phase++; waitUntil = ticks + 20; break;
                case 2:
                    if (!worldReady.isDone()) return;
                    worldReady.join();
                    for (String check : net.createmod.ponder.verification.optional.CraftTweakerChecks.verify())
                        require(true, check);
                    SceneDocument fixture = SceneDocument.create("smoke:editor", "minecraft:book", "Ponder editor smoke", "ponder:demo/basics");
                    Path fixturePath = ScenePackIO.resolveId(EditorWorkspace.packs(), "smoke:editor_pack", ".ponder.json");
                    ScenePackIO.save(fixturePath, fixture);
                    EditorWorkspace.open(fixture, fixturePath);
                    editor = (EditorScreen) mc.currentScreen;
                    EditorWorkspace.selectCorner(origin, false);
                    EditorWorkspace.selectCorner(origin.add(4, 2, 4), true);
                    EditorWorkspace.capture("smoke:captured");
                    phase++; break;
                case 3:
                    if (!"smoke:captured".equals(editor.document().scene().get("structure").getAsString())) return;
                    net.createmod.ponder.api.level.PonderLevel target = new net.createmod.ponder.api.level.PonderLevel(BlockPos.ORIGIN, null);
                    new PonderStructureLoader().load(new ResourceLocation("smoke:captured")).place(target);
                    TileEntityChest chest = (TileEntityChest) target.getTileEntity(new BlockPos(1, 1, 2));
                    require(chest != null && chest.getStackInSlot(0).getCount() == 3, "server capture preserves inventory NBT");
                    require("Captured chest".equals(chest.getName()), "server capture preserves custom name");
                    require(target.getBlockState(new BlockPos(2, 1, 2)).getValue(BlockPistonBase.FACING) == EnumFacing.EAST,
                        "server capture preserves 1.12 states and normalizes origin");
                    // Exercise actual selection, animation form and apply callbacks.
                    set(editor, "cornerA", new BlockPos(1, 1, 2));
                    set(editor, "cornerB", new BlockPos(2, 1, 2));
                    set(editor, "selected", 1);
                    click(editor, 110);
                    require(mc.currentScreen instanceof EditorFormScreen, "quick animation opens typed form");
                    phase++; waitUntil = ticks + 4; break;
                case 4:
                    click(mc.currentScreen, 2002);
                    require(mc.currentScreen == editor, "animation form applies and returns to editor");
                    require(editor.document().steps().toString().contains("world.rotate_section"), "quick animation adds rotation");
                    phase++; waitUntil = ticks + 10; break;
                case 5:
                    EditorPreview preview = (EditorPreview) get(editor, "preview");
                    require(!((Boolean) get(editor, "error")), "draft compiles in editor");
                    preview.seek(30);
                    PonderScene scene = preview.scene();
                    boolean moved = false, rotated = false;
                    for (net.createmod.ponder.api.element.PonderElement e : scene.getElements()) {
                        if (e instanceof WorldSectionElement) {
                            WorldSectionElement section = (WorldSectionElement) e;
                            moved |= section.getAnimatedOffset().y > .1;
                            rotated |= Math.abs(section.getAnimatedRotation().y) > 10;
                        }
                    }
                    require(moved && rotated, "preview executes movement and rotation");
                    editor.document().undo(); editor.document().redo();
                    click(editor, 104);
                    require(!editor.document().isDirty() && !((Boolean)get(editor, "error")), "editor saves and reloads JSON");
                    require(PonderJsonLoader.reload().errors == 0, "reload has zero errors");
                    require(ScriptSceneRegistry.localSnapshot(true).stream().anyMatch(d -> d.getSceneId().toString()
                        .equals(editor.document().scene().get("id").getAsString())), "saved scene is registered");
                    requestScreenshot("editor-en.png");
                    phase++; break;
                case 6:
                    net.createmod.ponder.render.SceneProjection.ProjectedPoint point =
                        net.createmod.ponder.render.SceneProjection.project(new net.minecraft.util.math.Vec3d(4.5, 1, 4.5));
                    BlockPos picked = ((EditorPreview)get(editor, "preview")).pick(Math.round(point.x), Math.round(point.y));
                    require(point.visible && new BlockPos(4, 0, 4).equals(picked), "preview ray picks a rendered floor block"
                        + " (projected=" + point.x + "," + point.y + "," + point.depth + "; visible=" + point.visible + "; picked=" + picked + ")");
                    // Verify editing an existing action via the real form.
                    set(editor, "selected", 4);
                    click(editor, 201);
                    require(mc.currentScreen instanceof EditorFormScreen, "existing action is editable");
                    requestScreenshot("animation-form.png");
                    phase++; break;
                case 7:
                    click(mc.currentScreen, 2001);
                    EditorPreview p = (EditorPreview)get(editor, "preview");
                    p.seek(0); p.seek(30); p.seek(10); p.seek(30);
                    require(p.tick() == 30, "timeline seeks backward and forward");
                    mc.gameSettings.language = "zh_cn";
                    mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguages().stream()
                        .filter(l -> "zh_cn".equals(l.getLanguageCode())).findFirst().orElseThrow());
                    mc.refreshResources();
                    mc.displayGuiScreen(editor);
                    phase++; waitUntil = ticks + 10; break;
                case 8:
                    require(!net.minecraft.client.resources.I18n.format("ponder.editor.editor").equals("ponder.editor.editor"),
                        "Chinese editor translations load");
                    requestScreenshot("editor-zh.png");
                    phase++; break;
                case 9:
                    Display.setDisplayMode(new DisplayMode(854, 480));
                    mc.resize(854, 480);
                    mc.gameSettings.guiScale = 2;
                    phase++; waitUntil = ticks + 10; break;
                case 10:
                    requestScreenshot("editor-small.png");
                    phase++; break;
                case 11:
                    mc.gameSettings.guiScale = 2;
                    mc.displayGuiScreen(PonderUI.of(new ResourceLocation("minecraft:book")));
                    phase++; waitUntil = ticks + 40; break;
                case 12:
                    require(mc.currentScreen instanceof PonderUI, "original Ponder viewer opens saved JSON scene");
                    requestScreenshot("ponder-viewer.png");
                    phase++; break;
                case 13:
                    EditorWorkspace.openScene(new ResourceLocation("smoke:editor"));
                    require(mc.currentScreen instanceof EditorScreen, "viewer scene source opens in editor");
                    require(((EditorScreen)mc.currentScreen).document().scene().get("id").getAsString().equals("smoke:editor"),
                        "source editor selects the requested scene");
                    phase++; break;
                default:
                    if (viewerSmoke == null)
                        viewerSmoke = new net.createmod.ponder.foundation.ui.PonderViewerSmoke(mc, this::require, this::requestScreenshot);
                    if (!viewerComplete) { viewerComplete = viewerSmoke.advance(); break; }
                    if (catalogSmoke == null)
                        catalogSmoke = new net.createmod.ponder.foundation.ui.PonderCatalogSmoke(mc, this::require, this::requestScreenshot);
                    if (!catalogComplete) { catalogComplete = catalogSmoke.advance(); break; }
                    if (guideSmoke == null) guideSmoke = new net.createmod.ponder.client.guide.GuideSmoke(mc, this::require, this::requestScreenshot);
                    if (!guideComplete) { guideComplete=guideSmoke.advance(); break; }
                    if (mediaSmoke==null) mediaSmoke=new net.createmod.ponder.client.guide.MarkdownMediaSmoke(mc,this::require,this::requestScreenshot);
                    if (!mediaComplete) { mediaComplete=mediaSmoke.advance(); break; }
                    if (placementSmoke==null) placementSmoke=new net.createmod.ponder.client.projection.ProjectionPlacementSmoke(mc,this::require,this::requestScreenshot);
                    if (placementSmoke.advance()) { report("PASS", null); exitAt=ticks+10; }
            }
        } catch (Throwable failure) {
            Ponder.LOGGER.error("Editor smoke failed at phase " + phase, failure);
            report("FAIL", failure); exitAt = ticks + 10;
        }
    }

    @SubscribeEvent public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || screenshot == null) return;
        try {
            ScreenShotHelper.saveScreenshot(output.toFile(), screenshot, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            if (viewerSmoke != null) viewerSmoke.verifyFrame(screenshot);
            if (catalogSmoke != null) catalogSmoke.verifyFrame(screenshot);
            if (guideSmoke != null) guideSmoke.verifyFrame(screenshot);
            if (placementSmoke != null) placementSmoke.verifyFrame(screenshot);
            if (guiTutorialSmoke != null) guiTutorialSmoke.verifyFrame(screenshot);
            require(GL11.glGetError() == GL11.GL_NO_ERROR, "render " + screenshot + " has no OpenGL errors");
        } catch (Throwable failure) {
            report("FAIL", failure); exitAt = ticks + 10;
        } finally { screenshot = null; }
    }

    private void requestScreenshot(String name) { screenshot = name; waitUntil = ticks + 5; }
    private void require(boolean condition, String description) {
        if (!condition) throw new IllegalStateException(description);
        checks.add(description);
        report("RUNNING", null);
    }
    private void report(String result, Throwable failure) {
        if (output == null) return;
        try {
            JsonObject report = new JsonObject();
            report.addProperty("result", result); report.addProperty("phase", phase);
            report.addProperty("minecraft", "1.12.2");
            report.addProperty("cleanroom", Loader.instance().getIndexedModList().get("cleanroom").getVersion());
            report.addProperty("java", System.getProperty("java.version")); report.add("checks", checks);
            java.net.URL source = net.createmod.ponder.PonderMod.class.getProtectionDomain().getCodeSource().getLocation();
            report.addProperty("ponderSource", source.toString());
            report.addProperty("renderer", GL11.glGetString(GL11.GL_RENDERER));
            if (source.getProtocol().equals("jar")) source = ((java.net.JarURLConnection)source.openConnection()).getJarFileURL();
            if (source.getProtocol().equals("file")) {
                Path artifact = Path.of(source.toURI());
                if (Files.isRegularFile(artifact)) report.addProperty("ponderSha256", java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
            }
            if (failure != null) {
                StringWriter trace = new StringWriter(); failure.printStackTrace(new PrintWriter(trace));
                report.addProperty("failure", trace.toString());
            }
            Files.writeString(output.resolve("report.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report), StandardCharsets.UTF_8);
        } catch (Exception failureWritingReport) { Ponder.LOGGER.error("Cannot write smoke report", failureWritingReport); }
    }
    private static Field field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static Object get(Object target, String name) throws Exception { return field(target, name).get(target); }
    private static void set(Object target, String name, Object value) throws Exception { field(target, name).set(target, value); }
    private static void click(GuiScreen screen, int id) throws Exception {
        Field buttons = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
        for (Object button : (java.util.List<?>)buttons.get(screen))
            if (((net.minecraft.client.gui.GuiButton)button).id == id) { ((EditorWidgets.Button)button).activate(); return; }
        throw new IllegalStateException("Button not found " + id + " in " + screen.getClass());
    }
}
