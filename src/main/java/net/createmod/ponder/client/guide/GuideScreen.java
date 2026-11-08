package net.createmod.ponder.client.guide;

import java.io.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.lwjgl.input.*;
import org.lwjgl.opengl.GL11;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.guide.Tutorial;
import net.createmod.ponder.api.guide.Tutorial.*;
import net.createmod.ponder.foundation.ui.*;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.*;

/** Reflowing native Minecraft guide with a clipped, independently scrollable page and navigation. */
@SideOnly(Side.CLIENT)
public final class GuideScreen extends AbstractPonderScreen {
    private final ResourceLocation component;
    private final List<Tutorial> pages;
    private final List<Placed> layout=new ArrayList<>();
    private final List<Nav> navigation=new ArrayList<>();
    private final Map<Snapshot,SceneWidget> widgets=new LinkedHashMap<>(8,.75f,true);
    private final GuideImages images=new GuideImages();
    private final Map<Block,net.createmod.ponder.compat.media.MediaVideo> videos=new LinkedHashMap<>();
    private net.createmod.ponder.compat.media.MediaVideo hoveredVideo;
    private Placed hoveredVideoBlock;
    private int hoveredVideoX,hoveredVideoY,hoveredVideoWidth;
    private GuiTextField search;
    private int pageIndex,side,contentX,contentWidth,documentHeight,scroll,navScroll;
    private ResourceLocation selected;
    private ItemStack hoveredItem=ItemStack.EMPTY;
    private SceneWidget hoveredWidget,dragWidget;
    private int lastX,lastY,dragDistance;
    private String link="",error="";
    private boolean scrollDragging;
    private final boolean preview;
    private GuiScreen back;
    private record Placed(Block block,int y,int height,List<String> lines) {}
    private record Nav(String title,int page,int block,boolean category) {}

