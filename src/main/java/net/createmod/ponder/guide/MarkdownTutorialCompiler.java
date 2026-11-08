package net.createmod.ponder.guide;

import java.util.*;
import com.google.gson.*;
import net.createmod.ponder.api.guide.Tutorial;
import net.createmod.ponder.api.guide.Tutorial.*;
import net.createmod.ponder.guide.libs.mdast.*;
import net.createmod.ponder.guide.libs.mdast.model.*;
import net.createmod.ponder.guide.libs.mdast.gfm.GfmTableMdastExtension;
import net.createmod.ponder.guide.libs.mdast.gfm.model.GfmTable;
import net.createmod.ponder.guide.libs.mdast.gfmstrikethrough.GfmStrikethroughMdastExtension;
import net.createmod.ponder.guide.libs.mdast.mdx.MdxMdastExtension;
import net.createmod.ponder.guide.libs.mdast.mdx.model.*;
import net.createmod.ponder.guide.libs.mdx.MdxSyntax;
import net.createmod.ponder.guide.libs.micromark.extensions.YamlFrontmatterSyntax;
import net.createmod.ponder.guide.libs.micromark.extensions.gfm.GfmTableSyntax;
import net.createmod.ponder.guide.libs.micromark.extensions.gfmstrikethrough.GfmStrikethroughSyntax;
import net.createmod.ponder.script.*;
import net.minecraft.util.ResourceLocation;

/** Compiles the GuideME Markdown AST to a document and the existing validated Ponder animation IR. */
public final class MarkdownTutorialCompiler {
    public static final int MAX_CHARACTERS = 512 * 1024;
    private final ResourceLocation resource;
    private final String source;
    private final List<Block> blocks = new ArrayList<>();
    private final JsonArray instructions = new JsonArray();
    private JsonArray pending = new JsonArray();
    private final List<String> narration = new ArrayList<>();
    private ResourceLocation id, structure, animation;
    private String title;
    private int stepTicks, cursor, longestAction, longestMotion, widgets;
    private boolean generated, stepContent, stepGuide;

