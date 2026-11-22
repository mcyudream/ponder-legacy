package net.createmod.ponder.client.theme;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.createmod.catnip.config.ConfigRegistry;
import net.createmod.catnip.config.ConfigType;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.theme.PonderTheme;
import net.createmod.ponder.api.theme.PonderTheme.Color;
import net.createmod.ponder.api.theme.PonderThemeLoader;
import net.createmod.ponder.api.theme.PonderThemeDocument;
import net.createmod.ponder.api.theme.PonderThemeOverrides;
import net.createmod.ponder.config.CClient;
import net.createmod.ponder.enums.PonderConfig;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-owned selection and resource-pack discovery. Server scenes never select a UI theme. */
@SideOnly(Side.CLIENT)
public final class PonderThemes {
    private static final ResourceLocation INDEX = new ResourceLocation("ponder:themes.json");
    private static final Map<ResourceLocation, JsonObject> BUILTIN_JSON = builtins();
    private static final Map<ResourceLocation, PonderTheme> BUILTINS =
        new PonderThemeLoader(BUILTIN_JSON, Map.of(PonderTheme.CLASSIC, PonderTheme.classic())).load().themes();
    private static final Map<Integer, Color> LEGACY_COLORS = aliases();
    private static volatile Map<ResourceLocation, PonderTheme> themes = BUILTINS;
    private static volatile List<String> errors = List.of();
    private static volatile long revision;
    private static long scriptRevision = -1;
    private static Map<ResourceLocation, JsonObject> resources = BUILTIN_JSON;
    private static List<String> resourceErrors = List.of();
    private static PonderThemeDocument local = new PonderThemeDocument(Map.of(), null);
    private static java.io.File localFile;
    private static long choiceRevision = Long.MIN_VALUE;
    private static String choice;
    private static PonderTheme choiceTheme;

    private PonderThemes() {}

    public static PonderTheme current() {
        refreshScripts();
        String selection = PonderConfig.client().getTheme();
        if (choiceRevision == revision && java.util.Objects.equals(choice, selection)) return choiceTheme;
        PonderTheme fallback = themes.getOrDefault(PonderTheme.CLASSIC, BUILTINS.get(PonderTheme.CLASSIC));
        try {
            ResourceLocation id = "auto".equals(selection) ? defaultTheme() : PonderThemeLoader.id(selection);
            choiceTheme = themes.getOrDefault(id, fallback);
        } catch (IllegalArgumentException malformed) { choiceTheme = fallback; }
        choice = selection;
        choiceRevision = revision;
        return choiceTheme;
    }

    public static List<PonderTheme> all() { refreshScripts(); return List.copyOf(themes.values()); }
    public static List<String> errors() { refreshScripts(); return errors; }
    public static long revision() { refreshScripts(); return revision; }
    public static void setLocalFile(java.io.File file) { localFile = file; }
    public static ResourceLocation defaultTheme() {
        ResourceLocation script = PonderThemeOverrides.defaultTheme();
        return script != null ? script : local.defaultTheme() != null ? local.defaultTheme() : PonderTheme.CLASSIC;
    }
    public static boolean modern() { return current().layout() == PonderTheme.Layout.MODERN; }
    public static int color(Color role) { return current().color(role); }

    /** Maps legacy chrome colors while retaining exact classic pixels and authored semantic colors. */
    public static int color(int legacy) {
        Color role = LEGACY_COLORS.get(legacy);
        if (role == null) return legacy;
        int color = current().color(role);
        return color == role.legacy ? legacy : color;
    }

    public static void select(ResourceLocation id) {
        refreshScripts();
        if (!themes.containsKey(id)) throw new IllegalArgumentException("Unknown UI theme: " + id);
        saveSelection(id.toString());
    }

    public static void selectAutomatic() { saveSelection("auto"); }

    private static void saveSelection(String value) {
        var config = ConfigRegistry.get(Ponder.MOD_ID, ConfigType.CLIENT);
        if (config instanceof CClient client) {
            client.uiTheme.set(value);
            client.onReload();
        } else PonderConfig.client().setTheme(value);
        revision++;
    }

    public static void reload(IResourceManager manager) {
        Map<ResourceLocation, JsonObject> definitions = new LinkedHashMap<>(BUILTIN_JSON);
        java.util.Set<ResourceLocation> ids = new java.util.LinkedHashSet<>(BUILTIN_JSON.keySet());
        List<String> failures = new ArrayList<>();
        try {
            for (IResource resource : manager.getAllResources(INDEX)) {
                try (resource; Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject index = new JsonParser().parse(reader).getAsJsonObject();
                    for (var entry : index.getAsJsonArray("themes")) ids.add(PonderThemeLoader.id(entry.getAsString()));
                } catch (Exception failure) { failures.add("Theme index " + resource.getResourcePackName() + ": " + failure.getMessage()); }
            }
        } catch (java.io.IOException missing) { /* Built-ins remain available before resource packs finish loading. */ }
        for (ResourceLocation id : ids) {
            ResourceLocation location = new ResourceLocation(id.getNamespace(), "themes/" + id.getPath() + ".json");
            try (IResource resource = manager.getResource(location);
                 Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                definitions.put(id, new JsonParser().parse(reader).getAsJsonObject());
            } catch (Exception failure) { failures.add(location + ": " + failure.getMessage()); }
        }
        if (localFile != null && localFile.isFile()) {
            try {
                if (java.nio.file.Files.size(localFile.toPath()) > 262144)
                    throw new IllegalArgumentException("Local themes.json exceeds 256 KiB");
                try (Reader reader = java.nio.file.Files.newBufferedReader(localFile.toPath(), StandardCharsets.UTF_8)) {
                    local = PonderThemeDocument.parse(new JsonParser().parse(reader).getAsJsonObject());
                }
            } catch (Exception failure) {
                failures.add("config/ponder/themes.json (retaining previous definitions): " + failure.getMessage());
            }
        } else local = new PonderThemeDocument(Map.of(), null);
        resources = definitions;
        resourceErrors = List.copyOf(failures);
        rebuild();
    }

