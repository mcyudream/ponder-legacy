package net.createmod.ponder.api.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.math.Vec3d;

/** Inserts a concurrent move/rotation and optional wait as one undoable edit. */
public final class EditorAnimations {
    private EditorAnimations() {}

    /** Inserts a sequential GUI segment and its wait as one validated, undoable edit. */
    public static int insertGuiClip(SceneDocument document,int index,JsonObject instruction){
        if(!instruction.has("op")||!"overlay.show_gui".equals(instruction.get("op").getAsString()))
            throw new IllegalArgumentException("Expected overlay.show_gui");
        SceneDocument candidate=new SceneDocument(document.snapshot());candidate.selectScene(document.sceneIndex());
        int insertion=Math.max(0,Math.min(index,candidate.steps().size()));
        for(int i=0;i<insertion;i++)if("scene.finish".equals(candidate.steps().get(i).getAsJsonObject().get("op").getAsString())){insertion=i;break;}
        candidate.insertStep(insertion,instruction);
        JsonObject idle=new JsonObject();idle.addProperty("op","scene.idle");idle.addProperty("ticks",instruction.get("duration").getAsInt());
        candidate.insertStep(insertion+1,idle);ScenePackIO.validate(candidate.snapshot());
        JsonObject replacement=candidate.scene();document.editScene(scene->scene.add("instructions",replacement.get("instructions")));
        return insertion;
    }

    public static void insert(SceneDocument document, int index, String handle, CuboidSelection selection,
                              Vec3d move, Vec3d rotation, int duration, boolean wait) {
        if (selection == null) throw new IllegalArgumentException("Pick both selection corners in the preview first");
        if (handle == null || handle.isBlank()) throw new IllegalArgumentException("Section handle is required");
        checkVector(move); checkVector(rotation);
        if (duration <= 0 || duration > 12000) throw new IllegalArgumentException("Animation duration must be 1..12000 ticks");
        int insertion = Math.max(0, Math.min(index, document.steps().size()));
        JsonArray added = new JsonArray();
        boolean exists = false;
        JsonArray steps = document.steps();
        for (int i = 0; i < insertion; i++) {
            JsonObject step = steps.get(i).getAsJsonObject();
            String op = step.get("op").getAsString();
            if (step.has("handle") && handle.equals(step.get("handle").getAsString())
                && (op.equals("world.make_independent") || op.startsWith("world.show_independent"))) exists = true;
        }
        if (!exists) {
            JsonObject create = action("world.make_independent", handle);
            create.add("selection", selection.toJson()); added.add(create);
            JsonObject center = action("world.center_section", handle);
            vector(center, new Vec3d((selection.min().getX() + (double)selection.max().getX() + 1) / 2,
                (selection.min().getY() + (double)selection.max().getY() + 1) / 2,
                (selection.min().getZ() + (double)selection.max().getZ() + 1) / 2));
            added.add(center);
        }
        if (move.lengthSquared() > 0) {
            JsonObject step = action("world.move_section", handle);
            vector(step, move); step.addProperty("duration", duration); added.add(step);
        }
        if (rotation.lengthSquared() > 0) {
            JsonObject step = action("world.rotate_section", handle);
            vector(step, rotation); step.addProperty("duration", duration); added.add(step);
        }
        if (wait) {
            JsonObject idle = new JsonObject(); idle.addProperty("op", "scene.idle");
            idle.addProperty("ticks", duration); added.add(idle);
        }
        SceneDocument candidate = new SceneDocument(document.snapshot());
        candidate.selectScene(document.sceneIndex());
        candidate.editScene(scene -> {
            JsonArray replacement = new JsonArray();
            for (int i = 0; i <= steps.size(); i++) {
                if (i == insertion) for (JsonElement element : added) replacement.add(element.deepCopy());
                if (i < steps.size()) replacement.add(steps.get(i));
            }
            scene.add("instructions", replacement);
        });
        ScenePackIO.validate(candidate.snapshot());
        JsonObject replacement = candidate.scene();
        document.editScene(scene -> scene.add("instructions", replacement.get("instructions")));
    }

    private static JsonObject action(String op, String handle) {
        JsonObject result = new JsonObject(); result.addProperty("op", op); result.addProperty("handle", handle); return result;
    }
    private static void vector(JsonObject step, Vec3d value) {
        step.addProperty("x", value.x); step.addProperty("y", value.y); step.addProperty("z", value.z);
    }
    private static void checkVector(Vec3d value) {
        if (value == null || !Double.isFinite(value.x) || !Double.isFinite(value.y) || !Double.isFinite(value.z))
            throw new IllegalArgumentException("Animation vectors must be finite");
    }
}
