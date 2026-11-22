package net.createmod.ponder.api.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

/** Local pack author overrides. This data is never sent by a server to its players. */
public final class PonderThemeOverrides {
    private static final Map<ResourceLocation, JsonObject> SCRIPTS = new LinkedHashMap<>();
    private static ResourceLocation defaultTheme;
    private static long revision;

    private PonderThemeOverrides() {}

    public static synchronized void register(ResourceLocation id, JsonObject definition, boolean patch) {
        // Validate individual fields now; resolve parents after resources and all scripts are available.
        PonderTheme.parse(id, definition, null);
        if (definition.has("parent")) PonderThemeLoader.id(definition.get("parent").getAsString());
        if (!patch && SCRIPTS.containsKey(id))
            throw new IllegalArgumentException("Duplicate scripted Ponder theme: " + id);
        if (SCRIPTS.size() >= 256 && !SCRIPTS.containsKey(id))
            throw new IllegalArgumentException("Too many scripted Ponder themes");
        SCRIPTS.put(id, patch && SCRIPTS.containsKey(id) ? merge(SCRIPTS.get(id), definition) : definition.deepCopy());
        revision++;
    }

    public static synchronized Map<ResourceLocation, JsonObject> snapshot() {
        Map<ResourceLocation, JsonObject> copy = new LinkedHashMap<>();
        SCRIPTS.forEach((id, json) -> copy.put(id, json.deepCopy()));
        return Collections.unmodifiableMap(copy);
    }

    public static synchronized void setDefault(ResourceLocation id) { defaultTheme = id; revision++; }
    public static synchronized ResourceLocation defaultTheme() { return defaultTheme; }
    public static synchronized long revision() { return revision; }

    /** Merge appearance patches, preserving inherited fields and unmodified color roles. */
    public static JsonObject merge(JsonObject base, JsonObject patch) {
        JsonObject merged = base == null ? new JsonObject() : base.deepCopy();
        patch.entrySet().forEach(entry -> {
            if (entry.getKey().equals("colors") && entry.getValue().isJsonObject()
                && merged.has("colors") && merged.get("colors").isJsonObject()) {
                entry.getValue().getAsJsonObject().entrySet().forEach(color ->
                    merged.getAsJsonObject("colors").add(color.getKey(), color.getValue().deepCopy()));
            } else merged.add(entry.getKey(), entry.getValue().deepCopy());
        });
        return merged;
    }
}
