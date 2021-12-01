package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.core.BlockPosition;
import net.minecraft.resources.MinecraftKey;

public interface PositionalRandomFactory {
    default RandomSource at(BlockPosition pos) {
        return this.at(pos.getX(), pos.getY(), pos.getZ());
    }

    default RandomSource fromHashOf(MinecraftKey id) {
        return this.fromHashOf(id.toString());
    }

    RandomSource fromHashOf(String string);

    RandomSource at(int x, int y, int z);

    @VisibleForTesting
    void parityConfigString(StringBuilder info);
}
