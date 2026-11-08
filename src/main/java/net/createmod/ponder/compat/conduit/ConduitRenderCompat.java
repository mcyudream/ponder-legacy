package net.createmod.ponder.compat.conduit;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.foundation.PonderWorld;
import net.createmod.ponder.foundation.structure.PonderStructure;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;

/**
 * Built-in optional compatibility for conduit and pipe connection rendering inside Ponder scenes.
 *
 * <p>Mekanism persists only the configured per-side connection values in tile NBT. The actual
 * connection masks ({@code currentTransmitterConnections}/{@code currentAcceptorConnections})
 * are transient state computed by the server and sent through the tile update packet, which a
 * Ponder scene never receives. Ender IO fills {@code clientConduits} from NBT but writes only
 * {@code serverConduits} back, so Ponder's backup/restore round-trip would otherwise drop them.</p>
 *
 * <p>Both mods are optional. All third-party access is reflective, and no compatibility code runs
 * on a dedicated server. When neither mod is installed these hooks return immediately.</p>
 */
public final class ConduitRenderCompat {
    private static final String MEKANISM_MOD_ID = "mekanism";
    private static final String ENDER_IO_MOD_ID = "enderio";
    private static final String THERMAL_DYNAMICS_MOD_ID = "thermaldynamics";
    private static final String SECTION_RENDER_CACHE =
        "net.createmod.ponder.render.SectionRenderCache";
    private static final int TICK_INTERVAL = 10;

    /** Original Ender IO conduit NBT captured from the structure before Ponder snapshots it. */
    private static final Map<PonderWorld, Map<BlockPos, NBTTagCompound>> EIO_TILE_DATA =
        Collections.synchronizedMap(new WeakHashMap<PonderWorld, Map<BlockPos, NBTTagCompound>>());

    private static int tickCounter;
    private static boolean failureReported;

    private ConduitRenderCompat() {
    }

    /**
     * Captures Ender IO conduit data from the source structure and applies the first refresh.
     * Called after {@link PonderStructure#place} and before the initial world backup.
     */
    public static synchronized void onStructurePlaced(PonderWorld world, PonderStructure structure) {
        if (world == null || !isActive()) {
            return;
        }
        if (structure != null && modLoaded(ENDER_IO_MOD_ID)) {
            Map<BlockPos, NBTTagCompound> data = EnderIoConduitSupport.collectTileData(structure);
            if (!data.isEmpty()) {
                EIO_TILE_DATA.put(world, data);
            }
        }
        apply(world);
    }

    /** Reapplies transient connection state after a snapshot or selection restore. */
    public static synchronized void onWorldRestored(PonderWorld world) {
        if (world == null || !isActive()) {
            return;
        }
        apply(world);
    }

    /**
     * Periodic safety net for instructions that replace blocks without taking a snapshot.
     * Runs on the client only and refreshes at most once every {@value #TICK_INTERVAL} ticks.
     */
    public static synchronized void onWorldTicked(PonderWorld world) {
        if (world == null || !isActive()) {
            return;
        }
        if (++tickCounter % TICK_INTERVAL != 0) {
            return;
        }
        apply(world);
    }

    private static boolean isActive() {
        try {
            if (!FMLCommonHandler.instance().getSide().isClient()) {
                return false;
            }
        } catch (Throwable unavailable) {
            // Tests and early lifecycle stages may not have a Forge runtime yet.
            return false;
        }
        return modLoaded(MEKANISM_MOD_ID) || modLoaded(ENDER_IO_MOD_ID)
            || modLoaded(THERMAL_DYNAMICS_MOD_ID);
    }

    private static boolean modLoaded(String modId) {
        try {
            return Loader.isModLoaded(modId);
        } catch (Throwable unavailable) {
            return false;
        }
    }

    private static int apply(PonderWorld world) {
        int changed = 0;
        Map<BlockPos, NBTTagCompound> eioData = EIO_TILE_DATA.get(world);
        for (TileEntity tile : world.getTileEntities()) {
            try {
                if (MekanismConduitSupport.isTransmitter(tile.getClass())
                    && MekanismConduitSupport.refresh(tile)) {
                    changed++;
                }
                if (EnderIoConduitSupport.isBundle(tile.getClass())) {
                    NBTTagCompound stored = eioData == null ? null : eioData.get(tile.getPos());
                    if (EnderIoConduitSupport.restore(tile, stored)) {
                        changed++;
                    }
                }
                if (ThermalDynamicsDuctSupport.isDuct(tile.getClass())
                    && ThermalDynamicsDuctSupport.refresh(tile)) {
                    changed++;
                }
            } catch (Throwable failure) {
                report(failure);
            }
        }
        if (changed > 0) {
            invalidateRenderCaches(world);
            EnderIoConduitSupport.invalidateModelCaches(world);
        }
        return changed;
    }

    /** Invalidated reflectively so this class stays safe to load on a dedicated server. */
    private static void invalidateRenderCaches(PonderWorld world) {
        try {
            Class<?> cache = Class.forName(SECTION_RENDER_CACHE, true,
                world.getClass().getClassLoader());
            Method invalidateAll = cache.getMethod("invalidateAll");
            invalidateAll.invoke(null);
        } catch (Throwable unavailable) {
            // The render cache is client-only; nothing to invalidate here.
        }
    }

    private static void report(Throwable failure) {
        if (failureReported) {
            return;
        }
        failureReported = true;
        Ponder.LOGGER.warn(
            "Conduit rendering compatibility failed; Ponder will continue without it", failure);
    }
}
