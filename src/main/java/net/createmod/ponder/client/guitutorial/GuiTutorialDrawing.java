package net.createmod.ponder.client.guitutorial;

import java.util.List;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.api.gui.GuiTutorial;
import net.createmod.ponder.api.gui.GuiTutorial.*;
import net.createmod.ponder.gui.GuiTutorialSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;

public final class GuiTutorialDrawing {
    public static final int ACCENT=0xff9fded4;
    private GuiTutorialDrawing(){}
    public static String text(String value){return value.startsWith("@")?I18n.format(value.substring(1)):value;}
    public static String tr(String key,Object... args){return I18n.format("ponder.gui."+key,args);}
    public static void box(Rect r,int color){
        if(r==null)return;Gui.drawRect(r.x()-2,r.y()-2,r.x()+r.width()+2,r.y(),color);Gui.drawRect(r.x()-2,r.y()+r.height(),r.x()+r.width()+2,r.y()+r.height()+2,color);
        Gui.drawRect(r.x()-2,r.y(),r.x(),r.y()+r.height(),color);Gui.drawRect(r.x()+r.width(),r.y(),r.x()+r.width()+2,r.y()+r.height(),color);
    }
    public static int body(String text,int x,int y,int width,int height,int scroll){
        var font=Minecraft.getMinecraft().fontRenderer;List<String> lines=font.listFormattedStringToWidth(text(text),Math.max(16,width));
        boolean footer=lines.size()>Math.max(1,height/12)&&height>=24;
        int count=Math.max(1,(height-(footer?10:0))/12),start=Math.max(0,Math.min(scroll,Math.max(0,lines.size()-count)));
        for(int i=start;i<Math.min(lines.size(),start+count);i++)font.drawString(lines.get(i),x,y+(i-start)*12,0xffd7e3e8);
        if(footer)font.drawString((start+1)+"–"+Math.min(lines.size(),start+count)+" / "+lines.size(),x,y+height-9,0xff819ba8);
        return Math.max(0,lines.size()-count);
    }
    public static Rect scaled(Rect rect,int x,int y,float scale){return new Rect(x+Math.round(rect.x()*scale),y+Math.round(rect.y()*scale),Math.max(1,Math.round(rect.width()*scale)),Math.max(1,Math.round(rect.height()*scale)));}
    /** Purely draws a declarative replica. No GuiContainer instance, native click, or packet is used. */
    public static void demo(GuiTutorialSession session,int x,int y,float scale){
        Minecraft mc=Minecraft.getMinecraft();GuiTutorial t=session.tutorial();
        try(GlStateGuard ignored=GlStateGuard.capture()){
            GlStateManager.disableDepth();GlStateManager.disableLighting();GlStateManager.color(1,1,1,1);
            GlStateManager.pushMatrix();GlStateManager.translate(x,y,0);GlStateManager.scale(scale,scale,1);
            if(t.texture().isEmpty()){
                Gui.drawRect(0,0,t.width(),t.height(),0xffc6c6c6);Gui.drawRect(2,2,t.width()-2,t.height()-2,0xff909090);
            }else{
                mc.getTextureManager().bindTexture(new ResourceLocation(t.texture()));
                Gui.drawModalRectWithCustomSizedTexture(0,0,0,0,t.width(),t.height(),256,256);
            }
            for(var entry:t.targets().entrySet()){
                Target target=entry.getValue();Rect r=target.bounds();
                if(target.type()==TargetType.SLOT){
                    if(t.texture().isEmpty()){Gui.drawRect(r.x()-1,r.y()-1,r.x()+17,r.y()+17,0xff373737);Gui.drawRect(r.x(),r.y(),r.x()+16,r.y()+16,0xff8b8b8b);}
                    var stack=session.demoSlot(entry.getKey());
                    if(!stack.item().isEmpty()&&stack.count()>0){
                        Item item=Item.REGISTRY.getObject(new ResourceLocation(stack.item()));
                        if(item!=null){ItemStack render=new ItemStack(item,stack.count());RenderHelper.enableGUIStandardItemLighting();mc.getRenderItem().renderItemAndEffectIntoGUI(render,r.x(),r.y());mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer,render,r.x(),r.y(),null);RenderHelper.disableStandardItemLighting();}
                    }
                }else if(target.type()==TargetType.BUTTON){
                    Gui.drawRect(r.x(),r.y(),r.x()+r.width(),r.y()+r.height(),0xff526571);
                    mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(text(target.label()),r.width()-4),r.x()+2,r.y()+Math.max(1,(r.height()-8)/2),0xffeef5f8);
                }else{
                    String value=session.demoValue(entry.getKey());
                    if(!value.isEmpty()){
                        Gui.drawRect(r.x(),r.y(),r.x()+r.width(),r.y()+r.height(),0xff17252c);
                        mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(text(value),Math.max(1,r.width()-4)),r.x()+2,r.y()+Math.max(1,(r.height()-8)/2),0xffeef5f8);
                    }
                }
            }
            if(t.screen().equals("net.minecraft.client.gui.inventory.GuiFurnace")){
                float burn=0;try{burn=Float.parseFloat(session.demoValue("inventory_field.0"));}catch(NumberFormatException ignored2){}
                if(burn>0&&!t.texture().isEmpty()){mc.getTextureManager().bindTexture(new ResourceLocation(t.texture()));GlStateManager.color(1,1,1,1);Gui.drawModalRectWithCustomSizedTexture(56,36,176,0,14,14,256,256);}
                float progress=0;try{progress=Float.parseFloat(session.demoValue("inventory_field.2"))/200;}catch(NumberFormatException ignored2){}
                if(progress>0){mc.getTextureManager().bindTexture(new ResourceLocation(t.texture()));GlStateManager.color(1,1,1,1);Gui.drawModalRectWithCustomSizedTexture(79,34,176,14,Math.min(24,(int)(24*progress)),16,256,256);}
            }
            GlStateManager.popMatrix();
            Rect target=t.targets().get(session.step().target()).bounds();Rect mark=scaled(target,x,y,scale);box(mark,ACCENT);
            Rect previous=t.targets().get(t.steps().get(Math.max(0,session.stepIndex()-1)).target()).bounds();Rect from=scaled(previous,x,y,scale);
            float travel=Math.min(1,session.tick()/20f);travel=travel*travel*(3-2*travel);
            int cx=Math.round(from.x()+from.width()/2f+(mark.x()+mark.width()/2f-from.x()-from.width()/2f)*travel);
            int cy=Math.round(from.y()+from.height()/2f+(mark.y()+mark.height()/2f-from.y()-from.height()/2f)*travel);
            Gui.drawRect(cx,cy,cx+2,cy+11,0xffffffff);Gui.drawRect(cx+2,cy+2,cx+4,cy+9,0xffffffff);Gui.drawRect(cx+4,cy+4,cx+6,cy+7,0xffffffff);
            if(session.tick()>=20&&session.tick()<28){box(new Rect(cx-5,cy-5,10,10),0xffe9cf7f);}
        }
    }
}
