package net.createmod.ponder.api.element;

import net.minecraft.util.math.Vec3d;

/** A GUI replica playing as part of the scene's own timeline. Coordinates are stage-relative, 0..1. */
public interface GuiElementBuilder {
    GuiElementBuilder onLeft();
    GuiElementBuilder onRight();
    GuiElementBuilder at(float x, float y);
    GuiElementBuilder scale(float scale);
    /** Inclusive GUI tutorial tick range, stretched over the scene clip's duration. */
    GuiElementBuilder frames(int from, int to);
    default GuiElementBuilder holdFrame(int frame) { return frames(frame, frame); }
    GuiElementBuilder pointAt(Vec3d machine);
}
