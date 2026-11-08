package net.createmod.ponder.mixin.revoui;

import net.createmod.ponder.foundation.ui.AbstractPonderScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ponder draws its own backgrounds; world placement and transparent tools also need an unblurred view. */
@Pseudo
@Mixin(targets = "neofontrender.addons.effects.ScreenEffectsRenderer", remap = false)
public abstract class RevoScreenEffectsMixin {
    @Shadow private void snapHidden() { throw new AssertionError(); }
    @Shadow private void discardShader() { throw new AssertionError(); }

    @Inject(method = "onGuiOpen", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void ponder$useOwnScreenBackground(GuiOpenEvent event, CallbackInfo callback) {
        GuiScreen screen = event.getGui();
        if (screen instanceof AbstractPonderScreen || screen != null && (screen.getClass().getName().startsWith("net.createmod.ponder.")
            || net.createmod.ponder.client.guitutorial.GuiTutorialEvents.handles(screen))) {
            // Snap both transitions, so closing a Ponder page cannot start a stale blur fade.
            snapHidden(); discardShader(); callback.cancel();
        }
    }
}
