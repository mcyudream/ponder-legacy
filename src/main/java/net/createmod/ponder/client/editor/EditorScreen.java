package net.createmod.ponder.client.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.editor.CuboidSelection;
import net.createmod.ponder.api.editor.EditorActions;
import net.createmod.ponder.api.editor.EditorAnimations;
import net.createmod.ponder.api.editor.SceneDocument;
import net.createmod.ponder.api.editor.ScenePackIO;
import net.createmod.ponder.script.PonderJsonLoader;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.BlockPos;
import static net.createmod.ponder.client.editor.EditorWidgets.*;

/** Ponder authoring UI: live viewport, searchable operations, reorderable steps and a scrubbable timeline. */
public final class EditorScreen extends GuiScreen {
    private final SceneDocument document;
    private final EditorPreview preview = new EditorPreview();
    private Path path;
    private byte[] lastDiskContent;
    private int selected = -1, scroll;
    private int leftWidth, mainTop, mainBottom, viewX, viewWidth;
    private long compiledRevision = -1;
    private String status = "";
    private boolean error, selecting, scrubbing, orbiting;
    private BlockPos cornerA, cornerB;
    private int lastMouseX, lastMouseY, dragStep = -1, dragY;
    private boolean draggedStep;

    public EditorScreen(SceneDocument document, Path path) {
        this.document = document; this.path = path;
        if (path != null) {
            try { lastDiskContent = Files.readAllBytes(path); }
            catch (IOException failure) { setStatus(failure.getMessage(), true); }
        }
    }

    @Override public void initGui() {
        buttonList.clear();
        leftWidth = Math.max(125, Math.min(200, width / 3));
        String[] names = {"open", "save", "undo", "world_select", "capture", "animate", "more"};
        int[] ids = {101, 104, 105, 107, 108, 110, 111};
        Runnable[] callbacks = {() -> protect(this::browsePacks), this::save, document::undo,
            EditorWorkspace::selectInWorld, this::captureDialog, this::animateSelection, this::moreActions};
        int cell = (width - 16) / names.length;
        for (int i = 0; i < names.length; i++)
            addButton(new Button(ids[i], 8 + i * cell, 27, cell - 3, tr(names[i]), callbacks[i]));
        mainTop = 52;
        mainBottom = height - 84;
        viewX = leftWidth + 17; viewWidth = Math.max(20, width - viewX - 8);
        int stepButtonWidth = Math.max(22, (leftWidth - 4) / 4);
        String[] stepNames = {"add", "edit", "copy", "delete"};
        Runnable[] stepCallbacks = {this::addAction, this::editSelected, this::duplicateSelected, this::deleteSelected};
        for (int i = 0; i < stepNames.length; i++)
            addButton(new Button(200 + i, 8 + i * (stepButtonWidth + 1), mainBottom + 3,
                stepButtonWidth, tr(stepNames[i]), stepCallbacks[i]));
        addButton(new Button(210, viewX, mainBottom + 3, 54, tr("play"), () -> {
            if (preview.scene() != null && preview.scene().isFinished()) preview.seek(0);
            preview.playing = !preview.playing;
        }));
        addButton(new Button(211, viewX + 58, mainBottom + 3, 42, tr("reset"), () -> { preview.playing = false; preview.seek(0); }));
        addButton(new Button(212, viewX + 104, mainBottom + 3, 54, tr("pick"), () -> { selecting = !selecting; preview.playing = false; }));
        if (viewWidth >= 226) addButton(new Button(213, viewX + 162, mainBottom + 3, 62, tr("clear_pick"), () -> { cornerA = null; cornerB = null; }));
        clamp(); rebuildPreview();
    }

    public SceneDocument document() { return document; }
    public void setStatus(String message, boolean failed) { status = message == null ? "Unknown error" : message; error = failed; }
    public void setStructure(String structure) {
        setStructure(document.scene().get("id").getAsString(), structure);
    }
    public void setStructure(String sceneId, String structure) {
        document.edit(pack -> {
            for (JsonElement element : pack.getAsJsonArray("scenes")) {
                JsonObject scene = element.getAsJsonObject();
                if (scene.get("id").getAsString().equals(sceneId)) scene.addProperty("structure", structure);
            }
        });
        cornerA = null; cornerB = null;
    }
    CuboidSelection selection() { return cornerA == null || cornerB == null ? null : new CuboidSelection(cornerA, cornerB); }

