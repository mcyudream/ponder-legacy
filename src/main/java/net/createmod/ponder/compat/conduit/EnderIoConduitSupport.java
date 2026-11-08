package net.createmod.ponder.compat.conduit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import net.createmod.ponder.foundation.PonderWorld;
import net.createmod.ponder.foundation.structure.PonderStructure;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Restores Ender IO conduit data inside Ponder scenes.
 *
 * <p>Ender IO reads the conduit list into {@code clientConduits}, but its
 * {@code onBeforeNbtWrite()} only serializes {@code serverConduits}. Ponder snapshots the scene
 * right after placement and then restores it, so the renderer sees empty conduit bundles. The
 * original conduit NBT is captured from the structure before that first snapshot and injected
 * back into both lists, which also makes later snapshots and projections keep the data.</p>
 */
final class EnderIoConduitSupport {
    private static final String BUNDLE =
        "crazypants.enderio.conduits.conduit.TileConduitBundle";
    private static final String CONDUIT_UTIL =
        "crazypants.enderio.base.conduit.ConduitUtil";
    private static final int MAX_CONDUITS = 16;

    private EnderIoConduitSupport() {
    }

    /** Collects the stored conduit NBT of every Ender IO bundle in the source structure. */
    static Map<BlockPos, NBTTagCompound> collectTileData(PonderStructure structure) {
        Map<BlockPos, NBTTagCompound> data = new HashMap<BlockPos, NBTTagCompound>();
        for (PonderStructure.BlockInfo block : structure.getBlocks()) {
            NBTTagCompound tag = block.getTileData();
            if (tag != null && isConduitBundleId(tag.getString("id"))) {
                data.put(block.getPosition(), tag);
            }
        }
        return data;
    }

    private static boolean isConduitBundleId(String id) {
        return id.startsWith("enderio:") && id.contains("conduit_bundle");
    }

    static boolean isBundle(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (BUNDLE.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rebuilds the client conduit list from the captured structure data, falling back to the
     * server list when a snapshot already preserved it.
     *
     * @return true when the bundle was changed
     */
    static boolean restore(TileEntity tile, NBTTagCompound stored) throws Exception {
        Field clientField = field(tile.getClass(), "clientConduits");
        Field serverField = field(tile.getClass(), "serverConduits");
        Object client = clientField.get(tile);
        Object server = serverField.get(tile);
        List<Object> clientConduits = new ArrayList<Object>();
        if (client instanceof Collection) {
            clientConduits.addAll((Collection<?>) client);
        }
        if (!clientConduits.isEmpty()) {
            // Keep the list NBT serialization writes to, so the next snapshot or projection
            // capture preserves the conduits instead of dropping them again.
            return mirrorTowardsServer(clientConduits, server);
        }
        List<Object> conduits = new ArrayList<Object>();
        if (stored != null) {
            for (NBTTagCompound element : conduitElements(stored)) {
                Object conduit = readConduit(element, tile);
                if (conduit != null) {
                    conduits.add(conduit);
                }
            }
        }
        if (conduits.isEmpty() && server instanceof Collection) {
            for (Object conduit : (Collection<?>) server) {
                bind(conduit, tile);
                conduits.add(conduit);
            }
        }
        if (conduits.isEmpty()) {
            return false;
        }
        clientField.set(tile, new CopyOnWriteArrayList<Object>(conduits));
        mirrorTowardsServer(conduits, server);
        try {
            tile.getClass().getMethod("dirty").invoke(tile);
        } catch (ReflectiveOperationException ignored) {
            // best effort
        }
        return true;
    }

    private static boolean mirrorTowardsServer(List<Object> conduits, Object server) {
        if (!(server instanceof Collection)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Collection<Object> serverConduits = (Collection<Object>) server;
        if (serverConduits.size() == conduits.size()) {
            boolean same = true;
            int index = 0;
            for (Object conduit : serverConduits) {
                if (conduit != conduits.get(index++)) {
                    same = false;
                    break;
                }
            }
            if (same) {
                return false;
            }
        }
        serverConduits.clear();
        serverConduits.addAll(conduits);
        return true;
    }

    /** Invalidates Ender IO's cached conduit models so the restored list is rendered. */
    static void invalidateModelCaches(PonderWorld world) {
        ClassLoader loader = world.getClass().getClassLoader();
        invalidateStaticCache("crazypants.enderio.conduits.render.BlockStateWrapperConduitBundle",
            loader);
        invalidateStaticCache("crazypants.enderio.base.render.pipeline.BlockStateWrapperBase",
            loader);
    }

    private static Object readConduit(NBTTagCompound element, TileEntity tile) throws Exception {
        Class<?> util = Class.forName(CONDUIT_UTIL, true, tile.getClass().getClassLoader());
        Method read = null;
        for (Method method : util.getMethods()) {
            if ("readConduitFromNBT".equals(method.getName())
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == NBTTagCompound.class) {
                read = method;
                break;
            }
        }
        if (read == null) {
            throw new NoSuchMethodException(CONDUIT_UTIL + "#readConduitFromNBT");
        }
        Object conduit = read.invoke(null, element);
        if (conduit != null) {
            bind(conduit, tile);
        }
        return conduit;
    }

    private static void bind(Object conduit, TileEntity bundle) {
        try {
            for (Method method : conduit.getClass().getMethods()) {
                if ("setBundle".equals(method.getName()) && method.getParameterCount() == 1) {
                    method.invoke(conduit, bundle);
                    return;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // the conduit keeps its previous bundle reference
        }
    }

    private static List<NBTTagCompound> conduitElements(NBTTagCompound tile) {
        List<NBTTagCompound> result = new ArrayList<NBTTagCompound>();
        if (tile.hasKey("conduits", 10)) {
            NBTTagCompound conduits = tile.getCompoundTag("conduits");
            int size = Math.max(0, Math.min(conduits.getInteger("size"), MAX_CONDUITS));
            for (int i = 0; i < size; i++) {
                String key = Integer.toString(i);
                if (conduits.hasKey(key, 10)) {
                    result.add(conduits.getCompoundTag(key));
                }
            }
            if (result.isEmpty()) {
                // Some AutoSave builds do not write a usable size; probe the indexed keys.
                for (int i = 0; i < MAX_CONDUITS; i++) {
                    String key = Integer.toString(i);
                    if (!conduits.hasKey(key, 10)) {
                        break;
                    }
                    result.add(conduits.getCompoundTag(key));
                }
            }
        }
        if (result.isEmpty() && tile.hasKey("conduits", 9)) {
            NBTTagList list = tile.getTagList("conduits", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                result.add(list.getCompoundTagAt(i));
            }
        }
        return result;
    }

    private static void invalidateStaticCache(String className, ClassLoader loader) {
        try {
            Class<?> type = Class.forName(className, true, loader);
            Field field = type.getDeclaredField("cache");
            field.setAccessible(true);
            Object cache = field.get(null);
            if (cache != null) {
                Method invalidateAll = cache.getClass().getMethod("invalidateAll");
                invalidateAll.setAccessible(true);
                invalidateAll.invoke(cache);
            }
        } catch (Throwable ignored) {
            // The cache class or field is absent in this Ender IO build.
        }
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null && current != Object.class;
             current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // keep looking in the superclass
            }
        }
        throw new NoSuchFieldException(name);
    }
}
