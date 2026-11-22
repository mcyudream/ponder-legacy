package net.createmod.ponder.foundation.ui;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.createmod.ponder.api.theme.PonderTheme;
import net.createmod.ponder.api.theme.PonderTheme.Color;
import net.createmod.ponder.api.theme.PonderThemeOverrides;
import net.createmod.ponder.client.guide.GuideScreen;
import net.createmod.ponder.client.theme.PonderThemeScreen;
import net.createmod.ponder.client.theme.PonderThemes;
import net.createmod.ponder.config.CClient;
import net.createmod.ponder.enums.PonderConfig;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.guide.GuideSources;
import net.createmod.ponder.script.ScriptSceneRegistry;
import net.createmod.ponder.script.ScriptSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/** Exercises actual selection, resources, configuration, script declarations and rendered pixels. */
public final class PonderThemeSmoke {
    private final Minecraft mc;
    private final BiConsumer<Boolean, String> check;
    private final Consumer<String> screenshot;
    private PonderUI viewer;
    private PonderScene original;
    private int tick, phase, wait;
    private final ResourceLocation component = new ResourceLocation("minecraft:compass");
    private final ResourceLocation custom = new ResourceLocation("theme_smoke:forest");
    private Path localFile;
    private boolean previousFbo;

    public PonderThemeSmoke(Minecraft mc, BiConsumer<Boolean, String> check, Consumer<String> screenshot) {
        this.mc = mc; this.check = check; this.screenshot = screenshot;
    }

