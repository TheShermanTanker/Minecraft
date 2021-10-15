package net.minecraft.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.World;

public final class GlobalPos {
    public static final Codec<GlobalPos> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(World.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(GlobalPos::getDimensionManager), BlockPosition.CODEC.fieldOf("pos").forGetter(GlobalPos::getBlockPosition)).apply(instance, GlobalPos::create);
    });
    private final ResourceKey<World> dimension;
    private final BlockPosition pos;

    private GlobalPos(ResourceKey<World> dimension, BlockPosition pos) {
        this.dimension = dimension;
        this.pos = pos;
    }

    public static GlobalPos create(ResourceKey<World> dimension, BlockPosition pos) {
        return new GlobalPos(dimension, pos);
    }

    public ResourceKey<World> getDimensionManager() {
        return this.dimension;
    }

    public BlockPosition getBlockPosition() {
        return this.pos;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            GlobalPos globalPos = (GlobalPos)object;
            return Objects.equals(this.dimension, globalPos.dimension) && Objects.equals(this.pos, globalPos.pos);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.dimension, this.pos);
    }

    @Override
    public String toString() {
        return this.dimension + " " + this.pos;
    }
}
