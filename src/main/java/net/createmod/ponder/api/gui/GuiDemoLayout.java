package net.createmod.ponder.api.gui;

import java.util.*;
import net.createmod.ponder.api.gui.GuiTutorial.Rect;

/**
 * A complete passive GUI appearance. Painting, clipping and highlight targets share one
 * coordinate tree. No screen, container, inventory action or network operation is created.
 * Build only visible nodes; use hideTarget for a target absent in the current state.
 */
public final class GuiDemoLayout {
    public static final int BACKGROUND=0, PANEL=10, SLOT=20, CONTENT=30, CONTROL=40, TEXT=50, MODAL=80;
    public record Node(String key,Rect bounds,Rect clip,int originX,int originY,int layer,Runnable paint) {}
    private final Rect viewport;
    private final List<Node> nodes;
    private final Map<String,Rect> targets;
    private GuiDemoLayout(Builder builder) {
        viewport=builder.viewport;
        // Stable ordering preserves declaration order within one layer.
        nodes=builder.nodes.stream().sorted(Comparator.comparingInt(Node::layer)).toList();
        targets=Collections.unmodifiableMap(new LinkedHashMap<>(builder.targets));
    }
    public Rect viewport(){return viewport;}
    public List<Node> nodes(){return nodes;}
    public Rect target(String key){return targets.get(key);}
    public static Builder builder(int width,int height){return new Builder(width,height);}
    public static Rect intersect(Rect a,Rect b) {
        int x=Math.max(a.x(),b.x()),y=Math.max(a.y(),b.y());
        return new Rect(x,y,Math.max(0,Math.min(a.x()+a.width(),b.x()+b.width())-x),Math.max(0,Math.min(a.y()+a.height(),b.y()+b.height())-y));
    }
    public static final class Builder {
        private final Rect viewport;
        private final List<Node> nodes=new ArrayList<>();
        private final Map<String,Rect> targets=new LinkedHashMap<>();
        private final Set<String> nodeKeys=new HashSet<>();
        private Builder(int width,int height) {
            if(width<=0||height<=0)throw new IllegalArgumentException("Empty demo viewport");
            viewport=new Rect(0,0,width,height);
        }
        public Group root(){return new Group(this,0,0,viewport);}
        public GuiDemoLayout build(){return new GuiDemoLayout(this);}
    }
    public static final class Group {
        private final Builder builder;
        private final int x,y;
        private final Rect clip;
        private Group(Builder builder,int x,int y,Rect clip){this.builder=builder;this.x=x;this.y=y;this.clip=clip;}
        public Group group(int dx,int dy){return new Group(builder,x+dx,y+dy,clip);}
        /** Clip is local to this group and intersects all ancestor clips. */
        public Group clip(Rect local){return new Group(builder,x,y,intersect(clip,local.move(x,y)));}
        /** A non-painting region, also usable as the fallback for a subsequently declared node. */
        public Group region(String key,Rect local){builder.targets.put(Objects.requireNonNull(key),intersect(local.move(x,y),clip));return this;}
        public Group hideTarget(String key){builder.targets.remove(key);return this;}
        public Group node(String key,Rect local,int layer,Runnable paint) {
            Objects.requireNonNull(key);Objects.requireNonNull(local);Objects.requireNonNull(paint);
            if(local.width()<=0||local.height()<=0)throw new IllegalArgumentException("Empty demo node: "+key);
            if(!key.isEmpty()&&!builder.nodeKeys.add(key))throw new IllegalArgumentException("Duplicate demo node: "+key);
            Rect absolute=local.move(x,y),visible=intersect(absolute,clip);
            if(visible.width()==0||visible.height()==0){if(!key.isEmpty())hideTarget(key);return this;}
            builder.nodes.add(new Node(key,absolute,clip,x,y,layer,paint));
            if(!key.isEmpty())builder.targets.put(key,visible);
            return this;
        }
    }
}
