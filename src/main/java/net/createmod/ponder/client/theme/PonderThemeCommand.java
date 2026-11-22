package net.createmod.ponder.client.theme;

import java.util.List;
import net.createmod.ponder.api.theme.PonderThemeLoader;
import net.createmod.ponder.client.DeferredClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.command.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

public final class PonderThemeCommand extends CommandBase {
    @Override public String getName() { return "pondertheme"; }
    @Override public String getUsage(ICommandSender sender) { return "/pondertheme [<namespace:id>|auto|reload|list]"; }
    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        try {
            if (args.length == 0) { DeferredClientScreen.open(new PonderThemeScreen(null)); return; }
            if (args.length != 1) throw new WrongUsageException(getUsage(sender));
            switch (args[0]) {
                case "reload" -> {
                    PonderThemes.reload(Minecraft.getMinecraft().getResourceManager());
                    sender.sendMessage(new TextComponentTranslation("ponder.theme.reloaded", PonderThemes.all().size(), PonderThemes.errors().size()));
                }
                case "list" -> sender.sendMessage(new net.minecraft.util.text.TextComponentString(
                    String.join(", ", PonderThemes.all().stream().map(theme -> theme.id().toString()).toList())));
                case "auto" -> PonderThemes.selectAutomatic();
                default -> PonderThemes.select(PonderThemeLoader.id(args[0]));
            }
        } catch (IllegalArgumentException failure) { throw new CommandException(failure.getMessage()); }
    }

    @Override public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length != 1) return List.of();
        var values = new java.util.ArrayList<>(List.of("auto", "reload", "list"));
        PonderThemes.all().forEach(theme -> values.add(theme.id().toString()));
        return getListOfStringsMatchingLastWord(args, values);
    }
}
