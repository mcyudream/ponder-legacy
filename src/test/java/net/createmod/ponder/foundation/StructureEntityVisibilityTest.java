package net.createmod.ponder.foundation;

import static org.junit.Assert.*;

import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class StructureEntityVisibilityTest {
    private static final BlockPos SUPPORT = new BlockPos(1, 1, 1);

    @BeforeClass public static void bootstrap() { Bootstrap.register(); }

    @Test public void capturedEntityIsHiddenAtZeroAndUntilItsOwnSectionAppears() {
        PonderScene scene = fixture(2.5);
        var builder = scene.builder();
        builder.world().showSection(SelectionImpl.of(BlockPos.ORIGIN), EnumFacing.DOWN);
        builder.idle(20);
        builder.world().showSection(SelectionImpl.of(SUPPORT), EnumFacing.DOWN);
        builder.idle(20);
        scene.begin();
        assertFalse(captured(scene).isVisible());
        scene.seek(20);
        assertFalse("another visible section cannot reveal this entity", captured(scene).isVisible());
        scene.seek(35);
        assertTrue(captured(scene).isVisible());
    }

    @Test public void hideReshowReplayAndKeyframeRestoreKeepTheCapturedVisibility() {
        PonderScene scene = fixture(2.5);
        var builder = scene.builder();
        builder.idle(10);
        builder.world().showSection(SelectionImpl.of(SUPPORT), EnumFacing.DOWN);
        builder.idle(20);
        builder.world().hideSection(SelectionImpl.of(SUPPORT), EnumFacing.UP);
        builder.idle(20);
        builder.world().showSection(SelectionImpl.of(SUPPORT), EnumFacing.DOWN);
        builder.idle(20);
        scene.declareKeyframe(25);
        scene.declareKeyframe(45);
        scene.begin();
        scene.seek(25); assertTrue(captured(scene).isVisible());
        scene.seek(45); assertFalse(captured(scene).isVisible());
        scene.seek(65); assertTrue(captured(scene).isVisible());
        scene.seek(45); assertFalse(captured(scene).isVisible());
        scene.seek(25); assertTrue(captured(scene).isVisible());
        scene.seek(0); assertFalse(captured(scene).isVisible());
        scene.restart(); assertFalse(captured(scene).isVisible());
        scene.seek(25); assertTrue(captured(scene).isVisible());
    }

    @Test public void independentSectionsOwnCapturedEntitiesWhileMovingAndAfterMerging() {
        PonderScene scene = fixture(2.5);
        var builder = scene.builder();
        var section = builder.world().showIndependentSectionImmediately(SelectionImpl.of(SUPPORT));
        builder.idle(1);
        builder.world().moveSection(section, new Vec3d(2, 0, 0), 10);
        builder.idle(15);
        builder.world().hideIndependentSection(section, EnumFacing.DOWN);
        builder.idle(20);
        scene.begin();
        assertFalse(captured(scene).isVisible());
        scene.seek(12);
        assertTrue(captured(scene).isVisible());
        assertEquals(2, scene.resolve(section).getAnimatedOffset().x, 0);
        scene.seek(31);
        assertFalse(captured(scene).isVisible());
        scene.seek(12);
        assertTrue(captured(scene).isVisible());
        scene.resolve(section).mergeOnto(scene.getBaseWorldSection());
        assertTrue(captured(scene).isVisible());
    }

    @Test public void unsupportedFloatingEntitiesCanBeShownByEverywhereEvenWithoutBlocksInTheirCell() {
        PonderScene scene = fixture(4.5);
        scene.builder().world().showSection(scene.getSceneBuildingUtil().select().everywhere(), EnumFacing.DOWN);
        scene.builder().idle(30);
        scene.begin();
        assertFalse(captured(scene).isVisible());
        scene.seek(20);
        assertTrue(captured(scene).isVisible());
        scene.seek(0);
        assertFalse(captured(scene).isVisible());
    }

    @Test public void explicitCreationRemainsIndependentOfTheSchematicSections() {
        PonderScene scene = fixture(2.5);
        scene.builder().idle(10);
        var explicit = scene.builder().world().createEntity(world -> {
            var entity = new EntityArmorStand(world, 5.5, 1, 1.5);
            entity.setNoGravity(true);
            return entity;
        });
        scene.builder().idle(10);
        scene.begin();
        assertNull(scene.resolve(explicit));
        scene.seek(11);
        assertTrue(scene.resolve(explicit).isVisible());
        assertFalse(captured(scene).isVisible());
        scene.seek(0);
        assertNull(scene.resolve(explicit));
        assertFalse(captured(scene).isVisible());
    }

    private static EntityElement captured(PonderScene scene) {
        return scene.getElements().stream().filter(EntityElement.class::isInstance)
            .map(EntityElement.class::cast).findFirst().orElseThrow();
    }

    private static PonderScene fixture(double entityY) {
        var world = new PonderLevel(BlockPos.ORIGIN, null);
        world.setBlockState(BlockPos.ORIGIN, Blocks.STONE.getDefaultState(), 0);
        world.setBlockState(SUPPORT, Blocks.STONE.getDefaultState(), 0);
        var entity = new EntityArmorStand(world, 1.5, entityY, 1.5);
        entity.setNoGravity(true);
        world.spawnEntity(entity);
        world.backup();
        return new PonderScene(world, new PonderLocalization(), "test",
            new ResourceLocation("minecraft:paper"), Collections.emptyList(), Collections.emptyList());
    }
}
