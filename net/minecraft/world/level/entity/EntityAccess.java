package net.minecraft.world.level.entity;

import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AxisAlignedBB;

public interface EntityAccess {
    int getId();

    UUID getUniqueID();

    BlockPosition getChunkCoordinates();

    AxisAlignedBB getBoundingBox();

    void setWorldCallback(IEntityCallback listener);

    Stream<? extends EntityAccess> recursiveStream();

    Stream<? extends EntityAccess> getPassengersAndSelf();

    void setRemoved(Entity.RemovalReason reason);

    boolean shouldBeSaved();

    boolean isAlwaysTicking();
}
