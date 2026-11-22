package net.createmod.ponder.script;

import crafttweaker.annotations.ZenRegister;
import net.createmod.ponder.api.theme.PonderThemeLoader;
import net.createmod.ponder.api.theme.PonderThemeOverrides;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** Side-neutral startup declarations; client selection and rendering stay in the client package. */
@ZenRegister
@ZenClass("mods.ponder.Theme")
public final class ScriptTheme {
    private ScriptTheme() {}

    @ZenMethod public static ScriptThemeBuilder create(String id) { return new ScriptThemeBuilder(id, false); }
    @ZenMethod public static ScriptThemeBuilder extend(String id, String parent) {
        return new ScriptThemeBuilder(id, false).parent(parent);
    }
    @ZenMethod public static ScriptThemeBuilder modify(String id) { return new ScriptThemeBuilder(id, true); }
    /** Pack default. A player's explicit theme preference takes precedence. */
    @ZenMethod public static void setDefault(String id) { PonderThemeOverrides.setDefault(PonderThemeLoader.id(id)); }
}
