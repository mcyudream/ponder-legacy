package net.createmod.ponder.foundation.element;

import java.util.Objects;
import net.createmod.ponder.api.element.GuiElementBuilder;
import net.createmod.ponder.api.gui.GuiTutorial;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

/** Build-time clip data; playback is derived from absolute scene time, including after snapshot restore. */
public final class GuiAnimationElement implements GuiElementBuilder {
    public enum Placement { LEFT, RIGHT, CUSTOM }
    private final ResourceLocation tutorial;
    private final GuiTutorial definition;
    private final int start, duration;
    private Placement placement = Placement.RIGHT;
    private float x = 1, y = .5f, scale = 1;
    private int from, to = -1;
    private Vec3d target;

    public GuiAnimationElement(ResourceLocation tutorial, GuiTutorial definition, int start, int duration) {
        this.tutorial = Objects.requireNonNull(tutorial, "GUI tutorial id");
        if (start < 0 || duration < 1 || duration > 72000)
            throw new IllegalArgumentException("GUI animation duration must be 1..72000 ticks");
        this.definition = definition;
        this.start = start;
        this.duration = duration;
    }
    public ResourceLocation tutorial() { return tutorial; }
    public GuiTutorial definition() { return definition; }
    public int start() { return start; }
    public int duration() { return duration; }
    public Placement placement() { return placement; }
    public float x() { return x; }
    public float y() { return y; }
    public float scale() { return scale; }
    public Vec3d target() { return target; }
    public boolean active(PonderScene scene) {
        int elapsed = scene.getCurrentTick() - start - 1;
        return elapsed >= 0 && elapsed < duration;
    }
    public int tutorialTick(PonderScene scene, int lastTutorialTick) {
        int first = Math.min(from, lastTutorialTick);
        int last = to < 0 ? lastTutorialTick : Math.min(to, lastTutorialTick);
        int elapsed = Math.max(0, Math.min(duration - 1, scene.getCurrentTick() - start - 1));
        return first + (int) ((long) (last - first) * elapsed / Math.max(1, duration - 1));
    }
    @Override public GuiElementBuilder onLeft() { placement = Placement.LEFT; return this; }
    @Override public GuiElementBuilder onRight() { placement = Placement.RIGHT; return this; }
    @Override public GuiElementBuilder at(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || x < 0 || x > 1 || y < 0 || y > 1)
            throw new IllegalArgumentException("GUI position must be between 0 and 1");
        this.x = x; this.y = y; placement = Placement.CUSTOM; return this;
    }
    @Override public GuiElementBuilder scale(float scale) {
        if (!Float.isFinite(scale) || scale <= 0 || scale > 4)
            throw new IllegalArgumentException("GUI scale must be > 0 and <= 4");
        this.scale = scale; return this;
    }
    @Override public GuiElementBuilder frames(int from, int to) {
        if (from < 0 || to < from || to > 720000)
            throw new IllegalArgumentException("GUI frames must satisfy 0 <= from <= to <= 720000");
        this.from = from; this.to = to; return this;
    }
    @Override public GuiElementBuilder pointAt(Vec3d machine) {
        if (machine == null || !Double.isFinite(machine.x) || !Double.isFinite(machine.y) || !Double.isFinite(machine.z))
            throw new IllegalArgumentException("GUI machine anchor must be finite");
        target = machine; return this;
    }
}
