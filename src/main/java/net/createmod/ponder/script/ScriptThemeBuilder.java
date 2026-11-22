package net.createmod.ponder.script;

import com.google.gson.JsonObject;
import crafttweaker.annotations.ZenRegister;
import net.createmod.ponder.api.theme.PonderTheme;
import net.createmod.ponder.api.theme.PonderThemeLoader;
import net.createmod.ponder.api.theme.PonderThemeOverrides;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ponder.ThemeBuilder")
public final class ScriptThemeBuilder {
    private final ResourceLocation id;
    private final boolean patch;
    private final JsonObject definition = new JsonObject();

    ScriptThemeBuilder(String id, boolean patch) { this.id = PonderThemeLoader.id(id); this.patch = patch; }
    @ZenMethod public ScriptThemeBuilder parent(String id) {
        definition.addProperty("parent", PonderThemeLoader.id(id).toString()); return this;
    }
    @ZenMethod public ScriptThemeBuilder name(String value) { return text("name", value); }
    @ZenMethod public ScriptThemeBuilder description(String value) { return text("description", value); }
    @ZenMethod public ScriptThemeBuilder layout(String value) { return text("layout", value); }
    @ZenMethod public ScriptThemeBuilder color(String role, String value) {
        if (role == null) throw new IllegalArgumentException("A theme color role is required");
        PonderTheme.Color.valueOf(role.toUpperCase(java.util.Locale.ROOT));
        PonderTheme.parseColor(value);
        if (!definition.has("colors")) definition.add("colors", new JsonObject());
        definition.getAsJsonObject("colors").addProperty(role.toLowerCase(java.util.Locale.ROOT), value);
        return this;
    }
    @ZenMethod public void register() { PonderThemeOverrides.register(id, definition, patch); }

    private ScriptThemeBuilder text(String key, String value) {
        if (value == null) throw new IllegalArgumentException("Theme " + key + " is required");
        definition.addProperty(key, value);
        PonderTheme.parse(id, definition, null);
        return this;
    }
}
