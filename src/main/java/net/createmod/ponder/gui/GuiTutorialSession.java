package net.createmod.ponder.gui;

import java.util.*;
import net.createmod.ponder.api.gui.GuiTutorial;
import net.createmod.ponder.api.gui.GuiTutorial.*;
import net.createmod.ponder.api.gui.GuiTutorial.Stack;

/** Deterministic demonstration playback and observation-only live completion checks. */
public final class GuiTutorialSession implements net.createmod.ponder.api.gui.GuiDemoState {
    public interface Observation {
        /** null means that the target is unavailable, rather than an empty slot. */
        Stack slot(String target);
        String value(String key);
        boolean clicked(String target);
    }
    private final GuiTutorial tutorial;
    private int step,tick;
    private boolean playing=true;
    private final Map<String,Stack> slots=new HashMap<>();
    private final Map<String,String> values=new HashMap<>();
    private final Set<String> clicks=new HashSet<>();
    public GuiTutorialSession(GuiTutorial tutorial){this.tutorial=tutorial;rebuild();}
    public GuiTutorial tutorial(){return tutorial;}
    public int stepIndex(){return step;}
    public Step step(){return tutorial.steps().get(step);}
    public int tick(){return tick;}
    public int lastTick(){return tutorial.steps().stream().mapToInt(s->s.duration()+1).sum()-1;}
    /** Samples an absolute GUI frame without advancing either scene or container state. */
    public void seekAbsolute(int time){
        int remaining=Math.max(0,Math.min(lastTick(),time));
        for(int i=0;i<tutorial.steps().size();i++){
            int length=tutorial.steps().get(i).duration()+1;
            if(remaining<length||i==tutorial.steps().size()-1){seek(i,remaining);return;}
            remaining-=length;
        }
    }
    public boolean playing(){return playing;}
    public void playing(boolean value){playing=value;}
    public void seek(int index,int time){step=Math.max(0,Math.min(tutorial.steps().size()-1,index));tick=Math.max(0,Math.min(step().duration(),time));rebuild();}
    public void next(){seek(step+1,0);}
    public void previous(){seek(step-1,0);}
    public void advanceDemo(){
        if(!playing)return;
        if(tick>=step().duration()){
            if(step==tutorial.steps().size()-1){playing=false;return;}
            next();
        }else{tick++;rebuild();}
    }
    public Stack demoSlot(String target){return slots.getOrDefault(target,Stack.EMPTY);}
    public String demoValue(String key){return values.getOrDefault(key,"");}
    public Observation demo(){return new Observation(){public Stack slot(String t){return tutorial.targets().containsKey(t)?demoSlot(t):null;}public String value(String k){return demoValue(k);}public boolean clicked(String t){return clicks.contains(t);}};}
    public boolean complete(Observation observation){
        Condition c=step().condition();Stack stack=observation.slot(c.target());
        return switch(c.type()){
            case NEXT -> true;
            case CLICK -> observation.clicked(c.target());
            case SLOT_HAS -> stack!=null&&stack.count()>=c.count()&&(c.item().isEmpty()||c.item().equals(stack.item()));
            case SLOT_EMPTY -> stack!=null&&stack.count()==0;
            case VALUE_EQUALS -> c.value().equals(observation.value(c.field()));
            case VALUE_AT_LEAST -> {try{yield GuiTutorialJson.number(observation.value(c.field()))>=GuiTutorialJson.number(c.value());}catch(IllegalArgumentException e){yield false;}}
        };
    }
    private void rebuild(){
        slots.clear();values.clear();clicks.clear();
        tutorial.targets().forEach((key,t)->slots.put(key,new Stack(t.item(),t.count())));
        for(int i=0;i<=step;i++)for(Action a:tutorial.steps().get(i).demo())if(i<step||a.at()<=tick){
            switch(a.op()){
                case SET_SLOT -> slots.put(a.target(),new Stack(a.item(),a.count()));
                case SET_VALUE -> values.put(a.field(),a.value());
                case CLICK -> clicks.add(a.target());
            }
        }
    }
}
