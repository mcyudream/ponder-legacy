package net.createmod.ponder.compat.ic2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.foundation.PonderWorld;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;

/**
 * Completes IC2 cable state inside Ponder's virtual world.
 *
 * <p>IC2's {@code TileEntityBlock.validate()} only marks the tile as loading and schedules
 * {@code onLoaded()} for a later world tick. Ponder's virtual world is not ticked by IC2's
 * world tick handler, so cable tiles never build their client render state. Without that
 * state {@code CableModel} falls back to the missing model, which is the purple/black cube.</p>
 *
 * <p>The cable connection mask is also server state: IC2 computes {@code connectivity} through
 * its energy net and sends it to clients with the tile update packet, which a Ponder scene never
 * receives. Every cable would therefore render as an isolated segment. This support reproduces
 * the server calculation against the Ponder world's neighbours using the same IC2 API calls.
 * Detector cables read the mask from this same field, so no per-cable special case is needed.</p>
 *
 * <p>Everything is reflective and client-only, and only IC2 cable tiles are touched.</p>
 */
public final class Ic2CableSupport {
    private static final String IC2_MOD_ID = "ic2";
    private static final String CABLE = "ic2.core.block.wiring.TileEntityCable";
    private static final String TILE_ENTITY_BLOCK = "ic2.core.block.TileEntityBlock";
    private static final String ENERGY_TILE = "ic2.api.energy.tile.IEnergyTile";
    private static final String ENERGY_ACCEPTOR = "ic2.api.energy.tile.IEnergyAcceptor";
    private static final String ENERGY_EMITTER = "ic2.api.energy.tile.IEnergyEmitter";
    private static final String SECTION_RENDER_CACHE =
        "net.createmod.ponder.render.SectionRenderCache";
    private static final byte LOADING = 1;
    private static final int TICK_INTERVAL = 10;

    private static final Access NONE = new Access(null, null, null, null, null, null);
    private static final Map<Class<?>, Access> ACCESS = new HashMap<Class<?>, Access>();

    private static Boolean active;
    private static boolean apiResolved;
    private static EnergyApi api;
    private static boolean failureReported;
    private static int tickCounter;

    private Ic2CableSupport() {
    }

    /** Called after a tile entity has been added to the Ponder world. */
    public static void onTileEntityAdded(TileEntity tile) {
        if (tile == null || !isActive()) {
            return;
        }
        Access access = access(tile.getClass());
        if (access == null) {
            return;
        }
        try {
            if (access.loadState.getByte(tile) != LOADING) {
                return;
            }
            access.onLoaded.invoke(tile);
        } catch (Throwable failure) {
            report(failure);
        }
    }

    /**
     * Recomputes cable connection masks after a structure was placed or a snapshot restored.
     *
     * <p>Connectivity depends on neighbours, so it cannot be computed while the structure is
     * placed tile by tile: the other end of a cable may not exist yet.</p>
     */
    public static synchronized void onWorldChanged(PonderWorld world) {
        if (world == null || !isActive()) {
            return;
        }
        int changed = 0;
        for (TileEntity tile : world.getTileEntities()) {
            if (tile == null) {
                continue;
            }
            Access access = access(tile.getClass());
            if (access == null || access.connectivity == null) {
                continue;
            }
            try {
                if (refreshConnectivity(world, tile, access)) {
                    changed++;
                }
            } catch (Throwable failure) {
                report(failure);
            }
        }
        if (changed > 0) {
            invalidateRenderCaches();
        }
    }

    /** Periodic safety net for scenes that change blocks after the initial structure. */
    public static synchronized void onWorldTicked(PonderWorld world) {
        if (world == null || !isActive()) {
            return;
        }
        if (++tickCounter % TICK_INTERVAL != 0) {
            return;
        }
        onWorldChanged(world);
    }

    private static boolean refreshConnectivity(PonderWorld world, TileEntity cable, Access access)
        throws Exception {
        EnergyApi energy = api();
        if (energy == null || access.canInteractWith == null) {
            return false;
        }
        int mask = 0;
        int bit = 1;
        BlockPos pos = cable.getPos();
        // IC2 walks EnumFacing.VALUES and shifts one bit per entry, so the order is part of the mask.
        for (EnumFacing side : EnumFacing.VALUES) {
            if (bit > 0x20) {
                break;
            }
            try {
                TileEntity neighbour = world.getTileEntity(pos.offset(side));
                if (neighbour != null && energy.tile.isInstance(neighbour)) {
                    EnumFacing opposite = side.getOpposite();
                    boolean linked = false;
                    if (energy.acceptor.isInstance(neighbour)
                        && Boolean.TRUE.equals(energy.accepts.invoke(neighbour, cable, opposite))) {
                        linked = true;
                    } else if (energy.emitter.isInstance(neighbour)
                        && Boolean.TRUE.equals(energy.emits.invoke(neighbour, cable, opposite))) {
                        linked = true;
                    }
                    if (linked
                        && Boolean.TRUE.equals(access.canInteractWith.invoke(cable, neighbour, side))) {
                        mask |= bit;
                    }
                }
            } catch (Throwable neighbourFailure) {
                // One incompatible neighbour must not hide the remaining connections.
            }
            bit <<= 1;
        }
        byte previous = access.connectivity.getByte(cable);
        if (previous == (byte) mask && access.renderState.get(cable) != null) {
            return false;
        }
        access.connectivity.setByte(cable, (byte) mask);
        access.updateRenderState.invoke(cable);
        return true;
    }