    public GuideScreen(ResourceLocation component,ResourceLocation selected) {
        this(component,selected,Tutorials.pages(component),false);
    }
    public GuideScreen(ResourceLocation component,ResourceLocation selected,List<Tutorial> pages,boolean preview) {
        this.component=component;this.selected=selected;this.pages=List.copyOf(pages);this.preview=preview;
        if(pages.isEmpty())throw new IllegalArgumentException("No guide presentation for "+component);
        for(int i=0;i<pages.size();i++)if(pages.get(i).id().equals(selected)||Objects.equals(pages.get(i).animation(),selected))pageIndex=i;
    }
    public GuideScreen returningTo(GuiScreen screen){back=screen;return this;}
    public Tutorial currentTutorial(){return pages.get(pageIndex);}
    public int getScroll(){return scroll;}
    public int getDocumentHeight(){return documentHeight;}
    @Override public void initGui() {
        side=width>=480?150:Math.min(112,width/3);contentX=side+14;contentWidth=Math.max(70,width-contentX-18);
        String query=search==null?"":search.getText();
        search=new GuiTextField(20,fontRenderer,8,42,side-16,18);search.setMaxStringLength(128);search.setText(query);
        buttonList.clear();
        addButton(new PonderButton(1,8,7,22,20,PonderButton.Icon.INDEX).withTooltip("ponder.ui.index_title")
            .withCallback(()->ScreenOpener.open(new PonderIndexScreen())));
        addButton(new PonderButton(2,36,7,22,20,PonderButton.Icon.PLAY).withTooltip("ponder.guide.animation")
            .enabled(currentTutorial().animation()!=null).withCallback(this::openAnimation));
        addButton(new PonderButton(3,64,7,22,20,PonderButton.Icon.STAR).active(Tutorials.preferGuide())
            .withTooltip("ponder.guide.default_guide").withCallback(()->{Tutorials.setPreferGuide(true);initGui();}));
        addButton(new PonderButton(4,width-30,7,22,20,PonderButton.Icon.CLOSE).withTooltip("ponder.ui.close")
            .withCallback(()->mc.displayGuiScreen(back)));
        addButton(new PonderButton(5,8,height-29,side-16,20,null).withLabel(I18n.format("ponder.guide.edit"))
            .withCallback(()->MarkdownEditor.open(currentTutorial(),this)));
        boolean hasScene = currentTutorial().blocks().stream().anyMatch(b -> b.snapshot()!=null);
        addButton(new PonderButton(6,width-60,height-29,22,20,PonderButton.Icon.PROJECT).enabled(hasScene).withTooltip("ponder.projection.project_tip")
            .withCallback(() -> {var scene=projectionScene();if(scene!=null)net.createmod.ponder.client.projection.ProjectionService.project(scene,this);}));
        addButton(new PonderButton(7,width-34,height-29,22,20,PonderButton.Icon.MATERIALS).enabled(hasScene).withTooltip("ponder.projection.pin_tip")
            .withCallback(() -> {var scene=projectionScene();if(scene!=null)net.createmod.ponder.client.projection.ProjectionService.pin(scene,this);}));
        rebuild();
    }
    private net.createmod.ponder.foundation.PonderScene projectionScene() {
        if (hoveredWidget!=null) return hoveredWidget.scene;
        for (Placed placed:layout) if(placed.block().snapshot()!=null && placed.y()+placed.height()>=scroll && placed.y()<=scroll+viewHeight()) {
            SceneWidget widget=widgets.get(placed.block().snapshot());if(widget!=null)return widget.scene;
        }
        for (var block:currentTutorial().blocks()) if(block.snapshot()!=null) {
            var own=currentTutorial().scenes().stream().filter(d->d.getSceneId().equals(block.snapshot().scene())).findFirst().orElse(null);
            var scene=own==null?Tutorials.compile(block.snapshot(),component):Tutorials.compile(own);scene.seekToTime(Math.min(block.snapshot().tick(),scene.getTotalTicks()));return scene;
        }
        return null;
    }
    private void rebuild() {
        layout.clear();int y=0;
        for(Block block:currentTutorial().blocks()) {
            List<String> lines=List.of();int h;
            switch(block.kind()) {
                case SCENE -> h=Math.max(100,Math.min(180,contentWidth*2/3));
                case ITEMS -> h=28;
                case RECIPE -> h=84;
                case IMAGE -> {int[] size=images.dimensions(block.target());h=block.media()!=null&&block.media().height()>0?Math.min(300,block.media().height()):size[0]==0?Math.min(180,mediaWidth(block)*9/16):Math.min(240,Math.max(16,mediaWidth(block)*size[1]/size[0]));}
                case VIDEO -> h=(block.media()!=null&&block.media().height()>0?Math.max(40,Math.min(300,block.media().height())):Math.max(40,Math.min(200,mediaWidth(block)*9/16)))+26;
                case QUEST_LINK, GUI_TUTORIAL -> h=26;
                case RULE -> h=8;
                case TABLE -> {lines=tableLines(block.text());h=tableHeight(lines);}
                default -> {
                    String text=block.kind()==Kind.HEADING?"§l"+block.text():block.text();
                    lines=fontRenderer.listFormattedStringToWidth(text,Math.max(20,contentWidth-(block.kind()==Kind.CODE?12:0)));
                    h=lines.size()*(block.kind()==Kind.HEADING?13:11)+(block.kind()==Kind.CODE?10:0);
                }
            }
            layout.add(new Placed(block,y,h,lines));y+=h+(block.kind()==Kind.HEADING?10:12);
        }
        documentHeight=y;clampScroll();rebuildNavigation();widgets.clear();
    }
    private int mediaWidth(Block block){
        int configured=block.media()==null?0:block.media().width();
        return configured==0?contentWidth:Math.min(contentWidth,Math.max(block.kind()==Kind.VIDEO?70:1,configured));
    }
    private void rebuildNavigation() {
        navigation.clear();String category=null;String q=search.getText().toLowerCase(Locale.ROOT).trim();
        for(int i=0;i<pages.size();i++) {
            Tutorial page=pages.get(i);
            if(!q.isEmpty()&&!(page.title()+" "+page.blocks().stream().map(Block::text).reduce("",(a,b)->a+" "+b)).toLowerCase(Locale.ROOT).contains(q))continue;
            if(!page.category().isEmpty()&&!page.category().equals(category)) {
                category=page.category();navigation.add(new Nav(category,-1,-1,true));
            }
            navigation.add(new Nav(page.title(),i,-1,false));
            if(i==pageIndex)for(int j=0;j<layout.size();j++) {
                Block block=layout.get(j).block();
                if(block.kind()==Kind.HEADING&&block.level()>1)navigation.add(new Nav("  "+block.text(),i,j,false));
            }
        }
        navScroll=Math.max(0,Math.min(navScroll,Math.max(0,navigation.size()*18-(height-106))));
    }
    private List<String> tableLines(String text){return Arrays.asList(text.split("\n",-1));}
    private int tableHeight(List<String> rows) {
        int h=0;
        for(String row:rows){String[] cells=row.split("\t",-1);int max=1;for(String cell:cells)max=Math.max(max,fontRenderer.listFormattedStringToWidth(cell,Math.max(16,contentWidth/cells.length-8)).size());h+=max*11+8;}
        return h;
    }
    @Override protected void renderWindow(int mx,int my,float partial) {
        hoveredItem=ItemStack.EMPTY;hoveredWidget=null;hoveredVideo=null;hoveredVideoBlock=null;link="";
        Gui.drawRect(0,34,side,height-38,0xff191f26);Gui.drawRect(side,34,side+1,height-38,0xff35434e);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(currentTutorial().title(),Math.max(10,width-140)),96,13,0xffeef2f5);
        search.drawTextBox();
        if(search.getText().isEmpty()&&!search.isFocused())fontRenderer.drawString(I18n.format("ponder.guide.search"),12,47,0xff7c8b99);
        try(GlStateGuard ignored=GlStateGuard.capture()) {
            scissor(0,66,side,height-106);
            for(int i=0;i<navigation.size();i++) {
                Nav nav=navigation.get(i);int y=68+i*18-navScroll;if(y+18<66||y>height-40)continue;
                boolean hover=mx>=0&&mx<side&&my>=Math.max(66,y)&&my<Math.min(y+18,height-40);
                if(!nav.category()&&(hover||nav.page()==pageIndex&&nav.block()<0))Gui.drawRect(4,y,side-4,y+17,hover?0xff374955:0xff273c46);
                fontRenderer.drawString(fontRenderer.trimStringToWidth(nav.title(),side-16),8,y+4,nav.category()?0xff96bab8:0xffd3dfe6);
            }
        }
        try(GlStateGuard ignored=GlStateGuard.capture()) {
            scissor(contentX,40,contentWidth,height-84);
            for(Placed placed:layout) {
                int y=44+placed.y()-scroll;if(y+placed.height()<40||y>height-44)continue;
                renderBlock(placed,contentX,y,mx,my,partial);
            }
        }
        // Retain worlds only for widgets currently in the viewport.
        Set<Snapshot> visible = new HashSet<>();
        for (Placed p : layout) if (p.block().snapshot()!=null && p.y()+p.height()>=scroll-4 && p.y()<=scroll+viewHeight()) visible.add(p.block().snapshot());
        widgets.keySet().retainAll(visible);
        Set<Block> visibleVideos = new HashSet<>();
        for(Placed p:layout) if(p.block().kind()==Kind.VIDEO && p.y()+p.height()>=scroll && p.y()<=scroll+viewHeight())visibleVideos.add(p.block());
        for(var entry:videos.entrySet())if(!visibleVideos.contains(entry.getKey())&&entry.getValue().playing())entry.getValue().toggle();
        if(videos.size()>2){var iterator=videos.entrySet().iterator();while(iterator.hasNext()&&videos.size()>2){var entry=iterator.next();if(!visibleVideos.contains(entry.getKey())){entry.getValue().close();iterator.remove();}}}
        if(documentHeight>viewHeight()) {
            int track=viewHeight(),thumb=Math.max(18,track*track/documentHeight);
            int y=44+(track-thumb)*scroll/Math.max(1,documentHeight-track);
            Gui.drawRect(width-9,44,width-5,height-44,0xff26323c);Gui.drawRect(width-9,y,width-5,y+thumb,0xff8fbdb9);
        }
        fontRenderer.drawString(fontRenderer.trimStringToWidth(error.isEmpty()?I18n.format("ponder.guide.controls"):error,Math.max(20,width-side-82)),side+12,height-22,error.isEmpty()?0xff9dabb8:0xfff0a69e);
    }
    private void renderBlock(Placed placed,int x,int y,int mx,int my,float partial) {
        Block b=placed.block();boolean hover=mx>=x&&mx<x+contentWidth&&my>=Math.max(40,y)&&my<Math.min(y+placed.height(),height-44);
        switch(b.kind()) {
            case SCENE -> {
                Gui.drawRect(x,y,x+contentWidth,y+placed.height(),0xff1c2932);
                try {
                    SceneWidget widget=widgets.get(b.snapshot());
                    if(widget==null) {
                        var snapshot=b.snapshot();
                        var own=currentTutorial().scenes().stream().filter(d->d.getSceneId().equals(snapshot.scene())).findFirst().orElse(null);
                        widget=new SceneWidget(own==null?Tutorials.compile(snapshot,component):Tutorials.compile(own),snapshot);widgets.put(snapshot,widget);
                    }
                    ItemStack item=widget.render(x,y,contentWidth,placed.height(),width,height,40,height-44,mx,my,partial);
                    if(hover){hoveredWidget=widget;hoveredItem=item;}
                }catch(RuntimeException failure){fontRenderer.drawSplitString(I18n.format("ponder.guide.scene_error",failure.getMessage()),x+8,y+8,contentWidth-16,0xffe49c93);}
                PonderButton.drawIcon(PonderButton.Icon.REPLAY,x+contentWidth-12,y+12,13,0xff8aa6b4);
            }
            case ITEMS -> {
                int n=0;for(String id:b.text().split("[ ,]+")) {
                    ItemStack item=stack(id);int ix=x+6+n++*24;drawItem(item,ix,y+4,mx,my);
                    if(!item.isEmpty()&&mx>=ix&&mx<ix+16&&my>=y&&my<y+24){hoveredItem=item;link="item:"+id;}
                }
                if(b.text().indexOf(' ')<0){ItemStack item=stack(b.text());if(!item.isEmpty())fontRenderer.drawString(item.getDisplayName(),x+30,y+9,0xffb8d8df);}
            }
            case RECIPE -> renderRecipe(b,x,y,mx,my);
            case IMAGE -> {
                int mediaWidth=mediaWidth(b);
                String status=images.draw(b.target(),x+(contentWidth-mediaWidth)/2,y,mediaWidth,placed.height());
                if(!status.isEmpty())fontRenderer.drawSplitString(status+(b.text().isBlank()?"":" · "+b.text()),x+6,y+6,contentWidth-12,0xffb7c9d5);
            }
            case VIDEO -> {
                int mediaWidth=mediaWidth(b);x+=(contentWidth-mediaWidth)/2;
                Gui.drawRect(x,y,x+mediaWidth,y+placed.height(),0xff1c2932);
                var video=videos.computeIfAbsent(b,key->new net.createmod.ponder.compat.media.MediaVideo(key.target()));
                int vh=placed.height()-26,texture=video.texture();
                if(texture>0){
                    int vw=video.width(),height=video.height();if(vw<=0||height<=0){vw=16;height=9;}
                    float scale=Math.min(mediaWidth/(float)vw,vh/(float)height);int iw=Math.max(1,(int)(vw*scale)),ih=Math.max(1,(int)(height*scale));
                    GlStateManager.bindTexture(texture);GlStateManager.color(1,1,1,1);Gui.drawModalRectWithCustomSizedTexture(x+(mediaWidth-iw)/2,y+(vh-ih)/2,0,0,iw,ih,iw,ih);
                }else if(b.media()!=null&&!b.media().poster().isBlank())images.draw(b.media().poster(),x,y,mediaWidth,vh);
                if(texture<=0)fontRenderer.drawSplitString(video.status(),x+6,y+8,mediaWidth-12,0xffd5dfe5);
                int cy=y+vh;Gui.drawRect(x,cy,x+mediaWidth,y+placed.height(),0xff293c49);
                PonderButton.drawIcon(video.playing()?PonderButton.Icon.PAUSE:PonderButton.Icon.PLAY,x+13,cy+12,16,0xffb5e8da);
                PonderButton.drawIcon(PonderButton.Icon.REPLAY,x+34,cy+12,16,0xffb5e8da);
                String label=texture<=0||b.text().isBlank()?video.status():b.text();fontRenderer.drawString(fontRenderer.trimStringToWidth(label,Math.max(10,mediaWidth-52)),x+47,cy+4,0xffc3d6dd);
                if(video.seekable()&&video.duration()>0){Gui.drawRect(x+47,cy+17,x+mediaWidth-5,cy+21,0xff455d6b);int fill=(int)((mediaWidth-52)*Math.min(1,video.position()/(double)video.duration()));Gui.drawRect(x+47,cy+17,x+47+fill,cy+21,0xff9fd6d2);}
                if(hover&&mx>=x&&mx<x+mediaWidth){hoveredVideo=video;hoveredVideoBlock=placed;hoveredVideoX=x;hoveredVideoY=y;hoveredVideoWidth=mediaWidth;}
            }
            case QUEST_LINK, GUI_TUTORIAL -> {
                Gui.drawRect(x,y,x+contentWidth,y+24,hover?0xff375564:0xff273d49);
                PonderButton.drawIcon(PonderButton.Icon.RIGHT,x+12,y+12,14,0xffa9ddd4);
                fontRenderer.drawString(fontRenderer.trimStringToWidth(b.kind()==Kind.GUI_TUTORIAL?net.createmod.ponder.client.guitutorial.GuiTutorialDrawing.text(b.text()):b.text(),contentWidth-32),x+26,y+8,0xffa9ddd4);
                if(hover)link=b.kind()==Kind.GUI_TUTORIAL?"gui:"+b.target():b.target();
            }
            case RULE -> Gui.drawRect(x,y+3,x+contentWidth,y+4,0xff3c4c57);
            case TABLE -> {
                int rowY=y,index=0;
                for(String row:placed.lines()) {
                    String[] cells=row.split("\t",-1);int cw=contentWidth/cells.length,max=1;
                    List<List<String>> wrapped=new ArrayList<>();for(String cell:cells){var lines=fontRenderer.listFormattedStringToWidth(cell,Math.max(16,cw-8));wrapped.add(lines);max=Math.max(max,lines.size());}
                    int rh=max*11+8;Gui.drawRect(x,rowY,x+contentWidth,rowY+rh,index++%2==0?0xff27343e:0xff1e2931);
                    for(int c=0;c<cells.length;c++){int ly=rowY+4;for(String line:wrapped.get(c)){fontRenderer.drawString(line,x+c*cw+4,ly,0xffd5dfe5);ly+=11;}Gui.drawRect(x+c*cw,rowY,x+c*cw+1,rowY+rh,0xff3c4c57);}
                    rowY+=rh;
                }
            }
            default -> {
                if(b.kind()==Kind.CODE)Gui.drawRect(x,y,x+contentWidth,y+placed.height(),0xff202b34);
                int ly=y+(b.kind()==Kind.CODE?5:0);
                for(String line:placed.lines()){fontRenderer.drawString(line,x+(b.kind()==Kind.CODE?6:0),ly,b.kind()==Kind.HEADING?0xffa9ddd4:0xffd6dee5);ly+=b.kind()==Kind.HEADING?13:11;}
                if(hover&&!b.target().isEmpty())link=b.target();
            }
        }
    }
    private void renderRecipe(Block block,int x,int y,int mx,int my) {
        Gui.drawRect(x,y,x+contentWidth,y+78,0xff202c35);
        boolean byOutput=block.target().startsWith("output:");ResourceLocation id=new ResourceLocation(block.target().substring(block.target().indexOf(':')+1));
        List<IRecipe> recipes=new ArrayList<>();
        if(byOutput)for(IRecipe recipe:CraftingManager.REGISTRY) {if(!recipe.getRecipeOutput().isEmpty()&&id.equals(recipe.getRecipeOutput().getItem().getRegistryName()))recipes.add(recipe);}
        else {IRecipe recipe=CraftingManager.REGISTRY.getObject(id);if(recipe!=null)recipes.add(recipe);}
        if(recipes.isEmpty()) {
            if(byOutput)for(var smelt:FurnaceRecipes.instance().getSmeltingList().entrySet())if(id.equals(smelt.getValue().getItem().getRegistryName())) {
                drawItem(smelt.getKey(),x+18,y+25,mx,my);fontRenderer.drawString("→",x+50,y+30,0xffa9ddd4);drawItem(smelt.getValue(),x+78,y+25,mx,my);return;
            }
            fontRenderer.drawSplitString(block.text().isBlank()?I18n.format("ponder.guide.no_recipe"):block.text(),x+8,y+12,contentWidth-16,0xffaab9c4);return;
        }
        IRecipe recipe=recipes.get((int)((System.currentTimeMillis()/4000)%recipes.size()));
        int columns = recipe instanceof ShapedRecipes shaped ? shaped.getRecipeWidth() : recipe instanceof net.minecraftforge.oredict.ShapedOreRecipe ore ? ore.getWidth() : 3;
        columns=Math.max(1,Math.min(3,columns));
        int i=0;for(Ingredient ingredient:recipe.getIngredients()) {
            if(i>=9)break;ItemStack[] matches=ingredient.getMatchingStacks();ItemStack item=matches.length==0?ItemStack.EMPTY:matches[(int)((System.currentTimeMillis()/1200)%matches.length)];
            drawItem(item,x+12+(i%columns)*19,y+10+(i/columns)*19,mx,my);i++;
        }
        fontRenderer.drawString("→",x+77,y+34,0xffa9ddd4);drawItem(recipe.getRecipeOutput(),x+99,y+30,mx,my);
        if(contentWidth>180)fontRenderer.drawSplitString(recipe.getRecipeOutput().getDisplayName(),x+126,y+24,contentWidth-136,0xffd6e2e5);
    }
    private void drawItem(ItemStack item,int x,int y,int mx,int my) {
        Gui.drawRect(x-1,y-1,x+17,y+17,0xff364651);
        if(item.isEmpty())return;
        try(GlStateGuard ignored=GlStateGuard.capture()){RenderHelper.enableGUIStandardItemLighting();mc.getRenderItem().renderItemAndEffectIntoGUI(item,x,y);mc.getRenderItem().renderItemOverlayIntoGUI(fontRenderer,item,x,y,null);}
        if(mx>=x&&mx<x+16&&my>=Math.max(40,y)&&my<Math.min(y+16,height-44)){hoveredItem=item;link="item:"+item.getItem().getRegistryName();}
    }
    private static ItemStack stack(String id){Item item=Item.REGISTRY.getObject(new ResourceLocation(id));return item==null?ItemStack.EMPTY:new ItemStack(item);}
    @Override protected void renderWindowForeground(int mx,int my,float partial) {
        if(!hoveredItem.isEmpty())renderToolTip(hoveredItem,mx,my);
        for(GuiButton button:buttonList)if(button instanceof PonderButton p&&p.isHoveredButton()&&p.getTooltipKey()!=null)drawHoveringText(I18n.format(p.getTooltipKey()),mx,my);
    }
    private void scissor(int x,int y,int w,int h) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(x*mc.displayWidth/width,mc.displayHeight-(y+h)*mc.displayHeight/height,w*mc.displayWidth/width,h*mc.displayHeight/height);
    }
    private int viewHeight(){return Math.max(1,height-88);}
    private void clampScroll(){scroll=Math.max(0,Math.min(scroll,Math.max(0,documentHeight-viewHeight())));}
    @Override public void handleMouseInput()throws IOException {
        super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel==0)return;
        int mx=Mouse.getEventX()*width/mc.displayWidth;
        if(mx<side){navScroll-=Integer.signum(wheel)*36;rebuildNavigation();}
        else if(hoveredWidget!=null&&isShiftKeyDown()){hoveredWidget.zoom=Math.max(.25f,Math.min(4,hoveredWidget.zoom*(wheel>0?1.1f:.9f)));}
        else {scroll-=Integer.signum(wheel)*30;clampScroll();}
    }
    @Override protected void mouseClicked(int mx,int my,int button)throws IOException {
        search.mouseClicked(mx,my,button);
        if(button==0&&my>=66&&my<height-40&&mx<side) {
            int index=(my-68+navScroll)/18;
            if(index>=0&&index<navigation.size()) {
                Nav nav=navigation.get(index);if(nav.category())return;
                if(pageIndex!=nav.page()){pageIndex=nav.page();scroll=0;rebuild();initGui();}
                if(nav.block()>=0){scroll=layout.get(nav.block()).y();clampScroll();}return;
            }
        }
        if(hoveredVideo!=null&&(button==0||button==1)) {
            int cy=hoveredVideoY+hoveredVideoBlock.height()-26;
            if(button==1||mx<hoveredVideoX+43&&mx>=hoveredVideoX+23)hoveredVideo.replay();
            else if(my>=cy+15&&mx>=hoveredVideoX+47&&hoveredVideo.seekable())hoveredVideo.seek((long)(Math.max(0,Math.min(1,(mx-hoveredVideoX-47)/(double)Math.max(1,hoveredVideoWidth-52)))*hoveredVideo.duration()));
            else hoveredVideo.toggle();
            return;
        }
        if(button==0&&mx>=width-12&&my>=44&&my<height-44){scrollDragging=true;scrollToMouse(my);return;}
        if(button==0&&hoveredWidget!=null&&hoveredWidget.interactive){dragWidget=hoveredWidget;lastX=mx;lastY=my;dragDistance=0;return;}
        if(button==0&&!link.isEmpty()&&my>=40&&my<height-44){follow(link);return;}
        super.mouseClicked(mx,my,button);
    }
    @Override protected void mouseClickMove(int mx,int my,int button,long elapsed) {
        if(scrollDragging){scrollToMouse(my);return;}
        if(dragWidget!=null){int dx=mx-lastX,dy=my-lastY;dragDistance+=Math.abs(dx)+Math.abs(dy);dragWidget.yaw+=dx*.7f;dragWidget.pitch=Math.max(-80,Math.min(80,dragWidget.pitch+dy*.5f));lastX=mx;lastY=my;}
    }
    private void scrollToMouse(int my){scroll=(int)((my-44)/(float)viewHeight()*documentHeight-viewHeight()/2f);clampScroll();}
    @Override protected void mouseReleased(int mx,int my,int button){dragWidget=null;scrollDragging=false;super.mouseReleased(mx,my,button);}
    private void follow(String target) {
        if(target.startsWith("gui:")) {
            try {mc.displayGuiScreen(new net.createmod.ponder.client.guitutorial.GuiDemoScreen(
                net.createmod.ponder.client.guitutorial.GuiTutorials.resolve(target.substring(4)),()->mc.displayGuiScreen(this),false));}
            catch(RuntimeException failure){error=failure.getMessage();}
            return;
        }
        try {
            if(target.startsWith("quest:")||target.startsWith("ftbquests:")) {
                if(!net.createmod.ponder.compat.ftbquests.FTBQuestBridge.available())error=I18n.format("ponder.guide.quest_missing");
                else if(!net.createmod.ponder.compat.ftbquests.FTBQuestBridge.openQuest(target.substring(target.indexOf(':')+1)))error=I18n.format("ponder.guide.quest_unavailable");
                return;
            }
            if(target.startsWith("https://")||target.startsWith("http://")) {
                var text = new net.minecraft.util.text.TextComponentString(target);
                text.getStyle().setClickEvent(new net.minecraft.util.text.event.ClickEvent(net.minecraft.util.text.event.ClickEvent.Action.OPEN_URL,target));
                handleComponentClick(text);return;
            }
            if(target.startsWith("#")){String anchor=target.substring(1);for(Placed p:layout)if(p.block().kind()==Kind.HEADING&&slug(p.block().text()).equals(anchor)){scroll=p.y();clampScroll();return;}return;}
            if(target.startsWith("item:")){ResourceLocation item=new ResourceLocation(target.substring(5));if(Tutorials.exists(item))ScreenOpener.open(Tutorials.open(item));return;}
            String pageTarget=target.replaceAll("\\.md(?:#.*)?$","");
            if(!pageTarget.contains(":")){String path=currentTutorial().id().getPath();int slash=path.lastIndexOf('/');pageTarget=currentTutorial().id().getNamespace()+":"+(slash<0?"":path.substring(0,slash+1))+pageTarget;}
            ResourceLocation id=new ResourceLocation(pageTarget);
            for(int i=0;i<pages.size();i++)if(pages.get(i).id().equals(id)){pageIndex=i;scroll=0;initGui();return;}
            Tutorial page=PonderIndex.getTutorials().get(id);if(page!=null){ScreenOpener.open(new GuideScreen(component,id,List.of(page),false));return;}
            error=I18n.format("ponder.guide.link_missing",target);
        }catch(ReflectiveOperationException|LinkageError|RuntimeException failure){error=I18n.format("ponder.guide.link_missing",target);}
    }
    private static String slug(String text){return text.toLowerCase(Locale.ROOT).replace(' ','-').replaceAll("[^\\p{L}\\p{N}_-]","");}
    private void openAnimation() {
        Tutorial tutorial=currentTutorial();
        if(!tutorial.scenes().isEmpty()) {
            var scenes=tutorial.scenes().stream().filter(d->d.getComponent().equals(component)).map(Tutorials::compile).toList();
            if(!scenes.isEmpty()){mc.displayGuiScreen(new PonderUI(scenes).withGuide(() -> this).returningTo(back));return;}
        }
        if(tutorial.animation()!=null) {
            GuiScreen screen = Tutorials.open(component,tutorial.animation(),false);
            if(screen instanceof PonderUI ui) ui.withGuide(() -> this).returningTo(back);
            mc.displayGuiScreen(screen);
        }
    }
    @Override protected void keyTyped(char c,int key)throws IOException {
        if(key==Keyboard.KEY_ESCAPE&&back!=null){mc.displayGuiScreen(back);return;}
        if(key==Keyboard.KEY_F7){MarkdownEditor.open(currentTutorial(),this);return;}
        if(search.textboxKeyTyped(c,key)){rebuildNavigation();return;}
        if(key==Keyboard.KEY_NEXT){scroll+=viewHeight()-20;clampScroll();return;}
        if(key==Keyboard.KEY_PRIOR){scroll-=viewHeight()-20;clampScroll();return;}
        if(key==Keyboard.KEY_HOME){scroll=0;return;}
        if(key==Keyboard.KEY_END){scroll=documentHeight;clampScroll();return;}
        if(key==Keyboard.KEY_TAB&&currentTutorial().animation()!=null){openAnimation();return;}
        super.keyTyped(c,key);
    }
    @Override public void onGuiClosed(){images.close();videos.values().forEach(net.createmod.ponder.compat.media.MediaVideo::close);videos.clear();}
    @Override public boolean isEquivalentTo(net.createmod.catnip.gui.NavigatableSimiScreen other){return other instanceof GuideScreen guide&&component.equals(guide.component)&&!preview&&!guide.preview;}
    @Override protected String getBreadcrumbTitle(){return currentTutorial().title();}
}
