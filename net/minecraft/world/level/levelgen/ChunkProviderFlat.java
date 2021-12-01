package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.RegionLimitedWorldAccess;
import net.minecraft.world.level.BlockColumn;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.WorldChunkManagerHell;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.GeneratorSettingsFlat;

public class ChunkProviderFlat extends ChunkGenerator {
    public static final Codec<ChunkProviderFlat> CODEC = GeneratorSettingsFlat.CODEC.fieldOf("settings").xmap(ChunkProviderFlat::new, ChunkProviderFlat::settings).codec();
    private final GeneratorSettingsFlat settings;

    public ChunkProviderFlat(GeneratorSettingsFlat config) {
        super(new WorldChunkManagerHell(config.getBiomeFromSettings()), new WorldChunkManagerHell(config.getBiome()), config.structureSettings(), 0L);
        this.settings = config;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed) {
        return this;
    }

    public GeneratorSettingsFlat settings() {
        return this.settings;
    }

    @Override
    public void buildSurface(RegionLimitedWorldAccess region, StructureManager structures, IChunkAccess chunk) {
    }

    @Override
    public int getSpawnHeight(IWorldHeightAccess world) {
        return world.getMinBuildHeight() + Math.min(world.getHeight(), this.settings.getLayers().size());
    }

    @Override
    protected boolean validBiome(IRegistry<BiomeBase> registry, Predicate<ResourceKey<BiomeBase>> condition, BiomeBase biome) {
        return registry.getResourceKey(this.settings.getBiome()).filter(condition).isPresent();
    }

    @Override
    public CompletableFuture<IChunkAccess> fillFromNoise(Executor executor, Blender blender, StructureManager structureAccessor, IChunkAccess chunk) {
        List<IBlockData> list = this.settings.getLayers();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        HeightMap heightmap = chunk.getOrCreateHeightmapUnprimed(HeightMap.Type.OCEAN_FLOOR_WG);
        HeightMap heightmap2 = chunk.getOrCreateHeightmapUnprimed(HeightMap.Type.WORLD_SURFACE_WG);

        for(int i = 0; i < Math.min(chunk.getHeight(), list.size()); ++i) {
            IBlockData blockState = list.get(i);
            if (blockState != null) {
                int j = chunk.getMinBuildHeight() + i;

                for(int k = 0; k < 16; ++k) {
                    for(int l = 0; l < 16; ++l) {
                        chunk.setType(mutableBlockPos.set(k, j, l), blockState, false);
                        heightmap.update(k, j, l, blockState);
                        heightmap2.update(k, j, l, blockState);
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, HeightMap.Type heightmap, IWorldHeightAccess world) {
        List<IBlockData> list = this.settings.getLayers();

        for(int i = Math.min(list.size(), world.getMaxBuildHeight()) - 1; i >= 0; --i) {
            IBlockData blockState = list.get(i);
            if (blockState != null && heightmap.isOpaque().test(blockState)) {
                return world.getMinBuildHeight() + i + 1;
            }
        }

        return world.getMinBuildHeight();
    }

    @Override
    public BlockColumn getBaseColumn(int x, int z, IWorldHeightAccess world) {
        return new BlockColumn(world.getMinBuildHeight(), this.settings.getLayers().stream().limit((long)world.getHeight()).map((state) -> {
            return state == null ? Blocks.AIR.getBlockData() : state;
        }).toArray((i) -> {
            return new IBlockData[i];
        }));
    }

    @Override
    public Climate.Sampler climateSampler() {
        return (i, j, k) -> {
            return Climate.target(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        };
    }

    @Override
    public void applyCarvers(RegionLimitedWorldAccess chunkRegion, long seed, BiomeManager biomeAccess, StructureManager structureAccessor, IChunkAccess chunk, WorldGenStage.Features generationStep) {
    }

    @Override
    public void addMobs(RegionLimitedWorldAccess region) {
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getGenerationDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }
}
