package net.createmod.ponder.editor;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;
import net.createmod.ponder.api.editor.CuboidSelection;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.createmod.ponder.foundation.structure.PonderStructure;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class StructureEntityCaptureTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }

    @Test public void capturePreservesEntitiesAndTheirLocalPositionsThroughReplay() throws Exception {
        PonderLevel source = new PonderLevel(BlockPos.ORIGIN, null);
        BlockPos origin = new BlockPos(-19, 64, -33);
        source.setBlockState(origin, Blocks.STONE.getDefaultState(), 0);
        EntityArmorStand stand = new EntityArmorStand(source, -18.5, 65.25, -32.5);
        stand.setCustomNameTag("Captured entity");
        stand.setNoGravity(true);
        source.spawnEntity(stand);
        EntityItem item = new EntityItem(source, -17.5, 65.5, -32.5, new ItemStack(Items.DIAMOND, 3));
        item.setNoGravity(true);
        item.motionX = item.motionY = item.motionZ = 0;
        source.spawnEntity(item);
        source.spawnEntity(new EntityArmorStand(source, -10, 65, -32.5));
        assertEquals(source.getEntities().stream().map(entity -> entity.getClass().getSimpleName()
            + ":" + entity.getEntityBoundingBox()).toList().toString(), 2, source.getEntitiesWithinAABB(Entity.class,
            new net.minecraft.util.math.AxisAlignedBB(origin, origin.add(3, 3, 3))).size());
        for (Entity entity : source.getEntities())
            assertTrue(entity.getClass().getName(), entity.writeToNBTOptional(new net.minecraft.nbt.NBTTagCompound()));

        PonderStructure structure = new PonderStructureLoader().load(new ByteArrayInputStream(
            StructureCapture.capture(source, new CuboidSelection(origin, origin.add(2, 2, 2)))),
            new ResourceLocation("test:captured_entities"));
        assertEquals(2, structure.getEntities().size());
        assertEquals(new Vec3d(.5, 1.25, .5), structure.getEntities().get(0).getPosition());
        PonderLevel target = new PonderLevel(BlockPos.ORIGIN, null);
        structure.place(target);
        Entity restored = target.getEntities().iterator().next();
        assertEquals("Captured entity", restored.getCustomNameTag());
        assertEquals(.5, restored.posX, 0);
        assertEquals(1.25, restored.posY, 0);
        assertEquals(.5, restored.lastTickPosX, 0);
        assertEquals(1.25, restored.lastTickPosY, 0);
        assertEquals(.5, restored.lastTickPosZ, 0);
        assertEquals(-18.5, stand.posX, 0);
        assertEquals(65.25, stand.posY, 0);

        target.backup();
        PonderScene scene = new PonderScene(target, new PonderLocalization(), "test",
            new ResourceLocation("minecraft:paper"), Collections.emptyList(), Collections.emptyList());
        scene.builder().idle(50);
        scene.begin();
        scene.seek(20);
        assertEquals(2, target.getEntities().size());
        scene.restart();
        scene.seek(10);
        assertEquals(2, target.getEntities().size());
        assertEquals(2, target.getEntitiesWithinAABB(Entity.class,
            new net.minecraft.util.math.AxisAlignedBB(0, 0, 0, 3, 3, 3)).size());
    }
}
