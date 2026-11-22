package net.createmod.ponder.foundation.element;

import java.util.List;
import java.util.function.Supplier;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.render.SceneProjection;
import net.createmod.ponder.render.SceneProjection.ProjectedPoint;
import net.createmod.ponder.client.theme.PonderThemes;
import net.createmod.ponder.client.theme.PonderThemeRenderer;
import net.createmod.ponder.api.theme.PonderTheme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class TextWindowElement extends PonderElementBase implements TextElementBuilder,PonderOverlayElement {
    private final PonderScene scene;
    private final int duration;
    private PonderPalette color=PonderPalette.WHITE;
    private Vec3d target;
    private boolean independent;
    private int independentY;
    private boolean nearTarget;
    private Supplier<String> text=new Supplier<String>(){@Override public String get(){return "";}};
    public TextWindowElement(PonderScene scene,int duration){this.scene=scene;this.duration=Math.max(1,duration);}
    @Override public TextElementBuilder colored(PonderPalette value){if(value!=null)color=value;return this;}
    @Override public TextElementBuilder pointAt(Vec3d value){target=value;return this;}
    @Override public TextElementBuilder independent(int y){independent=true;independentY=y;return this;}
    @Override public TextElementBuilder text(String value){text=scene.registerText(value);scene.recordNarration(this,duration,text);return this;}
    @Override public TextElementBuilder text(String value,Object... params){text=scene.registerText(value,params);scene.recordNarration(this,duration,text);return this;}
    @Override public TextElementBuilder sharedText(final ResourceLocation key){text=new Supplier<String>(){@Override public String get(){return PonderIndex.getLangAccess().getShared(key);}};scene.recordNarration(this,duration,text);return this;}
    @Override public TextElementBuilder sharedText(final ResourceLocation key,final Object... params){text=new Supplier<String>(){@Override public String get(){return PonderIndex.getLangAccess().getShared(key,params);}};scene.recordNarration(this,duration,text);return this;}
    @Override public TextElementBuilder sharedText(String key){return sharedText(new ResourceLocation(scene.getNamespace(),key));}
    @Override public TextElementBuilder sharedText(String key,Object... params){return sharedText(new ResourceLocation(scene.getNamespace(),key),params);}
    @Override public TextElementBuilder placeNearTarget(){nearTarget=true;return this;}
    @Override public TextElementBuilder attachKeyFrame(){scene.declareKeyframe(scene.getBuildCursor());return this;}
    @Override public void render(PonderScene scene,int mouseX,int mouseY,float partialTicks){
        if (PonderThemes.modern()) { renderModern(); return; }
        FontRenderer font=Minecraft.getMinecraft().fontRenderer;var viewport=SceneProjection.viewport();int width=Math.min(180,Math.max(20,viewport.width()-16));
        int x=viewport.x()+(viewport.width()-width)/2;
        int y=independent?independentY:18;
        if(target!=null){ProjectedPoint projected=SceneProjection.project(target);if(projected.visible){x=(int)projected.x+(nearTarget?14:-width/2);y=(int)projected.y-(nearTarget?10:48);}}
        List<String> lines=font.listFormattedStringToWidth(text.get(),width-12);int height=lines.size()*10+10;
        x=Math.max(viewport.x()+4,Math.min(x,viewport.x()+viewport.width()-width-4));
        y=Math.max(viewport.y()+6,Math.min(y,viewport.y()+viewport.height()-height-4));
        Gui.drawRect(x,y,x+width,y+height,PonderThemes.color(Color.SPEECH_BACKGROUND));Gui.drawRect(x,y,x+3,y+height,0xff000000|color.getColor());
        int lineY=y+5;for(String line:lines){font.drawString(line,x+8,lineY,PonderThemes.color(Color.SPEECH_TEXT));lineY+=10;}
    }

    private void renderModern() {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        var viewport = SceneProjection.viewport();
        ProjectedPoint point = target == null ? null : SceneProjection.project(target);
        boolean hasTarget = point != null && point.visible;
        float targetY = hasTarget ? point.y : viewport.y() + (viewport.height() - 200) / 2f
            + (independent ? independentY : 100) - 8;
        float yDiff = (viewport.y() + viewport.height() / 2f - targetY - 10) / 100f;
        float targetX = viewport.x() + viewport.width() * (.75f - .125f * yDiff * yDiff);
        if (hasTarget && nearTarget) targetX = Math.min(targetX, point.x + 50);
        targetX = Math.max(viewport.x() + 14, Math.min(targetX, viewport.x() + viewport.width() - 24));
        int boxX = (int) targetX - 10;
        int wrapWidth = Math.max(20, Math.min(180, viewport.x() + viewport.width() - boxX - 4));
        List<String> lines = font.listFormattedStringToWidth(text.get(), wrapWidth);
        int boxWidth = Math.max(1, lines.stream().mapToInt(font::getStringWidth).max().orElse(1));
        int textHeight = Math.max(1, lines.size()) * 9;
        int boxY = Math.max(viewport.y() + 4, Math.min((int) targetY + 3,
            viewport.y() + viewport.height() - textHeight - 4));
        PonderThemeRenderer.modernBox(boxX, boxY, boxWidth, textHeight - 1,
            PonderThemes.color(Color.SPEECH_BACKGROUND), PonderThemes.color(Color.SPEECH_BORDER),
            PonderThemes.color(Color.SPEECH_BORDER_BOTTOM));
        int palette = color.getColor(), warm = PonderThemes.color(Color.SPEECH_TEXT);
        int textColor = 0xff000000
            | ((palette >> 16 & 255) + (warm >> 16 & 255)) / 2 << 16
            | ((palette >> 8 & 255) + (warm >> 8 & 255)) / 2 << 8
            | ((palette & 255) + (warm & 255)) / 2;
        if (hasTarget) {
            int sourceX = Math.max(viewport.x(), Math.min((int) point.x, viewport.x() + viewport.width()));
            int lineY = boxY - 3, endX = boxX + 10;
            if (sourceX != endX) {
                Gui.drawRect(sourceX, lineY, endX, lineY + 1, textColor);
                Gui.drawRect(sourceX, lineY + 1, endX, lineY + 2, 0xff494949);
            }
        }
        for (int line = 0; line < lines.size(); line++)
            font.drawString(lines.get(line), boxX, boxY + line * 9, textColor);
    }

    public int getDuration(){return duration;}
}
