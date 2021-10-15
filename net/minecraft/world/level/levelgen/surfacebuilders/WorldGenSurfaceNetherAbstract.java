package net.minecraft.world.level.levelgen.surfacebuilders;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Codec;
import java.util.Comparator;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorOctaves;

public abstract class WorldGenSurfaceNetherAbstract extends WorldGenSurface<WorldGenSurfaceConfigurationBase> {
    private long seed;
    private ImmutableMap<IBlockData, NoiseGeneratorOctaves> floorNoises = ImmutableMap.of();
    private ImmutableMap<IBlockData, NoiseGeneratorOctaves> ceilingNoises = ImmutableMap.of();
    private NoiseGeneratorOctaves patchNoise;

    public WorldGenSurfaceNetherAbstract(Codec<WorldGenSurfaceConfigurationBase> codec) {
        super(codec);
    }

    @Override
    public void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l, WorldGenSurfaceConfigurationBase surfaceBuilderBaseConfiguration) {
        int j = seaLevel + 1;
        int k = x & 15;
        int m = z & 15;
        int n = (int)(noise / 3.0D + 3.0D + random.nextDouble() * 0.25D);
        int o = (int)(noise / 3.0D + 3.0D + random.nextDouble() * 0.25D);
        double d = 0.03125D;
        boolean bl = this.patchNoise.getValue((double)x * 0.03125D, 109.0D, (double)z * 0.03125D) * 75.0D + random.nextDouble() > 0.0D;
        IBlockData blockState = this.ceilingNoises.entrySet().stream().max(Comparator.comparing((entry) -> {
            return entry.getValue().getValue((double)x, (double)seaLevel, (double)z);
        })).get().getKey();
        IBlockData blockState2 = this.floorNoises.entrySet().stream().max(Comparator.comparing((entry) -> {
            return entry.getValue().getValue((double)x, (double)seaLevel, (double)z);
        })).get().getKey();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        IBlockData blockState3 = chunk.getType(mutableBlockPos.set(k, 128, m));

        for(int p = 127; p >= i; --p) {
            mutableBlockPos.set(k, p, m);
            IBlockData blockState4 = chunk.getType(mutableBlockPos);
            if (blockState3.is(defaultBlock.getBlock()) && (blockState4.isAir() || blockState4 == defaultFluid)) {
                for(int q = 0; q < n; ++q) {
                    mutableBlockPos.move(EnumDirection.UP);
                    if (!chunk.getType(mutableBlockPos).is(defaultBlock.getBlock())) {
                        break;
                    }

                    chunk.setType(mutableBlockPos, blockState, false);
                }

                mutableBlockPos.set(k, p, m);
            }

            if ((blockState3.isAir() || blockState3 == defaultFluid) && blockState4.is(defaultBlock.getBlock())) {
                for(int r = 0; r < o && chunk.getType(mutableBlockPos).is(defaultBlock.getBlock()); ++r) {
                    if (bl && p >= j - 4 && p <= j + 1) {
                        chunk.setType(mutableBlockPos, this.getPatchBlockState(), false);
                    } else {
                        chunk.setType(mutableBlockPos, blockState2, false);
                    }

                    mutableBlockPos.move(EnumDirection.DOWN);
                }
            }

            blockState3 = blockState4;
        }

    }

    @Override
    public void initNoise(long seed) {
        if (this.seed != seed || this.patchNoise == null || this.floorNoises.isEmpty() || this.ceilingNoises.isEmpty()) {
            this.floorNoises = initPerlinNoises(this.getFloorBlockStates(), seed);
            this.ceilingNoises = initPerlinNoises(this.getCeilingBlockStates(), seed + (long)this.floorNoises.size());
            this.patchNoise = new NoiseGeneratorOctaves(new SeededRandom(seed + (long)this.floorNoises.size() + (long)this.ceilingNoises.size()), ImmutableList.of(0));
        }

        this.seed = seed;
    }

    private static ImmutableMap<IBlockData, NoiseGeneratorOctaves> initPerlinNoises(ImmutableList<IBlockData> states, long seed) {
        Builder<IBlockData, NoiseGeneratorOctaves> builder = new Builder<>();

        for(IBlockData blockState : states) {
            builder.put(blockState, new NoiseGeneratorOctaves(new SeededRandom(seed), ImmutableList.of(-4)));
            ++seed;
        }

        return builder.build();
    }

    protected abstract ImmutableList<IBlockData> getFloorBlockStates();

    protected abstract ImmutableList<IBlockData> getCeilingBlockStates();

    protected abstract IBlockData getPatchBlockState();
}
