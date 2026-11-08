package net.createmod.ponder.gui;

import java.util.ArrayList;
import java.util.List;
import net.createmod.ponder.api.gui.GuiTutorial.Rect;
import net.createmod.ponder.foundation.element.GuiAnimationElement;
import net.createmod.ponder.foundation.element.GuiAnimationElement.Placement;

/** Stage layout shared by the player, animation editor and static scene widgets. */
public record GuiAnimationLayout(Rect world, List<Panel> panels) {
    public record Panel(GuiAnimationElement animation, Rect bounds) {}
    public static GuiAnimationLayout arrange(Rect stage, List<GuiAnimationElement> clips) {
        if (clips.isEmpty()) return new GuiAnimationLayout(stage,List.of());
        long left=clips.stream().filter(c->c.placement()==Placement.LEFT).count();
        long right=clips.stream().filter(c->c.placement()==Placement.RIGHT).count();
        int column=Math.min(280,Math.max(1,Math.round(stage.width()*(left>0&&right>0?.30f:.44f))));
        int padding=Math.min(6,Math.min(stage.width(),stage.height())/4);
        int reserveLeft=left>0?column:0,reserveRight=right>0?column:0;
        Rect world=new Rect(stage.x()+reserveLeft,stage.y(),Math.max(1,stage.width()-reserveLeft-reserveRight),stage.height());
        List<Panel> panels=new ArrayList<>();int li=0,ri=0;
        for(var clip:clips){
            Rect panel;
            if(clip.placement()==Placement.CUSTOM){
                int h=Math.max(1,Math.min(stage.height()-padding*2,280)),w=Math.max(1,column-padding*2);
                panel=new Rect(stage.x()+padding+Math.round((stage.width()-w-padding*2)*clip.x()),stage.y()+padding+Math.round((stage.height()-h-padding*2)*clip.y()),w,h);
            }else{
                boolean onLeft=clip.placement()==Placement.LEFT;int count=(int)(onLeft?left:right),index=onLeft?li++:ri++;
                int cell=stage.height()/count;
                int verticalPadding=Math.min(padding,cell/4);
                panel=new Rect(onLeft?stage.x()+padding:stage.x()+stage.width()-column+padding,stage.y()+index*cell+verticalPadding,Math.max(1,column-padding*2),Math.max(1,cell-verticalPadding*2));
            }
            panels.add(new Panel(clip,panel));
        }
        return new GuiAnimationLayout(world,List.copyOf(panels));
    }
}
