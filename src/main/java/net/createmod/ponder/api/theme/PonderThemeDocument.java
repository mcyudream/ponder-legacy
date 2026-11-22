package net.createmod.ponder.api.theme;

import java.util.LinkedHashMap;
import java.util.Map;
import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

/** The local JSON control file, independent of resource-pack installation and CraftTweaker. */
public record PonderThemeDocument(Map<ResourceLocation, JsonObject> themes, ResourceLocation defaultTheme) {
    public static PonderThemeDocument parse(JsonObject root) {
        for (String key : root.keySet())
            if (!java.util.Set.of("default", "themes").contains(key))
                throw new IllegalArgumentException("Unknown theme document field: " + key);
        ResourceLocation selected = root.has("default")
            ? PonderThemeLoader.id(root.get("default").getAsString()) : null;
        Map<ResourceLocation, JsonObject> themes = new LinkedHashMap<>();
        if (root.has("themes")) {
            if (!root.get("themes").isJsonObject()) throw new IllegalArgumentException("themes must be an object");
            if (root.getAsJsonObject("themes").size() > 256) throw new IllegalArgumentException("Too many local themes");
            for (var entry : root.getAsJsonObject("themes").entrySet()) {
                ResourceLocation id = PonderThemeLoader.id(entry.getKey());
                if (!entry.getValue().isJsonObject()) throw new IllegalArgumentException("Theme must be an object: " + id);
                JsonObject json = entry.getValue().getAsJsonObject().deepCopy();
                PonderTheme.parse(id, json, null);
                if (json.has("parent")) PonderThemeLoader.id(json.get("parent").getAsString());
                themes.put(id, json);
            }
        }
        return new PonderThemeDocument(java.util.Collections.unmodifiableMap(themes), selected);
    }
}
