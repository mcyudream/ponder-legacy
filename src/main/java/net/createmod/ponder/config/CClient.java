package net.createmod.ponder.config;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.ponder.enums.PonderConfig;

public final class CClient extends ConfigBase {
    public enum TutorialPresentation { ANIMATION, GUIDE }
    public final ConfigEnum<TutorialPresentation> preferredPresentation = e(TutorialPresentation.ANIMATION,
        "preferredPresentation", "Default tutorial presentation: ANIMATION or GUIDE.");
    public final ConfigString uiTheme = s("auto", "uiTheme",
        "UI theme ID, or auto for the pack's JSON/ZenScript default (otherwise ponder:classic).");
    public final ConfigBool comfyReading = b(false, "comfyReading",
        "Slow down a Ponder scene while text is visible.");
    public final ConfigBool editingMode = b(false, "editingMode",
        "Reload registrations and display scene authoring information.");
    public final ConfigGroup materialTargets = group(1, "materialTargets", "Pinned material panel positions. -1 selects automatic placement.");
    public final ConfigInt materialHudX = i(-1, -1, 10000, "hudX", "Horizontal position, relative to the available screen width.");
    public final ConfigInt materialHudY = i(-1, -1, 10000, "hudY", "Vertical position, relative to the available screen height.");
    public final ConfigInt materialInventoryX = i(-1, -1, 10000, "inventoryX", "Separate horizontal position for inventory screens.");
    public final ConfigInt materialInventoryY = i(-1, -1, 10000, "inventoryY", "Separate vertical position for inventory screens.");
    public final ConfigGroup placementAssist = group(1, "placementAssist",
        "Settings for assisted-placement indicators.");
    public final ConfigEnum<PlacementIndicatorSetting> placementIndicator =
        e(PlacementIndicatorSetting.TEXTURE, "indicatorType",
            "TEXTURE, TRIANGLE, or NONE.");
    public final ConfigFloat indicatorScale = f(1f, 0f, 8f, "indicatorScale",
        "Size multiplier for the assisted-placement indicator.");

    public enum PlacementIndicatorSetting {
        TEXTURE, TRIANGLE, NONE
    }

    @Override public String getName() { return "client"; }

    @Override public void onLoad() {
        super.onLoad();
        synchronizeFacade();
    }

    @Override public void onReload() {
        super.onReload();
        synchronizeFacade();
    }

    private void synchronizeFacade() {
        PonderConfig.client().setComfyReading(comfyReading.get());
        PonderConfig.client().setTheme(uiTheme.get());
        PonderConfig.client().setEditingMode(editingMode.get());
        PonderConfig.client().setIndicatorScale(indicatorScale.getF());
        PonderConfig.client().setPlacementIndicator(
            PonderConfig.PlacementIndicatorSetting.valueOf(placementIndicator.get().name()));
    }
}
