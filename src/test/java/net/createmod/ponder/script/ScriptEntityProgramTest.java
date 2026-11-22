package net.createmod.ponder.script;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;
import net.createmod.ponder.api.editor.ScenePackIO;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class ScriptEntityProgramTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }

    @Test public void jsonCreatesRegisteredEntitiesWithNbtAndStableReplay() {
        ScriptSceneDefinition definition = ScenePackIO.parse(new StringReader("""
            {"format":1,"id":"test:entity_pack","scenes":[{
              "id":"test:entity_program","component":"minecraft:paper","title":"Entities",
              "structure":"ponder:demo/basics","instructions":[
                {"op":"world.create_entity","handle":"pig","entity":"minecraft:pig",
                 "x":2.5,"y":3.25,"z":4.5,"nbt":"{NoAI:1b,NoGravity:1b,CustomName:\\"Script pig\\"}"},
                {"op":"scene.idle","ticks":60}]}]}
            """)).get(0);
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        world.backup();
        PonderScene scene = new PonderScene(world, new PonderLocalization(), "test",
            new ResourceLocation("minecraft:paper"), Collections.emptyList(), Collections.emptyList());
        definition.asStoryBoard().program(scene.builder(), scene.getSceneBuildingUtil());
        scene.begin();
        scene.tick();
        Entity entity = world.getEntities().iterator().next();
        assertTrue(entity instanceof EntityPig);
        assertEquals(10, ((EntityPig) entity).getHealth(), 0);
        assertEquals("Script pig", entity.getCustomNameTag());
        assertEquals(2.5, entity.posX, 0);
        assertEquals(2.5, entity.lastTickPosX, 0);
        assertEquals(3.25, entity.lastTickPosY, 0);
        scene.seek(40);
        scene.seek(10);
        assertEquals(1, world.getEntities().size());
        assertEquals("Script pig", world.getEntities().iterator().next().getCustomNameTag());
        scene.restart();
        scene.seek(10);
        assertEquals(1, world.getEntities().size());
    }

    @Test public void zenBuilderSupportsEntitiesWithoutNbtAndRejectsDuplicateHandles() {
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", "test:entity_builder",
            "Entity builder", "ponder:demo/basics");
        builder.world.createEntity("spark", "botania:spark", ScriptVector.of(.5, 1.5, .5));
        try {
            builder.world.createItemEntity("spark", 0, 1, 0, 0, 0, 0, "minecraft:apple", 1, 0);
            fail("Duplicate handle accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("spark"));
        }
    }

    @Test public void entityHandlesCannotBeUsedAsItemHandles() {
        ScriptSceneBuilder builder = new ScriptSceneBuilder("minecraft:paper", "test:entity_type",
            "Entity type", "ponder:demo/basics");
        builder.world.createEntity("spark", "botania:spark", .5, 1.5, .5);
        try {
            builder.world.moveItem("spark", 1, 0, 0, 10);
            fail("Entity accepted as item");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("item"));
        }
    }
}
