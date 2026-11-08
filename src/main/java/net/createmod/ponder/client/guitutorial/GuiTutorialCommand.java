package net.createmod.ponder.client.guitutorial;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.command.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public final class GuiTutorialCommand extends CommandBase {
    @Override public String getName(){return "pondergui";}
    @Override public String getUsage(ICommandSender sender){return "/pondergui [reload|demo <namespace:id>|edit <namespace:id>]";}
    @Override public int getRequiredPermissionLevel(){return 0;}
    @Override public void execute(MinecraftServer server,ICommandSender sender,String[] args)throws CommandException{
        var mc=Minecraft.getMinecraft();
        try{
            if(args.length==0){mc.displayGuiScreen(new GuiTutorialLibraryScreen(null));return;}
            if(args.length==1&&args[0].equals("reload")){GuiTutorials.reload();sender.sendMessage(new TextComponentString(GuiTutorialDrawing.tr("reloaded",GuiTutorials.all().size(),GuiTutorials.errors().size())));return;}
            if(args.length==2&&(args[0].equals("demo")||args[0].equals("edit"))){
                var lesson=GuiTutorials.resolve(args[1]);
                if(args[0].equals("edit"))mc.displayGuiScreen(new GuiTutorialEditorScreen(lesson,null));
                else mc.displayGuiScreen(new GuiDemoScreen(lesson,()->mc.displayGuiScreen(null),false));return;
            }
            throw new WrongUsageException(getUsage(sender));
        }catch(IllegalArgumentException e){throw new CommandException(e.getMessage());}
    }
    @Override public List<String> getTabCompletions(MinecraftServer server,ICommandSender sender,String[] args,net.minecraft.util.math.BlockPos pos){
        if(args.length==1)return getListOfStringsMatchingLastWord(args,"reload","demo","edit");
        if(args.length==2)return getListOfStringsMatchingLastWord(args,GuiTutorials.all().stream().map(t->t.id().toString()).toList());return List.of();
    }
}
