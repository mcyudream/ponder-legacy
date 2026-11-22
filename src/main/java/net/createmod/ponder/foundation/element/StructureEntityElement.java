package net.createmod.ponder.foundation.element;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.foundation.PonderElementFactories;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/** Schematic entities follow section visibility; explicitly created entities retain their own timeline. */
public final class StructureEntityElement implements EntityElement {
    private final PonderScene scene;
    private final EntityElement delegate;
    private final BlockPos anchor;

    public StructureEntityElement(PonderScene scene, Entity entity) {
        this.scene = scene;
        delegate = PonderElementFactories.get().createEntity(entity);
        anchor = anchor(scene.getWorld(), entity);
    }

    public static BlockPos anchor(@Nullable PonderWorld world, Entity entity) {
        BlockPos cell = new BlockPos(entity.posX, entity.posY, entity.posZ);
        // Sparks and similar attachments occupy air immediately above their supporting block.
        return world != null && world.isAirBlock(cell) && !world.isAirBlock(cell.down()) ? cell.down() : cell;
    }

    public BlockPos getAnchor() { return anchor; }

    @Nullable
    private WorldSectionElement section() {
        for (PonderElement element : scene.getElements())
            if (element instanceof WorldSectionElement section && section.isVisible() && section.contains(anchor))
                return section;
        return null;
    }

    @Override public boolean isVisible() { return delegate.isVisible() && section() != null; }
    @Override public void setVisible(boolean visible) { delegate.setVisible(visible); }
    @Override public void ifPresent(Consumer<Entity> consumer) { delegate.ifPresent(consumer); }
    @Override public boolean isStillValid(Entity entity) { return delegate.isStillValid(entity); }
    @Override public void tick(PonderScene scene) { delegate.tick(scene); }
    @Override public void whileSkipping(PonderScene scene) { delegate.whileSkipping(scene); }
    @Override public void reset(PonderScene scene) { delegate.reset(scene); }
    @Override public Object captureState() { return delegate.captureState(); }
    @Override public void restoreState(Object state) { delegate.restoreState(state); }

    @Override
    public void renderLast(PonderWorld world, float partialTicks) {
        WorldSectionElement section = section();
        if (delegate.isVisible() && section != null) section.renderAttachedEntity(delegate, world, partialTicks);
    }
}
