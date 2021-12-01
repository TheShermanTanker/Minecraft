package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class WorldGenCarverWrapper<WC extends WorldGenCarverConfiguration> {
    public static final Codec<WorldGenCarverWrapper<?>> DIRECT_CODEC = IRegistry.CARVER.byNameCodec().dispatch((configuredWorldCarver) -> {
        return configuredWorldCarver.worldCarver;
    }, WorldGenCarverAbstract::configuredCodec);
    public static final Codec<Supplier<WorldGenCarverWrapper<?>>> CODEC = RegistryFileCodec.create(IRegistry.CONFIGURED_CARVER_REGISTRY, DIRECT_CODEC);
    public static final Codec<List<Supplier<WorldGenCarverWrapper<?>>>> LIST_CODEC = RegistryFileCodec.homogeneousList(IRegistry.CONFIGURED_CARVER_REGISTRY, DIRECT_CODEC);
    private final WorldGenCarverAbstract<WC> worldCarver;
    private final WC config;

    public WorldGenCarverWrapper(WorldGenCarverAbstract<WC> carver, WC config) {
        this.worldCarver = carver;
        this.config = config;
    }

    public WC config() {
        return this.config;
    }

    public boolean isStartChunk(Random random) {
        return this.worldCarver.isStartChunk(this.config, random);
    }

    public boolean carve(CarvingContext context, IChunkAccess chunk, Function<BlockPosition, BiomeBase> posToBiome, Random random, Aquifer aquiferSampler, ChunkCoordIntPair pos, CarvingMask mask) {
        return SharedConstants.debugVoidTerrain(chunk.getPos()) ? false : this.worldCarver.carve(context, this.config, chunk, posToBiome, random, aquiferSampler, pos, mask);
    }
}
