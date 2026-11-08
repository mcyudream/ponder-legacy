package net.createmod.ponder.script;

import net.createmod.ponder.api.scene.OverlayInstructions;
import net.createmod.ponder.foundation.element.GuiAnimationElement;
import net.createmod.ponder.gui.GuiTutorialJson;
import net.createmod.ponder.guide.MarkdownTutorialCompiler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

/** Shared GUI clip validation for JSON, local builders and synchronized scene IR. */
final class ScriptGuiAnimation {
    private ScriptGuiAnimation() {}
    static void validate(NBTTagCompound data) { configure(data, create(data)); }
    static void apply(NBTTagCompound data, OverlayInstructions overlay) {
        var id = MarkdownTutorialCompiler.id(data.getString("tutorial"));
        var builder = data.hasKey("definition")
            ? overlay.showGui(data.getInteger("duration"), GuiTutorialJson.parse(data.getString("definition")))
            : overlay.showGui(data.getInteger("duration"), id);
        configure(data, builder);
    }
    private static GuiAnimationElement create(NBTTagCompound data) {
        if (!data.hasKey("duration", 3) || !data.hasKey("tutorial", 8))
            throw new IllegalArgumentException("GUI animation needs duration and tutorial");
        var id = MarkdownTutorialCompiler.id(data.getString("tutorial"));
        var definition = data.hasKey("definition") ? GuiTutorialJson.parse(string(data, "definition")) : null;
        if (definition != null && !definition.id().equals(id))
            throw new IllegalArgumentException("GUI animation definition must match its tutorial id");
        return new GuiAnimationElement(id, definition, 0, data.getInteger("duration"));
    }
    private static void configure(NBTTagCompound data, net.createmod.ponder.api.element.GuiElementBuilder builder) {
        if (data.hasKey("side")) {
            switch (string(data, "side")) {
                case "left" -> builder.onLeft();
                case "right", "auto" -> builder.onRight();
                default -> throw new IllegalArgumentException("GUI side must be left, right or auto");
            }
        }
        if (data.hasKey("x") || data.hasKey("y")) builder.at(number(data,"x"), number(data,"y"));
        if (data.hasKey("scale")) builder.scale(number(data,"scale"));
        if (data.hasKey("from") || data.hasKey("to")) {
            if (!data.hasKey("from",3) || !data.hasKey("to",3))
                throw new IllegalArgumentException("GUI frame range requires integer from and to");
            builder.frames(data.getInteger("from"), data.getInteger("to"));
        }
        if (data.hasKey("pointX") || data.hasKey("pointY") || data.hasKey("pointZ"))
            builder.pointAt(new Vec3d(number(data,"pointX"),number(data,"pointY"),number(data,"pointZ")));
    }
    private static String string(NBTTagCompound data, String key) {
        if (!data.hasKey(key,8)) throw new IllegalArgumentException("Expected string: "+key);
        return data.getString(key);
    }
    private static float number(NBTTagCompound data, String key) {
        if (!data.hasKey(key,99)) throw new IllegalArgumentException("Expected number: "+key);
        float value=data.getFloat(key);
        if (!Float.isFinite(value)) throw new IllegalArgumentException("Expected finite number: "+key);
        return value;
    }
}
