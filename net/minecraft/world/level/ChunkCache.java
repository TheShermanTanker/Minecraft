package net.minecraft.world.level;

import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkEmpty;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.IChunkProvider;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChunkCache implements IBlockAccess, ICollisionAccess {
    protected final int centerX;
    protected final int centerZ;
    protected final IChunkAccess[][] chunks;
    protected boolean allEmpty;
    protected final World level;

    public ChunkCache(World world, BlockPosition minPos, BlockPosition maxPos) {
        this.level = world;
        this.centerX = SectionPosition.blockToSectionCoord(minPos.getX());
        this.centerZ = SectionPosition.blockToSectionCoord(minPos.getZ());
        int i = SectionPosition.blockToSectionCoord(maxPos.getX());
        int j = SectionPosition.blockToSectionCoord(maxPos.getZ());
        this.chunks = new IChunkAccess[i - this.centerX + 1][j - this.centerZ + 1];
        IChunkProvider chunkSource = world.getChunkProvider();
        this.allEmpty = true;

        for(int k = this.centerX; k <= i; ++k) {
            for(int l = this.centerZ; l <= j; ++l) {
                this.chunks[k - this.centerX][l - this.centerZ] = chunkSource.getChunkNow(k, l);
            }
        }

        for(int m = SectionPosition.blockToSectionCoord(minPos.getX()); m <= SectionPosition.blockToSectionCoord(maxPos.getX()); ++m) {
            for(int n = SectionPosition.blockToSectionCoord(minPos.getZ()); n <= SectionPosition.blockToSectionCoord(maxPos.getZ()); ++n) {
                IChunkAccess chunkAccess = this.chunks[m - this.centerX][n - this.centerZ];
                if (chunkAccess != null && !chunkAccess.isYSpaceEmpty(minPos.getY(), maxPos.getY())) {
                    this.allEmpty = false;
                    return;
                }
            }
        }

    }

    private IChunkAccess getChunk(BlockPosition pos) {
        return this.getChunk(SectionPosition.blockToSectionCoord(pos.getX()), SectionPosition.blockToSectionCoord(pos.getZ()));
    }

    private IChunkAccess getChunk(int chunkX, int chunkZ) {
        int i = chunkX - this.centerX;
        int j = chunkZ - this.centerZ;
        if (i >= 0 && i < this.chunks.length && j >= 0 && j < this.chunks[i].length) {
            IChunkAccess chunkAccess = this.chunks[i][j];
            return (IChunkAccess)(chunkAccess != null ? chunkAccess : new ChunkEmpty(this.level, new ChunkCoordIntPair(chunkX, chunkZ)));
        } else {
            return new ChunkEmpty(this.level, new ChunkCoordIntPair(chunkX, chunkZ));
        }
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.level.getWorldBorder();
    }

    @Override
    public IBlockAccess getChunkForCollisions(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ);
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition pos) {
        IChunkAccess chunkAccess = this.getChunk(pos);
        return chunkAccess.getTileEntity(pos);
    }

    @Override
    public IBlockData getType(BlockPosition pos) {
        if (this.isOutsideWorld(pos)) {
            return Blocks.AIR.getBlockData();
        } else {
            IChunkAccess chunkAccess = this.getChunk(pos);
            return chunkAccess.getType(pos);
        }
    }

    @Override
    public Stream<VoxelShape> getEntityCollisions(@Nullable Entity entity, AxisAlignedBB box, Predicate<Entity> predicate) {
        return Stream.empty();
    }

    @Override
    public Stream<VoxelShape> getCollisions(@Nullable Entity entity, AxisAlignedBB box, Predicate<Entity> predicate) {
        return this.getBlockCollisions(entity, box);
    }

    @Override
    public Fluid getFluid(BlockPosition pos) {
        if (this.isOutsideWorld(pos)) {
            return FluidTypes.EMPTY.defaultFluidState();
        } else {
            IChunkAccess chunkAccess = this.getChunk(pos);
            return chunkAccess.getFluid(pos);
        }
    }

    @Override
    public int getMinBuildHeight() {
        return this.level.getMinBuildHeight();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    public GameProfilerFiller getProfiler() {
        return this.level.getMethodProfiler();
    }
}
