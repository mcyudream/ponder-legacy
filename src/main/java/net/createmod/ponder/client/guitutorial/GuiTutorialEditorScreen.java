package net.createmod.ponder.client.guitutorial;

import java.io.IOException;
import java.util.*;
import net.createmod.ponder.api.gui.GuiTutorial;
import net.createmod.ponder.api.gui.GuiTutorial.*;
import net.createmod.ponder.foundation.ui.*;
import net.createmod.ponder.gui.*;
import net.createmod.ponder.guide.MarkdownTutorialCompiler;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Keyboard;

/** Point-and-pick/rectangle authoring. Embedded capture never dispatches native container input. */
public final class GuiTutorialEditorScreen extends AbstractPonderScreen {
    private final GuiTutorial original;
    private final GuiScreen parent,host;
    private final Runnable embeddedClose;
    private final Map<String,Target> targets=new LinkedHashMap<>();
    private final List<Step> steps=new ArrayList<>();
    private GuiTextField id,title,description,demoItem;
    private int selected,panelX,panelY=40,panelWidth,modelX,modelY,dragX,dragY,cornerX,cornerY;
    private float scale;
    private boolean moving,region;
    private Wait wait;
    private String error="";
    private Rect machine;
    public GuiTutorialEditorScreen(GuiTutorial tutorial,GuiScreen parent){this(tutorial,parent,null,null);}
    public GuiTutorialEditorScreen(GuiTutorial tutorial,GuiScreen parent,GuiScreen host,Runnable close){
        this.original=tutorial;this.parent=parent;this.host=host;this.embeddedClose=close;targets.putAll(tutorial.targets());steps.addAll(tutorial.steps());wait=steps.get(0).condition().type();
    }
    public static GuiTutorial capture(GuiContainer host){
        var adapter=new StandardGuiAdapter();GuiTutorial seed=GuiTutorials.furnaceExample();Rect window=adapter.window(host,seed);
        Map<String,Target> targets=new LinkedHashMap<>();
        for(var slot:host.inventorySlots.inventorySlots)if(slot.isEnabled()&&!(slot.inventory instanceof InventoryPlayer)){
            var stack=slot.getStack();String item=stack.isEmpty()?"":stack.getItem().getRegistryName().toString();
            targets.put("slot_"+slot.slotNumber,new Target(TargetType.SLOT,slot.slotNumber,new Rect(slot.xPos,slot.yPos,16,16),"Slot "+slot.slotNumber,item,stack.getCount()));
        }
        for(var button:StandardGuiAdapter.buttons(host))if(button.visible)targets.put("button_"+button.id,new Target(TargetType.BUTTON,button.id,new Rect(button.x-window.x(),button.y-window.y(),button.width,button.height),button.displayString,"",0));
        if(targets.isEmpty())targets.put("machine",new Target(TargetType.REGION,0,new Rect(0,0,window.width(),window.height()),"GUI","",0));
        String key=targets.keySet().iterator().next();String name=host.getClass().getSimpleName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]","_");
        String texture=host.getClass().getName().equals(seed.screen())?seed.texture():"";
        Step step=new Step("",GuiTutorialDrawing.tr("new_description"),key,new Condition(Wait.NEXT,key,"",1,"",""),80,List.of());
        return new GuiTutorial(new net.minecraft.util.ResourceLocation("mypack:"+name),name,List.of(),host.getClass().getName(),"generic",window.width(),window.height(),texture,targets,List.of(step));
    }
    @Override public void initGui(){
        buttonList.clear();Keyboard.enableRepeatEvents(true);panelWidth=Math.min(224,Math.max(164,width/3));
        if(panelX==0)panelX=width-panelWidth-8;panelX=Math.max(4,Math.min(panelX,width-panelWidth-4));panelY=Math.max(36,Math.min(panelY,Math.max(36,height-210)));
        if(host==null){scale=Math.max(.05f,Math.min(1.3f,Math.min((width-panelWidth-28)/(float)original.width(),(height-88)/(float)original.height())));modelX=8;modelY=42;machine=new Rect(modelX,modelY,Math.round(original.width()*scale),Math.round(original.height()*scale));}
        else{machine=new StandardGuiAdapter().window(host,original);modelX=machine.x();modelY=machine.y();scale=1;}
        String resource=id==null?original.id().toString():id.getText();
        id=field(1,22,resource);title=field(2,48,GuiTutorialDrawing.text(steps.get(selected).title()));description=field(3,74,GuiTutorialDrawing.text(steps.get(selected).text()));
        String item=steps.get(selected).demo().stream().filter(a->a.op()==Operation.SET_SLOT).map(Action::item).findFirst().orElse("");demoItem=field(4,100,item);
        int bw=(panelWidth-20)/2;
        button(1,8,125,bw,"previous",()->navigate(-1));button(2,12+bw,125,bw,"next",()->navigate(1));
        button(3,8,149,bw,"add_step",()->{if(apply()){steps.add(selected+1,new Step("",GuiTutorialDrawing.tr("new_description"),steps.get(selected).target(),new Condition(Wait.NEXT,steps.get(selected).target(),"",1,"",""),80,List.of()));selected++;wait=Wait.NEXT;initGui();}});
        button(4,12+bw,149,bw,"wait."+wait.name().toLowerCase(Locale.ROOT),this::cycleWait);
        addButton(new PonderButton(10,8,height-29,64,20,null).withLabel(GuiTutorialDrawing.tr("save")).withCallback(this::save));
        addButton(new PonderButton(11,76,height-29,64,20,null).withLabel(GuiTutorialDrawing.tr("preview")).withCallback(this::preview));
        addButton(new PonderButton(12,width-72,height-29,64,20,null).withLabel(GuiTutorialDrawing.tr("cancel")).withCallback(this::close));
    }
    private GuiTextField field(int n,int offset,String text){GuiTextField f=new GuiTextField(n,fontRenderer,panelX+8,panelY+offset,panelWidth-16,18);f.setMaxStringLength(n==3?4096:256);f.setText(text);return f;}
    private void button(int id,int dx,int dy,int width,String key,Runnable callback){addButton(new PonderButton(id,panelX+dx,panelY+dy,width,20,null).withLabel(GuiTutorialDrawing.tr(key)).withCallback(callback));}
    private boolean apply(){
        try{
            if(!demoItem.getText().isBlank())MarkdownTutorialCompiler.id(demoItem.getText());
            Step old=steps.get(selected);String target=old.target();Condition previous=old.condition();
            Condition condition=previous.type()==wait&&previous.target().equals(target)?previous:new Condition(wait,target,demoItem.getText(),1,previous.field(),previous.value());
            List<Action> actions=new ArrayList<>(old.demo());
            if(targets.get(target).type()==TargetType.SLOT){actions.removeIf(a->a.op()==Operation.SET_SLOT&&a.target().equals(target));if(!demoItem.getText().isBlank()||wait==Wait.SLOT_EMPTY)actions.add(new Action(Operation.SET_SLOT,target,demoItem.getText(),demoItem.getText().isBlank()?0:1,"","",20));}
            if(wait==Wait.CLICK&&actions.stream().noneMatch(a->a.op()==Operation.CLICK&&a.target().equals(target)))actions.add(new Action(Operation.CLICK,target,"",0,"","",20));
            steps.set(selected,new Step(title.getText(),description.getText(),target,condition,old.duration(),actions));return true;
        }catch(RuntimeException e){error=e.getMessage();return false;}
    }
    private GuiTutorial definition(){return new GuiTutorial(MarkdownTutorialCompiler.id(id.getText()),original.title(),original.components(),original.screen(),original.adapter(),original.width(),original.height(),original.texture(),targets,steps);}
    private void navigate(int direction){if(!apply())return;String key=id.getText();selected=Math.max(0,Math.min(steps.size()-1,selected+direction));wait=steps.get(selected).condition().type();initGui();id.setText(key);}
    private void cycleWait(){if(!apply())return;Wait[] supported={Wait.NEXT,Wait.CLICK,Wait.SLOT_HAS,Wait.SLOT_EMPTY};int current=Arrays.asList(supported).indexOf(wait);wait=supported[(current+1)%supported.length];String key=id.getText();apply();initGui();id.setText(key);}
    public void save(){if(!apply())return;try{GuiTutorial t=definition();GuiTutorials.save(t);error=GuiTutorialDrawing.tr("saved",t.id());}catch(IOException|RuntimeException e){error=e.getMessage();}}
    private void preview(){if(!apply())return;try{GuiTutorial t=GuiTutorialJson.parse(GuiTutorialJson.write(definition()));if(host!=null)GuiTutorialEvents.preview(t);else mc.displayGuiScreen(new GuiDemoScreen(t,()->mc.displayGuiScreen(this),false));}catch(RuntimeException e){error=e.getMessage();}}
    private void close(){if(embeddedClose!=null)embeddedClose.run();else mc.displayGuiScreen(parent);}
    @Override protected void renderWindowBackground(int mx,int my,float pt){if(host==null)super.renderWindowBackground(mx,my,pt);}
    @Override protected void renderWindow(int mx,int my,float pt){
        if(host==null){var demo=new GuiTutorialSession(new GuiTutorial(original.id(),original.title(),original.components(),original.screen(),original.adapter(),original.width(),original.height(),original.texture(),targets,steps));demo.seek(selected,0);GuiTutorialDrawing.demo(demo,modelX,modelY,scale);}
        for(var e:targets.entrySet()){Rect r=host==null?GuiTutorialDrawing.scaled(e.getValue().bounds(),modelX,modelY,scale):new StandardGuiAdapter().target(host,definitionSafe(),e.getKey());GuiTutorialDrawing.box(r,e.getKey().equals(steps.get(selected).target())?0xffedce7f:0x8867828c);}
        Gui.drawRect(panelX,panelY,panelX+panelWidth,panelY+174,0xf522303a);fontRenderer.drawString(GuiTutorialDrawing.tr("editor")+" "+(selected+1)+"/"+steps.size(),panelX+8,panelY+4,GuiTutorialDrawing.ACCENT);
        String[] labels={"resource","step_title","description","demo_item"};int[] ys={12,38,64,90};
        for(int i=0;i<labels.length;i++)fontRenderer.drawString(GuiTutorialDrawing.tr(labels[i]),panelX+8,panelY+ys[i],0xff9eb4bd);
        for(GuiTextField f:List.of(id,title,description,demoItem))f.drawTextBox();
        fontRenderer.drawString(fontRenderer.trimStringToWidth(error.isEmpty()?GuiTutorialDrawing.tr("capture_hint"):error,width-16),8,height-45,0xffacc5cf);
    }
    private GuiTutorial definitionSafe(){return new GuiTutorial(original.id(),original.title(),original.components(),original.screen(),original.adapter(),original.width(),original.height(),original.texture(),targets,steps);}
    @Override protected void mouseClicked(int x,int y,int button)throws IOException{
        for(GuiTextField f:List.of(id,title,description,demoItem))f.mouseClicked(x,y,button);
        if(button==0&&new Rect(panelX,panelY,panelWidth,15).contains(x,y)){moving=true;dragX=x-panelX;dragY=y-panelY;return;}
        if(new Rect(panelX,panelY,panelWidth,174).contains(x,y)||y>=height-35){super.mouseClicked(x,y,button);return;}
        if(button==1&&machine.contains(x,y)){region=true;cornerX=x;cornerY=y;return;}
        if(button==0&&apply()){
            for(var e:targets.entrySet()){
                Rect r=host==null?GuiTutorialDrawing.scaled(e.getValue().bounds(),modelX,modelY,scale):new StandardGuiAdapter().target(host,definitionSafe(),e.getKey());
                if(r!=null&&r.contains(x,y)){select(e.getKey());return;}
            }
        }
    }
    private void select(String target){Step old=steps.get(selected);Condition c=old.condition();steps.set(selected,new Step(old.title(),old.text(),target,new Condition(c.type(),target,c.item(),c.count(),c.field(),c.value()),old.duration(),old.demo()));}
    @Override protected void mouseClickMove(int x,int y,int button,long elapsed){if(moving){int dx=Math.max(4,Math.min(width-panelWidth-4,x-dragX))-panelX,dy=Math.max(4,Math.min(height-210,y-dragY))-panelY;panelX+=dx;panelY+=dy;for(var b:buttonList)if(b.id<10){b.x+=dx;b.y+=dy;}for(var f:List.of(id,title,description,demoItem)){f.x+=dx;f.y+=dy;}}}
    @Override protected void mouseReleased(int x,int y,int button){
        moving=false;if(region){region=false;if(!apply())return;int left=Math.max(machine.x(),Math.min(cornerX,x)),top=Math.max(machine.y(),Math.min(cornerY,y)),right=Math.min(machine.x()+machine.width(),Math.max(cornerX,x)),bottom=Math.min(machine.y()+machine.height(),Math.max(cornerY,y));
            if(right-left>=3&&bottom-top>=3){String key="region_"+targets.size();while(targets.containsKey(key))key+="_";targets.put(key,new Target(TargetType.REGION,0,new Rect(Math.round((left-modelX)/scale),Math.round((top-modelY)/scale),Math.max(1,Math.round((right-left)/scale)),Math.max(1,Math.round((bottom-top)/scale))),key,"",0));select(key);}
        }
    }
    @Override protected void keyTyped(char c,int key)throws IOException{if(key==Keyboard.KEY_ESCAPE){close();return;}if(isCtrlKeyDown()&&key==Keyboard.KEY_S){save();return;}for(var f:List.of(id,title,description,demoItem))if(f.textboxKeyTyped(c,key))return;}
    @Override public void updateScreen(){for(var f:List.of(id,title,description,demoItem))f.updateCursorCounter();}
    public void mouse(int x,int y,int button,boolean down)throws IOException{if(button>=0){if(down)mouseClicked(x,y,button);else mouseReleased(x,y,button);}else mouseClickMove(x,y,0,0);}
    public void key(char c,int key)throws IOException{keyTyped(c,key);}
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);}
}
