package net.minecraft.world.level;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPosition;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.phys.AxisAlignedBB;

public interface IWorldReader extends IBlockLightAccess, ICollisionAccess, BiomeManager.Provider {
    @Nullable
    IChunkAccess getChunkAt(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create);

    /** @deprecated */
    @Deprecated
    boolean isChunkLoaded(int chunkX, int chunkZ);

    int getHeight(HeightMap.Type heightmap, int x, int z);

    int getSkyDarken();

    BiomeManager getBiomeManager();

    default BiomeBase getBiome(BlockPosition pos) {
        return this.getBiomeManager().getBiome(pos);
    }

    default Stream<IBlockData> getBlockStatesIfLoaded(AxisAlignedBB box) {
        int i = MathHelper.floor(box.minX);
        int j = MathHelper.floor(box.maxX);
        int k = MathHelper.floor(box.minY);
        int l = MathHelper.floor(box.maxY);
        int m = MathHelper.floor(box.minZ);
        int n = MathHelper.floor(box.maxZ);
        return this.isAreaLoaded(i, k, m, j, l, n) ? this.getBlockStates(box) : Stream.empty();
    }

    @Override
    default int getBlockTint(BlockPosition pos, ColorResolver colorResolver) {
        return colorResolver.getColor(this.getBiome(pos), (double)pos.getX(), (double)pos.getZ());
    }

    @Override
    default BiomeBase getBiome(int biomeX, int biomeY, int biomeZ) {
        IChunkAccess chunkAccess = this.getChunkAt(QuartPos.toSection(biomeX), QuartPos.toSection(biomeZ), ChunkStatus.BIOMES, false);
        return chunkAccess != null ? chunkAccess.getBiome(biomeX, biomeY, biomeZ) : this.getUncachedNoiseBiome(biomeX, biomeY, biomeZ);
    }

    BiomeBase getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ);

    boolean isClientSide();

    /** @deprecated */
    @Deprecated
    int getSeaLevel();

    DimensionManager getDimensionManager();

    @Override
    default int getMinBuildHeight() {
        return this.getDimensionManager().getMinY();
    }

    @Override
    default int getHeight() {
        return this.getDimensionManager().getHeight();
    }

    default BlockPosition getHighestBlockYAt(HeightMap.Type heightmap, BlockPosition pos) {
        return new BlockPosition(pos.getX(), this.getHeight(heightmap, pos.getX(), pos.getZ()), pos.getZ());
    }

    default boolean isEmpty(BlockPosition pos) {
        return this.getType(pos).isAir();
    }

    default boolean canSeeSkyFromBelowWater(BlockPosition pos) {
        if (pos.getY() >= this.getSeaLevel()) {
            return this.canSeeSky(pos);
        } else {
            BlockPosition blockPos = new BlockPosition(pos.getX(), this.getSeaLevel(), pos.getZ());
            if (!this.canSeeSky(blockPos)) {
                return false;
            } else {
                for(BlockPosition var4 = blockPos.below(); var4.getY() > pos.getY(); var4 = var4.below()) {
                    IBlockData blockState = this.getType(var4);
                    if (blockState.getLightBlock(this, var4) > 0 && !blockState.getMaterial().isLiquid()) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    /** @deprecated */
    @Deprecated
    default float getBrightness(BlockPosition pos) {
        return this.getDimensionManager().brightness(this.getLightLevel(pos));
    }

    default int getDirectSignal(BlockPosition pos, EnumDirection direction) {
        return this.getType(pos).getDirectSignal(this, pos, direction);
    }

    default IChunkAccess getChunk(BlockPosition pos) {
        return this.getChunkAt(SectionPosition.blockToSectionCoord(pos.getX()), SectionPosition.blockToSectionCoord(pos.getZ()));
    }

    default IChunkAccess getChunkAt(int chunkX, int chunkZ) {
        return this.getChunkAt(chunkX, chunkZ, ChunkStatus.FULL, true);
    }

    default IChunkAccess getChunkAt(int chunkX, int chunkZ, ChunkStatus status) {
        return this.getChunkAt(chunkX, chunkZ, status, true);
    }

    @Nullable
    @Override
    default IBlockAccess getChunkForCollisions(int chunkX, int chunkZ) {
        return this.getChunkAt(chunkX, chunkZ, ChunkStatus.EMPTY, false);
    }

    default boolean isWaterAt(BlockPosition pos) {
        return this.getFluid(pos).is(TagsFluid.WATER);
    }

    default boolean containsLiquid(AxisAlignedBB box) {
        int i = MathHelper.floor(box.minX);
        int j = MathHelper.ceil(box.maxX);
        int k = MathHelper.floor(box.minY);
        int l = MathHelper.ceil(box.maxY);
        int m = MathHelper.floor(box.minZ);
        int n = MathHelper.ceil(box.maxZ);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int o = i; o < j; ++o) {
            for(int p = k; p < l; ++p) {
                for(int q = m; q < n; ++q) {
                    IBlockData blockState = this.getType(mutableBlockPos.set(o, p, q));
                    if (!blockState.getFluid().isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    default int getLightLevel(BlockPosition pos) {
        return this.getMaxLocalRawBrightness(pos, this.getSkyDarken());
    }

    default int getMaxLocalRawBrightness(BlockPosition pos, int ambientDarkness) {
        return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000 ? this.getLightLevel(pos, ambientDarkness) : 15;
    }

    /** @deprecated */
    @Deprecated
    default boolean hasChunkAt(int x, int z) {
        return this.isChunkLoaded(SectionPosition.blockToSectionCoord(x), SectionPosition.blockToSectionCoord(z));
    }

    /** @deprecated */
    @Deprecated
    default boolean isLoaded(BlockPosition pos) {
        return this.hasChunkAt(pos.getX(), pos.getZ());
    }

    /** @deprecated */
    @Deprecated
    default boolean areChunksLoadedBetween(BlockPosition min, BlockPosition max) {
        return this.isAreaLoaded(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    /** @deprecated */
    @Deprecated
    default boolean isAreaLoaded(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return maxY >= this.getMinBuildHeight() && minY < this.getMaxBuildHeight() ? this.hasChunksAt(minX, minZ, maxX, maxZ) : false;
    }

    /** @deprecated */
    @Deprecated
    default boolean hasChunksAt(int minX, int minZ, int maxX, int maxZ) {
        int i = SectionPosition.blockToSectionCoord(minX);
        int j = SectionPosition.blockToSectionCoord(maxX);
        int k = SectionPosition.blockToSectionCoord(minZ);
        int l = SectionPosition.blockToSectionCoord(maxZ);

        for(int m = i; m <= j; ++m) {
            for(int n = k; n <= l; ++n) {
                if (!this.isChunkLoaded(m, n)) {
                    return false;
                }
            }
        }

        return true;
    }
}
