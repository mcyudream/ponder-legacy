package net.createmod.ponder.api.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.createmod.ponder.script.PonderJsonLoader;

/** Addons can expose custom codec instructions as named, editable action templates. */
public final class EditorActions {
    private static final Map<String, Action> CUSTOM = new LinkedHashMap<>();
    private EditorActions() {}

    public static synchronized void register(String id, String label, JsonObject template) {
        if (id == null || id.isBlank() || label == null || template == null || !template.has("op"))
            throw new IllegalArgumentException("Action ID, label and instruction template are required");
        if (CUSTOM.containsKey(id)) throw new IllegalArgumentException("Duplicate editor action " + id);
        CUSTOM.put(id, new Action(id, label, template));
    }

    public static synchronized List<Action> all(CuboidSelection selection) {
        List<Action> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : PonderJsonLoader.operationFields().entrySet()) {
            JsonObject template = new JsonObject();
            template.addProperty("op", entry.getKey());
            for (String field : entry.getValue()) template.add(field, defaultValue(entry.getKey(), field, selection));
            if (entry.getKey().equals("overlay.show_gui")) {
                for (String field : List.of("x","y","definition","pointX","pointY","pointZ")) template.remove(field);
                template.addProperty("duration",160);
            }
            result.add(new Action(entry.getKey(), entry.getKey(), template));
        }
        result.addAll(CUSTOM.values());
        return Collections.unmodifiableList(result);
    }

    private static JsonElement defaultValue(String op, String field, CuboidSelection selection) {
        JsonObject wrapper = new JsonObject();
        switch (field) {
            case "selection":
                if (selection != null) return selection.toJson();
                JsonObject all = new JsonObject(); all.addProperty("type", "everywhere"); return all;
            case "direction": case "pointing": case "side": wrapper.addProperty("v", op.equals("overlay.show_gui") ? "right" : "down"); break;
            case "tutorial": wrapper.addProperty("v", "ponder:examples/furnace"); break;
            case "scale": wrapper.addProperty("v", 1); break;
            case "to": wrapper.addProperty("v", 720000); break;
            case "handle": wrapper.addProperty("v", "section"); break;
            case "duration": wrapper.addProperty("v", 20); break;
            case "ticks": wrapper.addProperty("v", 20); break;
            case "size": wrapper.addProperty("v", 5); break;
            case "value": wrapper.addProperty("v", op.equals("scene.scale") ? 1 : 0); break;
            case "degrees": wrapper.addProperty("v", 45); break;
            case "text": wrapper.addProperty("v", "Describe this step"); break;
            case "color":
                if (op.equals("effects.redstone_particles")) wrapper.addProperty("v", 0xff4444);
                else wrapper.addProperty("v", "white"); break;
            case "item": wrapper.addProperty("v", "minecraft:stone"); break;
            case "state": wrapper.addProperty("v", "minecraft:stone"); break;
            case "count": case "amount": case "cycles": wrapper.addProperty("v", 1); break;
            case "near": case "enabled": case "visible": case "redraw": wrapper.addProperty("v", true); break;
            case "particles": case "keyframe": case "replace": case "big": wrapper.addProperty("v", false); break;
            case "action": wrapper.addProperty("v", "right_click"); break;
            case "payload": case "nbt": wrapper.addProperty("v", "{}"); break;
            case "params": return new com.google.gson.JsonArray();
            case "codec": wrapper.addProperty("v", "myaddon:animation"); break;
            case "key": wrapper.addProperty("v", "mypack:message"); break;
            case "slot": wrapper.addProperty("v", "selection"); break;
            case "property": wrapper.addProperty("v", "facing"); break;
            case "pose": wrapper.addProperty("v", "flappy"); break;
            case "type": wrapper.addProperty("v", op.contains("minecart") ? "empty" : "villager_happy"); break;
            default: wrapper.addProperty("v", 0); break;
        }
        return wrapper.get("v");
    }

    public static final class Action {
        private final String id;
        private final String label;
        private final JsonObject template;
        private Action(String id, String label, JsonObject template) {
            this.id = id; this.label = label; this.template = template.deepCopy();
        }
        public String id() { return id; }
        public String label() { return label; }
        public JsonObject template() { return template.deepCopy(); }
    }
}
