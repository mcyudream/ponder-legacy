package net.createmod.ponder.server;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Verifies the installation jar on a physical dedicated server. */
@Mod(modid = "ponder_server_harness", name = "Ponder dedicated server verification", version = "1",
    dependencies = "required-after:ponder-legacy", serverSideOnly = true, acceptableRemoteVersions = "*")
public final class CleanroomServerSmoke {
    private boolean finished;
    private int ticks;

    @Mod.EventHandler public void prepare(FMLPreInitializationEvent event) {
        if (!Boolean.getBoolean("ponder.dev.serverSmoke")) return;
        net.createmod.ponder.verification.optional.CraftTweakerChecks.prepare();
        // Default demos are intentionally not installed. The isolated harness
        // provides its own scene instead of depending on user content.
        PonderIndex.addPlugin(new net.createmod.ponder.api.registration.PonderPlugin() {
            @Override public String getModId() { return "ponder_server_harness"; }
            @Override public void registerScenes(net.createmod.ponder.api.registration.PonderSceneRegistrationHelper<ResourceLocation> helper) {
                helper.addStoryBoard(new ResourceLocation("minecraft:crafting_table"),
                    new ResourceLocation("ponder:demo/basics"), (scene, util) -> {
                        scene.title("server_runtime", "Dedicated server fixture");
                        scene.configureBasePlate(0, 0, 5);
                        scene.showBasePlate();
                        scene.overlay().showGui(8,new ResourceLocation("server_only:unavailable_gui")).holdFrame(24);
                        scene.idle(20);
                        scene.markAsFinished();
                    }).identifiedBy(new ResourceLocation("ponder_server_harness:server_runtime"));
            }
        });
    }

    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("ponder.dev.serverSmoke")) MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler public void started(FMLServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("ponder.dev.serverSmoke")) return;
        JsonObject report = new JsonObject();
        report.addProperty("minecraft", "1.12.2");
        report.addProperty("cleanroom", Loader.instance().getIndexedModList().get("cleanroom").getVersion());
        try {
            com.google.gson.JsonArray checks = new com.google.gson.JsonArray();
            for (String check : net.createmod.ponder.verification.optional.CraftTweakerChecks.verify()) checks.add(check);
            report.add("checks", checks);
            var themeJson = new JsonObject();
            themeJson.addProperty("layout", "modern");
            net.createmod.ponder.api.theme.PonderThemeOverrides.register(
                new ResourceLocation("server_smoke:theme"), themeJson, false);
            var themes = new net.createmod.ponder.api.theme.PonderThemeLoader(
                net.createmod.ponder.api.theme.PonderThemeOverrides.snapshot(),
                java.util.Map.of(net.createmod.ponder.api.theme.PonderTheme.CLASSIC,
                    net.createmod.ponder.api.theme.PonderTheme.classic())).load();
            if (!themes.themes().containsKey(new ResourceLocation("server_smoke:theme")))
                throw new IllegalStateException("Side-neutral theme registry failed");
            checks.add("Theme data parses and registers on a dedicated server without loading client rendering");
            if (Boolean.getBoolean("ponder.dev.themeZen")) {
                if (!net.createmod.ponder.api.theme.PonderThemeOverrides.snapshot()
                    .containsKey(new ResourceLocation("theme_smoke:fresh")))
                    throw new IllegalStateException("Theme ZenScript did not execute on the dedicated server");
                checks.add("Theme ZenScript startup declarations are safe on a physical dedicated server");
            }
            PonderScene scene = PonderIndex.getSceneAccess().compile(new ResourceLocation("minecraft:crafting_table")).get(0);
            scene.begin();scene.seek(4);
            if(scene.getActiveGuiAnimations().size()!=1||scene.isPaused())throw new IllegalStateException("Headless GUI clip failed");
            checks.add("GUI animation data compiles and seeks without client resources or GUI classes");
            var level = new net.createmod.ponder.api.level.PonderLevel(net.minecraft.util.math.BlockPos.ORIGIN, null);
            var support = new net.minecraft.util.math.BlockPos(1, 1, 1);
            level.setBlockState(support, net.minecraft.init.Blocks.STONE.getDefaultState(), 0);
            var stand = new net.minecraft.entity.item.EntityArmorStand(level, 1.5, 2.5, 1.5);
            stand.setNoGravity(true); level.spawnEntity(stand); level.backup();
            var captured = new PonderScene(level, new net.createmod.ponder.foundation.registration.PonderLocalization(),
                "server_smoke", new ResourceLocation("minecraft:paper"), java.util.List.of(), java.util.List.of());
            var steps = captured.builder();
            steps.idle(10);
            steps.world().showSection(net.createmod.ponder.foundation.SelectionImpl.of(support), net.minecraft.util.EnumFacing.DOWN);
            steps.idle(20);
            steps.world().hideSection(net.createmod.ponder.foundation.SelectionImpl.of(support), net.minecraft.util.EnumFacing.DOWN);
            steps.idle(20);
            captured.begin();
            assertCapturedVisible(captured, false);
            checks.add("Captured structure entities are hidden at tick zero on a physical dedicated server");
            captured.seek(25); assertCapturedVisible(captured, true);
            captured.seek(45); assertCapturedVisible(captured, false);
            checks.add("Captured entity visibility follows section show and hide without client classes");
            captured.seek(25); assertCapturedVisible(captured, true);
            captured.seek(0); assertCapturedVisible(captured, false);
            captured.restart(); assertCapturedVisible(captured, false);
            checks.add("Captured entity replay and backward seeking restore the hidden initial state");
            scene.seek(scene.getTotalTicks());
            if(!scene.getActiveGuiAnimations().isEmpty())throw new IllegalStateException("Headless GUI clip did not end");
            scene.restart();
            if (scene.getWorld() == null || scene.getCurrentTick() != 0) throw new IllegalStateException("Server scene runtime failed");
            report.addProperty("scene", scene.getId().toString());
            report.addProperty("result", "PASS");
        } catch (Throwable error) {
            report.addProperty("result", "FAIL"); report.addProperty("failure", error.toString());
            Ponder.LOGGER.error("Dedicated server verification failed", error);
        }
        Files.writeString(Path.of("ponder-server-smoke.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        finished = true;
    }

    private static void assertCapturedVisible(PonderScene scene, boolean visible) {
        var elements = scene.getElements().stream().filter(net.createmod.ponder.api.element.EntityElement.class::isInstance).toList();
        if (elements.size() != 1 || elements.get(0).isVisible() != visible || scene.getWorld().getEntities().size() != 1)
            throw new IllegalStateException("Captured entity visibility mismatch at tick " + scene.getCurrentTick());
    }

    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (finished && event.phase == TickEvent.Phase.END && ++ticks >= 10) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            server.initiateShutdown();
        }
    }
}
