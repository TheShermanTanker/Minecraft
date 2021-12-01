package net.minecraft.world.level.levelgen.material;

import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.NoiseChunk;

public interface WorldGenMaterialRule {
    @Nullable
    IBlockData apply(NoiseChunk sampler, int x, int y, int z);
}