    private MarkdownTutorialCompiler(ResourceLocation resource, String source) {
        this.resource = resource; this.source = source;
    }
    public static Tutorial compile(ResourceLocation resource, String markdown, String source) {
        try { return new MarkdownTutorialCompiler(resource, source).compile(markdown); }
        catch (RuntimeException failure) {
            throw new IllegalArgumentException(source + ": " + failure.getMessage(), failure);
        }
    }
    private Tutorial compile(String markdown) {
        if (markdown.length() > MAX_CHARACTERS) throw new IllegalArgumentException("Markdown page exceeds " + MAX_CHARACTERS + " characters");
        MdastOptions options = new MdastOptions()
            .withSyntaxExtension(MdxSyntax.INSTANCE).withSyntaxExtension(YamlFrontmatterSyntax.INSTANCE)
            .withSyntaxExtension(GfmTableSyntax.INSTANCE).withSyntaxExtension(GfmStrikethroughSyntax.INSTANCE)
            .withMdastExtension(MdxMdastExtension.INSTANCE).withMdastExtension(YamlFrontmatterExtension.INSTANCE)
            .withMdastExtension(GfmTableMdastExtension.INSTANCE).withMdastExtension(GfmStrikethroughMdastExtension.INSTANCE);
        MdAstRoot root = MdAst.fromMarkdown(markdown, options);
        GuideMetadata metadata = GuideMetadata.parse(root.children().stream()
            .filter(n -> n instanceof MdAstYamlFrontmatter).map(n -> ((MdAstYamlFrontmatter)n).value).findFirst().orElse(""));
        id = id(metadata.get("id", resource.toString()));
        title = metadata.get("title", metadata.get("navigation.title", ""));
        if (title.isBlank()) title = root.children().stream().filter(n -> n instanceof MdAstHeading)
            .map(n -> ((MdAstNode)n).toText()).findFirst().orElse(id.getPath());
        List<ResourceLocation> components = metadata.list("item_ids").stream().map(MarkdownTutorialCompiler::id).toList();
        List<ResourceLocation> tags = metadata.list("tags").stream().map(MarkdownTutorialCompiler::id).toList();
        Presentation presentation;
        try { presentation = Presentation.valueOf(metadata.get("presentation", "both").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException bad) { throw new IllegalArgumentException("presentation must be both, guide or animation"); }
        String structureId = metadata.get("structure", "");
        String animationId = metadata.get("scene", "");
        if (!structureId.isEmpty()) structure = id(structureId);
        if (!animationId.isEmpty()) animation = id(animationId);
        if (structure != null && animation != null) throw new IllegalArgumentException("Use structure to create an animation, or scene to link an existing one");
        generated = structure != null && presentation != Presentation.GUIDE;
        if (generated && components.isEmpty()) throw new IllegalArgumentException("An animated tutorial needs item_ids");
        if (generated) animation = id;
        stepTicks = integer(metadata.get("step_ticks", "160"), 20, 1200, "step_ticks");
        if (generated) {
            instructions.add(op("world.show_section", "selection", selection("everywhere"), "direction", "down"));
            instructions.add(op("scene.idle", "ticks", 20)); cursor = 20;
        }
        for (var child : root.children()) flow((MdAstNode)child, true, true);
        finishStep();
        List<ScriptSceneDefinition> scenes = new ArrayList<>();
        if (generated) {
            instructions.add(op("scene.finish"));
            for (ResourceLocation component : components) scenes.add(definition(id, component, title, structure, tags, instructions));
        } else if (presentation == Presentation.ANIMATION && animation == null) {
            throw new IllegalArgumentException("Animation presentation needs a structure or scene");
        }
        String category = metadata.list("categories").stream().findFirst().orElse(metadata.get("navigation.parent", ""));
        return new Tutorial(id, title, components, tags, category, source, markdown, presentation, animation, blocks, scenes);
    }
    private void flow(MdAstNode node, boolean guide, boolean animate) {
        if (node instanceof MdAstYamlFrontmatter) return;
        if (node instanceof MdxJsxElementFields jsx) {
            validateAttributes(jsx);
            String name = jsx.name();
            if ("GuiTutorial".equals(name)) {
                String target = id(attr(jsx,"id","")).toString();
                if (guide) blocks.add(new Block(Kind.GUI_TUTORIAL,attr(jsx,"title","GUI"),0,target,null));
                if (generated && animate) {
                    int duration=integer(attr(jsx,"duration",Integer.toString(stepTicks)),1,72000,"duration");
                    JsonObject action=op("overlay.show_gui","duration",duration,"tutorial",target);
                    for(String key:List.of("side","x","y","scale","from","to","pointX","pointY","pointZ")) {
                        String value=attr(jsx,key,"");if(value.isEmpty())continue;
                        if(key.equals("side"))action.addProperty(key,value);
                        else if(key.equals("from")||key.equals("to"))action.addProperty(key,integer(value,0,720000,key));
                        else action.addProperty(key,number(value));
                    }
                    guiAnimation(action,guide);
                }
                return;
            }
            if ("GuideOnly".equals(name) || "AnimationOnly".equals(name)) {
                finishStep();
                for (var child : jsx.children()) flow((MdAstNode)child,
                    guide && !"AnimationOnly".equals(name), animate && !"GuideOnly".equals(name));
                finishStep(); return;
            }
            if ("Video".equals(name) || "Image".equals(name)) {
                String media = mediaSource(attr(jsx,"src",""));
                if (guide) blocks.add(new Block("Video".equals(name)?Kind.VIDEO:Kind.IMAGE, attr(jsx,"title",attr(jsx,"alt","")),0,media,null,
                    new Media(attr(jsx,"poster","").isBlank()?"":mediaSource(attr(jsx,"poster","")),
                        integer(attr(jsx,"width","0"),0,2048,"width"),integer(attr(jsx,"height","0"),0,2048,"height"))));
                return;
            }
            if ("QuestLink".equals(name)) {
                String quest = attr(jsx,"id","");
                if (quest.isBlank() || quest.length()>256) throw error(node,"QuestLink needs a task or quest identifier");
                if (guide) blocks.add(new Block(Kind.QUEST_LINK,attr(jsx,"title",node.toText().isBlank()?quest:node.toText()),0,"quest:"+quest,null));
                return;
            }
            if ("GameScene".equals(name)) {
                if (guide) blocks.add(Block.scene(gameScene(jsx))); return;
            }
            if (Set.of("ItemLink", "ItemImage", "ItemIcon").contains(name)) {
                String item = attr(jsx, "id", ""); id(item);
                if (guide) blocks.add(new Block(Kind.ITEMS, item, 0, "item:" + item, null)); return;
            }
            if ("ItemGrid".equals(name)) {
                for (var child : jsx.children()) flow((MdAstNode)child, guide, animate); return;
            }
            if (Set.of("Recipe", "RecipeFor", "RecipesFor").contains(name)) {
                String recipe = attr(jsx, "id", attr(jsx, "item", "")); id(recipe);
                if (guide) blocks.add(new Block(Kind.RECIPE, attr(jsx, "fallbackText", ""), 0,
                    ("Recipe".equals(name) ? "recipe:" : "output:") + recipe, null)); return;
            }
            if (Set.of("Row", "Column", "OptionalSection", "Color").contains(name)) {
                for (var child : jsx.children()) flow((MdAstNode)child, guide, animate); return;
            }
            if ("KeyBind".equals(name)) {
                if (guide) blocks.add(Block.text(Kind.TEXT, attr(jsx, "id", ""))); return;
            }
            throw error(node, "Unsupported MDX element: " + name);
        }
        if (node instanceof MdAstCode code && "ponder-gui".equals(code.lang)) {
            var tutorial = net.createmod.ponder.gui.GuiTutorialJson.parse(code.value);
            if (guide) blocks.add(new Block(Kind.GUI_TUTORIAL,tutorial.title(),0,net.createmod.ponder.gui.GuiTutorialJson.write(tutorial),null));
            if(generated && animate) {
                int duration=Math.min(72000,new net.createmod.ponder.gui.GuiTutorialSession(tutorial).lastTick()+1);
                guiAnimation(op("overlay.show_gui","duration",duration,"tutorial",tutorial.id().toString(),
                    "definition",net.createmod.ponder.gui.GuiTutorialJson.write(tutorial)),guide);
            }
            return;
        }
        if (node instanceof MdAstCode code && "ponder".equals(code.lang)) {
            if (!generated) throw error(node, "ponder actions need structure and presentation: both or animation");
            if (!animate) throw error(node, "Put static scene actions in GameScene; ponder actions belong to animation steps");
            JsonElement value = new JsonParser().parse(code.value);
            if (!value.isJsonArray()) throw error(node, "A ponder block contains a JSON array of existing Ponder operations");
            for (JsonElement action : value.getAsJsonArray()) {
                if (!action.isJsonObject()) throw error(node, "Ponder actions must be objects");
                JsonObject object = action.getAsJsonObject();
                String operation = object.has("op") ? object.get("op").getAsString() : "";
                if (Set.of("scene.finish", "finish").contains(operation)) throw error(node, "The Markdown compiler finishes the scene automatically");
                pending.add(object);
                if (Set.of("scene.idle", "idle").contains(operation)) {
                    cursor += object.get("ticks").getAsInt();
                } else if (object.has("duration")) {
                    int end = cursor + object.get("duration").getAsInt();
                    longestAction = Math.max(longestAction, end);
                    if (operation.startsWith("world.")) longestMotion = Math.max(longestMotion, end);
                }
            }
            stepContent = true; stepGuide |= guide; return;
        }
        if (node instanceof MdAstHeading heading) {
            if (heading.depth <= 2) finishStep();
            if (guide) blocks.add(Block.heading(node.toText(), heading.depth));
            return;
        }
        if (node instanceof MdAstList list) {
            int index = list.start;
            for (var child : list.children()) {
                MdAstNode li = (MdAstNode)child;
                String text = (list.ordered ? (index++) + ". " : "• ") + inline(li);
                if (guide) blocks.add(new Block(Kind.TEXT, text, 0, firstLink(li), null));
                if (animate && generated) { narration.add(li.toText()); stepContent = true; stepGuide |= guide; }
            }
            return;
        }
        if (node instanceof GfmTable table) {
            StringJoiner rows = new StringJoiner("\n");
            for (var row : table.children()) {
                StringJoiner cells = new StringJoiner("\t");
                for (var cell : ((MdAstParent<?>)row).children()) cells.add(((MdAstNode)cell).toText());
                rows.add(cells.toString());
            }
            if (guide) blocks.add(Block.text(Kind.TABLE, rows.toString())); return;
        }
        if (node instanceof MdAstImage image) {
            if (guide) blocks.add(new Block(Kind.IMAGE, image.alt == null ? "" : image.alt, 0,
                mediaSource(image.url), null)); return;
        }
        if (node instanceof MdAstCode code) {
            if (guide) blocks.add(Block.text(Kind.CODE, code.value)); return;
        }
        if (node instanceof MdAstThematicBreak) {
            if (guide) blocks.add(Block.text(Kind.RULE, "")); return;
        }
        if (node instanceof MdAstParagraph || node instanceof MdAstBlockquote) {
            // Images and standalone item widgets retain their native display.
            if (node instanceof MdAstParent<?> parent && parent.children().size() == 1
                && parent.children().get(0) instanceof MdAstImage image) { flow(image, guide, false); return; }
            String text = inline(node);
            if (guide && !text.isBlank()) blocks.add(new Block(Kind.TEXT, text, 0, firstLink(node), null));
            if (animate && generated && !node.toText().isBlank()) { narration.add(node.toText()); stepContent = true; stepGuide |= guide; }
            return;
        }
        if (!node.toText().isBlank()) throw error(node, "Unsupported Markdown node: " + node.type());
    }
    private void guiAnimation(JsonObject action,boolean guide) {
        pending.add(action);longestAction=Math.max(longestAction,cursor+action.get("duration").getAsInt());
        stepContent=true;stepGuide|=guide;
    }
    private void finishStep() {
        if (!generated || !stepContent) return;
        for (JsonElement action : pending) instructions.add(action);
        pending = new JsonArray();
        int wait = Math.max(stepTicks, longestAction - cursor + 1);
        instructions.add(op("scene.keyframe"));
        if (!narration.isEmpty()) instructions.add(op("overlay.show_independent_text", "duration", wait - 10,
            "text", String.join("\n", narration), "y", 45, "color", "white", "keyframe", false));
        instructions.add(op("scene.idle", "ticks", wait));
        int snapshotTick = cursor + Math.min(wait - 1, Math.max(30, longestMotion - cursor));
        cursor += wait;
        if (stepGuide) blocks.add(Block.scene(new Snapshot(id, snapshotTick, null, -35, 25, 1, true)));
        narration.clear(); stepContent = false; stepGuide = false; longestAction = cursor; longestMotion = cursor;
    }
    private Snapshot gameScene(MdxJsxElementFields jsx) {
        String linked = attr(jsx, "scene", "");
        int tick = integer(attr(jsx, "tick", "0"), 0, 72000, "tick");
        float yaw = number(attr(jsx, "yaw", "-35")), pitch = number(attr(jsx, "pitch", "25"));
        float zoom = number(attr(jsx, "zoom", "1"));
        if (zoom <= 0 || zoom > 8) throw new IllegalArgumentException("GameScene zoom must be > 0 and <= 8");
        boolean interactive = bool(attr(jsx, "interactive", "true"));
        if (!linked.isEmpty()) {
            if (!sceneElements(jsx).isEmpty()) throw new IllegalArgumentException("GameScene scene references cannot contain structure edits");
            return new Snapshot(id(linked), tick, null, yaw, pitch, zoom, interactive);
        }
        ResourceLocation imported = new ResourceLocation("ponder", "empty");
        JsonArray actions = new JsonArray();
        boolean hasImport = false;
        for (MdxJsxElementFields part : sceneElements(jsx)) {
            validateAttributes(part);
            if ("ImportStructure".equals(part.name())) {
                if (hasImport) throw new IllegalArgumentException("One native structure can be imported per GameScene");
                if (!attr(part, "pos", "0 0 0").equals("0 0 0")) throw new IllegalArgumentException("Native structure imports use their stored coordinates");
                imported = id(attr(part, "src", "")); hasImport = true;
            } else if ("IsometricCamera".equals(part.name())) {
                yaw = number(attr(part, "yaw", "-35")); pitch = number(attr(part, "pitch", "25"));
            } else if ("Block".equals(part.name())) {
                int x = coordinate(part, "x"), y = coordinate(part, "y"), z = coordinate(part, "z");
                String state = attr(part, "id", ""); id(state);
                StringJoiner props = new StringJoiner(",");
                for (var a : part.attributes()) if (a instanceof MdxJsxAttribute attr && attr.name.startsWith("p:"))
                    props.add(attr.name.substring(2) + "=" + attributeValue(attr));
                if (props.length() > 0) state += "[" + props + "]";
                actions.add(op("world.set_block", "x", x, "y", y, "z", z, "state", state, "particles", false));
                JsonObject selected = selection("position"); selected.add("pos", ints(x,y,z));
                actions.add(op("world.show_section", "selection", selected, "direction", "down"));
            } else if ("BoxAnnotation".equals(part.name())) {
                double[] min = vector(attr(part,"min","0 0 0")), max = vector(attr(part,"max","1 1 1"));
                actions.add(op("overlay.show_bounding_box", "color", palette(attr(part,"color","green")), "slot", "guide_"+actions.size(),
                    "minX", min[0], "minY", min[1], "minZ", min[2], "maxX", max[0], "maxY", max[1], "maxZ", max[2], "duration", 1200));
            } else if ("LineAnnotation".equals(part.name())) {
                double[] from = vector(attr(part,"from","0 0 0")), to = vector(attr(part,"to","1 1 1"));
                actions.add(op("overlay.show_line", "color", palette(attr(part,"color","green")),
                    "x1", from[0], "y1", from[1], "z1", from[2], "x2", to[0], "y2", to[1], "z2", to[2], "duration", 1200, "big", true));
            } else if ("BlockAnnotation".equals(part.name())) {
                int x = coordinate(part,"x"), y = coordinate(part,"y"), z = coordinate(part,"z");
                JsonObject selected = selection("position"); selected.add("pos", ints(x,y,z));
                actions.add(op("overlay.show_outline", "color", palette(attr(part,"color","green")), "slot", "guide_" + actions.size(),
                    "selection", selected, "duration", 1200));
                String label = ((MdAstNode)part).toText();
                if (!label.isBlank()) actions.add(op("overlay.show_text", "duration", 1200, "text", label,
                    "x", x+.5, "y", y+1, "z", z+.5, "color", "white", "near", true, "keyframe", false));
            } else throw new IllegalArgumentException("Unsupported GameScene element: " + part.name());
        }
        actions.add(op("world.show_section", "selection", selection("everywhere"), "direction", "down"));
        actions.add(op("scene.idle", "ticks", 25));
        ResourceLocation widgetId = new ResourceLocation(id.getNamespace(), id.getPath()+"/widget_"+(widgets++));
        ScriptSceneDefinition definition = definition(widgetId, id, title, imported, List.of(), actions);
        return new Snapshot(widgetId, 24, definition, yaw, pitch, zoom, interactive);
    }
    private static double[] vector(String text) {
        String[] parts = text.trim().split("[ ,]+");
        if (parts.length != 3) throw new IllegalArgumentException("Expected a three-coordinate vector: "+text);
        double[] result = new double[3];
        for (int i=0;i<3;i++) { result[i]=Double.parseDouble(parts[i]); if (!Double.isFinite(result[i]) || Math.abs(result[i])>1024) throw new IllegalArgumentException("Vector outside scene bounds"); }
        return result;
    }
    private static String palette(String value) {
        if (!value.startsWith("#")) return value;
        if (!value.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Expected #RRGGBB annotation color");
        int rgb = Integer.parseInt(value.substring(1),16), distance = Integer.MAX_VALUE;
        net.createmod.ponder.api.PonderPalette nearest = net.createmod.ponder.api.PonderPalette.WHITE;
        for (var color : net.createmod.ponder.api.PonderPalette.values()) {
            int sample=color.getColor(),dr=((rgb>>16)&255)-((sample>>16)&255),dg=((rgb>>8)&255)-((sample>>8)&255),db=(rgb&255)-(sample&255);
            int d=dr*dr+dg*dg+db*db;if(d<distance){distance=d;nearest=color;}
        }
        return nearest.name().toLowerCase(Locale.ROOT);
    }
    private static List<MdxJsxElementFields> sceneElements(MdxJsxElementFields jsx) {
        List<MdxJsxElementFields> result = new ArrayList<>();
        for (var child : jsx.children()) collectSceneElements((MdAstNode) child, result);
        return result;
    }
    private static void collectSceneElements(MdAstNode node, List<MdxJsxElementFields> result) {
        if (node instanceof MdxJsxElementFields element) { result.add(element); return; }
        if (node instanceof MdAstParent<?> parent) { for (var child : parent.children()) collectSceneElements((MdAstNode)child, result); return; }
        if (!node.toText().isBlank()) throw new IllegalArgumentException("GameScene contains only declarative scene elements");
    }
    private ScriptSceneDefinition definition(ResourceLocation sceneId, ResourceLocation component, String title,
                                              ResourceLocation structure, List<ResourceLocation> tags, JsonArray actions) {
        JsonObject pack = op("", "format", 1, "id", sceneId.toString()); pack.remove("op");
        JsonObject scene = new JsonObject();
        scene.addProperty("id", sceneId.toString()); scene.addProperty("component", component.toString());
        scene.addProperty("title", title); scene.addProperty("structure", structure.toString());
        JsonArray tagArray = new JsonArray(); tags.forEach(t -> tagArray.add(t.toString())); scene.add("tags", tagArray);
        scene.add("instructions", actions);
        JsonArray scenes = new JsonArray(); scenes.add(scene); pack.add("scenes", scenes);
        return PonderJsonLoader.parseScenes(pack, source).get(0);
    }
    public static ResourceLocation id(String value) {
        if (value == null || !value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || value.contains("..") || value.length() > 256)
            throw new IllegalArgumentException("Expected a namespaced resource id: " + value);
        return new ResourceLocation(value);
    }
    private String mediaSource(String path) {
        if (path.length()>2048 || path.isBlank() || path.indexOf('\0')>=0) throw new IllegalArgumentException("Invalid media source");
        if (path.startsWith("https://") || path.startsWith("http://")) {
            java.net.URI uri=java.net.URI.create(path);
            if (uri.getHost()==null || uri.getUserInfo()!=null) throw new IllegalArgumentException("Expected HTTP(S) media URL without credentials");
            return path;
        }
        if (path.startsWith("local:") || path.startsWith("media:") || path.startsWith("asset:")) {
            String name=path.substring(path.indexOf(':')+1);
            if (!name.matches("[a-zA-Z0-9_./ -]+") || name.startsWith("/") || name.contains("..")) throw new IllegalArgumentException("Invalid local media path");
            return path;
        }
        if (path.matches("(?:BV[0-9A-Za-z]+|av[0-9]+)(?:[?].*)?")) return path;
        return resolveResource(path);
    }
    private String resolveResource(String path) {
        if (path.contains(":")) return id(path).toString();
        String base = resource.getPath(); int slash = base.lastIndexOf('/');
        return id(resource.getNamespace()+":"+(slash<0?"":base.substring(0,slash+1))+path).toString();
    }
    public static JsonObject op(String operation, Object... pairs) {
        JsonObject result = new JsonObject(); result.addProperty("op", operation);
        for (int i=0;i<pairs.length;i+=2) {
            String key = (String)pairs[i]; Object v=pairs[i+1];
            if (v instanceof JsonElement e) result.add(key,e);
            else if (v instanceof Boolean b) result.addProperty(key,b);
            else if (v instanceof Number n) result.addProperty(key,n);
            else result.addProperty(key,String.valueOf(v));
        }
        return result;
    }
    private static JsonObject selection(String type) { JsonObject s=new JsonObject(); s.addProperty("type",type);return s; }
    private static JsonArray ints(int... values) { JsonArray a=new JsonArray(); for(int i:values)a.add(i);return a; }
    private static void validateAttributes(MdxJsxElementFields jsx) {
        for (var attr : jsx.attributes()) {
            if (!(attr instanceof MdxJsxAttribute attribute)) throw new IllegalArgumentException("MDX attribute spreads and JavaScript are unsupported");
            attributeValue(attribute);
        }
    }
    private static String attributeValue(MdxJsxAttribute attribute) {
        if (attribute.hasStringValue()) return attribute.getStringValue();
        if (!attribute.hasExpressionValue()) return "true";
        String expression = attribute.getExpressionValue().trim();
        // JSON literals only. Never evaluate JavaScript from resource packs.
        if (!expression.matches("true|false|-?[0-9]+(?:\\.[0-9]+)?|\"(?:[^\"\\\\]|\\\\.)*\""))
            throw new IllegalArgumentException("Only string, number and boolean literals are allowed in MDX: " + attribute.name);
        return new JsonParser().parse(expression).getAsString();
    }
    private static String attr(MdxJsxElementFields jsx, String name, String fallback) {
        MdxJsxAttribute a=jsx.getAttribute(name); return a==null?fallback:attributeValue(a);
    }
    private static int coordinate(MdxJsxElementFields jsx,String axis) { return integer(attr(jsx,axis,"0"),-512,512,axis); }
    private static int integer(String value,int min,int max,String key) {
        try { int v=Integer.parseInt(value);if(v>=min&&v<=max)return v; } catch(NumberFormatException ignored) {}
        throw new IllegalArgumentException(key+" must be between "+min+" and "+max);
    }
    private static float number(String value) { float f=Float.parseFloat(value);if(!Float.isFinite(f))throw new IllegalArgumentException("Non-finite camera value");return f; }
    private static boolean bool(String value) { if(!value.equals("true")&&!value.equals("false"))throw new IllegalArgumentException("Expected boolean: "+value);return Boolean.parseBoolean(value); }
    private static String inline(MdAstNode node) {
        if(node instanceof MdAstStrong)return "§l"+childrenText(node)+"§r";
        if(node instanceof MdAstEmphasis)return "§o"+childrenText(node)+"§r";
        if(node instanceof MdAstInlineCode)return "§e"+node.toText()+"§r";
        if(node instanceof MdAstLink)return "§b§n"+childrenText(node)+"§r";
        if(node instanceof MdAstBreak)return "\n";
        if(node instanceof MdxJsxElementFields jsx && Set.of("ItemLink","ItemIcon","ItemImage").contains(jsx.name()))
            return "§b"+attr(jsx,"id","")+"§r";
        if(node instanceof MdAstParent<?>)return childrenText(node);
        return node.toText();
    }
    private static String childrenText(MdAstNode node) {
        StringBuilder out=new StringBuilder();
        if(node instanceof MdAstParent<?> parent)for(var child:parent.children())out.append(inline((MdAstNode)child));
        return out.toString();
    }
    private static String firstLink(MdAstNode node) {
        if(node instanceof MdAstLink link)return link.url;
        if(node instanceof MdAstParent<?> parent)for(var child:parent.children()){String s=firstLink((MdAstNode)child);if(!s.isEmpty())return s;}
        return "";
    }
    private IllegalArgumentException error(MdAstNode node,String message) {
        return new IllegalArgumentException(message+(node.position==null?"":" at "+node.position));
    }
}
