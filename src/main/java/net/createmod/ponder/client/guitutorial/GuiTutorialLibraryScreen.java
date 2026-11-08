package net.createmod.ponder.client.guitutorial;

import java.io.IOException;
import java.util.*;
import net.createmod.ponder.api.gui.GuiTutorial;
import net.createmod.ponder.foundation.ui.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** An explicit authoring/example entry; no example scenes are inserted into the ordinary catalog. */
public final class GuiTutorialLibraryScreen extends AbstractPonderScreen {
    private final GuiScreen parent;
    private final java.util.function.Consumer<GuiTutorial> selection;
    private GuiTextField search;
    private int first;
    private String error="";
    private List<GuiTutorial> visible=List.of();
    public GuiTutorialLibraryScreen(GuiScreen parent){this(parent,null);}
    public GuiTutorialLibraryScreen(GuiScreen parent,java.util.function.Consumer<GuiTutorial> selection){this.parent=parent;this.selection=selection;}
    @Override public void initGui(){
        String query=search==null?"":search.getText();buttonList.clear();Keyboard.enableRepeatEvents(true);
        search=new GuiTextField(1,fontRenderer,12,42,width-24,18);search.setText(query);
        addButton(new PonderButton(1,8,height-29,92,20,null).withLabel(GuiTutorialDrawing.tr("example")).withCallback(()->open(GuiTutorials.furnaceExample())));
        addButton(new PonderButton(2,104,height-29,92,20,null).withLabel(GuiTutorialDrawing.tr("reload")).withCallback(()->{GuiTutorials.reload();rebuild();}));
        addButton(new PonderButton(3,width-30,7,22,20,PonderButton.Icon.CLOSE).withCallback(()->mc.displayGuiScreen(parent)));
        rebuild();
    }
    private void rebuild(){
        String query=search.getText().toLowerCase(Locale.ROOT);visible=GuiTutorials.all().stream().filter(t->(GuiTutorialDrawing.text(t.title())+" "+t.id()).toLowerCase(Locale.ROOT).contains(query)).toList();
        int rows=Math.max(1,(height-120)/28);first=Math.max(0,Math.min(first,Math.max(0,visible.size()-rows)));buttonList.removeIf(b->b.id>=100);
        for(int i=first;i<Math.min(visible.size(),first+rows);i++){
            GuiTutorial lesson=visible.get(i);addButton(new PonderButton(100+i,12,72+(i-first)*28,width-24,24,null).withLabel(GuiTutorialDrawing.text(lesson.title()))
                .withCallback(()->{if(selection==null&&isShiftKeyDown())mc.displayGuiScreen(new GuiTutorialEditorScreen(lesson,this));else open(lesson);}));
        }
    }
    private void open(GuiTutorial lesson){try{if(selection!=null)selection.accept(lesson);else mc.displayGuiScreen(new GuiDemoScreen(lesson,()->mc.displayGuiScreen(this),false));}catch(RuntimeException e){error=e.getMessage();}}
    @Override protected void renderWindow(int mx,int my,float partial){
        fontRenderer.drawString(GuiTutorialDrawing.tr("library"),10,11,GuiTutorialDrawing.ACCENT);search.drawTextBox();
        String status=error.isEmpty()?GuiTutorialDrawing.tr("library_hint"):error;
        fontRenderer.drawString(fontRenderer.trimStringToWidth(status,width-24),12,height-47,0xff9ab1bd);
        if(visible.isEmpty())GuiTutorialDrawing.body(GuiTutorialDrawing.tr("empty"),14,78,width-28,height-145,0);
    }
    @Override protected void keyTyped(char c,int key)throws IOException{
        if(key==Keyboard.KEY_ESCAPE){mc.displayGuiScreen(parent);return;}if(search.textboxKeyTyped(c,key)){first=0;rebuild();return;}super.keyTyped(c,key);
    }
    @Override protected void mouseClicked(int x,int y,int button)throws IOException{search.mouseClicked(x,y,button);super.mouseClicked(x,y,button);}
    @Override public void handleMouseInput()throws IOException{super.handleMouseInput();if(Mouse.getEventDWheel()!=0){first+=Mouse.getEventDWheel()>0?-3:3;rebuild();}}
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);}
}
