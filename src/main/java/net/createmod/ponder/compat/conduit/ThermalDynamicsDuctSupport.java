package net.createmod.ponder.compat.conduit;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.createmod.ponder.Ponder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Rebuilds Thermal Dynamics client connection types inside Ponder scenes.
 *
 * <p>Thermal Dynamics computes the connection mask on the server and ships it to clients in the
 * tile update packet. The client renderer reads only {@code TileGrid.clientConnections}, which is
 * null after Ponder restores a tile from persistent NBT, so every duct side renders as
 * {@code ConnectionType.NONE}. This support repopulates the duct unit neighbour caches from the
 * surrounding virtual world and recomputes the same values the server packet would have sent.</p>
 *
 * <p>Thermal Dynamics is optional and touched only through reflection.</p>
 */
final class ThermalDynamicsDuctSupport {
    private static final String TILE_GRID =
        "cofh.thermaldynamics.duct.tiles.TileGrid";
    private static final String DUCT_UNIT =
        "cofh.thermaldynamics.duct.tiles.DuctUnit";
    private static final String IDUCT_HOLDER =
        "cofh.thermaldynamics.duct.tiles.IDuctHolder";
    private static final String ATTACHMENT =
        "cofh.thermaldynamics.duct.Attachment";
    private static final int SIDES = 6;

    private static final TileAccess NONE = new TileAccess();
    private static final Map<Class<?>, TileAccess> ACCESS =
        new HashMap<Class<?>, TileAccess>();

    private static boolean failureReported;

    private ThermalDynamicsDuctSupport() {
    }

    static boolean isDuct(Class<?> type) {
        return access(type) != null;
    }

    static boolean refresh(TileEntity tile) throws Exception {
        TileAccess access = access(tile.getClass());
        if (access == null) {
            return false;
        }
        World world = tile.getWorld();
        if (world == null) {
            return false;
        }
        BlockPos pos = tile.getPos();
        TileEntity[] tiles = new TileEntity[SIDES];
        Object holders = Array.newInstance(access.holderType, SIDES);
        for (int side = 0; side < SIDES; side++) {
            TileEntity neighbor = world.getTileEntity(pos.offset(EnumFacing.VALUES[side]));
            tiles[side] = neighbor;
            if (neighbor != null && access.holderType.isInstance(neighbor)) {
                Array.set(holders, side, neighbor);
            }
        }
        Iterable<?> units = (Iterable<?>) access.getDuctUnits.invoke(tile);
        if (units == null) {
            return false;
        }
        for (Object unit : units) {
            access.updateAllSides.invoke(unit, tiles, holders);
        }
        Object[] visual = (Object[]) Array.newInstance(access.connectionType, SIDES);
        Arrays.fill(visual, access.none);
        for (int side = 0; side < SIDES; side++) {
            Object attachment = access.getAttachment.invoke(tile, Integer.valueOf(side));
            if (attachment != null) {
                Object type = access.getNeighborType.invoke(attachment);
                if (type != null) {
                    visual[side] = type;
                }
                continue;
            }
            Object best = access.none;
            for (Object unit : units) {
                Object type = access.getRenderConnectionType.invoke(unit, Integer.valueOf(side));
                if (type instanceof Enum
                    && ((Enum<?>) type).ordinal() > ((Enum<?>) best).ordinal()) {
                    best = type;
                }
            }
            visual[side] = best;
        }
        Object current = access.clientConnections.get(tile);
        if (sameConnections(current, visual)) {
            return false;
        }
        access.clientConnections.set(tile, visual);
        return true;
    }

    private static boolean sameConnections(Object current, Object[] next) {
        if (!(current instanceof Object[]) || ((Object[]) current).length != next.length) {
            return false;
        }
        Object[] existing = (Object[]) current;
        for (int side = 0; side < next.length; side++) {
            if (existing[side] != next[side]) {
                return false;
            }
        }
        return true;
    }

    private static synchronized TileAccess access(Class<?> type) {
        TileAccess cached = ACCESS.get(type);
        if (cached != null) {
            return cached == NONE ? null : cached;
        }
        Class<?> owner = null;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (TILE_GRID.equals(current.getName())) {
                owner = current;
                break;
            }
        }
        if (owner == null) {
            ACCESS.put(type, NONE);
            return null;
        }
        try {
            ClassLoader loader = owner.getClassLoader();
            Class<?> holderType = Class.forName(IDUCT_HOLDER, false, loader);
            Class<?> ductUnitType = Class.forName(DUCT_UNIT, false, loader);
            Class<?> attachmentType = Class.forName(ATTACHMENT, false, loader);
            Field clientConnections = owner.getField("clientConnections");
            Class<?> connectionType = clientConnections.getType().getComponentType();
            Object none = null;
            for (Object constant : connectionType.getEnumConstants()) {
                if ("NONE".equals(((Enum<?>) constant).name())) {
                    none = constant;
                    break;
                }
            }
            if (none == null) {
                throw new IllegalStateException("ConnectionType.NONE is missing");
            }
            TileAccess access = new TileAccess(
                owner.getMethod("getDuctUnits"),
                owner.getMethod("getAttachment", int.class),
                attachmentType.getMethod("getNeighborType"),
                ductUnitType.getMethod("updateAllSides", TileEntity[].class,
                    Array.newInstance(holderType, 0).getClass()),
                ductUnitType.getMethod("getRenderConnectionType", int.class),
                clientConnections, holderType, connectionType, none);
            ACCESS.put(type, access);
            return access;
        } catch (Throwable failure) {
            ACCESS.put(type, NONE);
            if (!failureReported) {
                failureReported = true;
                Ponder.LOGGER.warn(
                    "Thermal Dynamics duct connection compatibility is unavailable for "
                        + type.getName(), failure);
            }
            return null;
        }
    }

    private static final class TileAccess {
        final Method getDuctUnits;
        final Method getAttachment;
        final Method getNeighborType;
        final Method updateAllSides;
        final Method getRenderConnectionType;
        final Field clientConnections;
        final Class<?> holderType;
        final Class<?> connectionType;
        final Object none;

        TileAccess() {
            this(null, null, null, null, null, null, null, null, null);
        }

        TileAccess(Method getDuctUnits, Method getAttachment, Method getNeighborType,
                   Method updateAllSides, Method getRenderConnectionType,
                   Field clientConnections, Class<?> holderType, Class<?> connectionType,
                   Object none) {
            this.getDuctUnits = getDuctUnits;
            this.getAttachment = getAttachment;
            this.getNeighborType = getNeighborType;
            this.updateAllSides = updateAllSides;
            this.getRenderConnectionType = getRenderConnectionType;
            this.clientConnections = clientConnections;
            this.holderType = holderType;
            this.connectionType = connectionType;
            this.none = none;
        }
    }
}
