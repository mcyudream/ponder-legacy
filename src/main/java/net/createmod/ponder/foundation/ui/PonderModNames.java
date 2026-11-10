package net.createmod.ponder.foundation.ui;

import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Localized catalog labels, overridable by any resource pack without changing registry identities. */
@SideOnly(Side.CLIENT)
final class PonderModNames {
    // Resource/save namespaces are stable even when the owning loader ID changes.
    private static final Map<String, String> OWNERS = Map.of(
        "ponder", "ponder-legacy",
        "appliedpneumatics", "ae2s-appliedpneumatics",
        "bmaddon", "bmaddon-legacy",
        "statpatterns", "ae2s-probability-pattern-legacy");

    private PonderModNames() {}

    static String localized(String namespace) {
        return localized(namespace, key -> I18n.hasKey(key) ? I18n.format(key) : null, PonderModNames::metadataName);
    }

    static String localized(String namespace, Function<String, String> translations, Function<String, String> metadata) {
        String owner = owner(namespace);
        for (String id : namespace.equals(owner) ? new String[] {namespace} : new String[] {namespace, owner}) {
            for (String key : new String[] {"ponder.mod_name." + id, "mod." + id + ".name"}) {
                String value = translations.apply(key);
                if (value != null && !value.isBlank() && !value.trim().equals(key)) return value.trim();
            }
        }
        return original(namespace, metadata);
    }

    static String original(String namespace) {
        return original(namespace, PonderModNames::metadataName);
    }

    static String original(String namespace, Function<String, String> metadata) {
        if ("minecraft".equals(namespace)) return "Minecraft";
        String name = metadata.apply(namespace);
        if (name == null || name.isBlank()) name = metadata.apply(owner(namespace));
        return name == null || name.isBlank() ? namespace : name;
    }

    static String owner(String namespace) { return OWNERS.getOrDefault(namespace, namespace); }

    private static String metadataName(String modId) {
        ModContainer mod = Loader.instance().getIndexedModList().get(modId);
        return mod == null ? null : mod.getName();
    }
}
