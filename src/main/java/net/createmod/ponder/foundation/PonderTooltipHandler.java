package net.createmod.ponder.foundation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.catnip.platform.CatnipClientServices;
import net.createmod.ponder.api.subject.PonderSubjectResolvers;
import net.createmod.ponder.api.subject.ResolvedPonderSubject;
import net.createmod.ponder.enums.PonderKeybinds;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.createmod.ponder.compat.revoui.RevoTooltipLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;

public final class PonderTooltipHandler {
    // Reserve a text row; the post-text event draws a bar at the tooltip's actual wrapped width.
    private static final String PROGRESS_ROW = "\u00a7r\u00a7r ";
    private static final List<Consumer<ItemStack>> CALLBACKS = new ArrayList<Consumer<ItemStack>>();
    private static ItemStack tracked = ItemStack.EMPTY;
    private static ResourceLocation trackedComponent;
    private static long lastSeen;
    private static float progress;

    private PonderTooltipHandler() {
    }

    public static void onTooltip(ItemTooltipEvent event) {
        if (net.createmod.ponder.client.guitutorial.GuiTutorialEvents.handles(Minecraft.getMinecraft().currentScreen)) return;
        if (event.getEntityPlayer() == null || event.getItemStack().isEmpty()) return;

        ResolvedPonderSubject subject = PonderSubjectResolvers.resolve(event.getItemStack());
        if (!subject.isHandled()) return;
        ResourceLocation component = subject.getComponent();
        boolean scenesExist = net.createmod.ponder.client.guide.Tutorials.exists(component);
        if (!scenesExist) {
            if (shouldShowMissingSceneMessage(subject, false))
                event.getToolTip().add(TextFormatting.GRAY + I18n.format("ponder.ui.no_scenes_configured"));
            return;
        }

        if (!isTrackedSubject(event.getItemStack(), component)) {
            tracked = event.getItemStack().copy();
            trackedComponent = component;
            progress = 0;
        }
        lastSeen = System.currentTimeMillis();
        for (Consumer<ItemStack> callback : CALLBACKS) callback.accept(tracked.copy());

        String keyName = PonderKeybinds.PONDER.message().getUnformattedText();
        event.getToolTip().add(TextFormatting.GRAY + I18n.format("ponder.ui.hold_to_ponder", keyName));
        event.getToolTip().add(PROGRESS_ROW);
    }

    public static void renderProgress(RenderTooltipEvent.PostText event) {
        RevoTooltipLayout.Bounds modern = RevoTooltipLayout.consume(event);
        int row = progressRow(event.getLines());
        if (row < 0 || tracked.isEmpty()) return;
        int x = modern == null ? event.getX() : modern.x;
        // PostText reports the tooltip's top, including its title gap and any wrapped lines.
        int y = modern == null ? event.getY() + 2 + row * 10 + 2 : modern.y;
        int width = modern == null ? Math.max(1, event.getWidth()) : modern.width;
        int height = modern == null ? 6 : modern.height;
        if (width < 3 || height < 3) return;
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            Gui.drawRect(x, y, x + width, y + height, 0xff526373);
            Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xff1c2630);
            int fill = progressPixels(progress, Math.max(0, width - 2));
            if (fill > 0) {
                Gui.drawRect(x + 1, y + 1, x + 1 + fill, y + height - 1, 0xff9fd6d2);
                Gui.drawRect(x + 1, y + 1, x + 1 + fill, y + 2, 0xffc6eee5);
            }
        }
    }

    public static int progressRow(List<String> lines) {
        // GuiScreen prepends its gray text style, so match the reserved suffix.
        if (lines != null) for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line != null && line.endsWith(PROGRESS_ROW)) return i;
        }
        return -1;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (net.createmod.ponder.client.guitutorial.GuiTutorialEvents.handles(minecraft.currentScreen)) {
            progress = 0; clearTrackedSubject(); return;
        }
        boolean fresh = !tracked.isEmpty() && System.currentTimeMillis() - lastSeen < 180;
        boolean held = fresh && minecraft.currentScreen != null
            && CatnipClientServices.CLIENT_HOOKS.isKeyPressed(PonderKeybinds.PONDER.getMapping());
        progress = advanceProgress(progress, held);

        if (progress >= 1) {
            ResourceLocation opening = trackedComponent;
            progress = 0;
            clearTrackedSubject();
            try {
                if (opening != null) ScreenOpener.open(net.createmod.ponder.client.guide.Tutorials.open(opening));
            } catch (RuntimeException ignored) {
            }
        }
        if (!fresh && progress == 0) clearTrackedSubject();
    }

    static boolean shouldShowMissingSceneMessage(ResolvedPonderSubject subject, boolean scenesExist) {
        return subject != null && subject.isHandled() && !subject.isDefaultResolver() && !scenesExist;
    }

    static boolean isSameSubject(ItemStack first, ResourceLocation firstComponent,
                                 ItemStack second, ResourceLocation secondComponent) {
        return first != null && second != null && !first.isEmpty() && !second.isEmpty()
            && firstComponent != null && firstComponent.equals(secondComponent)
            && first.getItem() == second.getItem()
            && first.getMetadata() == second.getMetadata()
            && ItemStack.areItemStackTagsEqual(first, second);
    }

    private static boolean isTrackedSubject(ItemStack stack, ResourceLocation component) {
        return isSameSubject(tracked, trackedComponent, stack, component);
    }

    private static void clearTrackedSubject() {
        tracked = ItemStack.EMPTY;
        trackedComponent = null;
    }

    static float advanceProgress(float current, boolean held) {
        return Math.max(0, Math.min(1, current + (held ? .09f : -.12f)));
    }

    static int progressPixels(float value, int width) {
        return Math.round(Math.max(0, Math.min(1, value)) * Math.max(0, width));
    }

    public static float getProgress() {
        return progress;
    }

    public static synchronized void registerHoveredPonderStackCallback(Consumer<ItemStack> callback) {
        if (callback != null) CALLBACKS.add(callback);
    }

    public static synchronized void removeHoveredPonderStackCallback(Consumer<ItemStack> callback) {
        CALLBACKS.remove(callback);
    }
}
