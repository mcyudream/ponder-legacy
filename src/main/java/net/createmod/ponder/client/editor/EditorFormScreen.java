package net.createmod.ponder.client.editor;

import net.createmod.ponder.client.theme.PonderThemes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.lwjgl.input.Keyboard;
import net.createmod.ponder.api.editor.CuboidSelection;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import static net.createmod.ponder.client.editor.EditorWidgets.*;

/** Typed forms share the JSON operation contract and never silently coerce invalid numbers. */
final class EditorFormScreen extends GuiScreen {
    private final GuiScreen parent;
    private final String title;
    private final JsonObject value;
    private final Consumer<JsonObject> accept;
    private final CuboidSelection selection;
    private final List<Field> fields = new ArrayList<>();
    private int page, perPage;
    private String error = "";

    EditorFormScreen(GuiScreen parent, String title, JsonObject initial, CuboidSelection selection,
                     Consumer<JsonObject> accept) {
        this.parent = parent; this.title = title; this.value = initial.deepCopy();
        this.accept = accept; this.selection = selection;
        for (Map.Entry<String, JsonElement> entry : value.entrySet())
            if (!"op".equals(entry.getKey())) fields.add(new Field(entry.getKey(), entry.getValue()));
    }

    @Override public void initGui() {
        stash();
        Keyboard.enableRepeatEvents(true);
        perPage = Math.max(2, (height - 112) / 29);
        page = Math.min(page, Math.max(0, (fields.size() - 1) / perPage));
        buttonList.clear();
        int left = Math.max(12, (width - 500) / 2);
        int inputX = left + Math.min(122, width / 3), inputWidth = Math.min(345, width - inputX - left);
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i); field.widget = null;
            if (i < page * perPage || i >= (page + 1) * perPage) continue;
            int y = 50 + (i % perPage) * 29;
            if (field.original.isJsonPrimitive() && field.original.getAsJsonPrimitive().isBoolean()) {
                addButton(new Button(i, inputX, y, inputWidth, field.text, () -> {
                    stash(); field.text = Boolean.toString(!Boolean.parseBoolean(field.text)); initGui();
                }));
            } else if (field.key.equals("selection") && selection != null) {
                field.widget = new GuiTextField(i, fontRenderer, inputX, y, Math.max(60, inputWidth - 80), 20);
                field.widget.setMaxStringLength(16384); field.widget.setText(field.text);
                addButton(new Button(i, inputX + inputWidth - 76, y, 76, tr("use_selection"), () -> {
                    stash(); field.text = selection.toJson().toString(); initGui();
                }));
            } else {
                field.widget = new GuiTextField(i, fontRenderer, inputX, y, inputWidth, 20);
                field.widget.setMaxStringLength(16384); field.widget.setText(field.text);
            }
        }
        int bottom = height - 34;
        addButton(new Button(2001, left, bottom, 74, tr("cancel"), () -> mc.displayGuiScreen(parent)));
        addButton(new Button(2002, width - left - 80, bottom, 80, tr("apply"), this::apply));
        if (fields.size() > perPage) {
            addButton(new Button(2003, width / 2 - 62, bottom, 28, "<", () -> { stash(); page = Math.max(0, page - 1); initGui(); }));
            addButton(new Button(2004, width / 2 + 34, bottom, 28, ">", () -> {
                stash(); page = Math.min((fields.size() - 1) / perPage, page + 1); initGui();
            }));
        }
    }

    private void stash() { for (Field field : fields) if (field.widget != null) field.text = field.widget.getText(); }

    private void apply() {
        try {
            stash(); JsonObject result = value.deepCopy();
            for (Field field : fields) {
                JsonElement parsed;
                if (field.original.isJsonPrimitive() && field.original.getAsJsonPrimitive().isString()) {
                    parsed = new JsonPrimitive(field.text);
                } else {
                    parsed = new JsonParser().parse(field.text);
                    if (field.original.isJsonPrimitive() && field.original.getAsJsonPrimitive().isNumber()) {
                        if (!parsed.isJsonPrimitive() || !parsed.getAsJsonPrimitive().isNumber()
                            || !Double.isFinite(parsed.getAsDouble())) throw new IllegalArgumentException(field.key + ": expected a finite number");
                    }
                }
                result.add(field.key, parsed);
            }
            accept.accept(result);
            if (mc.currentScreen == this) mc.displayGuiScreen(parent);
        } catch (RuntimeException failure) {
            error = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
        }
    }

    @Override public void drawScreen(int mx, int my, float partial) {
        drawRect(0, 0, width, height, PonderThemes.color(BACKGROUND));
        int left = Math.max(12, (width - 500) / 2);
        fontRenderer.drawString(tr("edit") + " / " + title, left, 15, PonderThemes.color(TEXT));
        fontRenderer.drawString(tr("form_hint"), left, 31, PonderThemes.color(MUTED));
        for (int i = page * perPage; i < Math.min(fields.size(), (page + 1) * perPage); i++) {
            Field field = fields.get(i);
            String key = tr("param." + field.key);
            if (key.startsWith("ponder.editor.")) key = field.key;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(key, Math.min(115, width / 3 - 5)),
                left, 56 + (i % perPage) * 29, PonderThemes.color(MUTED));
            if (field.widget != null) field.widget.drawTextBox();
        }
        fontRenderer.drawString(fontRenderer.trimStringToWidth(error, width - left * 2), left, height - 52, PonderThemes.color(0xffff9c9c));
        if (fields.size() > perPage)
            drawCenteredString(fontRenderer, (page + 1) + "/" + ((fields.size() - 1) / perPage + 1), width / 2, height - 28, PonderThemes.color(MUTED));
        super.drawScreen(mx, my, partial); GlStateManager.color(1, 1, 1, 1);
    }

    @Override protected void mouseClicked(int mx, int my, int button) throws IOException {
        for (Field field : fields) if (field.widget != null) field.widget.mouseClicked(mx, my, button);
        super.mouseClicked(mx, my, button);
    }
    @Override protected void actionPerformed(GuiButton button) { if (button instanceof Button) ((Button) button).activate(); }
    @Override protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        if (key == Keyboard.KEY_RETURN && isCtrlKeyDown()) { apply(); return; }
        List<GuiTextField> inputs = new ArrayList<>();
        for (Field field : fields) if (field.widget != null) inputs.add(field.widget);
        if (key == Keyboard.KEY_TAB && !inputs.isEmpty()) {
            int focused = -1;
            for (int i = 0; i < inputs.size(); i++) if (inputs.get(i).isFocused()) focused = i;
            for (GuiTextField input : inputs) input.setFocused(false);
            inputs.get((focused + 1) % inputs.size()).setFocused(true); return;
        }
        for (GuiTextField input : inputs) input.textboxKeyTyped(c, key);
    }
    @Override public void updateScreen() { for (Field field : fields) if (field.widget != null) field.widget.updateCursorCounter(); }
    @Override public void onGuiClosed() { stash(); Keyboard.enableRepeatEvents(false); }
    @Override public boolean doesGuiPauseGame() { return false; }

    private static final class Field {
        final String key;
        final JsonElement original;
        String text;
        GuiTextField widget;
        Field(String key, JsonElement value) {
            this.key = key; original = value;
            text = value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : value.toString();
        }
    }
}
