package net.createmod.ponder.compat.conduit;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ConduitRenderCompatTest {
    @Test
    public void ignoresMissingWorld() {
        ConduitRenderCompat.onStructurePlaced(null, null);
        ConduitRenderCompat.onWorldRestored(null);
        ConduitRenderCompat.onWorldTicked(null);
    }

    @Test
    public void doesNotMatchUnrelatedTypes() {
        assertFalse(MekanismConduitSupport.isTransmitter(String.class));
        assertFalse(EnderIoConduitSupport.isBundle(String.class));
        assertFalse(ThermalDynamicsDuctSupport.isDuct(String.class));
    }
}
