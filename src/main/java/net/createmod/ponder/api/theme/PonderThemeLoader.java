package net.createmod.ponder.api.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

/** Resolves resource-pack theme inheritance without any client or OpenGL classes. */
public final class PonderThemeLoader {
    public record Result(Map<ResourceLocation, PonderTheme> themes, List<String> errors) {}
    private final Map<ResourceLocation, JsonObject> definitions;
    private final Map<ResourceLocation, PonderTheme> fallbacks;
    private final Map<ResourceLocation, PonderTheme> resolved = new LinkedHashMap<>();
    private final Set<ResourceLocation> resolving = new LinkedHashSet<>();
    private final Set<ResourceLocation> failed = new LinkedHashSet<>();
    private final List<String> errors = new ArrayList<>();

    public PonderThemeLoader(Map<ResourceLocation, JsonObject> definitions,
                             Map<ResourceLocation, PonderTheme> fallbacks) {
        this.definitions = new LinkedHashMap<>(definitions);
        this.fallbacks = new LinkedHashMap<>(fallbacks);
    }

    public Result load() {
        for (ResourceLocation id : definitions.keySet()) {
            try { resolve(id); }
            catch (RuntimeException failure) { errors.add(id + ": " + failure.getMessage()); }
        }
        Map<ResourceLocation, PonderTheme> result = new LinkedHashMap<>(fallbacks);
        result.putAll(resolved);
        return new Result(Collections.unmodifiableMap(result), List.copyOf(errors));
    }

    private PonderTheme resolve(ResourceLocation id) {
        if (resolved.containsKey(id)) return resolved.get(id);
        if (failed.contains(id)) throw new IllegalArgumentException("Invalid parent theme " + id);
        JsonObject json = definitions.get(id);
        if (json == null) {
            if (fallbacks.containsKey(id)) return fallbacks.get(id);
            throw new IllegalArgumentException("Missing parent theme " + id);
        }
        if (!resolving.add(id)) {
            boolean cycle = false;
            for (ResourceLocation member : resolving) {
                if (member.equals(id)) cycle = true;
                if (cycle) failed.add(member);
            }
            throw new IllegalArgumentException("Theme inheritance cycle: " + resolving + " -> " + id);
        }
        try {
            PonderTheme parent = null;
            if (json.has("parent")) parent = resolve(id(json.get("parent").getAsString()));
            else if (!id.equals(PonderTheme.CLASSIC)) parent = resolve(PonderTheme.CLASSIC);
            if (failed.contains(id)) throw new IllegalArgumentException("Theme inheritance cycle includes " + id);
            PonderTheme theme = PonderTheme.parse(id, json, parent);
            resolved.put(id, theme);
            return theme;
        } catch (RuntimeException failure) {
            failed.add(id);
            if (fallbacks.containsKey(id)) {
                PonderTheme fallback = fallbacks.get(id);
                resolved.put(id, fallback);
                errors.add(id + ": " + failure.getMessage());
                return fallback;
            }
            throw failure;
        } finally { resolving.remove(id); }
    }

    public static ResourceLocation id(String value) {
        if (value == null || !value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || value.contains(".."))
            throw new IllegalArgumentException("Invalid theme ID: " + value);
        return new ResourceLocation(value);
    }
}
