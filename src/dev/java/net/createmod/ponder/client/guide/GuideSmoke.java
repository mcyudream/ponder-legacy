package net.createmod.ponder.client.guide;

import java.nio.file.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.*;
import net.createmod.ponder.api.guide.Tutorial;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.createmod.ponder.guide.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

/** Packaged-production verification of Markdown compilation, native rendering, editing and routing. */
public final class GuideSmoke {
    private final Minecraft mc;
    private final BiConsumer<Boolean,String> check;
    private final Consumer<String> screenshot;
    private final ResourceLocation id=new ResourceLocation("smoke:markdown_network"),component=new ResourceLocation("minecraft:clock");
    private int phase,wait;
    private GuideScreen guide;
    private MarkdownEditor editor;
    private net.createmod.ponder.client.projection.ProjectionSession projection;
    private net.minecraft.item.ItemStack originalStack;
    private final net.minecraft.util.math.BlockPos anchor=new net.minecraft.util.math.BlockPos(-3,70,-5);
    private net.createmod.ponder.compat.media.MediaVideo video;
    private int mediaWait;
    private java.awt.image.BufferedImage projectionFrame;
    private net.minecraft.tileentity.TileEntity projectionChest;
    public GuideSmoke(Minecraft mc,BiConsumer<Boolean,String> check,Consumer<String> screenshot){this.mc=mc;this.check=check;this.screenshot=screenshot;}
    public boolean advance()throws Exception{
        if(wait-->0)return false;
        switch(phase++){
            case 0:{
                net.createmod.ponder.client.projection.ProjectionOverlay.INSTANCE.reset(false);net.createmod.ponder.client.projection.ProjectionOverlay.INSTANCE.reset(true);
                StringBuilder md=new StringBuilder("---\nid: smoke:markdown_network\ntitle: Shared Markdown tutorial\nitem_ids: [minecraft:clock]\nstructure: ponder:empty\nstep_ticks: 40\n---\n# Shared Markdown tutorial\n\n");
                for(int i=1;i<=10;i++)md.append("## Step "+i+"\n\nShared narration "+i+". **Read** and animate from one source.\n\n```ponder\n[{\"op\":\"world.set_block\",\"x\":"+(i%3)+",\"y\":1,\"z\":0,\"state\":\"minecraft:stone\",\"particles\":false},{\"op\":\"world.show_section\",\"selection\":{\"type\":\"position\",\"pos\":["+(i%3)+",1,0]},\"direction\":\"down\"}]\n```\n\n");
                md.append("<GuideOnly>\n\n## Guide details\n\n| Input | Output |\n| - | - |\n| A | B |\n\n<ItemLink id=\"minecraft:stone\" />\n\n<RecipeFor item=\"minecraft:crafting_table\" />\n\n<GameScene>\n<Block id=\"minecraft:chest\" x={1} y={1} />\n<BlockAnnotation x={1} y={1}>Inventory</BlockAnnotation>\n</GameScene>\n\nFinal paragraph.\n\n</GuideOnly>\n");
                GuideSources.save(id,md.toString());PonderIndex.reload();
                check.accept(PonderIndex.getTutorials().get(id)!=null,"Markdown file registers document and animation atomically");
                check.accept(net.createmod.ponder.enums.PonderKeybinds.values().length==1,"only W is registered; G shortcut absent");
                Tutorials.setPreferGuide(false);check.accept(Tutorials.open(component) instanceof PonderUI,"W defaults to animation initially");
                Tutorials.setPreferGuide(true);check.accept(Tutorials.open(component) instanceof GuideScreen,"W honors saved guide preference");
                guide=new GuideScreen(component,id);mc.displayGuiScreen(guide);wait=5;break;
            }
            case 1:
                check.accept(guide.currentTutorial().id().equals(id),"static view keeps logical tutorial selection");
                check.accept(guide.getDocumentHeight()>guide.height,"long Markdown page reflows beyond viewport");screenshot.accept("markdown-guide.png");break;
            case 2:
                key(guide,Keyboard.KEY_END);check.accept(guide.getScroll()>0,"End key scrolls long guide to final content");wait=4;break;
            case 3:screenshot.accept("markdown-guide-bottom.png");break;
            case 4:
                key(guide,Keyboard.KEY_TAB);check.accept(mc.currentScreen instanceof PonderUI&&((PonderUI)mc.currentScreen).getActiveScene().getId().equals(id),"guide switches to matching animation");
                MarkdownEditor.open(PonderIndex.getTutorials().get(id),guide);editor=(MarkdownEditor)mc.currentScreen;wait=4;break;
            case 5:{
                String original=Files.readString(GuideSources.path(id));
                editor.document().selectAll();editor.document().insert("---\nunknown: false\n---\n# Invalid");
                Method save=MarkdownEditor.class.getDeclaredMethod("save");save.setAccessible(true);save.invoke(editor);
                check.accept(Files.readString(GuideSources.path(id)).equals(original),"invalid Markdown editor save preserves valid source");
                editor.document().undo();editor.document().select(editor.document().text().length(),false);editor.document().insert("\nAuthor note.\n");save.invoke(editor);
                check.accept(Files.readString(GuideSources.path(id)).contains("Author note"),"editor saves validated Markdown and reloads tutorials");
                check.accept(!editor.document().dirty(),"saved editor clears dirty state");screenshot.accept("markdown-editor.png");break;
            }
            case 6:
                key(editor,Keyboard.KEY_F5);check.accept(mc.currentScreen instanceof GuideScreen,"editor previews current Markdown document");wait=4;break;
            case 7:screenshot.accept("markdown-preview.png");break;
            case 8:{
                net.createmod.ponder.api.level.PonderLevel world=new net.createmod.ponder.api.level.PonderLevel(net.minecraft.util.math.BlockPos.ORIGIN,null);
                world.setBlockState(net.minecraft.util.math.BlockPos.ORIGIN,net.minecraft.init.Blocks.STONE.getDefaultState(),2);
                world.setBlockState(new net.minecraft.util.math.BlockPos(1,0,0),net.minecraft.init.Blocks.STONE.getDefaultState(),2);
                world.setBlockState(new net.minecraft.util.math.BlockPos(0,1,0),net.minecraft.init.Blocks.CHEST.getDefaultState(),2);world.createBackup();
                var scene=new net.createmod.ponder.foundation.PonderScene(world,new net.createmod.ponder.foundation.registration.PonderLocalization(),"smoke",component,List.of(),List.of());scene.setSceneTitle("smoke:projection","Construction target");
                originalStack=mc.player.inventory.mainInventory.get(0).copy();mc.player.inventory.mainInventory.set(0,new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.STONE,1));
                net.createmod.ponder.client.projection.ProjectionService.project(scene,null);net.createmod.ponder.client.projection.ProjectionService.confirmPlacement();projection=net.createmod.ponder.client.projection.ProjectionService.current();projection.anchor=anchor;projection.refresh();
                check.accept(mc.world.isAirBlock(anchor)&&mc.world.isAirBlock(anchor.up()),"projecting never places blocks in the player world");
                var stone=projection.rows.stream().filter(r->r.target().item().toString().equals("minecraft:stone")).findFirst().orElseThrow();
                check.accept(stone.target().count()==2&&stone.available()==1&&stone.missing()==1,"pinned materials count backpack stock and missing quantity");
                check.accept(projection.unknown.isEmpty(),"native structure material forms are resolved");
                check.accept(projection.ghost.getSceneElements().stream().anyMatch(e->e.isVisible()),"world projection contains a visible native render section");
                mc.player.setPositionAndRotation(anchor.getX()+4,anchor.getY()+3,anchor.getZ()+5,145,24);mc.player.capabilities.isFlying=true;wait=8;break;
            }
            case 9:screenshot.accept("projection-world-materials.png");break;
            case 10:
                projectionChest=projection.ghost.getWorld().getTileEntity(new net.minecraft.util.math.BlockPos(0,1,0));
                check.accept(projectionChest instanceof net.minecraft.tileentity.TileEntityChest,"projection fixture has a native chest tile renderer");
                projectionChest.invalidate();
                screenshot.accept("projection-world-without-chest.png");phase=30;break;
            case 30:
                projectionChest.validate();
                mc.player.inventory.mainInventory.set(0,new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.STONE,2));projection.refresh();
                check.accept(projection.rows.stream().filter(r->r.target().item().toString().equals("minecraft:stone")).findFirst().orElseThrow().missing()==0,"material goal refreshes when inventory changes");
                mc.world.setBlockState(anchor,net.minecraft.init.Blocks.STONE.getDefaultState(),2);projection.refresh();
                check.accept(projection.complete==1&&projection.rows.stream().filter(r->r.target().item().toString().equals("minecraft:stone")).findFirst().orElseThrow().target().count()==1,"already built blocks reduce remaining material goals");
                mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiInventory(mc.player));wait=5;phase=11;break;
            case 11:{
                screenshot.accept("projection-inventory-materials.png");
                var overlay=net.createmod.ponder.client.projection.ProjectionOverlay.INSTANCE;var bounds=overlay.bounds();
                check.accept(bounds.height()>0,"inventory material panel is laid out before the container and its tooltips");
                check.accept(overlay.mouseDown(mc.currentScreen,bounds.x()+20,bounds.y()+8,0),"inventory material header captures drag without clicking slots");
                overlay.drag(mc.currentScreen,mc.currentScreen.width-90,mc.currentScreen.height-75);wait=4;phase=21;break;
            }
            case 12:
                net.createmod.ponder.client.projection.ProjectionOverlay.INSTANCE.reset(false);net.createmod.ponder.client.projection.ProjectionOverlay.INSTANCE.reset(true);
                mc.world.setBlockToAir(anchor);projection.turns=1;projection.mirror=true;projection.rebuild();
                check.accept(projection.total==3&&projection.ghost.getWorld().getOccupiedPositions().size()==3,"rotated mirrored ghost retains all native structure cells");
                mc.displayGuiScreen(new net.createmod.ponder.client.projection.ProjectionScreen(projection,null));wait=5;break;
            case 13:screenshot.accept("projection-settings.png");break;
            case 14:{
                net.createmod.ponder.client.projection.ProjectionService.clear();mc.player.inventory.mainInventory.set(0,originalStack);
                java.nio.file.Path imagePath=GuideMediaSources.root().resolve("guide-smoke.png");java.nio.file.Files.createDirectories(imagePath.getParent());
                java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(160,90,java.awt.image.BufferedImage.TYPE_INT_RGB);
                var graphics=image.createGraphics();graphics.setColor(new java.awt.Color(0x23516b));graphics.fillRect(0,0,160,90);graphics.setColor(new java.awt.Color(0x9fe2cf));graphics.fillRect(20,20,120,50);graphics.dispose();javax.imageio.ImageIO.write(image,"png",imagePath.toFile());
                var page=MarkdownTutorialCompiler.compile(new ResourceLocation("smoke:media"),"# Media and quest links\n\n<Image src=\"local:guide-smoke.png\" height={80} />\n\n<Video src=\"local:guide-smoke.mp4\" title=\"Video example\" />\n\n<QuestLink id=\"1234abcd\" title=\"Quest example\" />\n","media.md");
                guide=new GuideScreen(component,page.id(),List.of(page),true);mc.displayGuiScreen(guide);wait=15;break;
            }
            case 15:screenshot.accept("markdown-media-image.png");break;
            case 16:
                if(net.minecraftforge.fml.common.Loader.isModLoaded("media_api")){video=new net.createmod.ponder.compat.media.MediaVideo("local:guide-smoke.mp4");video.toggle();wait=10;}
                else{var missing=new net.createmod.ponder.compat.media.MediaVideo("local:guide-smoke.mp4");check.accept(missing.texture()<0&&!missing.status().isEmpty(),"missing optional Media API displays a readable fallback");missing.close();}
                break;
            case 17:
                if(video!=null&&video.texture()<=0){if(++mediaWait>80)throw new IllegalStateException("Optional video did not decode: "+video.status());phase--;wait=5;return false;}
                if(video!=null){check.accept(video.width()>0&&video.height()>0,"optional Media API decodes local guide video into a real texture");check.accept(video.seekable()&&video.duration()>0,"optional video supports duration and seek controls");video.seek(video.duration()/2);video.close();}
                key(guide,Keyboard.KEY_END);wait=5;break;
            case 18:screenshot.accept("markdown-media-video-quest.png");break;
            case 19:
                if(!net.createmod.ponder.compat.ftbquests.FTBQuestBridge.available()){
                    Method follow=GuideScreen.class.getDeclaredMethod("follow",String.class);follow.setAccessible(true);follow.invoke(guide,"quest:1234abcd");
                    check.accept(mc.currentScreen==guide,"missing optional FTB Quests leaves task link safely in the guide");
                }else{
                    Class<?> fileClass=Class.forName("com.feed_the_beast.ftbquests.client.ClientQuestFile"),questFile=Class.forName("com.feed_the_beast.ftbquests.quest.QuestFile"),chapterClass=Class.forName("com.feed_the_beast.ftbquests.quest.Chapter"),questClass=Class.forName("com.feed_the_beast.ftbquests.quest.Quest");
                    Object file=fileClass.getField("INSTANCE").get(null);if(file==null)throw new IllegalStateException("FTBQ client file not synchronized");
                    Object chapter=chapterClass.getConstructor(questFile).newInstance(file);chapterClass.getField("id").setInt(chapter,0x1234abcc);chapterClass.getField("title").set(chapter,"Ponder integration fixture");chapterClass.getMethod("onCreated").invoke(chapter);
                    Object quest=questClass.getConstructor(chapterClass).newInstance(chapter);questClass.getField("id").setInt(quest,0x1234abcd);questClass.getField("title").set(quest,"Linked tutorial task");questClass.getMethod("onCreated").invoke(quest);questFile.getMethod("refreshIDMap").invoke(file);
                    check.accept(net.createmod.ponder.compat.ftbquests.FTBQuestBridge.openQuest("1234abcd"),"optional FTB Quests bridge opens the specified quest through its native link handler");wait=5;
                }
                break;
            case 20:if(net.createmod.ponder.compat.ftbquests.FTBQuestBridge.available())screenshot.accept("markdown-ftb-quest-link.png");phase=24;break;
            case 21:{
                var overlay=net.createmod.ponder.client.projection.ProjectionOverlay.INSTANCE;overlay.release(mc.currentScreen);
                var config=(net.createmod.ponder.config.CClient)net.createmod.catnip.config.ConfigRegistry.get(net.createmod.ponder.Ponder.MOD_ID,net.createmod.catnip.config.ConfigType.CLIENT);
                check.accept(config.materialInventoryX.get()>=0&&config.materialHudX.get()==-1,"inventory drag saves only the separate inventory position");
                config.reload();check.accept(config.materialInventoryX.get()>=0,"dragged material position survives a disk config reload");
                screenshot.accept("projection-inventory-dragged.png");
                mc.displayGuiScreen(new net.createmod.ponder.client.projection.ProjectionLayoutScreen(null));wait=4;break;
            }
            case 22:{
                var overlay=net.createmod.ponder.client.projection.ProjectionOverlay.INSTANCE;var bounds=overlay.bounds();
                check.accept(overlay.mouseDown(mc.currentScreen,bounds.x()+20,bounds.y()+8,0),"HUD layout editor exposes a draggable panel header");overlay.drag(mc.currentScreen,28,80);wait=4;break;
            }
            case 23:{
                var overlay=net.createmod.ponder.client.projection.ProjectionOverlay.INSTANCE;overlay.release(mc.currentScreen);
                var config=(net.createmod.ponder.config.CClient)net.createmod.catnip.config.ConfigRegistry.get(net.createmod.ponder.Ponder.MOD_ID,net.createmod.catnip.config.ConfigType.CLIENT);
                check.accept(config.materialHudX.get()>=0&&config.materialInventoryX.get()>=0,"HUD and inventory placements persist independently");
                check.accept(overlay.bounds().x()<40,"HUD panel actually follows the drag to the opposite side of the screen");
                screenshot.accept("projection-hud-dragged.png");wait=4;phase=12;break;
            }
            default:Tutorials.setPreferGuide(false);return true;
        }
        return false;
    }
    public void verifyFrame(String name) {
        if(!name.equals("projection-world-materials.png")&&!name.equals("projection-world-without-chest.png"))return;
        var image=net.minecraft.util.ScreenShotHelper.createScreenshot(mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
        if(name.equals("projection-world-materials.png")){projectionFrame=image;return;}
        check.accept(projectionFrame!=null,"projection reference frame was captured before disabling the chest");
        int changed=0;
        for(int y=image.getHeight()/5;y<image.getHeight()*4/5;y++)for(int x=image.getWidth()/5;x<image.getWidth()*4/5;x++){
            int before=projectionFrame.getRGB(x,y),after=image.getRGB(x,y);
            int delta=Math.max(Math.abs((before>>16&255)-(after>>16&255)),
                Math.max(Math.abs((before>>8&255)-(after>>8&255)),Math.abs((before&255)-(after&255))));
            if(delta>20)changed++;
        }
        check.accept(changed>100,"paired framebuffers contain the native projected chest model ("+changed+" changed pixels)");
        projectionFrame=null;
    }
    private static void key(GuiScreen screen,int key)throws Exception{
        Method method=net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(screen.getClass(),"keyTyped","func_73869_a",char.class,int.class);method.setAccessible(true);method.invoke(screen,'\0',key);
    }
}
