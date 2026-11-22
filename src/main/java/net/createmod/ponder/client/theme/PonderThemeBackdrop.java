package net.createmod.ponder.client.theme;

import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.foundation.ui.AbstractPonderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Native 1.12 post processing, applied before any Ponder content is drawn. */
@SideOnly(Side.CLIENT)
public final class PonderThemeBackdrop {
    private static ShaderGroup blur;
    private static int width, height;
    private static boolean failed;

    private PonderThemeBackdrop() {}

    public static boolean active() {
        return Minecraft.getMinecraft().currentScreen instanceof AbstractPonderScreen && PonderThemes.modern();
    }

    public static void tick() {
        if (blur != null && !active()) invalidate();
    }

    public static boolean blur() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!active() || mc.world == null || failed || !OpenGlHelper.shadersSupported
            || !OpenGlHelper.isFramebufferEnabled()) return false;
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            try {
                if (blur == null) {
                    blur = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(),
                        new ResourceLocation("ponder:shaders/post/theme_blur.json"));
                    width = height = 0;
                }
                if (width != mc.displayWidth || height != mc.displayHeight) {
                    width = mc.displayWidth; height = mc.displayHeight;
                    blur.createBindFramebuffers(width, height);
                }
                blur.render(0);
                return true;
            } catch (Exception failure) {
                invalidate();
                failed = true;
                Ponder.LOGGER.warn("Ponder theme blur unavailable; using a solid backdrop", failure);
                return false;
            } finally {
                mc.getFramebuffer().bindFramebuffer(true);
            }
        }
    }

    public static void invalidate() {
        if (blur != null) { blur.deleteShaderGroup(); blur = null; }
        width = height = 0;
        failed = false;
    }
}
