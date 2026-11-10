package net.createmod.ponder.foundation.ui;

import static org.junit.Assert.*;

import java.util.Map;
import org.junit.Test;

public class PonderModNamesTest {
    @Test public void resolvesContentNamespacesThroughTheirActualLoaderIds() {
        Map<String, String> metadata = Map.of("bmaddon-legacy", "BloodMagic AE2 Addition Legacy",
            "ae2s-probability-pattern-legacy", "AE2S Probability Pattern Legacy",
            "ae2s-appliedpneumatics", "AE2S Applied Pneumatics", "ponder-legacy", "Ponder Legacy");
        assertEquals("BloodMagic AE2 Addition Legacy", PonderModNames.original("bmaddon", metadata::get));
        assertEquals("AE2S Probability Pattern Legacy", PonderModNames.original("statpatterns", metadata::get));
        assertEquals("AE2S Applied Pneumatics", PonderModNames.original("appliedpneumatics", metadata::get));
        assertEquals("Ponder Legacy", PonderModNames.original("ponder", metadata::get));
    }

    @Test public void directlyLoadedNamespaceTakesPriorityOverAliasMetadata() {
        Map<String, String> metadata = Map.of("bmaddon", "Original BMAddon", "bmaddon-legacy", "Ported BMAddon");
        assertEquals("Original BMAddon", PonderModNames.original("bmaddon", metadata::get));
    }

    @Test public void namespaceOverridesPrecedeOwnerAndNativeTranslations() {
        Map<String, String> translations = Map.of("ponder.mod_name.bmaddon", "资源包名称",
            "mod.bmaddon.name", "原生名称", "ponder.mod_name.bmaddon-legacy", "所属模组名称");
        assertEquals("资源包名称", PonderModNames.localized("bmaddon", translations::get, id -> "Metadata"));
        assertEquals("原生名称", PonderModNames.localized("bmaddon",
            key -> key.equals("ponder.mod_name.bmaddon") ? null : translations.get(key), id -> "Metadata"));
    }

    @Test public void ownerTranslationsServeAliasesWithoutChangingRegistryIdentity() {
        Map<String, String> translations = Map.of("ponder.mod_name.bmaddon-legacy", "血魔法 AE2S 附属",
            "mod.ae2s-probability-pattern-legacy.name", "AE2S 概率样板");
        assertEquals("血魔法 AE2S 附属", PonderModNames.localized("bmaddon", translations::get, id -> null));
        assertEquals("AE2S 概率样板", PonderModNames.localized("statpatterns", translations::get, id -> null));
        assertEquals("bmaddon-legacy", PonderModNames.owner("bmaddon"));
        assertEquals("unknown_mod", PonderModNames.owner("unknown_mod"));
    }

    @Test public void invalidTranslationsAndMissingMetadataKeepUsefulFallbacks() {
        assertEquals("Metadata", PonderModNames.localized("bmaddon", key -> "  " + key + "  ", id -> "Metadata"));
        assertEquals("Metadata", PonderModNames.localized("bmaddon", key -> "  ", id -> "Metadata"));
        assertEquals("unknown_mod", PonderModNames.localized("unknown_mod", key -> null, id -> null));
        assertEquals("unknown_mod", PonderModNames.original("unknown_mod", id -> "  "));
        assertEquals("Minecraft", PonderModNames.original("minecraft", id -> null));
    }
}