    public boolean advance() throws Exception {
        if (wait-- > 0) return false;
        switch (phase++) {
            case 0 -> {
                if (Boolean.getBoolean("ponder.dev.themesZh")) {
                    mc.gameSettings.language = "zh_cn";
                    mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguage("zh_cn"));
                    mc.refreshResources();
                }
                check.accept(PonderThemes.all().stream().map(PonderTheme::id).toList()
                    .containsAll(List.of(PonderTheme.CLASSIC, PonderTheme.MODERN)), "both built-in UI themes load");
                check.accept(Loader.isModLoaded("crafttweaker") == Boolean.getBoolean("ponder.dev.themeZen"),
                    "expected optional CraftTweaker installation");
                if (Boolean.getBoolean("ponder.dev.themeZen")) {
                    var declarations = PonderThemeOverrides.snapshot();
                    check.accept(declarations.containsKey(new ResourceLocation("theme_smoke:zen"))
                        && declarations.containsKey(new ResourceLocation("theme_smoke:fresh")),
                        "real ZenScript creates inherited and independent themes at startup");
                    check.accept(PonderThemes.all().stream().anyMatch(t -> t.id().toString().equals("theme_smoke:zen")
                        && t.layout() == PonderTheme.Layout.MODERN && t.color(Color.ACCENT) == 0xffffcc66
                        && t.color(Color.TEXT) == 0xffeef2ff), "real ZenScript patch preserves inherited colors and layout");
                    PonderThemes.selectAutomatic();
                    check.accept(PonderThemes.current().id().toString().equals("theme_smoke:zen"), "real ZenScript selects the automatic pack default");
                }
                for (int i = 1; i <= 2; i++) {
                    var builder = ScriptSceneRegistry.create(component.toString(), "theme_smoke:scene_" + i,
                        i == 1 ? "Building a simple machine" : "Inputs and outputs", "ponder:demo/basics");
                    builder.configureBasePlate(0, 0, 5).showBasePlate();
                    builder.world.showSection(ScriptSelection.everywhere(), "down");
                    builder.idle(20);
                    builder.overlay.showText(180, "Build the base, then connect the input and output.", 2.5, 1, 2.5, "green", true, true);
                    builder.idle(600).markAsFinished().register();
                }
                PonderIndex.reload();
                PonderThemes.select(PonderTheme.CLASSIC);
                viewer = PonderUI.of(component);
                mc.displayGuiScreen(viewer);
                original = viewer.getActiveScene();
                original.seek(40); original.setPaused(true); tick = original.getCurrentTick();
                screenshot.accept("theme-classic.png");
            }
            case 1 -> {
                click(viewer, 15);
                check.accept(mc.currentScreen instanceof PonderThemeScreen, "viewer theme button opens live selector");
                screenshot.accept("theme-selector-classic.png");
            }
            case 2 -> {
                int index = PonderThemes.all().stream().map(PonderTheme::id).toList().indexOf(PonderTheme.MODERN);
                click(mc.currentScreen, 100 + index);
                check.accept(PonderThemes.current().id().equals(PonderTheme.MODERN), "click selects modern theme immediately");
                click(mc.currentScreen, 1);
                check.accept(mc.currentScreen == viewer && viewer.getActiveScene() == original
                    && original.isPaused() && original.getCurrentTick() == tick, "theme selection preserves viewer, scene identity, pause and timeline");
                CClient reloaded = new CClient();
                reloaded.registerAll(new Configuration(mc.gameDir.toPath().resolve("config/ponder-client.cfg").toFile()));
                check.accept(reloaded.uiTheme.get().equals("ponder:modern"), "explicit selection is persisted on disk");
                screenshot.accept("theme-modern.png");
            }
            case 3 -> {
                checkThemeHud(true);
                if (net.minecraft.client.renderer.OpenGlHelper.shadersSupported
                    && net.minecraft.client.renderer.OpenGlHelper.isFramebufferEnabled())
                    check.accept(ReflectionHelper.findField(net.createmod.ponder.client.theme.PonderThemeBackdrop.class, "blur").get(null) != null,
                        "supported native backdrop blur is initialized before rendering the controls");
                checkModernControls();
                GuiButton side = button(viewer, 11);
                float yaw = ReflectionHelper.findField(PonderUI.class, "yaw").getFloat(viewer);
                viewer.mouseClicked(side.x - 3, side.y + 10, 0);
                viewer.mouseClickMove(side.x + 20, side.y + 20, 0, 50);
                viewer.mouseReleased(side.x + 20, side.y + 20, 0);
                check.accept(ReflectionHelper.findField(PonderUI.class, "yaw").getFloat(viewer) == yaw,
                    "clicking a modern side control's outer frame cannot rotate the scene");
                GuiButton bar = button(viewer, 7);
                viewer.mouseClicked(bar.x + bar.width * 3 / 4, bar.y + 12, 0);
                check.accept(original.getCurrentTick() > 400 && original.isPaused(), "modern progress hitbox seeks while paused");
                viewer.keyTyped(' ', Keyboard.KEY_SPACE);
                check.accept(!original.isPaused(), "modern playback control resumes after seeking");
                viewer.keyTyped(' ', Keyboard.KEY_SPACE);
                viewer.keyTyped('r', Keyboard.KEY_R);
                check.accept(original.getCurrentTick() == 0, "modern replay restarts the scene");
                original.seek(40); original.setPaused(true);
                viewer.keyTyped('\t', Keyboard.KEY_TAB);
                viewer.keyTyped('\0', Keyboard.KEY_DOWN);
                viewer.keyTyped('\r', Keyboard.KEY_RETURN);
                check.accept(viewer.getActiveScene() != original, "modern scene popup supports keyboard navigation");
                viewer.keyTyped('\t', Keyboard.KEY_TAB);
                viewer.keyTyped('\0', Keyboard.KEY_HOME);
                viewer.keyTyped('\r', Keyboard.KEY_RETURN);
                original.seek(40); original.setPaused(true);
                viewer.keyTyped('\t', Keyboard.KEY_TAB);
                screenshot.accept("theme-modern-menu.png");
            }
            case 4 -> {
                viewer.keyTyped('\0', Keyboard.KEY_ESCAPE);
                resize(854, 480); wait = 6;
            }
            case 5 -> {
                checkModernControls();
                for (GuiButton button : buttons(viewer))
                    check.accept(button.x >= 0 && button.y >= 0 && button.x + button.width <= viewer.width
                        && button.y + button.height <= viewer.height, "small modern control stays in bounds: " + button.id);
                screenshot.accept("theme-modern-small.png");
            }
            case 6 -> {
                click(viewer, 15);
                key(mc.currentScreen, Keyboard.KEY_UP);
                key(mc.currentScreen, Keyboard.KEY_RETURN);
                screenshot.accept("theme-selector-small.png");
            }
            case 7 -> {
                click(mc.currentScreen, 1);
                PonderThemes.select(PonderTheme.MODERN);
                mc.displayGuiScreen(new PonderIndexScreen());
                screenshot.accept("theme-index-small.png");
            }
            case 8 -> {
                resize(1280, 720);
                var page = new ResourceLocation("theme_smoke:guide");
                GuideSources.save(page, """
                    ---
                    id: theme_smoke:guide
                    title: A guide with the modern theme
                    item_ids: [minecraft:clock]
                    structure: ponder:demo/basics
                    ---
                    # A guide with the modern theme

                    The same appearance is used throughout the viewer and guide.

                    ## Build and connect

                    Place a chest beside the machine, then connect its input.

                    <GuideOnly>

                    | Step | Action |
                    | --- | --- |
                    | 1 | Build the base |
                    | 2 | Connect the input |

                    </GuideOnly>
                    """);
                PonderIndex.reload();
                mc.displayGuiScreen(new GuideScreen(new ResourceLocation("minecraft:clock"), page));
                screenshot.accept("theme-guide-modern.png");
            }
            case 9 -> {
                PonderThemeOverrides.setDefault(null);
                localFile = mc.gameDir.toPath().resolve("config/ponder/themes.json");
                Files.createDirectories(localFile.getParent());
                Files.writeString(localFile, """
                    {"default":"theme_smoke:forest","themes":{
                      "theme_smoke:forest":{"parent":"ponder:modern","name":"Forest",
                        "colors":{"accent":"#77ddaa","text":"#ddeeff","button":"#dd10251c"}},
                      "ponder:classic":{"colors":{"accent":"#55dd88"}}
                    }}
                    """);
                PonderThemes.reload(mc.getResourceManager());
                PonderThemes.selectAutomatic();
                check.accept(PonderThemes.current().id().equals(custom)
                    && PonderThemes.modern() && PonderThemes.color(Color.ACCENT) == 0xff77ddaa,
                    "local JSON adds a theme, inherits modern layout and controls the automatic default");
                check.accept(PonderThemes.all().stream().filter(t -> t.id().equals(PonderTheme.CLASSIC)).findFirst()
                    .orElseThrow().color(Color.ACCENT) == 0xff55dd88, "local JSON can patch an existing built-in theme");
                mc.displayGuiScreen(viewer);
                original.seek(40); original.setPaused(true);
                screenshot.accept("theme-json-forest.png");
            }
            case 10 -> {
                PonderThemes.select(PonderTheme.MODERN);
                PonderThemes.reload(mc.getResourceManager());
                check.accept(PonderThemes.current().id().equals(PonderTheme.MODERN), "player preference wins over JSON pack default after reload");
                Files.writeString(localFile, "{\"themes\":{\"theme_smoke:forest\":{\"colors\":{\"typo\":\"#ffffff\"}}}}");
                PonderThemes.reload(mc.getResourceManager());
                check.accept(!PonderThemes.errors().isEmpty(), "bad JSON reports a theme diagnostic");
                PonderThemes.select(custom);
                check.accept(PonderThemes.color(Color.ACCENT) == 0xff77ddaa, "bad JSON retains the previous valid local definitions");
                Files.delete(localFile);
                installResourcePack();
                wait = 5;
            }
            case 11 -> {
                var packTheme = new ResourceLocation("theme_smoke:resource");
                check.accept(PonderThemes.all().stream().anyMatch(t -> t.id().equals(packTheme)
                    && t.layout() == PonderTheme.Layout.MODERN && t.color(Color.ACCENT) == 0xffcc88ff),
                    "real resource-pack manifest adds an inherited theme on resource reload");
                check.accept(PonderThemes.errors().stream().anyMatch(s -> s.contains("theme_smoke:bad")),
                    "invalid resource theme is isolated from valid siblings");
                PonderConfig.client().setTheme("theme_smoke:missing");
                check.accept(PonderThemes.current().id().equals(PonderTheme.CLASSIC), "missing selected theme falls back to classic");
                PonderConfig.client().setTheme("Bad theme ID");
                check.accept(PonderThemes.current().id().equals(PonderTheme.CLASSIC), "malformed selected theme cannot break the viewer");
                PonderThemes.select(packTheme);
                mc.displayGuiScreen(new PonderThemeScreen(viewer));
                screenshot.accept("theme-resource-selector.png");
            }
            case 12 -> {
                check.accept(original.getCurrentTick() == 40 && original.isPaused(), "reload and all theme changes preserve the paused scene");
                var lesson = net.createmod.ponder.client.guitutorial.GuiTutorials.furnaceExample();
                net.createmod.ponder.client.guitutorial.GuiTutorials.register(lesson);
                var builder = ScriptSceneRegistry.create("minecraft:furnace", "theme_smoke:gui",
                    "Machine and GUI animation", "ponder:demo/smelting");
                builder.world.showSection(ScriptSelection.everywhere(), "down");
                builder.idle(20);
                builder.overlay.showGui(180, lesson.id().toString(), "right", 0, 120);
                builder.idle(220).markAsFinished().register();
                PonderIndex.reload();
                PonderThemes.select(PonderTheme.MODERN);
                viewer = PonderUI.of(new ResourceLocation("minecraft:furnace"));
                mc.displayGuiScreen(viewer);
                viewer.getActiveScene().seek(60); viewer.getActiveScene().setPaused(true);
                screenshot.accept("theme-modern-gui.png");
            }
            case 13 -> { resize(854, 480); wait = 5; }
            case 14 -> screenshot.accept("theme-modern-gui-small.png");
            case 15 -> {
                PonderThemes.select(PonderTheme.CLASSIC);
                submit("/pondertheme");
                wait = 3;
            }
            case 16 -> {
                check.accept(mc.currentScreen instanceof PonderThemeScreen, "native chat submission opens the deferred theme selector");
                screenshot.accept("theme-command-selector.png");
            }
            case 17 -> {
                click(mc.currentScreen, 1);
                submit("/pondertheme ponder:modern");
                check.accept(PonderThemes.current().id().equals(PonderTheme.MODERN), "native chat command selects and persists an explicit theme");
                submit("/pondertheme reload");
                check.accept(PonderThemes.current().id().equals(PonderTheme.MODERN), "native reload command retains the saved theme");
                submit("/pondertheme auto");
                check.accept(PonderThemes.current().id().equals(PonderTheme.CLASSIC), "native automatic command restores the current pack default");
            }
            case 18 -> {
                PonderThemes.select(PonderTheme.MODERN);
                mc.displayGuiScreen(viewer);
                viewer.getActiveScene().seek(60); viewer.getActiveScene().setPaused(true);
                resize(640, 480); wait = 5;
            }
            case 19 -> {
                check.accept(viewer.width == 320 && viewer.height == 240,
                    "modern controls are exercised at Minecraft's minimum logical GUI size");
                checkModernControls();
                screenshot.accept("theme-modern-gui-minimum.png");
            }
            case 20 -> {
                previousFbo = mc.gameSettings.fboEnable;
                mc.gameSettings.fboEnable = false;
                wait = 3;
            }
            case 21 -> {
                check.accept(!net.createmod.ponder.client.theme.PonderThemeBackdrop.blur(),
                    "modern backdrop supports disabled framebuffer post processing");
                screenshot.accept("theme-modern-no-shaders.png");
            }
            case 22 -> {
                mc.gameSettings.fboEnable = previousFbo;
                click(viewer, 15);
                screenshot.accept("theme-selector-minimum.png");
            }
            case 23 -> {
                mc.displayGuiScreen(null);
                checkThemeHud(false);
                return true;
            }
        }
        return false;
    }

    private void installResourcePack() throws Exception {
        Path pack = mc.gameDir.toPath().resolve("resourcepacks/PonderThemeSmoke");
        Files.createDirectories(pack.resolve("assets/ponder"));
        Files.createDirectories(pack.resolve("assets/theme_smoke/themes"));
        Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":3,\"description\":\"Ponder theme smoke\"}}");
        Files.writeString(pack.resolve("assets/ponder/themes.json"),
            "{\"themes\":[\"theme_smoke:resource\",\"theme_smoke:bad\"]}");
        Files.writeString(pack.resolve("assets/theme_smoke/themes/resource.json"),
            "{\"parent\":\"ponder:modern\",\"name\":\"Resource pack (100%)\",\"colors\":{\"accent\":\"#cc88ff\"}}");
        Files.writeString(pack.resolve("assets/theme_smoke/themes/bad.json"), "{\"parent\":\"theme_smoke:missing\"}");
        ResourcePackRepository repository = mc.getResourcePackRepository();
        repository.updateRepositoryEntriesAll();
        var selected = new java.util.ArrayList<>(repository.getRepositoryEntries());
        var entry = repository.getRepositoryEntriesAll().stream()
            .filter(e -> e.getResourcePackName().equals("PonderThemeSmoke")).findFirst().orElseThrow();
        if (!selected.contains(entry)) selected.add(entry);
        repository.setRepositories(selected);
        mc.refreshResources();
    }

    public void verifyFrame(String name) throws Exception {
        if (!name.startsWith("theme-")) return;
        BufferedImage image = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        int scale = new ScaledResolution(mc).getScaleFactor();
        check.accept(image.getWidth() == mc.displayWidth && image.getHeight() == mc.displayHeight, "theme framebuffer dimensions: " + name);
        int background = image.getRGB(2 * scale, 90 * scale) & 0xffffff;
        check.accept(background != 0xffffff, "theme background is rendered: " + name);
        if (name.equals("theme-modern-no-shaders.png"))
            check.accept(background == 0x101010, "unsupported post processing uses an opaque themed backdrop");
        if (name.startsWith("theme-modern-gui")) {
            var stage = new net.createmod.ponder.api.gui.GuiTutorial.Rect(66, 72, viewer.width - 132, viewer.height - 148);
            var layout = net.createmod.ponder.gui.GuiAnimationLayout.arrange(stage, viewer.getActiveScene().getActiveGuiAnimations());
            var panel = layout.panels().get(0).bounds();
            for (GuiButton control : buttons(viewer))
                if (control instanceof PonderButton)
                    check.accept(control.x + control.width + 4 <= panel.x() || control.x - 4 >= panel.x() + panel.width()
                        || control.y + control.height + 4 <= panel.y() || control.y - 4 >= panel.y() + panel.height(),
                        "modern GUI animation clears control " + control.id + ": " + name);
            int nativePixels = 0;
            for (int x = panel.x() * scale; x < (panel.x() + panel.width()) * scale; x++)
                for (int y = panel.y() * scale; y < (panel.y() + panel.height()) * scale; y++)
                    if ((image.getRGB(x, y) & 0xffffff) == 0xc6c6c6) nativePixels++;
            check.accept(nativePixels > 50, "modern theme retains the native furnace GUI texture: " + name);
        }
        if (name.equals("theme-classic.png") || name.equals("theme-modern.png") || name.equals("theme-modern-small.png")) {
            boolean modern = !name.equals("theme-classic.png");
            int top = image.getRGB(3 * scale, 3 * scale) & 0xffffff;
            check.accept(modern ? top != 0x171b20 && top != 0x161a1f : top == 0x161a1f || top == 0x171b20,
                "theme header pixels match the chosen composition: " + name + " rgb=" + Integer.toHexString(top));
            GuiButton play = button(viewer, 4);
            int bright = 0;
            for (int x = (play.x + 3) * scale; x < (play.x + play.width - 3) * scale; x++)
                for (int y = (play.y + 3) * scale; y < (play.y + play.height - 3) * scale; y++)
                    if ((image.getRGB(x, y) & 0xffffff) == (PonderThemes.color(Color.ICON) & 0xffffff)) bright++;
            check.accept(bright >= 30, "theme playback icon is visible at its actual hitbox: " + name);
            if (modern) {
                int model = 0;
                for (int x = image.getWidth() / 4; x < image.getWidth() * 3 / 4; x++)
                    for (int y = 74 * scale; y < (viewer.height - 76) * scale; y++) {
                        int pixel = image.getRGB(x, y), r = pixel >> 16 & 255, g = pixel >> 8 & 255, b = pixel & 255;
                        if (r > 40 && g > 40 && b > 35 && Math.abs(r - g) < 25) model++;
                    }
                check.accept(model > 100, "modern viewport contains the rendered structure: " + name);
            }
        }
    }

    private void checkModernControls() throws Exception {
        int center = viewer.width / 2, controlsY = viewer.height - 51;
        int[] ids = { 1, 8, 2, 6, 5, 3, 4 };
        int[] positions = { 31, center - 96, center - 38, center - 10, center + 18, center + 76, viewer.width - 51 };
        for (int i = 0; i < ids.length; i++) {
            GuiButton control = button(viewer, ids[i]);
            check.accept(control.x == positions[i] && control.y == controlsY && control.width == 20 && control.height == 20,
                "modern native control position and size: " + ids[i] + " at " + viewer.width + "x" + viewer.height);
        }
        for (GuiButton control : buttons(viewer)) {
            if (!(control instanceof PonderButton)) continue;
            check.accept(control.x - 4 >= 0 && control.y - 4 >= 0
                && control.x + control.width + 4 <= viewer.width && control.y + control.height + 4 <= viewer.height,
                "modern painted frame stays inside the screen: " + control.id + " at " + viewer.width + "x" + viewer.height);
        }
        for (int i = 1; i < ids.length; i++) {
            GuiButton previous = button(viewer, ids[i - 1]), next = button(viewer, ids[i]);
            check.accept(previous.x + previous.width + 4 <= next.x - 4,
                "modern bottom controls have separate painted frames: " + previous.id + " / " + next.id);
        }
        GuiButton bar = button(viewer, 7);
        check.accept(bar.x == center - 110 && bar.y == viewer.height - 27 && bar.width == 220 && bar.height == 1,
            "modern native thin progress bar at " + viewer.width + "x" + viewer.height);
    }

    private void checkThemeHud(boolean hidden) {
        var hand = new net.minecraftforge.client.event.RenderHandEvent(mc.renderGlobal, 0, 0);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(hand);
        var parent = new net.minecraftforge.client.event.RenderGameOverlayEvent(0, new ScaledResolution(mc));
        var hud = new net.minecraftforge.client.event.RenderGameOverlayEvent.Pre(parent,
            net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(hud);
        check.accept(hand.isCanceled() == hidden && hud.isCanceled() == hidden,
            "modern page hides the world hand and HUD only while open: " + hidden);
    }

    private void resize(int width, int height) throws Exception {
        Display.setDisplayMode(new DisplayMode(width, height)); mc.resize(width, height);
    }
    private void submit(String command) throws Exception {
        var chat = new SubmittedChat(command); mc.displayGuiScreen(chat); chat.submit();
    }
    private static final class SubmittedChat extends net.minecraft.client.gui.GuiChat {
        private SubmittedChat(String command) { super(command); }
        private void submit() throws java.io.IOException { keyTyped('\r', Keyboard.KEY_RETURN); }
    }
    @SuppressWarnings("unchecked") private static List<GuiButton> buttons(GuiScreen screen) throws Exception {
        return (List<GuiButton>) ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n").get(screen);
    }
    private static GuiButton button(GuiScreen screen, int id) throws Exception {
        return buttons(screen).stream().filter(b -> b.id == id).findFirst().orElseThrow();
    }
    private static void click(GuiScreen screen, int id) throws Exception {
        GuiButton button = button(screen, id);
        Method method = ReflectionHelper.findMethod(GuiScreen.class, "mouseClicked", "func_73864_a", int.class, int.class, int.class);
        method.invoke(screen, button.x + button.width / 2, button.y + button.height / 2, 0);
    }
    private static void key(GuiScreen screen, int key) throws Exception {
        Method method = ReflectionHelper.findMethod(GuiScreen.class, "keyTyped", "func_73869_a", char.class, int.class);
        method.invoke(screen, '\0', key);
    }
}
