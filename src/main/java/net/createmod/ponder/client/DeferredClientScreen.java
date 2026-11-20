package net.createmod.ponder.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class DeferredClientScreen {
    private static Request pending;

    private DeferredClientScreen() {}

    public static void open(GuiScreen screen) {
        Minecraft minecraft = Minecraft.getMinecraft();
        pending = new Request(screen, minecraft.currentScreen, minecraft.world);
    }

    public static void tick() {
        Request request = pending;
        pending = null;
        if (request == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == request.world
            && (minecraft.currentScreen == null || minecraft.currentScreen == request.source))
            minecraft.displayGuiScreen(request.screen);
    }

    private record Request(GuiScreen screen, GuiScreen source, WorldClient world) {}
}
