package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.server.level.RegionLimitedWorldAccess;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.BlockColumn;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.WorldChunkManagerHell;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.IChunkAccess;

public class ChunkProviderDebug extends ChunkGenerator {
    public static final Codec<ChunkProviderDebug> CODEC = RegistryLookupCodec.create(IRegistry.BIOME_REGISTRY).xmap(ChunkProviderDebug::new, ChunkProviderDebug::biomes).stable().codec();
    private static final int BLOCK_MARGIN = 2;
    private static final List<IBlockData> ALL_BLOCKS = StreamSupport.stream(IRegistry.BLOCK.spliterator(), false).flatMap((block) -> {
        return block.getStates().getPossibleStates().stream();
    }).collect(Collectors.toList());
    private static final int GRID_WIDTH = MathHelper.ceil(MathHelper.sqrt((float)ALL_BLOCKS.size()));
    private static final int GRID_HEIGHT = MathHelper.ceil((float)ALL_BLOCKS.size() / (float)GRID_WIDTH);
    protected static final IBlockData AIR = Blocks.AIR.getBlockData();
    protected static final IBlockData BARRIER = Blocks.BARRIER.getBlockData();
    public static final int HEIGHT = 70;
    public static final int BARRIER_HEIGHT = 60;
    private final IRegistry<BiomeBase> biomes;

    public ChunkProviderDebug(IRegistry<BiomeBase> biomeRegistry) {
        super(new WorldChunkManagerHell(biomeRegistry.getOrThrow(Biomes.PLAINS)), new StructureSettings(false));
        this.biomes = biomeRegistry;
    }

    public IRegistry<BiomeBase> biomes() {
        return this.biomes;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed) {
        return this;
    }

    @Override
    public void buildBase(RegionLimitedWorldAccess region, IChunkAccess chunk) {
    }

    @Override
    public void doCarving(long seed, BiomeManager access, IChunkAccess chunk, WorldGenStage.Features carver) {
    }

    @Override
    public void addDecorations(RegionLimitedWorldAccess region, StructureManager accessor) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        ChunkCoordIntPair chunkPos = region.getCenter();

        for(int i = 0; i < 16; ++i) {
            for(int j = 0; j < 16; ++j) {
                int k = SectionPosition.sectionToBlockCoord(chunkPos.x, i);
                int l = SectionPosition.sectionToBlockCoord(chunkPos.z, j);
                region.setTypeAndData(mutableBlockPos.set(k, 60, l), BARRIER, 2);
                IBlockData blockState = getBlockStateFor(k, l);
                if (blockState != null) {
                    region.setTypeAndData(mutableBlockPos.set(k, 70, l), blockState, 2);
                }
            }
        }

    }

    @Override
    public CompletableFuture<IChunkAccess> buildNoise(Executor executor, StructureManager accessor, IChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, HeightMap.Type heightmap, IWorldHeightAccess world) {
        return 0;
    }

    @Override
    public BlockColumn getBaseColumn(int x, int z, IWorldHeightAccess world) {
        return new BlockColumn(0, new IBlockData[0]);
    }

    public static IBlockData getBlockStateFor(int x, int z) {
        IBlockData blockState = AIR;
        if (x > 0 && z > 0 && x % 2 != 0 && z % 2 != 0) {
            x = x / 2;
            z = z / 2;
            if (x <= GRID_WIDTH && z <= GRID_HEIGHT) {
                int i = MathHelper.abs(x * GRID_WIDTH + z);
                if (i < ALL_BLOCKS.size()) {
                    blockState = ALL_BLOCKS.get(i);
                }
            }
        }

        return blockState;
    }
}