    private static boolean isActive() {
        if (Boolean.TRUE.equals(active)) {
            return true;
        }
        try {
            if (!FMLCommonHandler.instance().getSide().isClient()
                || !Loader.isModLoaded(IC2_MOD_ID)) {
                return false;
            }
            active = Boolean.TRUE;
            return true;
        } catch (Throwable unavailable) {
            return false;
        }
    }

    private static synchronized Access access(Class<?> type) {
        Access cached = ACCESS.get(type);
        if (cached != null) {
            return cached == NONE ? null : cached;
        }
        Class<?> cable = null;
        Class<?> owner = null;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (CABLE.equals(current.getName())) {
                cable = current;
            }
            if (TILE_ENTITY_BLOCK.equals(current.getName())) {
                owner = current;
            }
        }
        if (cable == null || owner == null) {
            ACCESS.put(type, NONE);
            return null;
        }
        try {
            Field loadState = owner.getDeclaredField("loadState");
            loadState.setAccessible(true);
            Field connectivity = cable.getDeclaredField("connectivity");
            connectivity.setAccessible(true);
            Field renderState = cable.getDeclaredField("renderState");
            renderState.setAccessible(true);
            Method onLoaded = method(cable, "onLoaded");
            Method updateRenderState = method(cable, "updateRenderState");
            EnergyApi energy = api();
            Method canInteractWith = energy == null ? null
                : method(cable, "canInteractWith", energy.tile, EnumFacing.class);
            Access access = new Access(loadState, onLoaded, connectivity, renderState,
                updateRenderState, canInteractWith);
            ACCESS.put(type, access);
            return access;
        } catch (Throwable failure) {
            ACCESS.put(type, NONE);
            report(failure);
            return null;
        }
    }

    private static synchronized EnergyApi api() {
        if (apiResolved) {
            return api;
        }
        apiResolved = true;
        try {
            ClassLoader loader = Ic2CableSupport.class.getClassLoader();
            Class<?> tile = Class.forName(ENERGY_TILE, false, loader);
            Class<?> acceptor = Class.forName(ENERGY_ACCEPTOR, false, loader);
            Class<?> emitter = Class.forName(ENERGY_EMITTER, false, loader);
            api = new EnergyApi(tile, acceptor, emitter,
                acceptor.getMethod("acceptsEnergyFrom", emitter, EnumFacing.class),
                emitter.getMethod("emitsEnergyTo", acceptor, EnumFacing.class));
        } catch (Throwable unavailable) {
            api = null;
        }
        return api;
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters)
        throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // keep looking in the superclass
            }
        }
        throw new NoSuchMethodException(name);
    }

    /** Invalidated reflectively so this class stays safe to load on a dedicated server. */
    private static void invalidateRenderCaches() {
        try {
            Class<?> cache = Class.forName(SECTION_RENDER_CACHE, true,
                Ic2CableSupport.class.getClassLoader());
            cache.getMethod("invalidateAll").invoke(null);
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
            "IC2 cable compatibility failed; Ponder will continue without it", failure);
    }

    private static final class Access {
        final Field loadState;
        final Method onLoaded;
        final Field connectivity;
        final Field renderState;
        final Method updateRenderState;
        final Method canInteractWith;

        Access(Field loadState, Method onLoaded, Field connectivity, Field renderState,
            Method updateRenderState, Method canInteractWith) {
            this.loadState = loadState;
            this.onLoaded = onLoaded;
            this.connectivity = connectivity;
            this.renderState = renderState;
            this.updateRenderState = updateRenderState;
            this.canInteractWith = canInteractWith;
        }
    }

    private static final class EnergyApi {
        final Class<?> tile;
        final Class<?> acceptor;
        final Class<?> emitter;
        final Method accepts;
        final Method emits;

        EnergyApi(Class<?> tile, Class<?> acceptor, Class<?> emitter, Method accepts, Method emits) {
            this.tile = tile;
            this.acceptor = acceptor;
            this.emitter = emitter;
            this.accepts = accepts;
            this.emits = emits;
        }
    }
}
