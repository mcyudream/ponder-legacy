package net.createmod.ponder.api.gui;

import java.util.List;
import java.util.Map;
import net.minecraft.util.ResourceLocation;

/** Declarative GUI lessons. These immutable values do not reference client screens or containers. */
public record GuiTutorial(ResourceLocation id, String title, List<ResourceLocation> components,
                          String screen, String adapter, int width, int height, String texture,
                          Map<String, Target> targets, List<Step> steps) {
    public GuiTutorial { components=List.copyOf(components); targets=java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(targets)); steps=List.copyOf(steps); }
    public record Rect(int x,int y,int width,int height) {
        public boolean contains(int px,int py){return px>=x&&py>=y&&px<x+width&&py<y+height;}
        public Rect move(int dx,int dy){return new Rect(x+dx,y+dy,width,height);}
    }
    public enum TargetType { SLOT, BUTTON, REGION }
    public record Target(TargetType type,int index,Rect bounds,String label,String item,int count) {}
    public enum Wait { NEXT, CLICK, SLOT_HAS, SLOT_EMPTY, VALUE_EQUALS, VALUE_AT_LEAST }
    public record Condition(Wait type,String target,String item,int count,String field,String value) {}
    public enum Operation { SET_SLOT, SET_VALUE, CLICK }
    public record Action(Operation op,String target,String item,int count,String field,String value,int at) {}
    public record Step(String title,String text,String target,Condition condition,int duration,List<Action> demo) {
        public Step { demo=List.copyOf(demo); }
    }
    public record Stack(String item,int count) {
        public static final Stack EMPTY=new Stack("",0);
    }
}
