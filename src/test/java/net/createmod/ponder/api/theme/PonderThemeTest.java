package net.createmod.ponder.api.theme;

import static org.junit.Assert.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.createmod.ponder.api.theme.PonderTheme.Color;
import net.createmod.ponder.config.CClient;
import net.createmod.ponder.script.ScriptTheme;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PonderThemeTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private static ResourceLocation id(String value) { return PonderThemeLoader.id(value); }
    private static JsonObject json(String value) { return new JsonParser().parse(value).getAsJsonObject(); }
    private static Map<ResourceLocation, PonderTheme> fallback() { return Map.of(PonderTheme.CLASSIC, PonderTheme.classic()); }

    @Test public void inheritanceIsOrderIndependentAndPreservesAlphaAndLayout() {
        Map<ResourceLocation, JsonObject> definitions = new LinkedHashMap<>();
        definitions.put(id("test:child"), json("""
            {"parent":"test:parent","colors":{"text":"#abcdef","button":"#23010203"}}
            """));
        definitions.put(id("test:parent"), json("""
            {"layout":"modern","colors":{"accent":"#123456","text":"#999999"}}
            """));
        var result = new PonderThemeLoader(definitions, fallback()).load();
        assertTrue(result.errors().toString(), result.errors().isEmpty());
        var child = result.themes().get(id("test:child"));
        assertEquals(PonderTheme.Layout.MODERN, child.layout());
        assertEquals(0xffabcdef, child.color(Color.TEXT));
        assertEquals(0x23010203, child.color(Color.BUTTON));
        assertEquals(0xff123456, child.color(Color.ACCENT));
        assertEquals("test:child", child.name());
        assertThrows(UnsupportedOperationException.class, () -> child.colors().put(Color.TEXT, 0));
    }

    @Test public void invalidThemesDoNotRemoveValidSiblingsOrBuiltinFallbacks() {
        var result = new PonderThemeLoader(Map.of(
            PonderTheme.CLASSIC, json("{\"colors\":{\"text\":\"broken\"}}"),
            id("test:valid"), json("{\"parent\":\"ponder:classic\",\"colors\":{\"accent\":\"#00ff00\"}}"),
            id("test:invalid"), json("{\"colors\":{\"unknown\":\"#ffffff\"}}")), fallback()).load();
        assertEquals(2, result.errors().size());
        assertEquals(Color.TEXT.legacy, result.themes().get(PonderTheme.CLASSIC).color(Color.TEXT));
        assertEquals(0xff00ff00, result.themes().get(id("test:valid")).color(Color.ACCENT));
        assertFalse(result.themes().containsKey(id("test:invalid")));
    }

    @Test public void cyclesAndMissingParentsAreReportedWithoutPublishingPartialThemes() {
        var result = new PonderThemeLoader(Map.of(
            id("test:a"), json("{\"parent\":\"test:b\"}"),
            id("test:b"), json("{\"parent\":\"test:a\"}"),
            id("test:c"), json("{\"parent\":\"test:missing\"}")), fallback()).load();
        assertEquals(3, result.errors().size());
        assertEquals(fallback().keySet(), result.themes().keySet());
    }

    @Test public void cyclicBuiltinOverridesRetainBothFallbacksAndAllowValidChildren() {
        var modern = PonderTheme.parse(PonderTheme.MODERN, json("{\"layout\":\"modern\"}"), null);
        Map<ResourceLocation, JsonObject> definitions = new LinkedHashMap<>();
        definitions.put(PonderTheme.CLASSIC, json("{\"parent\":\"ponder:modern\"}"));
        definitions.put(PonderTheme.MODERN, json("{\"parent\":\"ponder:classic\"}"));
        definitions.put(id("test:child"), json("{\"parent\":\"ponder:classic\"}"));
        var result = new PonderThemeLoader(definitions,
            Map.of(PonderTheme.CLASSIC, PonderTheme.classic(), PonderTheme.MODERN, modern)).load();
        assertEquals(2, result.errors().size());
        assertEquals(PonderTheme.Layout.CLASSIC, result.themes().get(PonderTheme.CLASSIC).layout());
        assertEquals(PonderTheme.Layout.MODERN, result.themes().get(PonderTheme.MODERN).layout());
        assertEquals(PonderTheme.Layout.CLASSIC, result.themes().get(id("test:child")).layout());
    }

    @Test public void definitionValidationCatchesAuthorTyposAndWrongTypes() {
        for (String invalid : new String[] {
            "{\"colour\":{}}", "{\"colors\":[]}", "{\"colors\":{\"text\":123}}",
            "{\"colors\":{\"text\":\"#12345\"}}", "{\"layout\":\"blue\"}", "{\"name\":false}",
            "{\"parent\":12}", "{\"parent\":\"../classic\"}"})
            assertThrows(invalid, IllegalArgumentException.class,
                () -> PonderTheme.parse(id("test:invalid"), json(invalid), null));
        assertEquals(0, PonderTheme.parseColor("#00000000"));
        assertEquals(0xffffffff, PonderTheme.parseColor("#ffffff"));
    }

    @Test public void localJsonCanPatchBuiltinAndDefineNewTheme() {
        var document = PonderThemeDocument.parse(json("""
            {"default":"pack:new","themes":{
              "ponder:classic":{"colors":{"accent":"#ff0000"}},
              "pack:new":{"parent":"ponder:classic","layout":"modern","colors":{"text":"#001122"}}
            }}
            """));
        var result = new PonderThemeLoader(document.themes(), fallback()).load();
        assertEquals(id("pack:new"), document.defaultTheme());
        assertEquals(0xffff0000, result.themes().get(id("pack:new")).color(Color.ACCENT));
        assertEquals(0xff001122, result.themes().get(id("pack:new")).color(Color.TEXT));
        for (String invalid : new String[] {"{\"selected\":\"pack:new\"}", "{\"themes\":[]}", "{\"themes\":{\"NoNamespace\":{}}}"})
            assertThrows(IllegalArgumentException.class, () -> PonderThemeDocument.parse(json(invalid)));
    }

    @Test public void patchesPreserveUnmodifiedDefinitionAndColorFields() {
        JsonObject source = json("{\"name\":\"Original\",\"parent\":\"ponder:modern\",\"colors\":{\"text\":\"#eeeeee\",\"accent\":\"#ff0000\"}}");
        var merged = PonderThemeOverrides.merge(source, json("{\"colors\":{\"accent\":\"#00ff00\"}}"));
        assertEquals("Original", merged.get("name").getAsString());
        assertEquals("ponder:modern", merged.get("parent").getAsString());
        assertEquals("#eeeeee", merged.getAsJsonObject("colors").get("text").getAsString());
        assertEquals("#00ff00", merged.getAsJsonObject("colors").get("accent").getAsString());
        assertEquals("#ff0000", source.getAsJsonObject("colors").get("accent").getAsString());
    }

    @Test public void zenScriptSupportsNewInheritedAndPatchedThemes() {
        ScriptTheme.create("theme_test:blank").name("Blank").layout("classic").color("text", "#ffcc00").register();
        ScriptTheme.extend("theme_test:child", "theme_test:blank").color("accent", "#112233").register();
        ScriptTheme.modify("theme_test:child").description("Changed").color("text", "#aabbcc").register();
        var snapshot = PonderThemeOverrides.snapshot();
        var result = new PonderThemeLoader(snapshot, fallback()).load();
        var child = result.themes().get(id("theme_test:child"));
        assertEquals("Changed", child.description());
        assertEquals(0xffaabbcc, child.color(Color.TEXT));
        assertEquals(0xff112233, child.color(Color.ACCENT));
        snapshot.get(id("theme_test:child")).addProperty("name", "Mutated");
        assertFalse(PonderThemeOverrides.snapshot().get(id("theme_test:child")).has("name"));
        assertThrows(IllegalArgumentException.class, () -> ScriptTheme.create("theme_test:blank").register());
        assertThrows(IllegalArgumentException.class, () -> ScriptTheme.create("theme_test:bad").color("typo", "#ffffff"));
    }

    @Test public void builtinResourcesFollowThePublicContract() throws Exception {
        Map<ResourceLocation, JsonObject> definitions = new LinkedHashMap<>();
        for (String name : new String[] {"classic", "modern"}) {
            String text = Files.readString(Path.of("src/main/resources/assets/ponder/themes/" + name + ".json"));
            definitions.put(id("ponder:" + name), json(text));
        }
        var result = new PonderThemeLoader(definitions, fallback()).load();
        assertTrue(result.errors().toString(), result.errors().isEmpty());
        var modern = result.themes().get(PonderTheme.MODERN);
        assertEquals(PonderTheme.Layout.MODERN, modern.layout());
        assertEquals(0x60c0c0ff, modern.color(Color.BORDER));
        assertEquals(0x30c0c0ff, modern.color(Color.BORDER_BOTTOM));
    }

    @Test public void selectedThemeSurvivesConfigurationReload() throws Exception {
        var file = temporary.newFile("ponder-client.cfg");
        CClient config = new CClient();
        config.registerAll(new Configuration(file));
        assertEquals("auto", config.uiTheme.get());
        config.uiTheme.set("pack:custom");
        CClient reopened = new CClient();
        reopened.registerAll(new Configuration(file));
        assertEquals("pack:custom", reopened.uiTheme.get());
    }
}
