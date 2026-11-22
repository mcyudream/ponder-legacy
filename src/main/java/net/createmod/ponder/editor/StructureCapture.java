package net.createmod.ponder.editor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import net.createmod.ponder.api.editor.CuboidSelection;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Captures a real 1.12.2 world into vanilla palette-based structure NBT. */
public final class StructureCapture {
    public static final int MAX_COMPRESSED_BYTES = 900_000;
    private StructureCapture() {}

    public static byte[] capture(World world, CuboidSelection selection) throws IOException {
        selection.checkCaptureSize();
        if (selection.min().getY() < 0 || selection.max().getY() >= world.getHeight())
            throw new IllegalArgumentException("Selection is outside world height");
        if (!world.isAreaLoaded(selection.min(), selection.max()))
            throw new IllegalArgumentException("Load every chunk in the selection first");
        Map<IBlockState, Integer> states = new LinkedHashMap<>();
        NBTTagList palette = new NBTTagList();
        NBTTagList blocks = new NBTTagList();
        for (BlockPos position : BlockPos.getAllInBox(selection.min(), selection.max())) {
            IBlockState state = world.getBlockState(position);
            Integer index = states.get(state);
            if (index == null) {
                index = states.size(); states.put(state, index); palette.appendTag(stateTag(state));
            }
            BlockPos local = position.subtract(selection.min());
            NBTTagCompound block = new NBTTagCompound();
            block.setTag("pos", vector(local)); block.setInteger("state", index);
            TileEntity tile = world.getTileEntity(position);
            if (tile != null) {
                NBTTagCompound data = tile.writeToNBT(new NBTTagCompound());
                data.setInteger("x", local.getX()); data.setInteger("y", local.getY()); data.setInteger("z", local.getZ());
                block.setTag("nbt", data);
            }
            blocks.appendTag(block);
        }
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("DataVersion", 1343);
        root.setTag("size", vector(selection.max().subtract(selection.min()).add(1, 1, 1)));
        root.setTag("palette", palette); root.setTag("blocks", blocks);
        root.setTag("entities", captureEntities(world, selection));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(root, output);
        if (output.size() > MAX_COMPRESSED_BYTES) throw new IOException("Structure NBT exceeds 900 KB; select a smaller area");
        return output.toByteArray();
    }

    private static NBTTagList captureEntities(World world, CuboidSelection selection) {
        NBTTagList entities = new NBTTagList();
        AxisAlignedBB bounds = new AxisAlignedBB(selection.min(), selection.max().add(1, 1, 1));
        Vec3d origin = new Vec3d(selection.min());
        for (Entity entity : world.getEntitiesWithinAABB(Entity.class, bounds)) {
            if (entity instanceof EntityPlayer || entity.isDead) continue;
            NBTTagCompound data = new NBTTagCompound();
            // Riding entities are serialized with their vehicle, rather than captured twice.
            if (!entity.writeToNBTOptional(data)) continue;
            Vec3d local = entity.getPositionVector().subtract(origin);
            data.setTag("Pos", vector(local));
            BlockPos blockPos = new BlockPos(local);
            if (entity instanceof EntityHanging) {
                blockPos = ((EntityHanging) entity).getHangingPosition().subtract(selection.min());
                data.setInteger("TileX", blockPos.getX());
                data.setInteger("TileY", blockPos.getY());
                data.setInteger("TileZ", blockPos.getZ());
            }
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag("pos", vector(local));
            entry.setTag("blockPos", vector(blockPos));
            entry.setTag("nbt", data);
            entities.appendTag(entry);
        }
        return entities;
    }

    private static NBTTagCompound stateTag(IBlockState state) {
        NBTTagCompound result = new NBTTagCompound();
        result.setString("Name", Block.REGISTRY.getNameForObject(state.getBlock()).toString());
        NBTTagCompound properties = new NBTTagCompound();
        for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet())
            properties.setString(entry.getKey().getName(), propertyName(entry.getKey(), entry.getValue()));
        result.setTag("Properties", properties);
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String propertyName(IProperty property, Comparable value) { return property.getName(value); }
    private static NBTTagList vector(BlockPos pos) {
        NBTTagList result = new NBTTagList();
        result.appendTag(new NBTTagInt(pos.getX())); result.appendTag(new NBTTagInt(pos.getY()));
        result.appendTag(new NBTTagInt(pos.getZ())); return result;
    }
    private static NBTTagList vector(Vec3d pos) {
        NBTTagList result = new NBTTagList();
        result.appendTag(new NBTTagDouble(pos.x)); result.appendTag(new NBTTagDouble(pos.y));
        result.appendTag(new NBTTagDouble(pos.z)); return result;
    }
}
