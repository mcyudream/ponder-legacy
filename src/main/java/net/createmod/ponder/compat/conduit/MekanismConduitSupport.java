package net.createmod.ponder.compat.conduit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.createmod.ponder.Ponder;
import net.minecraft.tileentity.TileEntity;

/**
 * Recomputes Mekanism's transient transmitter and acceptor connection masks.
 *
 * <p>Mekanism derives the real masks from the surrounding blocks when its transmitter updates.
 * Ponder restores tile entities straight from persistent NBT, so the masks stay at zero and the
 * client renders every cable as an isolated stub. The mod is accessed reflectively because it is
 * an optional dependency.</p>
 */
final class MekanismConduitSupport {
    private static final String PIPE_BASE =
        "mekanism.common.tile.transmitter.TileEntitySidedPipe";
    private static final PipeAccess NONE = new PipeAccess(null, null, null, null);
    private static final Map<Class<?>, PipeAccess> ACCESS =
        new HashMap<Class<?>, PipeAccess>();

    private static boolean failureReported;
    private static boolean acceptorQueryUnavailable;

    private MekanismConduitSupport() {
    }

    static boolean isTransmitter(Class<?> type) {
        return access(type) != null;
    }

    static boolean refresh(TileEntity tile) throws Exception {
        PipeAccess access = access(tile.getClass());
        if (access == null) {
            return false;
        }
        boolean changed = false;
        byte previousTransmitters = access.transmitterField.getByte(tile);
        try {
            byte transmitters = ((Number) access.transmitters.invoke(tile)).byteValue();
            if (transmitters != previousTransmitters) {
                access.transmitterField.setByte(tile, transmitters);
                changed = true;
            }
        } catch (Throwable unavailable) {
            // Transmitter connections do not depend on other mods; keep the previous mask.
        }
        // With IC2 installed Mekanism's acceptor check reaches IC2's server-only energy net,
        // which throws on a client. The transmitter mask above is still correct, so only the
        // acceptor half is skipped instead of losing both.
        if (!acceptorQueryUnavailable) {
            byte previousAcceptors = access.acceptorField.getByte(tile);
            try {
                byte acceptors = ((Number) access.acceptors.invoke(tile)).byteValue();
                if (acceptors != previousAcceptors) {
                    access.acceptorField.setByte(tile, acceptors);
                    changed = true;
                }
            } catch (InvocationTargetException failure) {
                if (isClientSideUnavailable(failure.getCause())) {
                    acceptorQueryUnavailable = true;
                    Ponder.LOGGER.debug(
                        "Mekanism acceptor connections need a server-side energy net; "
                            + "Ponder will sync transmitter connections only");
                }
            } catch (Throwable unavailable) {
                // Keep the previous acceptor mask.
            }
        }
        return changed;
    }

    /** Matches IC2's {@code EnergyNetGlobal#getLocal} rejection on a physical client. */
    private static boolean isClientSideUnavailable(Throwable cause) {
        return cause instanceof IllegalStateException
            && "not applicable clientside".equals(cause.getMessage());
    }

    private static synchronized PipeAccess access(Class<?> type) {
        PipeAccess cached = ACCESS.get(type);
        if (cached != null) {
            return cached == NONE ? null : cached;
        }
        Class<?> owner = null;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (PIPE_BASE.equals(current.getName())) {
                owner = current;
                break;
            }
        }
        if (owner == null) {
            ACCESS.put(type, NONE);
            return null;
        }
        try {
            PipeAccess access = new PipeAccess(
                method(owner, "getPossibleTransmitterConnections"),
                method(owner, "getPossibleAcceptorConnections"),
                field(owner, "currentTransmitterConnections"),
                field(owner, "currentAcceptorConnections"));
            ACCESS.put(type, access);
            return access;
        } catch (Throwable failure) {
            ACCESS.put(type, NONE);
            if (!failureReported) {
                failureReported = true;
                Ponder.LOGGER.warn(
                    "Mekanism conduit connection compatibility is unavailable for "
                        + type.getName(), failure);
            }
            return null;
        }
    }

    private static Method method(Class<?> owner, String name) throws NoSuchMethodException {
        try {
            return owner.getMethod(name);
        } catch (NoSuchMethodException missing) {
            Method method = owner.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        }
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        for (Class<?> current = owner; current != null && current != Object.class;
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

    private static final class PipeAccess {
        final Method transmitters;
        final Method acceptors;
        final Field transmitterField;
        final Field acceptorField;

        PipeAccess(Method transmitters, Method acceptors,
                   Field transmitterField, Field acceptorField) {
            this.transmitters = transmitters;
            this.acceptors = acceptors;
            this.transmitterField = transmitterField;
            this.acceptorField = acceptorField;
        }
    }
}
