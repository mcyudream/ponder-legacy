package net.createmod.ponder.client.editor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.createmod.ponder.api.editor.CuboidSelection;
import net.createmod.ponder.api.editor.ScenePackIO;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.editor.StructureCapture;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.createmod.ponder.foundation.structure.PonderStructure;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.createmod.ponder.script.ScriptSceneDefinition;
import net.createmod.ponder.script.ScriptSceneRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.fml.common.Loader;

/** Real Botania entities, server capture, vanilla NBT import and client framebuffer checks. */
public final class BotaniaSparkSmoke {
    private static final BlockPos ORIGIN = new BlockPos(-32, 64, -32);
    private final Minecraft mc;
    private final BiConsumer<Boolean, String> check;
    private final Consumer<String> screenshot;
    private CompletableFuture<byte[][]> captured;
    private final List<PonderScene> scenes = new ArrayList<>();
    private PonderUI viewer;
    private int phase;
    private boolean optionalOnly;

    public BotaniaSparkSmoke(Minecraft mc, BiConsumer<Boolean, String> check, Consumer<String> screenshot) {
        this.mc = mc; this.check = check; this.screenshot = screenshot;
    }

    public boolean advance() throws Exception {
        if (optionalOnly) return true;
        if (phase == 0) {
            if (!Loader.isModLoaded("botania")) {
                check.accept(!Loader.isModLoaded("crafttweaker"), "client starts without Botania or CraftTweaker");
                check.accept(java.util.Arrays.stream(net.createmod.ponder.script.ScriptWorldBuilder.class.getDeclaredMethods())
                    .filter(method -> method.getName().equals("createEntity"))
                    .allMatch(method -> java.util.Arrays.stream(method.getParameterTypes())
                        .noneMatch(type -> type.getName().startsWith("crafttweaker."))),
                    "optional IData overloads are stripped without linking absent CraftTweaker classes");
                var builder = ScriptSceneRegistry.create("minecraft:paper", "spark_smoke:optional",
                    "Vanilla entity without Botania", "ponder:demo/basics");
                builder.world.createEntity("stand", "minecraft:armor_stand", 1.5, 1, 1.5);
                builder.idle(60).register();
                PonderLevel world = new PonderLevel(BlockPos.ORIGIN, mc.world);
                for (BlockPos pos : BlockPos.getAllInBox(BlockPos.ORIGIN, new BlockPos(2, 0, 2)))
                    world.setBlockState(pos, Blocks.STONEBRICK.getDefaultState(), 0);
                world.backup();
                PonderScene scene = new PonderScene(world, new PonderLocalization(), "spark_smoke",
                    new ResourceLocation("minecraft:paper"), Collections.emptyList(), Collections.emptyList());
                ScriptSceneRegistry.localSnapshot(true).stream().filter(def -> def.getSceneId().toString()
                    .equals("spark_smoke:optional")).findFirst().orElseThrow().asStoryBoard()
                    .program(scene.builder(), scene.getSceneBuildingUtil());
                scene.begin(); scene.seek(30); scene.restart(); scene.seek(10); scene.setPaused(true);
                check.accept(world.getEntities().size() == 1, "vanilla entity creation and replay work without optional mods");
                viewer = new PonderUI(Collections.singletonList(scene)); mc.displayGuiScreen(viewer);
                scene.seek(10); scene.setPaused(true);
                screenshot.accept("entities-no-botania.png");
                optionalOnly = true;
                return false;
            }
            check.accept(Loader.isModLoaded("botania"), "actual Botania r1.10-364.4 is installed");
            captured = new CompletableFuture<>();
            mc.getIntegratedServer().addScheduledTask(() -> {
                try {
                    World world = mc.getIntegratedServer().getWorld(0);
                    for (BlockPos pos : BlockPos.getAllInBox(ORIGIN, ORIGIN.add(14, 0, 4)))
                        world.setBlockState(pos, Blocks.STONEBRICK.getDefaultState(), 2);
                    for (int i = 0; i < 7; i++) {
                        BlockPos block = ORIGIN.add(1 + i * 2, 1, 2);
                        world.setBlockState(block, i < 5
                            ? Block.REGISTRY.getObject(new ResourceLocation("botania:pool")).getDefaultState()
                            : Blocks.CHEST.getDefaultState(), 2);
                        if (!world.spawnEntity(spark(world, i, new Vec3d(ORIGIN))))
                            throw new IllegalStateException("Could not spawn real fixture spark " + i);
                    }
                    CuboidSelection selection = new CuboidSelection(ORIGIN, ORIGIN.add(14, 3, 4));
                    byte[] editor = StructureCapture.capture(world, selection);
                    Template vanilla = new Template();
                    vanilla.takeBlocksFromWorld(world, ORIGIN, new BlockPos(15, 4, 5), true, Blocks.AIR);
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    CompressedStreamTools.writeCompressed(vanilla.writeToNBT(new NBTTagCompound()), output);
                    captured.complete(new byte[][] {editor, output.toByteArray()});
                } catch (Throwable failure) { captured.completeExceptionally(failure); }
            });
            phase++;
            return false;
        }
        if (phase == 1) {
            if (!captured.isDone()) return false;
            byte[][] bytes = captured.join();
            Path fixtures = mc.gameDir.toPath().resolve("ponder-smoke/fixtures");
            Files.createDirectories(fixtures);
            Files.write(fixtures.resolve("captured.nbt"), bytes[0]);
            Files.write(fixtures.resolve("vanilla-import.nbt"), bytes[1]);
            PonderStructure capture = load(bytes[0], "capture");
            PonderStructure imported = load(bytes[1], "import");
            check.accept(capture.getEntities().size() == 7, "server editor capture retains all seven real sparks");
            check.accept(imported.getEntities().size() == 7, "independent vanilla structure NBT contains seven sparks");
            scenes.add(scene(capture, "Captured sparks"));
            scenes.add(scene(imported, "Imported sparks"));
            PonderScene java = scene(capture, "Java factory sparks");
            java.getWorld().getEntities().forEach(java.getWorld()::removeEntity);
            java.getWorld().backup();
            for (int i = 0; i < 7; i++) {
                final int variant = i;
                java.builder().world().createEntity(world -> spark(world, variant, Vec3d.ZERO));
            }
            scenes.add(java);
            PonderScene json = scene(capture, "JSON sparks");
            json.getWorld().getEntities().forEach(json.getWorld()::removeEntity);
            json.getWorld().backup();
            StringBuilder steps = new StringBuilder();
            for (int i = 0; i < 7; i++) {
                if (i > 0) steps.append(',');
                steps.append("{\"op\":\"world.create_entity\",\"handle\":\"spark").append(i)
                    .append("\",\"entity\":\"").append(entityId(i)).append("\",\"x\":")
                    .append(1.5 + 2 * i).append(",\"y\":2.5,\"z\":2.5,\"nbt\":\"")
                    .append(i < 5 ? "{upgrade:" + i + "}" : "{master:" + (i == 6 ? "1b" : "0b") + "}")
                    .append("\"}");
            }
            ScriptSceneDefinition program = ScenePackIO.parse(new StringReader(
                "{\"format\":1,\"id\":\"spark_smoke:json\",\"scenes\":[{\"id\":\"spark_smoke:json\","
                + "\"component\":\"minecraft:paper\",\"title\":\"JSON sparks\",\"structure\":\"ponder:demo/basics\","
                + "\"instructions\":[" + steps + ",{\"op\":\"scene.idle\",\"ticks\":100}]}]}")).get(0);
            program.asStoryBoard().program(json.builder(), json.getSceneBuildingUtil());
            scenes.add(json);
            if (Loader.isModLoaded("crafttweaker")) {
                ScriptSceneDefinition zen = ScriptSceneRegistry.localSnapshot(true).stream()
                    .filter(def -> def.getSceneId().toString().equals("spark_smoke:zen")).findFirst().orElseThrow();
                PonderScene zs = scene(capture, "ZenScript sparks");
                zs.getWorld().getEntities().forEach(zs.getWorld()::removeEntity);
                zs.getWorld().backup();
                zen.asStoryBoard().program(zs.builder(), zs.getSceneBuildingUtil());
                scenes.add(zs);
                check.accept(true, "real CraftTweaker ZenScript fixture compiled and registered");
            }
            phase++;
            return false;
        }
        int step = phase++ - 2;
        if (step >= scenes.size() * 2) return true;
        PonderScene scene = scenes.get(step / 2);
        if (step % 2 == 0) {
            scene.begin(); scene.seek(40); scene.setPaused(true);
            verifyEntities(scene);
            viewer = new PonderUI(Collections.singletonList(scene));
            mc.displayGuiScreen(viewer);
            scene.seek(40); scene.setPaused(true);
            screenshot.accept("botania-sparks-" + (step / 2) + ".png");
        } else {
            scene.restart(); scene.seek(70); scene.seek(10); scene.setPaused(true);
            verifyEntities(scene);
            check.accept(true, "seven sparks retain their NBT after replay and backward seeking: " + scene.getTitle());
            screenshot.accept("botania-sparks-" + (step / 2) + "-replay.png");
        }
        return false;
    }