    private int rows() { return Math.max(1, (mainBottom - mainTop - 22) / 20); }
    private void clamp() {
        selected = Math.max(-1, Math.min(selected, document.steps().size() - 1));
        scroll = Math.max(0, Math.min(scroll, Math.max(0, document.steps().size() - rows())));
    }

    private void rebuildPreview() {
        if (compiledRevision == document.revision()) return;
        int time = preview.tick();
        try {
            preview.compile(document); preview.seek(time);
            setStatus(tr("preview_ready"), false);
        } catch (RuntimeException failure) {
            preview.invalidate(); setStatus(failure.getMessage(), true);
            Ponder.LOGGER.debug("Ponder editor preview could not compile", failure);
        }
        compiledRevision = document.revision(); clamp();
    }

    @Override public void updateScreen() {
        rebuildPreview(); preview.tickPreview();
        for (GuiButton button : buttonList) {
            if (button.id == 105) button.enabled = document.canUndo();
            if (button.id == 106) button.enabled = document.canRedo();
            if (button.id >= 201 && button.id <= 203) button.enabled = selected >= 0;
            if (button.id == 210) button.displayString = tr(preview.playing ? "pause" : "play");
            if (button.id == 212) button.displayString = tr(selecting ? "picking" : "pick");
        }
    }