    private static void refreshScripts() {
        if (scriptRevision != PonderThemeOverrides.revision()) rebuild();
    }

    private static synchronized void rebuild() {
        Map<ResourceLocation, JsonObject> definitions = new LinkedHashMap<>(resources);
        local.themes().forEach((id, json) -> definitions.put(id, PonderThemeOverrides.merge(definitions.get(id), json)));
        PonderThemeOverrides.snapshot().forEach((id, json) ->
            definitions.put(id, PonderThemeOverrides.merge(definitions.get(id), json)));
        PonderThemeLoader.Result result = new PonderThemeLoader(definitions, BUILTINS).load();
        List<String> failures = new ArrayList<>(resourceErrors);
        failures.addAll(result.errors());
        if (!result.themes().containsKey(defaultTheme()))
            failures.add("Unknown default theme: " + defaultTheme());
        themes = result.themes();
        errors = List.copyOf(failures);
        scriptRevision = PonderThemeOverrides.revision();
        revision++;
        for (String failure : failures) Ponder.LOGGER.warn("Ponder UI theme: {}", failure);
    }

    private static Map<ResourceLocation, JsonObject> builtins() {
        Map<ResourceLocation, JsonObject> result = new LinkedHashMap<>();
        for (ResourceLocation id : List.of(PonderTheme.CLASSIC, PonderTheme.MODERN)) {
            String path = "/assets/ponder/themes/" + id.getPath() + ".json";
            try (var stream = PonderThemes.class.getResourceAsStream(path)) {
                if (stream == null) throw new IllegalStateException("Missing built-in theme " + path);
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    result.put(id, new JsonParser().parse(reader).getAsJsonObject());
                }
            } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot read " + path, failure); }
        }
        return result;
    }

    private static Map<Integer, Color> aliases() {
        Map<Integer, Color> result = new LinkedHashMap<>();
        for (Color role : Color.values()) result.putIfAbsent(role.legacy, role);
        alias(result, Color.HEADER, 0xdd171b20, 0xaa171b20);
        alias(result, Color.BACKGROUND_TOP, 0xff111821, 0xff101820, 0xff111820);
        alias(result, Color.PANEL, 0xff1a2431, 0xff18212a, 0xff1c2932, 0xff202b34, 0xff202c35, 0xff17252c);
        alias(result, Color.PANEL_BORDER, 0xff304255, 0xff364651, 0xff3a4f5c, 0xff303f4a);
        alias(result, Color.TEXT, 0xffe4edf5, 0xffd6e0e8, 0xffd6dee5, 0xffd6e2e5, 0xffd8e3e8, 0xffd9e1e8, 0xffc9d6dc, 0xffeef5f8);
        alias(result, Color.MUTED, 0xff9baec2, 0xff91a0ac, 0xffb5c2cd, 0xffaab9c4, 0xffb5c8d1, 0xffcadbdc);
        alias(result, Color.SUBTLE, 0xff7c8a95, 0xff82939e, 0xff667d8d, 0xff5c6a79);
        alias(result, Color.ACCENT, 0xff72e3bd, 0xffa9ddd4, 0xffe3faf2);
        alias(result, Color.SELECTED_TEXT, 0xffc9efeb);
        alias(result, Color.BUTTON_HOVER, 0xff29343e, 0xff355648, 0xff365c70);
        alias(result, Color.PANEL_ALT, 0xff191f26, 0xff18212b, 0xff1e2931);
        alias(result, Color.PANEL_BORDER, 0xff35434e, 0xff3c4c57, 0xff27313d);
        alias(result, Color.MENU_HOVER, 0xff374955, 0xff375564);
        alias(result, Color.MENU_SELECTED, 0xff273c46, 0xff273d49, 0xff2e4c43);
        alias(result, Color.TEXT, 0xffd3dfe6, 0xffd5dfe5, 0xffc3d6dd, 0xffb8d8df);
        alias(result, Color.MUTED, 0xffb7c9d5, 0xff9dabb8, 0xff8fabb9, 0xff8aa6b4);
        alias(result, Color.SUBTLE, 0xff7c8b99, 0xff667c92);
        alias(result, Color.ACCENT, 0xff96bab8, 0xffb5e8da);
        alias(result, Color.SCROLL_TRACK, 0xff26323c, 0xff455d6b);
        alias(result, Color.SCROLL_THUMB, 0xff8fbdb9, 0xff6b91ad);
        alias(result, Color.ERROR, 0xfff0a69e, 0xffe49c93, 0xffff9c9c);
        alias(result, Color.PANEL, 0xe51a252f, 0xff1c2a33);
        alias(result, Color.TEXT, 0xffd7e3e8, 0xffe6edf1, 0xffe0f2ef);
        alias(result, Color.MUTED, 0xff819ba8, 0xff849aa5);
        alias(result, Color.ACCENT, 0xff9fded4);
        alias(result, Color.PANEL_BORDER, 0xff36505b);
        alias(result, Color.PROGRESS_TRACK, 0xff3b4e5a);
        return Map.copyOf(result);
    }

    private static void alias(Map<Integer, Color> colors, Color role, int... values) {
        for (int value : values) colors.put(value, role);
    }
}
