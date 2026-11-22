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
import net.createmod.ponder.api.editor.SceneDocument;
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
import net.minecraftforge.fml.relauncher.ReflectionHelper;

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
    private PonderStructure capturedStructure;
    private EditorScreen editor;
    private EditorPreview preview;
    private final java.util.Map<Integer, Vec3d> shownCenters = new java.util.HashMap<>();
    private int vanillaStep;

    public BotaniaSparkSmoke(Minecraft mc, BiConsumer<Boolean, String> check, Consumer<String> screenshot) {
        this.mc = mc; this.check = check; this.screenshot = screenshot;
    }

    public boolean advance() throws Exception {
        if (optionalOnly) return vanillaTimeline();
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
            capturedStructure = capture;
            Path editorStructure = Path.of(PonderStructureLoader.expectedExternalPath(new ResourceLocation("spark_smoke:timeline")));
            Files.createDirectories(editorStructure.getParent());
            Files.write(editorStructure, bytes[0]);
            PonderStructureLoader.invalidateCaches();
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
        if (Boolean.getBoolean("ponder.dev.sparkTimelineOnly")) step += scenes.size() * 2;
        if (step >= scenes.size() * 2) return timeline(step - scenes.size() * 2);
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

    private boolean vanillaTimeline() throws Exception {
        switch (vanillaStep++) {
            case 0 -> {
                var world = new PonderLevel(BlockPos.ORIGIN, mc.world);
                world.setBlockState(BlockPos.ORIGIN, Blocks.STONE.getDefaultState(), 0);
                var stand = new net.minecraft.entity.item.EntityArmorStand(world, .5, 1.25, .5);
                stand.setNoGravity(true); world.spawnEntity(stand);
                var id = new ResourceLocation("spark_smoke:vanilla_timeline");
                Path file = Path.of(PonderStructureLoader.expectedExternalPath(id));
                Files.createDirectories(file.getParent());
                Files.write(file, StructureCapture.capture(world, new CuboidSelection(BlockPos.ORIGIN, new BlockPos(2, 3, 2))));
                PonderStructureLoader.invalidateCaches();
                editor = new EditorScreen(SceneDocument.create("spark_smoke:vanilla_timeline", "minecraft:paper",
                    "Captured vanilla entity timeline", id.toString()), null);
                mc.displayGuiScreen(editor);
                preview = (EditorPreview) ReflectionHelper.findField(EditorScreen.class, "preview").get(editor);
                visible(preview.scene(), 0, "vanilla editor tick zero without optional mods");
                screenshot.accept("spark-timeline-editor-vanilla-zero.png");
            }
            case 1 -> {
                preview.seek(20);
                visible(preview.scene(), 1, "vanilla editor selected structure shown");
                screenshot.accept("spark-timeline-editor-vanilla-visible.png");
            }
            case 2 -> {
                preview.seek(0);
                visible(preview.scene(), 0, "vanilla editor rewound without optional mods");
                screenshot.accept("spark-timeline-editor-vanilla-rewind-zero.png");
            }
            default -> { return true; }
        }
        return false;
    }

    private boolean timeline(int step) throws Exception {
        switch (step) {
            case 0 -> {
                editor = new EditorScreen(SceneDocument.create("spark_smoke:timeline", "minecraft:paper",
                    "Captured sparks follow the timeline", "spark_smoke:timeline"), null);
                mc.displayGuiScreen(editor);
                preview = (EditorPreview) ReflectionHelper.findField(EditorScreen.class, "preview").get(editor);
                check.accept(preview.tick() == 0 && preview.total() == 20,
                    "editor reproduces the default three-step show / wait / finish scene at tick zero");
                visible(preview.scene(), 0, "editor tick zero");
                screenshot.accept("spark-timeline-editor-zero.png");
            }
            case 1 -> {
                preview.seek(20);
                visible(preview.scene(), 7, "editor after the selected structure is revealed");
                screenshot.accept("spark-timeline-editor-visible.png");
            }
            case 2 -> {
                preview.seek(0);
                visible(preview.scene(), 0, "editor rewind to zero");
                screenshot.accept("spark-timeline-editor-rewind-zero.png");
            }
            case 3 -> {
                preview.seek(20);
                var reset = (java.util.List<?>) ReflectionHelper.findField(net.minecraft.client.gui.GuiScreen.class,
                    "buttonList", "field_146292_n").get(editor);
                var button = reset.stream().map(net.minecraft.client.gui.GuiButton.class::cast)
                    .filter(b -> b.id == 211).findFirst().orElseThrow();
                editor.mouseClicked(button.x + 4, button.y + 4, 0);
                visible(preview.scene(), 0, "actual editor replay button returns to a hidden zero frame");
                screenshot.accept("spark-timeline-editor-replay-zero.png");
            }
            case 4 -> {
                PonderLevel world = new PonderLevel(BlockPos.ORIGIN, mc.world);
                capturedStructure.place(world); world.backup();
                PonderScene scene = new PonderScene(world, new PonderLocalization(), "spark_smoke",
                    new ResourceLocation("minecraft:paper"), Collections.emptyList(), Collections.emptyList());
                var builder = scene.builder();
                builder.title("timeline", "Captured sparks follow their section");
                var all = scene.getSceneBuildingUtil().select().everywhere();
                builder.idle(20);
                var section = builder.world().showIndependentSectionImmediately(all);
                builder.idle(5);
                builder.world().moveSection(section, new Vec3d(0, 1, 0), 10);
                builder.idle(15);
                builder.world().hideIndependentSection(section, EnumFacing.DOWN);
                builder.idle(20);
                builder.world().showSection(all, EnumFacing.DOWN);
                builder.idle(20);
                scene.declareKeyframe(21); scene.declareKeyframe(35); scene.declareKeyframe(55);
                viewer = new PonderUI(Collections.singletonList(scene));
                mc.displayGuiScreen(viewer); scene.setPaused(true);
                visible(scene, 0, "player tick zero");
                scene.seek(20);
                visible(scene, 0, "player before delayed reveal at tick twenty");
                screenshot.accept("spark-timeline-player-before.png");
            }
            case 5 -> {
                viewer.getActiveScene().seek(21);
                visible(viewer.getActiveScene(), 7, "player independent section shown");
                screenshot.accept("spark-timeline-player-shown.png");
            }
            case 6 -> {
                viewer.getActiveScene().seek(35);
                visible(viewer.getActiveScene(), 7, "player independent section moved");
                screenshot.accept("spark-timeline-player-moved.png");
            }
            case 7 -> {
                viewer.getActiveScene().seek(55);
                visible(viewer.getActiveScene(), 0, "player independent section hidden");
                screenshot.accept("spark-timeline-player-hidden.png");
            }
            case 8 -> {
                viewer.getActiveScene().seek(75);
                visible(viewer.getActiveScene(), 7, "player selected structure revealed again");
                viewer.getActiveScene().seek(21);
                visible(viewer.getActiveScene(), 7, "player restores a visible keyframe");
                verifyEntities(viewer.getActiveScene());
                screenshot.accept("spark-timeline-player-restored.png");
            }
            case 9 -> {
                viewer.getActiveScene().seek(0);
                visible(viewer.getActiveScene(), 0, "player rewinds to the initial snapshot");
                viewer.getActiveScene().restart();
                viewer.getActiveScene().setPaused(true);
                visible(viewer.getActiveScene(), 0, "player replay");
                screenshot.accept("spark-timeline-player-replay-zero.png");
            }
            default -> { return true; }
        }
        return false;
    }

    private void visible(PonderScene scene, int expected, String context) {
        List<EntityElement> entities = new ArrayList<>();
        scene.forEach(EntityElement.class, entities::add);
        int total = optionalOnly ? 1 : 7;
        check.accept(entities.size() == total && scene.getWorld().getEntities().size() == total,
            "captured sparks remain loaded without being removed: " + context);
        check.accept(entities.stream().filter(EntityElement::isVisible).count() == expected,
            "captured spark visibility matches the section: " + context + " (" + expected + "/" + total + ")");
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
        if (name.startsWith("spark-timeline-")) { verifyTimelineFrame(name); return; }
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

    private void verifyTimelineFrame(String name) {
        boolean editing = name.startsWith("spark-timeline-editor");
        var screen = editing ? editor : viewer;
        PonderScene scene = editing ? preview.scene() : viewer.getActiveScene();
        List<EntityElement> elements = new ArrayList<>();
        scene.forEach(EntityElement.class, elements::add);
        int index = 0;
        for (EntityElement entity : elements) {
            boolean expected = entity.isVisible();
            screen.drawScreen(-100, -100, 0);
            BufferedImage visible = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            entity.setVisible(false);
            screen.drawScreen(-100, -100, 0);
            BufferedImage hidden = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            entity.setVisible(true);
            int changed = 0; double xTotal = 0, yTotal = 0;
            for (int y = 110; y < visible.getHeight() - 170; y++)
                for (int x = 0; x < visible.getWidth(); x++)
                    if (visible.getRGB(x, y) != hidden.getRGB(x, y)) { changed++; xTotal += x; yTotal += y; }
            check.accept(expected ? changed > 8 : changed == 0,
                "framebuffer " + (expected ? "contains" : "does not contain") + " spark " + index
                    + ": " + name + " (" + changed + " pixels)");
            if (name.equals("spark-timeline-player-shown.png"))
                shownCenters.put(index, new Vec3d(xTotal / Math.max(1, changed), yTotal / Math.max(1, changed), 0));
            if (name.equals("spark-timeline-player-moved.png")) {
                Vec3d old = shownCenters.get(index);
                check.accept(old != null && old.squareDistanceTo(new Vec3d(xTotal / Math.max(1, changed),
                    yTotal / Math.max(1, changed), 0)) > 36,
                    "native spark renderer follows the independently moved section in the framebuffer: " + index);
            }
            index++;
        }
        screen.drawScreen(-100, -100, 0);
    }
}
