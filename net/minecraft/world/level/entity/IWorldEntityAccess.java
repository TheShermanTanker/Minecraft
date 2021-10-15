package net.minecraft.world.level.entity;

import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.world.phys.AxisAlignedBB;

public interface IWorldEntityAccess<T extends EntityAccess> {
    @Nullable
    T get(int id);

    @Nullable
    T get(UUID uuid);

    Iterable<T> getAll();

    <U extends T> void get(EntityTypeTest<T, U> filter, Consumer<U> action);

    void get(AxisAlignedBB box, Consumer<T> action);

    <U extends T> void get(EntityTypeTest<T, U> filter, AxisAlignedBB box, Consumer<U> action);
}