    @Override public void drawScreen(int mx, int my, float partial) {
        drawRect(0, 0, width, height, BACKGROUND);
        fontRenderer.drawString("PONDER / " + tr("editor"), 10, 11, ACCENT);
        String title = document.scene().get("title").getAsString() + (document.isDirty() || path == null ? " *" : "");
        fontRenderer.drawString(fontRenderer.trimStringToWidth(title, Math.max(20, width - 165)), 159, 11, TEXT);
        drawRect(8, mainTop, leftWidth + 8, mainBottom, PANEL);
        fontRenderer.drawString(tr("steps") + " / " + document.steps().size(), 14, mainTop + 6, MUTED);
        JsonArray steps = document.steps();
        for (int i = scroll; i < Math.min(steps.size(), scroll + rows()); i++) {
            int y = mainTop + 21 + (i - scroll) * 20;
            JsonObject step = steps.get(i).getAsJsonObject();
            if (i == selected) drawRect(9, y - 2, leftWidth + 7, y + 17, 0xff2e4c43);
            fontRenderer.drawString(String.format(Locale.ROOT, "%03d", document.stepStartTick(i)), 14, y + 3, i == selected ? ACCENT : MUTED);
            String op = step.get("op").getAsString();
            String label = tr("op." + op);
            if (label.startsWith("ponder.editor.")) label = op.substring(op.indexOf('.') + 1);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(label, leftWidth - 38), 41, y + 3, TEXT);
        }
        drawRect(viewX, mainTop, width - 8, mainBottom, 0xff18212b);
        if (preview.scene() != null) {
            try { preview.render(viewX, mainTop, viewWidth, Math.max(8, mainBottom - mainTop),
                width, height, mx, my, partial, selection()); }
            catch (RuntimeException failure) { setStatus(failure.getMessage(), true); preview.playing = false; }
        }
        GlStateManager.disableDepth(); GlStateManager.disableLighting();
        var worldArea=preview.worldArea();int overlayX=worldArea==null?viewX:worldArea.x(),overlayWidth=worldArea==null?viewWidth:worldArea.width();
        fontRenderer.drawString(tr("preview"), overlayX + 8, mainTop + 7, MUTED);
        String hint = selecting ? tr("pick_hint") : tr("orbit_hint");
        fontRenderer.drawString(fontRenderer.trimStringToWidth(hint, Math.max(1,overlayWidth - 14)), overlayX + 7, mainBottom - 15, MUTED);
        if (selection() != null)
            fontRenderer.drawString(fontRenderer.trimStringToWidth(selection().toString(), Math.max(1,overlayWidth - 14)), overlayX + 7, mainBottom - 28, ACCENT);
        drawTimeline();
        fontRenderer.drawString(fontRenderer.trimStringToWidth(status, width - 20), 10, height - 26, error ? 0xffff9c9c : MUTED);
        fontRenderer.drawString(tr("shortcuts"), 10, height - 13, 0xff667c92);
        super.drawScreen(mx, my, partial);
        GlStateManager.color(1, 1, 1, 1);
    }

    private void drawTimeline() {
        int x = 12, y = height - 53, w = width - 24;
        drawRect(x, y, x + w, y + 24, PANEL);
        int total = preview.total();
        for (int t = 0; t <= 10; t++) {
            int tickX = x + t * w / 10;
            drawRect(tickX, y + 15, tickX + 1, y + 24, LINE);
        }
        JsonArray steps = document.steps();
        for (int i = 0; i < steps.size(); i++) {
            int start = document.stepStartTick(i);
            int marker = x + (int) Math.min(w - 1, (long) start * w / Math.max(1, total));
            drawRect(marker, y + 13, marker + 2, y + 22, i == selected ? ACCENT : 0xff6b91ad);
            JsonObject step = steps.get(i).getAsJsonObject();
            if (i == selected && step.has("duration")) {
                int end = x + (int) Math.min(w, ((long) start + step.get("duration").getAsInt()) * w / Math.max(1, total));
                drawRect(marker, y + 10, end, y + 13, ACCENT);
            }
        }
        int playhead = x + (int) ((long) preview.tick() * w / Math.max(1, total));
        drawRect(playhead, y, playhead + 2, y + 24, 0xfff3d597);
        String clock = tr("timeline") + "  " + preview.tick() + " / " + total + " t  (" + String.format(Locale.ROOT, "%.2f", preview.tick() / 20d) + " s)";
        fontRenderer.drawString(clock, x + 5, y + 2, TEXT);
    }

    private void addAction() {
        List<EditorListScreen.Entry<EditorActions.Action>> entries = new ArrayList<>();
        for (EditorActions.Action action : EditorActions.all(selection())) {
            String label = tr("op." + action.id());
            if (label.startsWith("ponder.editor.")) label = action.label();
            entries.add(new EditorListScreen.Entry<>(label, action.id(), action));
        }
        mc.displayGuiScreen(new EditorListScreen<>(this, tr("add_action"), entries, action -> {
            JsonObject template = action.template();
            suggestHandle(template);
            if (action.id().equals("overlay.show_gui")) {
                mc.displayGuiScreen(new net.createmod.ponder.client.guitutorial.GuiTutorialLibraryScreen(this,lesson->{
                    template.addProperty("tutorial",lesson.id().toString());
                    insertAction(action.id(),template);
                }));
            } else insertAction(action.id(),template);
        }));
    }

    private void insertAction(String operation,JsonObject template) {
        int index = selected < 0 ? Math.max(0, document.steps().size() - 1) : selected + 1;
        mc.displayGuiScreen(new EditorFormScreen(this, operation, template, selection(), changed -> {
            if(operation.equals("overlay.show_gui")){
                selected=EditorAnimations.insertGuiClip(document,index,changed);revealSelected();return;
            }
            SceneDocument candidate = new SceneDocument(document.snapshot()); candidate.selectScene(document.sceneIndex());
            candidate.insertStep(index, changed); ScenePackIO.validate(candidate.snapshot());
            document.insertStep(index, changed); selected = index; revealSelected();
        }));
    }

    private void suggestHandle(JsonObject template) {
        if (!template.has("handle")) return;
        String op = template.get("op").getAsString();
        String kind = op.contains("item") ? "item" : op.contains("minecart") ? "minecart" : op.contains("parrot") ? "parrot" : "section";
        String last = kind;
        boolean creates = op.contains("show_independent") || op.equals("world.make_independent") || op.contains("world.create_");
        int insertion = selected < 0 ? document.steps().size() - 1 : selected + 1;
        for (int i = 0; i < Math.min(insertion, document.steps().size()); i++) {
            JsonObject step = document.steps().get(i).getAsJsonObject();
            String source = step.get("op").getAsString();
            boolean sameKind = kind.equals("section")
                ? source.equals("world.make_independent") || source.startsWith("world.show_independent")
                : source.equals("world.create_" + kind);
            if (sameKind && step.has("handle")) last = step.get("handle").getAsString();
        }
        template.addProperty("handle", creates ? kind + "_" + (document.steps().size() + 1) : last);
    }

    private void animateSelection() {
        CuboidSelection selection = selection();
        if (selection == null) { selecting = true; preview.playing = false; setStatus(tr("animate_pick"), true); return; }
        JsonObject form = new JsonObject();
        form.addProperty("handle", "assembly_" + (document.steps().size() + 1));
        form.addProperty("dx", 0); form.addProperty("dy", 1); form.addProperty("dz", 0);
        form.addProperty("rx", 0); form.addProperty("ry", 90); form.addProperty("rz", 0);
        form.addProperty("duration", 20); form.addProperty("wait", true);
        int index = selected < 0 ? Math.max(0, document.steps().size() - 1) : selected + 1;
        mc.displayGuiScreen(new EditorFormScreen(this, tr("animate_detail"), form, selection, values -> {
            EditorAnimations.insert(document, index, values.get("handle").getAsString(), selection,
                new net.minecraft.util.math.Vec3d(values.get("dx").getAsDouble(), values.get("dy").getAsDouble(), values.get("dz").getAsDouble()),
                new net.minecraft.util.math.Vec3d(values.get("rx").getAsDouble(), values.get("ry").getAsDouble(), values.get("rz").getAsDouble()),
                values.get("duration").getAsBigDecimal().intValueExact(), values.get("wait").getAsBoolean());
            selected = index; revealSelected();
        }));
    }

    private void editSelected() {
        if (selected < 0) return;
        int index = selected; JsonObject step = document.steps().get(index).getAsJsonObject();
        mc.displayGuiScreen(new EditorFormScreen(this, step.get("op").getAsString(), step, selection(), changed -> {
            SceneDocument candidate = new SceneDocument(document.snapshot()); candidate.selectScene(document.sceneIndex());
            candidate.replaceStep(index, changed); ScenePackIO.validate(candidate.snapshot());
            document.replaceStep(index, changed);
        }));
    }
    private void duplicateSelected() {
        if (selected < 0) return;
        document.insertStep(selected + 1, document.steps().get(selected).getAsJsonObject()); selected++; revealSelected();
    }
    private void deleteSelected() {
        if (selected >= 0) { document.removeStep(selected); clamp(); }
    }
    private void revealSelected() {
        if (selected < scroll) scroll = selected;
        if (selected >= scroll + rows()) scroll = selected - rows() + 1;
    }

    private void metadata() {
        JsonObject original = document.scene(); original.remove("instructions");
        mc.displayGuiScreen(new EditorFormScreen(this, tr("metadata"), original, selection(), replacement -> {
            SceneDocument candidate = new SceneDocument(document.snapshot()); candidate.selectScene(document.sceneIndex());
            candidate.editScene(scene -> replacement.entrySet().forEach(e -> scene.add(e.getKey(), e.getValue())));
            ScenePackIO.validate(candidate.snapshot());
            document.editScene(scene -> replacement.entrySet().forEach(e -> scene.add(e.getKey(), e.getValue())));
        }));
    }

    private void moreActions() {
        String[] names = {"new", "scenes", "metadata", "redo", "close"};
        Runnable[] actions = {() -> protect(this::newDocument), this::browseScenes, this::metadata,
            () -> { document.redo(); mc.displayGuiScreen(this); }, () -> protect(() -> mc.displayGuiScreen(null))};
        List<EditorListScreen.Entry<Runnable>> entries = new ArrayList<>();
        for (int i = 0; i < names.length; i++) entries.add(new EditorListScreen.Entry<>(tr(names[i]), "", actions[i]));
        mc.displayGuiScreen(new EditorListScreen<>(this, tr("more"), entries, Runnable::run));
    }

    private void save() {
        try {
            Path target = path == null ? ScenePackIO.resolveId(EditorWorkspace.packs(),
                document.snapshot().get("id").getAsString(), ".ponder.json") : path;
            if (Files.exists(target)) {
                byte[] current = Files.readAllBytes(target);
                if (lastDiskContent == null || !Arrays.equals(current, lastDiskContent))
                    throw new IOException(tr("external_change"));
            }
            ScenePackIO.save(target, document); path = target; lastDiskContent = Files.readAllBytes(target);
            net.createmod.ponder.foundation.structure.PonderStructureLoader.invalidateCaches();
            PonderJsonLoader.ReloadResult result = PonderJsonLoader.reload();
            setStatus(tr("saved", target.getFileName()) + (result.errors > 0 ? " / " + result.errors + " reload error(s)" : ""), result.errors > 0);
        } catch (IOException | RuntimeException failure) { setStatus(failure.getMessage(), true); }
    }

    private void browsePacks() {
        List<EditorListScreen.Entry<Path>> entries = new ArrayList<>();
        try {
            Files.createDirectories(EditorWorkspace.packs());
            try (Stream<Path> stream = Files.walk(EditorWorkspace.packs())) {
                stream.filter(p -> p.toString().endsWith(".ponder.json") && !Files.isSymbolicLink(p)).sorted()
                    .forEach(p -> entries.add(new EditorListScreen.Entry<>(p.getFileName().toString(),
                        EditorWorkspace.packs().relativize(p).toString(), p)));
            }
            mc.displayGuiScreen(new EditorListScreen<>(this, tr("open_pack"), entries, p -> {
                try { EditorWorkspace.open(ScenePackIO.read(p), p); }
                catch (IOException failure) { throw new IllegalArgumentException(failure.getMessage(), failure); }
            }));
        } catch (IOException failure) { setStatus(failure.getMessage(), true); }
    }

    private void browseScenes() {
        List<EditorListScreen.Entry<Integer>> entries = new ArrayList<>();
        JsonArray scenes = document.snapshot().getAsJsonArray("scenes");
        for (int i = 0; i < scenes.size(); i++) {
            JsonObject scene = scenes.get(i).getAsJsonObject();
            entries.add(new EditorListScreen.Entry<>(scene.get("title").getAsString(), scene.get("id").getAsString(), i));
        }
        mc.displayGuiScreen(new EditorListScreen<>(this, tr("scenes"), entries, i -> {
            document.selectScene(i); selected = -1; scroll = 0; cornerA = null; cornerB = null;
            preview.seek(0); mc.displayGuiScreen(this);
        }));
    }

    private void newDocument() {
        JsonObject defaults = new JsonObject();
        defaults.addProperty("id", "local:scene_" + Long.toString(System.currentTimeMillis(), 36));
        defaults.addProperty("component", "minecraft:book"); defaults.addProperty("title", tr("new_scene"));
        defaults.addProperty("structure", "ponder:demo/basics");
        mc.displayGuiScreen(new EditorFormScreen(this, tr("new_scene"), defaults, null, values -> {
            SceneDocument created = SceneDocument.create(values.get("id").getAsString(), values.get("component").getAsString(),
                values.get("title").getAsString(), values.get("structure").getAsString());
            ScenePackIO.validate(created.snapshot());
            EditorWorkspace.open(created, null);
        }));
    }

    private void captureDialog() {
        JsonObject initial = new JsonObject();
        initial.addProperty("structure", document.scene().get("id").getAsString() + "/structure");
        mc.displayGuiScreen(new EditorFormScreen(this, tr("capture"), initial, null, values -> {
            try {
                EditorWorkspace.capture(values.get("structure").getAsString());
                setStatus(tr("capture_wait"), false);
            } catch (IOException failure) { throw new IllegalArgumentException(failure.getMessage(), failure); }
        }));
    }

    private void protect(Runnable action) {
        if (!document.isDirty() && path != null) { action.run(); return; }
        mc.displayGuiScreen(new GuiYesNo((confirmed, id) -> {
            if (confirmed) action.run(); else mc.displayGuiScreen(this);
        }, tr("unsaved"), tr("unsaved_detail"), 0));
    }

    private boolean inViewport(int mx, int my) { return mx >= viewX && mx < width - 8 && my >= mainTop && my < mainBottom; }
    private void scrub(int mx) { preview.playing = false; preview.seek((int) ((long) Math.max(0, Math.min(width - 24, mx - 12)) * preview.total() / Math.max(1, width - 24))); }

    @Override protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button == 0 && my >= height - 53 && my < height - 29) { scrubbing = true; scrub(mx); return; }
        if (inViewport(mx, my)) {
            if(preview.guiContains(mx,my))return;
            if (selecting && (button == 0 || button == 1)) {
                BlockPos picked = preview.pick(mx, my);
                if (picked != null) {
                    if (button == 0) cornerA = picked; else cornerB = picked;
                    if (cornerA != null && cornerB == null) cornerB = cornerA;
                }
            } else if (button == 1 || button == 2) {
                orbiting = true; lastMouseX = mx; lastMouseY = my;
            }
            return;
        }
        if (button == 0 && mx >= 8 && mx < leftWidth + 8 && my >= mainTop + 21 && my < mainBottom) {
            int index = scroll + (my - mainTop - 21) / 20;
            if (index < document.steps().size()) {
                selected = index; preview.playing = false; preview.seek(document.stepStartTick(index));
                dragStep = index; dragY = my; draggedStep = false;
            }
            return;
        }
        super.mouseClicked(mx, my, button);
    }

    @Override protected void mouseClickMove(int mx, int my, int button, long elapsed) {
        if (scrubbing) scrub(mx);
        if (orbiting) {
            preview.yaw += (mx - lastMouseX) * .7f;
            preview.pitch = Math.max(-80, Math.min(80, preview.pitch + (my - lastMouseY) * .7f));
            lastMouseX = mx; lastMouseY = my;
        }
        if (dragStep >= 0 && Math.abs(my - dragY) > 5) draggedStep = true;
    }

    @Override protected void mouseReleased(int mx, int my, int state) {
        if (dragStep >= 0 && draggedStep && mx >= 8 && mx < leftWidth + 8) {
            int target = Math.max(0, Math.min(document.steps().size() - 1, scroll + (my - mainTop - 21) / 20));
            document.moveStep(dragStep, target); selected = target; revealSelected();
        }
        scrubbing = false; orbiting = false; dragStep = -1;
        super.mouseReleased(mx, my, state);
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput(); int wheel = Mouse.getEventDWheel(); if (wheel == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height-Mouse.getEventY()*height/mc.displayHeight-1;
        if(preview.scrollGui(wheel,mx,my))return;
        if (mx < leftWidth + 8) scroll = Math.max(0, Math.min(scroll - Integer.signum(wheel), Math.max(0, document.steps().size() - rows())));
        else preview.zoom = Math.max(.35f, Math.min(3.5f, preview.zoom * (wheel > 0 ? .9f : 1.1f)));
    }
    @Override protected void actionPerformed(GuiButton button) { if (button instanceof Button) ((Button) button).activate(); }
    @Override protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { protect(() -> mc.displayGuiScreen(null)); return; }
        if (isCtrlKeyDown()) {
            if (key == Keyboard.KEY_S) save();
            if (key == Keyboard.KEY_Z) { if (isShiftKeyDown()) document.redo(); else document.undo(); }
            if (key == Keyboard.KEY_Y) document.redo();
            if (key == Keyboard.KEY_D) duplicateSelected();
            return;
        }
        if (key == Keyboard.KEY_SPACE) preview.playing = !preview.playing;
        if (key == Keyboard.KEY_DELETE) deleteSelected();
        if (key == Keyboard.KEY_RETURN) editSelected();
        if (key == Keyboard.KEY_INSERT) addAction();
        if (key == Keyboard.KEY_LEFT) { preview.playing = false; preview.seek(preview.tick() - 1); }
        if (key == Keyboard.KEY_RIGHT) { preview.playing = false; preview.seek(preview.tick() + 1); }
        if (isShiftKeyDown() && selected >= 0 && (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN)) {
            int target = Math.max(0, Math.min(document.steps().size() - 1, selected + (key == Keyboard.KEY_UP ? -1 : 1)));
            document.moveStep(selected, target); selected = target; revealSelected();
        }
    }
    @Override public void onGuiClosed() { preview.playing = false; scrubbing = false; orbiting = false; }
    @Override public boolean doesGuiPauseGame() { return false; }
}
