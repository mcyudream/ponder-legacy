package net.createmod.ponder.api.theme;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

/** Immutable, side-neutral appearance data. Themes never change authored scene content. */
public final class PonderTheme {
    public static final ResourceLocation CLASSIC = new ResourceLocation("ponder:classic");
    public static final ResourceLocation MODERN = new ResourceLocation("ponder:modern");

    public enum Layout { CLASSIC, MODERN }

    public enum Color {
        BACKGROUND_TOP(0xff101215), BACKGROUND_BOTTOM(0xff101215),
        HEADER(0xff171b20), FOOTER(0xff171b20),
        PANEL(0xff171e25), PANEL_ALT(0xff161b21), PANEL_BORDER(0xff586b79),
        TEXT(0xffeef2f5), MUTED(0xffaeb8c2), SUBTLE(0xff8d9eab),
        ACCENT(0xff9fd6d2), SELECTED_TEXT(0xffbce8df),
        BUTTON(0xff292f36), BUTTON_HOVER(0xff3b4652), BUTTON_ACTIVE(0xff31525a),
        BUTTON_DISABLED(0xff20242a), BORDER(0xff58636e), BORDER_BOTTOM(0xff58636e),
        BORDER_HOVER(0xff9fb5c9), BORDER_HOVER_BOTTOM(0xff9fb5c9),
        BORDER_ACTIVE(0xff9fd6d2), BORDER_ACTIVE_BOTTOM(0xff9fd6d2),
        BORDER_DISABLED(0xff3d444c), BORDER_DISABLED_BOTTOM(0xff3d444c),
        ICON(0xffe3e8ed), ICON_DISABLED(0xff68727e),
        PROGRESS_TRACK(0xff303740), PROGRESS_FILL(0xff94aeca), KEYFRAME(0xffd8e1ea),
        PROGRESS_FILL_BOTTOM(0xff94aeca), PROGRESS_BORDER(0x40ffeedd), PROGRESS_BORDER_BOTTOM(0x20ffeedd),
        MENU_HOVER(0xff354752), MENU_SELECTED(0xff24393f),
        SCROLL_TRACK(0xff232c35), SCROLL_THUMB(0xff78949f), SCROLL_ACTIVE(0xffb8e3df),
        SPEECH_BACKGROUND(0xcc101418), SPEECH_TEXT(0xffeeeeee),
        SPEECH_BORDER(0x607a6000), SPEECH_BORDER_BOTTOM(0x207a6000),
        TITLE_BACKGROUND(0xff000000), TITLE_BORDER(0x40ffeedd), TITLE_BORDER_BOTTOM(0x20ffeedd),
        STREAK(0xff101010), ERROR(0xffff8888);

        public final int legacy;
        Color(int legacy) { this.legacy = legacy; }
        public String key() { return name().toLowerCase(Locale.ROOT); }
    }

    private final ResourceLocation id;
    private final String name, description;
    private final Layout layout;
    private final Map<Color, Integer> colors;

    private PonderTheme(ResourceLocation id, String name, String description, Layout layout,
                        Map<Color, Integer> colors) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.layout = layout;
        this.colors = Collections.unmodifiableMap(new EnumMap<>(colors));
    }

    public static PonderTheme classic() {
        EnumMap<Color, Integer> colors = new EnumMap<>(Color.class);
        for (Color color : Color.values()) colors.put(color, color.legacy);
        return new PonderTheme(CLASSIC, "ponder.theme.classic.name", "ponder.theme.classic.description",
            Layout.CLASSIC, colors);
    }

    public static PonderTheme parse(ResourceLocation id, JsonObject json, PonderTheme parent) {
        for (String key : json.keySet())
            if (!java.util.Set.of("parent", "name", "description", "layout", "colors").contains(key))
                throw new IllegalArgumentException("Unknown theme field: " + key);
        PonderTheme base = parent == null ? classic() : parent;
        String name = text(json, "name", id.toString());
        String description = text(json, "description", "");
        if (json.has("parent")) {
            if (!json.get("parent").isJsonPrimitive() || !json.getAsJsonPrimitive("parent").isString())
                throw new IllegalArgumentException("Theme parent must be an ID string");
            PonderThemeLoader.id(json.get("parent").getAsString());
        }
        Layout layout = json.has("layout")
            ? Layout.valueOf(text(json, "layout", "").toUpperCase(Locale.ROOT)) : base.layout;
        EnumMap<Color, Integer> colors = new EnumMap<>(base.colors);
        if (json.has("colors")) {
            if (!json.get("colors").isJsonObject()) throw new IllegalArgumentException("Theme colors must be an object");
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("colors").entrySet()) {
                Color role;
                try { role = Color.valueOf(entry.getKey().toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException unknown) {
                    throw new IllegalArgumentException("Unknown theme color: " + entry.getKey());
                }
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString())
                    throw new IllegalArgumentException("Theme color must be #RRGGBB or #AARRGGBB: " + entry.getKey());
                colors.put(role, parseColor(entry.getValue().getAsString()));
            }
        }
        return new PonderTheme(id, name, description, layout, colors);
    }

    public static int parseColor(String value) {
        if (value == null || !value.matches("#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})"))
            throw new IllegalArgumentException("Theme color must be #RRGGBB or #AARRGGBB: " + value);
        long parsed = Long.parseLong(value.substring(1), 16);
        return (int) (value.length() == 7 ? parsed | 0xff000000L : parsed);
    }

    private static String text(JsonObject json, String key, String fallback) {
        if (!json.has(key)) return fallback;
        JsonElement value = json.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || value.getAsString().length() > 1024)
            throw new IllegalArgumentException("Invalid theme " + key);
        return value.getAsString();
    }

    public ResourceLocation id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public Layout layout() { return layout; }
    public int color(Color role) { return colors.get(role); }
    public Map<Color, Integer> colors() { return colors; }
}
