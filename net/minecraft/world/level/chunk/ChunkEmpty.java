package net.minecraft.world.level.chunk;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.data.worldgen.biome.BiomeRegistry;
import net.minecraft.server.level.PlayerChunk;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;

public class ChunkEmpty extends Chunk {
    public ChunkEmpty(World world, ChunkCoordIntPair pos) {
        super(world, pos, new ChunkEmpty.EmptyChunkBiomeContainer(world));
    }

    @Override
    public IBlockData getType(BlockPosition pos) {
        return Blocks.VOID_AIR.getBlockData();
    }

    @Nullable
    @Override
    public IBlockData setType(BlockPosition pos, IBlockData state, boolean moved) {
        return null;
    }

    @Override
    public Fluid getFluid(BlockPosition pos) {
        return FluidTypes.EMPTY.defaultFluidState();
    }

    @Override
    public int getLightEmission(BlockPosition pos) {
        return 0;
    }

    @Nullable
    @Override
    public TileEntity getBlockEntity(BlockPosition pos, Chunk.EnumTileEntityState creationType) {
        return null;
    }

    @Override
    public void addAndRegisterBlockEntity(TileEntity blockEntity) {
    }

    @Override
    public void setTileEntity(TileEntity blockEntity) {
    }

    @Override
    public void removeTileEntity(BlockPosition pos) {
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isYSpaceEmpty(int lowerHeight, int upperHeight) {
        return true;
    }

    @Override
    public PlayerChunk.State getState() {
        return PlayerChunk.State.BORDER;
    }

    static class EmptyChunkBiomeContainer extends BiomeStorage {
        private static final BiomeBase[] EMPTY_BIOMES = new BiomeBase[0];

        public EmptyChunkBiomeContainer(World world) {
            super(world.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY), world, EMPTY_BIOMES);
        }

        @Override
        public int[] writeBiomes() {
            throw new UnsupportedOperationException("Can not write biomes of an empty chunk");
        }

        @Override
        public BiomeBase getBiome(int biomeX, int biomeY, int biomeZ) {
            return BiomeRegistry.PLAINS;
        }
    }
}
