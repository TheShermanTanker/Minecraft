package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;

public class RegistryBlocks<T> extends RegistryMaterials<T> {
    private final MinecraftKey defaultKey;
    private T defaultValue;

    public RegistryBlocks(String defaultId, ResourceKey<? extends IRegistry<T>> key, Lifecycle lifecycle) {
        super(key, lifecycle);
        this.defaultKey = new MinecraftKey(defaultId);
    }

    @Override
    public <V extends T> V registerMapping(int rawId, ResourceKey<T> key, V entry, Lifecycle lifecycle) {
        if (this.defaultKey.equals(key.location())) {
            this.defaultValue = (T)entry;
        }

        return super.registerMapping(rawId, key, entry, lifecycle);
    }

    @Override
    public int getId(@Nullable T entry) {
        int i = super.getId(entry);
        return i == -1 ? super.getId(this.defaultValue) : i;
    }

    @Nonnull
    @Override
    public MinecraftKey getKey(T entry) {
        MinecraftKey resourceLocation = super.getKey(entry);
        return resourceLocation == null ? this.defaultKey : resourceLocation;
    }

    @Nonnull
    @Override
    public T get(@Nullable MinecraftKey id) {
        T object = super.get(id);
        return (T)(object == null ? this.defaultValue : object);
    }

    @Override
    public Optional<T> getOptional(@Nullable MinecraftKey id) {
        return Optional.ofNullable(super.get(id));
    }

    @Nonnull
    @Override
    public T fromId(int index) {
        T object = super.fromId(index);
        return (T)(object == null ? this.defaultValue : object);
    }

    @Nonnull
    @Override
    public T getRandom(Random random) {
        T object = super.getRandom(random);
        return (T)(object == null ? this.defaultValue : object);
    }

    public MinecraftKey getDefaultKey() {
        return this.defaultKey;
    }
}
