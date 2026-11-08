package net.createmod.ponder.client.guide;

import java.io.*;
import java.util.*;
import org.lwjgl.input.*;
import org.lwjgl.opengl.GL11;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.api.editor.MarkdownDocument;
import net.createmod.ponder.api.guide.Tutorial;
import net.createmod.ponder.foundation.ui.*;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.guide.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.*;

@SideOnly(Side.CLIENT)
public final class MarkdownEditor extends AbstractPonderScreen {
    private static final Map<ResourceLocation,MarkdownEditor> SESSIONS=new HashMap<>();
    private final ResourceLocation resource;
    private final MarkdownDocument document;
    private final ResourceLocation component;
    private GuiScreen parent;
    private Tutorial compiled;
    private SceneWidget preview;
    private int revision=-1,debounce,topLine,horizontal,editorWidth,ticks,previewTick,contentTop=40;
    private String status="";
    private boolean selectionDrag,playing,compilationValid;
    private MarkdownEditor(Tutorial tutorial){
        resource=tutorial.id();document=new MarkdownDocument(tutorial.markdown());
        component=tutorial.components().isEmpty()?tutorial.id():tutorial.components().get(0);
    }
    public static void open(Tutorial tutorial,GuiScreen parent) {
        MarkdownEditor editor=SESSIONS.computeIfAbsent(tutorial.id(),id->new MarkdownEditor(tutorial));
        editor.parent=parent;Minecraft.getMinecraft().displayGuiScreen(editor);
    }
    public MarkdownDocument document(){return document;}
    @Override public void initGui(){
        Keyboard.enableRepeatEvents(true);editorWidth=width>=480?width*3/5:width-8;buttonList.clear();
        int columns=Math.min(8,Math.max(4,(width-12)/76)),bw=(width-16-(columns-1)*4)/columns;
        button(bw,columns,"ponder.guide.save",this::save);
        button(bw,columns,"ponder.guide.preview",this::openPreview);
        button(bw,columns,"ponder.guide.animation",this::openAnimation);
        button(bw,columns,"ponder.guide.step",()->insert("\n## "+I18n.format("ponder.guide.new_step")+"\n\n"));
        button(bw,columns,"ponder.guide.action",()->insert("\n```ponder\n[\n  {\"op\":\"world.show_section\",\"selection\":{\"type\":\"from_to\",\"from\":[0,1,0],\"to\":[2,1,2]},\"direction\":\"down\"},\n  {\"op\":\"scene.idle\",\"ticks\":20}\n]\n```\n"));
        button(bw,columns,"ponder.guide.insert_media.image",()->mc.displayGuiScreen(new MarkdownMediaScreen(this,MarkdownMedia.Type.IMAGE)));
        button(bw,columns,"ponder.guide.insert_media.video",()->mc.displayGuiScreen(new MarkdownMediaScreen(this,MarkdownMedia.Type.VIDEO)));
        button(bw,columns,"ponder.gui.insert",()->mc.displayGuiScreen(new net.createmod.ponder.client.guitutorial.GuiTutorialLibraryScreen(this,lesson->{
            document.insertBlock("```ponder-gui\n"+net.createmod.ponder.gui.GuiTutorialJson.write(lesson)+"\n```");changed();mc.displayGuiScreen(this);
        })));
        button(bw,columns,"ponder.guide.back",()->mc.displayGuiScreen(parent));
        contentTop=40+((buttonList.size()-1)/columns)*24;ensureCursor();
        compile();
    }
    private void button(int w,int columns,String key,Runnable action){int i=buttonList.size();addButton(new PonderButton(i+1,8+(i%columns)*(w+4),7+(i/columns)*24,w,20,null).withLabel(I18n.format(key)).withTooltip(key).withCallback(action));}
    void insertMedia(MarkdownMedia.Type type,String source,String title,String poster,int width,int height){
        String block=MarkdownMedia.create(resource,type,source,title,poster,width,height);document.insertBlock(block);changed();
    }
    private void insert(String text){document.insert(text);changed();}
    private void changed(){debounce=10;ensureCursor();}
    private void compile(){
        revision=document.revision();compilationValid=false;
        try{
            Tutorial candidate=MarkdownTutorialCompiler.compile(resource,document.text(),resource+".md");
            if(!candidate.id().equals(resource))throw new IllegalArgumentException("Keep id: "+resource+" when editing this tutorial");
            SceneWidget next=null;
            if(!candidate.scenes().isEmpty()){
                var definition=candidate.scenes().get(0);
                var snapshot=new Tutorial.Snapshot(definition.getSceneId(),0,definition,-35,25,1,true);
                next=new SceneWidget(Tutorials.compile(definition),snapshot);
            }else for(var block:candidate.blocks())if(block.snapshot()!=null){next=new SceneWidget(Tutorials.compile(block.snapshot(),component),block.snapshot());break;}
            compilationValid=true;compiled=candidate;preview=next;previewTick=0;playing=false;status=I18n.format("ponder.guide.valid");
        }catch(RuntimeException failure){status=failure.getMessage();}
    }
    private void save(){
        compile();if(!compilationValid)return;
        try{
            GuideSources.save(resource,document.text());PonderIndex.reload();document.saved();status=I18n.format("ponder.guide.saved",GuideSources.path(resource));
        }catch(RuntimeException|IOException failure){status=failure.getMessage();}
    }
    private boolean valid(){compile();return compilationValid;}
    private void openPreview(){if(valid())mc.displayGuiScreen(new GuideScreen(component,compiled.id(),List.of(compiled),true).returningTo(this));}
    private void openAnimation(){if(!valid())return;
        if(!compiled.scenes().isEmpty())mc.displayGuiScreen(new PonderUI(List.of(Tutorials.compile(compiled.scenes().get(0))))
            .withGuide(() -> new GuideScreen(component,compiled.id(),List.of(compiled),true).returningTo(this)).returningTo(this));
        else if(compiled.animation()!=null)mc.displayGuiScreen(Tutorials.open(component,compiled.animation(),false));
    }
    @Override public void updateScreen(){
        ticks++;if(revision!=document.revision()&&debounce>0&&--debounce==0)compile();
        if(playing&&preview!=null){preview.scene.setPaused(false);preview.scene.tick();previewTick=preview.scene.getCurrentTick();if(preview.scene.isFinished())playing=false;}
    }
    private int visibleLines(){return Math.max(1,(height-contentTop-60)/12);}
    private void ensureCursor(){int line=document.line();if(line<topLine)topLine=line;if(line>=topLine+visibleLines())topLine=line-visibleLines()+1;
        String prefix=document.text().substring(document.lineStart(document.cursor()),document.cursor());int px=fontRenderer.getStringWidth(prefix);horizontal=Math.max(0,Math.max(horizontal,px-editorWidth+70));if(px<horizontal)horizontal=px;
    }
    @Override protected void renderWindow(int mx,int my,float partial){
        Gui.drawRect(8,contentTop-1,editorWidth,height-49,0xff18212a);
        String[] lines=document.text().split("\n",-1);int lineStart=0;
        try(GlStateGuard ignored=GlStateGuard.capture()){
            scissor(8,contentTop,editorWidth-8,Math.max(0,height-contentTop-50));
            for(int n=0;n<lines.length;n++){
                String line=lines[n];int y=contentTop+3+(n-topLine)*12;
                if(n>=topLine&&n<topLine+visibleLines()+1){
                    fontRenderer.drawString(Integer.toString(n+1),11,y,0xff667d8d);
                    int a=Math.max(lineStart,Math.min(document.cursor(),document.anchor()));
                    int b=Math.min(lineStart+line.length(),Math.max(document.cursor(),document.anchor()));
                    if(a<b){int ax=42+fontRenderer.getStringWidth(line.substring(0,a-lineStart))-horizontal,bx=42+fontRenderer.getStringWidth(line.substring(0,b-lineStart))-horizontal;Gui.drawRect(ax,y-1,bx,y+10,0xff365c70);}
                    fontRenderer.drawString(line,42-horizontal,y,line.startsWith("#")?0xffa9ddd4:line.startsWith("```")?0xffe2ca8b:0xffd6e0e8);
                    if(document.cursor()>=lineStart&&document.cursor()<=lineStart+line.length()&&(ticks/10)%2==0){int cx=42+fontRenderer.getStringWidth(line.substring(0,document.cursor()-lineStart))-horizontal;Gui.drawRect(cx,y-1,cx+1,y+10,0xffe3faf2);}
                }
                lineStart+=line.length()+1;
            }
        }
        if(editorWidth<width-16){
            int x=editorWidth+10,w=width-x-8;
            fontRenderer.drawString(I18n.format("ponder.guide.preview")+(document.dirty()?" *":""),x,contentTop+2,0xffa9ddd4);
            if(compiled!=null)fontRenderer.drawSplitString(compiled.title(),x,contentTop+18,w,0xffd6e0e8);
            if(preview!=null){int y=contentTop+42,h=Math.min(150,Math.max(36,height-y-86));Gui.drawRect(x,y,x+w,y+h,0xff1c2932);preview.render(x,y,w,h,width,height,y,y+h,mx,my,partial);
                int total=Math.max(1,preview.scene.getTotalTicks());
                Gui.drawRect(x,y+6+h,x+w,y+12+h,0xff3a4f5c);Gui.drawRect(x,y+6+h,x+w*previewTick/total,y+12+h,0xff9fd6d2);
                PonderButton.drawIcon(playing?PonderButton.Icon.PAUSE:PonderButton.Icon.PLAY,x+8,y+25+h,14,0xffcadbdc);
                fontRenderer.drawString(previewTick+" / "+total,x+22,y+20+h,0xffcadbdc);
            }
        }
        fontRenderer.drawString(fontRenderer.trimStringToWidth(status,Math.max(10,width-20)),10,height-38,0xffb5c8d1);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(I18n.format("ponder.guide.editor_keys")+(document.dirty()?" *":""),width-20),10,height-21,0xff8fabb9);
    }
    private void scissor(int x,int y,int w,int h){GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(x*mc.displayWidth/width,mc.displayHeight-(y+h)*mc.displayHeight/height,w*mc.displayWidth/width,h*mc.displayHeight/height);}
    private int position(int mx,int my){
        String[] lines=document.text().split("\n",-1);int row=Math.max(0,Math.min(lines.length-1,topLine+(my-contentTop-3)/12)),position=0;
        for(int i=0;i<row;i++)position+=lines[i].length()+1;
        int px=mx-42+horizontal;String line=lines[row];int c=0;
        while(c<line.length()&&fontRenderer.getStringWidth(line.substring(0,c+1))<=px)c++;
        return position+c;
    }
    @Override protected void mouseClicked(int mx,int my,int button)throws IOException{
        if(button==0&&mx>=8&&mx<editorWidth&&my>=contentTop&&my<height-49){document.select(position(mx,my),isShiftKeyDown());selectionDrag=true;return;}
        if(button==0&&preview!=null&&mx>editorWidth&&my>=contentTop+42&&my<height-49){
            int y=contentTop+42,h=Math.min(150,Math.max(36,height-y-86));
            if(my>=y+6+h&&my<=y+14+h){int x=editorWidth+10,w=width-x-8;previewTick=(int)Math.max(0,Math.min(preview.scene.getTotalTicks(),(mx-x)/(float)w*preview.scene.getTotalTicks()));preview.scene.seekToTime(previewTick);playing=false;}
            else playing=!playing;return;
        }
        super.mouseClicked(mx,my,button);
    }
    @Override protected void mouseClickMove(int mx,int my,int button,long elapsed){if(selectionDrag){document.select(position(mx,my),true);ensureCursor();}}
    @Override protected void mouseReleased(int mx,int my,int button){selectionDrag=false;super.mouseReleased(mx,my,button);}
    @Override public void handleMouseInput()throws IOException{super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel!=0)topLine=Math.max(0,Math.min(Math.max(0,document.text().split("\n",-1).length-visibleLines()),topLine-Integer.signum(wheel)*3));}
    @Override protected void keyTyped(char c,int key)throws IOException{
        boolean ctrl=isCtrlKeyDown(),shift=isShiftKeyDown();
        if(ctrl)switch(key){
            case Keyboard.KEY_S:save();return;
            case Keyboard.KEY_A:document.selectAll();return;
            case Keyboard.KEY_C:GuiScreen.setClipboardString(document.selected());return;
            case Keyboard.KEY_X:GuiScreen.setClipboardString(document.selected());document.insert("");changed();return;
            case Keyboard.KEY_V:insert(GuiScreen.getClipboardString());return;
            case Keyboard.KEY_Z:if(shift)document.redo();else document.undo();changed();return;
            case Keyboard.KEY_Y:document.redo();changed();return;
        }
        switch(key){
            case Keyboard.KEY_ESCAPE:mc.displayGuiScreen(parent);return;
            case Keyboard.KEY_RETURN:case Keyboard.KEY_NUMPADENTER:insert("\n");return;
            case Keyboard.KEY_TAB:insert("  ");return;
            case Keyboard.KEY_BACK:document.erase(true);changed();return;
            case Keyboard.KEY_DELETE:document.erase(false);changed();return;
            case Keyboard.KEY_LEFT:document.select(document.cursor()-1,shift);ensureCursor();return;
            case Keyboard.KEY_RIGHT:document.select(document.cursor()+1,shift);ensureCursor();return;
            case Keyboard.KEY_UP:document.moveVertical(-1,shift);ensureCursor();return;
            case Keyboard.KEY_DOWN:document.moveVertical(1,shift);ensureCursor();return;
            case Keyboard.KEY_HOME:document.select(ctrl?0:document.lineStart(document.cursor()),shift);ensureCursor();return;
            case Keyboard.KEY_END:document.select(ctrl?document.text().length():document.lineEnd(document.cursor()),shift);ensureCursor();return;
            case Keyboard.KEY_F5:openPreview();return;
        }
        if(ChatAllowedCharacters.isAllowedCharacter(c))insert(Character.toString(c));
    }
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);}
    @Override protected void renderWindowForeground(int mx,int my,float partial){
        for(var button:buttonList)if(button instanceof PonderButton ponder&&ponder.isHoveredButton()&&ponder.getTooltipKey()!=null)
            drawHoveringText(List.of(I18n.format(ponder.getTooltipKey())),mx,my);
    }
    @Override protected String getBreadcrumbTitle(){return resource+".md";}
}
