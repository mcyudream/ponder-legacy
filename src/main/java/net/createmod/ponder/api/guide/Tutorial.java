package net.createmod.ponder.api.guide;

import java.util.List;
import net.createmod.ponder.script.ScriptSceneDefinition;
import net.minecraft.util.ResourceLocation;

/** A shared, immutable source for the guide and animated presentations. No client classes. */
public record Tutorial(ResourceLocation id, String title, List<ResourceLocation> components,
                       List<ResourceLocation> tags, String category, String source, String markdown,
                       Presentation presentation, ResourceLocation animation,
                       List<Block> blocks, List<ScriptSceneDefinition> scenes) {
    public Tutorial {
        components = List.copyOf(components);
        tags = List.copyOf(tags);
        blocks = List.copyOf(blocks);
        scenes = List.copyOf(scenes);
    }
    public enum Presentation { BOTH, ANIMATION, GUIDE }
    public enum Kind { HEADING, TEXT, CODE, RULE, ITEMS, RECIPE, IMAGE, SCENE, TABLE, VIDEO, QUEST_LINK, GUI_TUTORIAL }
    /** target is a tutorial, item, recipe, image resource, or heading anchor, depending on kind. */
    public record Media(String poster, int width, int height) {}
    public record Block(Kind kind, String text, int level, String target, Snapshot snapshot, Media media) {
        public Block(Kind kind, String text, int level, String target, Snapshot snapshot) { this(kind,text,level,target,snapshot,null); }
        public static Block text(Kind kind, String text) { return new Block(kind, text, 0, "", null); }
        public static Block heading(String text, int depth) { return new Block(Kind.HEADING, text, depth, "", null); }
        public static Block scene(Snapshot snapshot) { return new Block(Kind.SCENE, "", 0, "", snapshot); }
    }
    /** A reference to an existing animation, or a standalone declarative scene. */
    public record Snapshot(ResourceLocation scene, int tick, ScriptSceneDefinition definition,
                           float yaw, float pitch, float zoom, boolean interactive) {}
}
