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
            PonderScene scene = PonderIndex.getSceneAccess().compile(new ResourceLocation("minecraft:crafting_table")).get(0);
            scene.begin();scene.seek(4);
            if(scene.getActiveGuiAnimations().size()!=1||scene.isPaused())throw new IllegalStateException("Headless GUI clip failed");
            checks.add("GUI animation data compiles and seeks without client resources or GUI classes");
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

    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (finished && event.phase == TickEvent.Phase.END && ++ticks >= 10) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            server.initiateShutdown();
        }
    }
}
