package net.createmod.ponder.client.guide;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import net.createmod.ponder.api.guide.Tutorial;
import net.createmod.ponder.compat.media.MediaVideo;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.guide.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/** Exercises the actual production screens, imported files and rendered media, including a 320x240 GUI. */
public final class MarkdownMediaSmoke {
    private final Minecraft mc;
    private final BiConsumer<Boolean, String> check;
    private final Consumer<String> screenshot;
    private final ResourceLocation id = new ResourceLocation("smoke:media_editor");
    private MarkdownEditor editor;
    private MarkdownMediaScreen form;
    private GuideScreen preview;
    private String original, withImage, imageSource, videoSource;
    private Path externalImage, externalVideo;
    private int phase, wait, importWait, videoWait;

    public MarkdownMediaSmoke(Minecraft mc, BiConsumer<Boolean, String> check, Consumer<String> screenshot) {
        this.mc = mc; this.check = check; this.screenshot = screenshot;
    }

    public boolean advance() throws Exception {
        if (wait-- > 0) return false;
        switch (phase++) {
            case 0: {
                language("zh_cn"); resize(1280, 720);
                var tutorial = MarkdownTutorialCompiler.compile(id, "---\nid: smoke:media_editor\nitem_ids: [minecraft:clock]\npresentation: guide\n---\n# 媒体插入演示\n\n从面板插入图片和视频。\n", "media-editor.md");
                MarkdownEditor.open(tutorial, mc.currentScreen); editor = (MarkdownEditor) mc.currentScreen;
                original = editor.document().text(); editor.document().select(original.length(), false);
                Path external = mc.gameDir.toPath().resolve("media-editor-import-fixture"); Files.createDirectories(external);
                externalImage = external.resolve("电路图.PNG");
                var bitmap = new java.awt.image.BufferedImage(160,90,java.awt.image.BufferedImage.TYPE_INT_RGB);
                var graphics = bitmap.createGraphics(); graphics.setColor(new java.awt.Color(0x23516b)); graphics.fillRect(0,0,160,90);
                graphics.setColor(new java.awt.Color(0x9fe2cf)); graphics.fillRect(20,20,120,50); graphics.dispose();
                javax.imageio.ImageIO.write(bitmap,"png",externalImage.toFile());
                externalVideo = external.resolve("building.mp4");
                Path clip = GuideMediaSources.root().resolve("guide-smoke.mp4");
                if (Files.isRegularFile(clip)) Files.copy(clip, externalVideo, StandardCopyOption.REPLACE_EXISTING);
                else Files.writeString(externalVideo, "No decoder installed in this fixture");
                wait = 4; break;
            }
            case 1:
                click(editor, "image"); form = (MarkdownMediaScreen) mc.currentScreen;
                check.accept(editor.document().text().equals(original), "opening image insertion preserves the draft and selection");
                invoke(form, "apply");
                check.accept(mc.currentScreen == form && !((String) get(form, "error")).isEmpty(), "empty source stays in the media panel with a useful error");
                invoke(form, "browse", new Class<?>[] {String.class}, "source");
                check.accept(mc.currentScreen instanceof MediaFileBrowserScreen, "image panel opens the game-native file browser");
                ((GuiTextField) get(mc.currentScreen, "path")).setText(externalImage.toString());
                invoke(mc.currentScreen, "openPath"); wait = 4; break;
            case 2:
                screenshot.accept("markdown-media-file-browser.png"); break;
            case 3:
                invoke(mc.currentScreen, "select"); check.accept(mc.currentScreen == form, "file selection returns to the same authoring form"); wait = 3; break;
            case 4:
                if (get(form, "importing") != null) { if (++importWait > 80) throw new IllegalStateException("Image import timed out"); phase--; wait = 2; break; }
                imageSource = fields(form).get("source").getText();
                check.accept(imageSource.startsWith("local:") && Files.mismatch(externalImage, GuideMediaSources.local(imageSource.substring(6))) == -1,
                    "external non-ASCII image is copied byte-for-byte and has a portable local source");
                fields(form).get("title").setText("电路图 \"输入\" <标签> & {示意}");
                fields(form).get("width").setText("200"); fields(form).get("height").setText("80");
                screenshot.accept("markdown-insert-image-zh.png"); break;
            case 5:
                invoke(form, "apply"); check.accept(mc.currentScreen == editor, "image form inserts and returns to Markdown editor");
                withImage = editor.document().text();
                var image = compiled().blocks().stream().filter(b -> b.kind() == Tutorial.Kind.IMAGE).findFirst().orElseThrow();
                check.accept(image.target().equals(imageSource) && image.text().equals("电路图 \"输入\" <标签> & {示意}"), "inserted image preserves literal title punctuation");
                editor.document().undo(); check.accept(editor.document().text().equals(original), "one undo removes the entire media insertion");
                editor.document().redo(); check.accept(editor.document().text().equals(withImage), "redo restores the entire media insertion");
                resize(640, 480); wait = 4; break;
            case 6: {
                var buttons = buttons(editor); boolean bounded = true, overlap = false;
                for (var b : buttons) bounded &= b.x >= 0 && b.y >= 0 && b.x + b.width <= editor.width && b.y + b.height < (int) get(editor, "contentTop");
                for (var a : buttons) for (var b : buttons) if (a != b) overlap |= a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y;
                check.accept(editor.width == 320 && bounded && !overlap && buttons.size() == 9
                    && buttons.stream().anyMatch(b->b.displayString.equals(I18n.format("ponder.gui.insert"))),
                    "all nine toolbar controls, including GUI insertion, fit without overlap in a 320x240 GUI");
                screenshot.accept("markdown-editor-media-small-zh.png"); break;
            }
            case 7:
                click(editor, "video"); form = (MarkdownMediaScreen) mc.currentScreen;
                fields(form).get("source").setText("local:missing.mp4"); fields(form).get("height").setText("9999");
                invoke(form, "apply");
                check.accept(mc.currentScreen == form && editor.document().text().equals(withImage), "invalid media dimensions cannot mutate the document");
                fields(form).get("height").setText("100");
                for (int i = 0; i < 4; i++) key(form, Keyboard.KEY_TAB);
                check.accept((int) get(form, "scroll") > 0 && fields(form).get("height").y + 20 <= form.height - 57,
                    "Tab scrolls the video form to the last input at the minimum GUI size");
                screenshot.accept("markdown-insert-video-small-zh.png"); break;
            case 8:
                key(form, Keyboard.KEY_ESCAPE);
                check.accept(mc.currentScreen == editor && editor.document().text().equals(withImage), "cancelling media insertion leaves the draft unchanged");
                resize(1280, 720); language("en_us"); editor.initGui();
                click(editor, "video"); form = (MarkdownMediaScreen) mc.currentScreen;
                invoke(form, "browse", new Class<?>[] {String.class}, "source");
                ((GuiTextField) get(mc.currentScreen, "path")).setText(externalVideo.toString()); invoke(mc.currentScreen, "openPath");
                invoke(mc.currentScreen, "select"); wait = 4; break;
            case 9:
                if (get(form, "importing") != null) { if (++importWait > 120) throw new IllegalStateException("Video import timed out"); phase--; wait = 2; break; }
                videoSource = fields(form).get("source").getText();
                check.accept(videoSource.startsWith("local:") && Files.mismatch(externalVideo, GuideMediaSources.local(videoSource.substring(6))) == -1,
                    "selected video is imported byte-for-byte into the pack media directory");
                fields(form).get("title").setText("Building the network"); fields(form).get("poster").setText(imageSource);
                fields(form).get("width").setText("260"); fields(form).get("height").setText("100");
                screenshot.accept("markdown-insert-video-en.png"); break;
            case 10: {
                invoke(form, "apply"); check.accept(mc.currentScreen == editor, "video insertion works regardless of optional decoder presence");
                var video = compiled().blocks().stream().filter(b -> b.kind() == Tutorial.Kind.VIDEO).findFirst().orElseThrow();
                check.accept(video.target().equals(videoSource) && video.media().poster().equals(imageSource) && video.media().width() == 260 && video.media().height() == 100,
                    "video source, poster and custom dimensions survive compilation");
                invoke(editor, "save");
                check.accept(!editor.document().dirty() && PonderIndex.getTutorials().get(id).markdown().equals(editor.document().text()), "inserted image and video survive validation, save and index reload");
                key(editor, Keyboard.KEY_F5); preview = (GuideScreen) mc.currentScreen; wait = 20; break;
            }
            case 11: {
                GuideImages images = (GuideImages) get(preview, "images");
                check.accept(images.dimensions(imageSource)[0] == 160 && images.dimensions(imageSource)[1] == 90, "preview decodes the image inserted through the panel");
                var videoBlock = preview.currentTutorial().blocks().stream().filter(b -> b.kind() == Tutorial.Kind.VIDEO).findFirst().orElseThrow();
                int mediaWidth = (int) invoke(preview, "mediaWidth", new Class<?>[] {Tutorial.Block.class}, videoBlock);
                check.accept(mediaWidth == 260, "preview applies the requested media width");
                for (Object placed : (List<?>) get(preview, "layout")) if (invoke(placed, "block") == videoBlock)
                    check.accept((int) invoke(placed, "height") == 126, "preview applies video height with a separate 26-pixel control bar");
                screenshot.accept("markdown-inserted-media-preview.png");
                key(preview, Keyboard.KEY_END); wait = 5; break;
            }
            case 12:
                if (Loader.isModLoaded("media_api")) for (MediaVideo video : videos().values()) if (!video.playing()) video.toggle();
                wait = 10; break;
            case 13:
                if (Loader.isModLoaded("media_api") && videos().values().stream().noneMatch(v -> v.texture() > 0)) {
                    if (++videoWait > 80) throw new IllegalStateException("Inserted video did not decode"); phase--; wait = 4; break;
                }
                check.accept(!videos().isEmpty() && videos().values().stream().allMatch(v -> Loader.isModLoaded("media_api") ? v.texture() > 0 : !v.status().isBlank()),
                    Loader.isModLoaded("media_api") ? "preview decodes the actual video inserted through the panel" : "missing optional video decoder shows an explanatory fallback and keeps authoring available");
                screenshot.accept("markdown-inserted-video-preview.png"); break;
            case 14:
                key(preview, Keyboard.KEY_ESCAPE); check.accept(mc.currentScreen == editor, "media preview returns to the same Markdown draft");
                resize(1280, 720); break;
            case 15: {
                String before=editor.document().text();
                GuiButton insert=buttons(editor).stream().filter(b->b.displayString.equals(I18n.format("ponder.gui.insert"))).findFirst().orElseThrow();
                clickButton(editor,insert);check.accept(mc.currentScreen instanceof net.createmod.ponder.client.guitutorial.GuiTutorialLibraryScreen,"Markdown GUI insertion opens the tutorial library");
                GuiButton example=buttons(mc.currentScreen).stream().filter(b->b.id==1).findFirst().orElseThrow();clickButton(mc.currentScreen,example);
                check.accept(mc.currentScreen==editor&&compiled().blocks().stream().anyMatch(b->b.kind()==Tutorial.Kind.GUI_TUTORIAL),"selecting a GUI lesson inserts its complete definition into the same Markdown draft");
                editor.document().undo();check.accept(editor.document().text().equals(before),"one undo removes the entire GUI lesson insertion and preserves media content");
                return true;
            }
            default: return true;
        }
        return false;
    }

