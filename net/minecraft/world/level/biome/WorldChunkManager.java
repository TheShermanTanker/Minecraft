package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;

public abstract class WorldChunkManager implements BiomeManager.Provider {
    public static final Codec<WorldChunkManager> CODEC = IRegistry.BIOME_SOURCE.dispatchStable(WorldChunkManager::codec, Function.identity());
    protected final Map<StructureGenerator<?>, Boolean> supportedStructures = Maps.newHashMap();
    protected final Set<IBlockData> surfaceBlocks = Sets.newHashSet();
    protected final List<BiomeBase> possibleBiomes;

    protected WorldChunkManager(Stream<Supplier<BiomeBase>> stream) {
        this(stream.map(Supplier::get).collect(ImmutableList.toImmutableList()));
    }

    protected WorldChunkManager(List<BiomeBase> biomes) {
        this.possibleBiomes = biomes;
    }

    protected abstract Codec<? extends WorldChunkManager> codec();

    public abstract WorldChunkManager withSeed(long seed);

    public List<BiomeBase> possibleBiomes() {
        return this.possibleBiomes;
    }

    public Set<BiomeBase> getBiomesWithin(int x, int y, int z, int radius) {
        int i = QuartPos.fromBlock(x - radius);
        int j = QuartPos.fromBlock(y - radius);
        int k = QuartPos.fromBlock(z - radius);
        int l = QuartPos.fromBlock(x + radius);
        int m = QuartPos.fromBlock(y + radius);
        int n = QuartPos.fromBlock(z + radius);
        int o = l - i + 1;
        int p = m - j + 1;
        int q = n - k + 1;
        Set<BiomeBase> set = Sets.newHashSet();

        for(int r = 0; r < q; ++r) {
            for(int s = 0; s < o; ++s) {
                for(int t = 0; t < p; ++t) {
                    int u = i + s;
                    int v = j + t;
                    int w = k + r;
                    set.add(this.getBiome(u, v, w));
                }
            }
        }

        return set;
    }

    @Nullable
    public BlockPosition findBiomeHorizontal(int x, int y, int z, int radius, Predicate<BiomeBase> predicate, Random random) {
        return this.findBiomeHorizontal(x, y, z, radius, 1, predicate, random, false);
    }

    @Nullable
    public BlockPosition findBiomeHorizontal(int x, int y, int z, int radius, int i, Predicate<BiomeBase> predicate, Random random, boolean bl) {
        int j = QuartPos.fromBlock(x);
        int k = QuartPos.fromBlock(z);
        int l = QuartPos.fromBlock(radius);
        int m = QuartPos.fromBlock(y);
        BlockPosition blockPos = null;
        int n = 0;
        int o = bl ? 0 : l;

        for(int p = o; p <= l; p += i) {
            for(int q = -p; q <= p; q += i) {
                boolean bl2 = Math.abs(q) == p;

                for(int r = -p; r <= p; r += i) {
                    if (bl) {
                        boolean bl3 = Math.abs(r) == p;
                        if (!bl3 && !bl2) {
                            continue;
                        }
                    }

                    int s = j + r;
                    int t = k + q;
                    if (predicate.test(this.getBiome(s, m, t))) {
                        if (blockPos == null || random.nextInt(n + 1) == 0) {
                            blockPos = new BlockPosition(QuartPos.toBlock(s), y, QuartPos.toBlock(t));
                            if (bl) {
                                return blockPos;
                            }
                        }

                        ++n;
                    }
                }
            }
        }

        return blockPos;
    }

    public boolean canGenerateStructure(StructureGenerator<?> feature) {
        return this.supportedStructures.computeIfAbsent(feature, (structureFeature) -> {
            return this.possibleBiomes.stream().anyMatch((biome) -> {
                return biome.getGenerationSettings().isValidStart(structureFeature);
            });
        });
    }

    public Set<IBlockData> getSurfaceBlocks() {
        if (this.surfaceBlocks.isEmpty()) {
            for(BiomeBase biome : this.possibleBiomes) {
                this.surfaceBlocks.add(biome.getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial());
            }
        }

        return this.surfaceBlocks;
    }

    static {
        IRegistry.register(IRegistry.BIOME_SOURCE, "fixed", WorldChunkManagerHell.CODEC);
        IRegistry.register(IRegistry.BIOME_SOURCE, "multi_noise", WorldChunkManagerMultiNoise.CODEC);
        IRegistry.register(IRegistry.BIOME_SOURCE, "checkerboard", WorldChunkManagerCheckerBoard.CODEC);
        IRegistry.register(IRegistry.BIOME_SOURCE, "vanilla_layered", WorldChunkManagerOverworld.CODEC);
        IRegistry.register(IRegistry.BIOME_SOURCE, "the_end", WorldChunkManagerTheEnd.CODEC);
    }
}