    private PonderStructure load(byte[] bytes, String name) throws Exception {
        return new PonderStructureLoader().load(new ByteArrayInputStream(bytes),
            new ResourceLocation("spark_smoke", name));
    }

    private PonderScene scene(PonderStructure structure, String title) {
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, mc.world);
        structure.place(world); world.backup();
        PonderScene scene = new PonderScene(world, new PonderLocalization(), "spark_smoke",
            new ResourceLocation("minecraft:paper"), Collections.emptyList(), Collections.emptyList());
        var builder = scene.builder();
        builder.title(title.toLowerCase(java.util.Locale.ROOT).replace(' ', '_'), title);
        builder.configureBasePlate(0, 0, 15); builder.showBasePlate();
        builder.world().showSection(scene.getSceneBuildingUtil().select().everywhere(), EnumFacing.DOWN);
        // Keep cursor zero so the programmatic creation instructions also execute at the start.
        builder.addInstruction(new net.createmod.ponder.foundation.instruction.TickingInstruction(false, 100) {
            @Override protected void tickRunning(PonderScene value, int elapsed, float progress) {}
        });
        return scene;
    }

    private static String entityId(int variant) {
        return variant < 5 ? "botania:spark" : "botania:corporea_spark";
    }

    private static Entity spark(World world, int variant, Vec3d origin) {
        Entity entity = EntityList.createEntityByIDFromName(new ResourceLocation(entityId(variant)), world);
        if (entity == null) throw new IllegalStateException("Missing real Botania entity");
        NBTTagCompound data = entity.writeToNBT(new NBTTagCompound());
        if (variant < 5) data.setInteger("upgrade", variant);
        else data.setBoolean("master", variant == 6);
        entity.readFromNBT(data);
        entity.setPosition(origin.x + 1.5 + 2 * variant, origin.y + 2.5, origin.z + 2.5);
        return entity;
    }

    private void verifyEntities(PonderScene scene) {
        List<Entity> entities = new ArrayList<>(scene.getWorld().getEntities());
        check.accept(entities.size() == 7 && entities.stream().noneMatch(entity -> entity.isDead),
            "seven live scene sparks: " + scene.getTitle());
        for (int i = 0; i < 7; i++) {
            Entity entity = entities.get(i);
            NBTTagCompound data = entity.writeToNBT(new NBTTagCompound());
            check.accept(entityId(i).equals(EntityList.getKey(entity).toString())
                && (i < 5 ? data.getInteger("upgrade") == i : data.getBoolean("master") == (i == 6)),
                "native spark type and upgrade/master NBT survive: " + i);
            check.accept(mc.getRenderManager().getEntityRenderObject(entity) != null,
                "Botania registers its native spark renderer: " + i);
        }
    }

    public void verifyFrame(String name) {
        if (!name.startsWith("botania-sparks-") && !name.equals("entities-no-botania.png")) return;
        List<EntityElement> elements = new ArrayList<>();
        viewer.getActiveScene().forEach(EntityElement.class, elements::add);
        check.accept(elements.size() == (optionalOnly ? 1 : 7), "scene exposes the expected renderable entity elements");
        for (int i = 0; i < elements.size(); i++) {
            viewer.drawScreen(-100, -100, 0);
            BufferedImage visible = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            elements.get(i).setVisible(false);
            viewer.drawScreen(-100, -100, 0);
            BufferedImage hidden = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            elements.get(i).setVisible(true);
            int changed = 0;
            for (int y = 100; y < visible.getHeight() - 100; y++)
                for (int x = 0; x < visible.getWidth(); x++)
                    if (visible.getRGB(x, y) != hidden.getRGB(x, y)) changed++;
            check.accept(changed > 8, "framebuffer contains visible pixels for native entity " + i + ": " + name
                + " (" + changed + " pixels)");
        }
        viewer.drawScreen(-100, -100, 0);
    }
}