    private Tutorial compiled() { return MarkdownTutorialCompiler.compile(id, editor.document().text(), "media-editor.md"); }
    @SuppressWarnings("unchecked") private Map<String, GuiTextField> fields(MarkdownMediaScreen screen) throws Exception { return (Map<String, GuiTextField>) get(screen, "fields"); }
    @SuppressWarnings("unchecked") private Map<Tutorial.Block, MediaVideo> videos() throws Exception { return (Map<Tutorial.Block, MediaVideo>) get(preview, "videos"); }
    @SuppressWarnings("unchecked") private List<GuiButton> buttons(GuiScreen screen) throws Exception { return (List<GuiButton>) ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n").get(screen); }
    private void click(GuiScreen screen, String key) throws Exception {
        GuiButton button = buttons(screen).stream().filter(b -> b.displayString.equals(I18n.format("ponder.guide.insert_media." + key))).findFirst().orElseThrow();
        clickButton(screen,button);
    }
    private void clickButton(GuiScreen screen,GuiButton button)throws Exception{
        Method method = ReflectionHelper.findMethod(screen.getClass(), "mouseClicked", "func_73864_a", int.class, int.class, int.class);
        method.setAccessible(true); method.invoke(screen, button.x + button.width / 2, button.y + button.height / 2, 0);
    }
    private static void key(GuiScreen screen, int key) throws Exception {
        Method method = ReflectionHelper.findMethod(screen.getClass(), "keyTyped", "func_73869_a", char.class, int.class);
        method.setAccessible(true); method.invoke(screen, '\0', key);
    }
    private static Object get(Object object, String field) throws Exception { Field f = object.getClass().getDeclaredField(field); f.setAccessible(true); return f.get(object); }
    private static Object invoke(Object object, String method) throws Exception { return invoke(object, method, new Class<?>[0]); }
    private static Object invoke(Object object, String method, Class<?>[] types, Object... args) throws Exception {
        Method m = object.getClass().getDeclaredMethod(method, types); m.setAccessible(true); return m.invoke(object, args);
    }
    private void language(String code) {
        mc.gameSettings.language = code;
        mc.getLanguageManager().setCurrentLanguage(mc.getLanguageManager().getLanguages().stream().filter(l -> code.equals(l.getLanguageCode())).findFirst().orElseThrow());
        mc.refreshResources();
    }
    private void resize(int width, int height) throws Exception { Display.setDisplayMode(new DisplayMode(width, height)); mc.resize(width, height); }
}
