package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;

public class WorldChunkManagerHell extends WorldChunkManager {
    public static final Codec<WorldChunkManagerHell> CODEC = BiomeBase.CODEC.fieldOf("biome").xmap(WorldChunkManagerHell::new, (fixedBiomeSource) -> {
        return fixedBiomeSource.biome;
    }).stable().codec();
    private final Supplier<BiomeBase> biome;

    public WorldChunkManagerHell(BiomeBase biome) {
        this(() -> {
            return biome;
        });
    }

    public WorldChunkManagerHell(Supplier<BiomeBase> biome) {
        super(ImmutableList.of(biome.get()));
        this.biome = biome;
    }

    @Override
    protected Codec<? extends WorldChunkManager> codec() {
        return CODEC;
    }

    @Override
    public WorldChunkManager withSeed(long seed) {
        return this;
    }

    @Override
    public BiomeBase getBiome(int biomeX, int biomeY, int biomeZ) {
        return this.biome.get();
    }

    @Nullable
    @Override
    public BlockPosition findBiomeHorizontal(int x, int y, int z, int radius, int i, Predicate<BiomeBase> predicate, Random random, boolean bl) {
        if (predicate.test(this.biome.get())) {
            return bl ? new BlockPosition(x, y, z) : new BlockPosition(x - radius + random.nextInt(radius * 2 + 1), y, z - radius + random.nextInt(radius * 2 + 1));
        } else {
            return null;
        }
    }

    @Override
    public Set<BiomeBase> getBiomesWithin(int x, int y, int z, int radius) {
        return Sets.newHashSet(this.biome.get());
    }
}
