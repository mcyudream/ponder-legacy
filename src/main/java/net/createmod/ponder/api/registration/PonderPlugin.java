package net.createmod.ponder.api.registration;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.util.ResourceLocation;

public interface PonderPlugin {
    String getModId();

    default void registerTutorials(net.createmod.ponder.api.guide.TutorialRegistrationHelper helper) {
    }

    /** Read on the client; data values are safe to reference from a common plugin. */
    default void registerGuiTutorials(java.util.function.Consumer<net.createmod.ponder.api.gui.GuiTutorial> lessons) {
    }

    default void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
    }

    default void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
    }

    default void registerSharedText(SharedTextRegistrationHelper helper) {
    }

    default void onPonderLevelRestore(PonderLevel ponderLevel) {
    }

    default void indexExclusions(IndexExclusionHelper helper) {
    }
}
