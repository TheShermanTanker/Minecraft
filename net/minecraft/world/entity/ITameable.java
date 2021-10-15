package net.minecraft.world.entity;

import java.util.UUID;
import javax.annotation.Nullable;

public interface ITameable {
    @Nullable
    UUID getOwnerUUID();

    @Nullable
    Entity getOwner();
}
