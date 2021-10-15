package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.BitSet;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;

public class ProtoChunkExtension extends ProtoChunk {
    private final Chunk wrapped;

    public ProtoChunkExtension(Chunk wrapped) {
        super(wrapped.getPos(), ChunkConverter.EMPTY, wrapped);
        this.wrapped = wrapped;
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition pos) {
        return this.wrapped.getTileEntity(pos);
    }

    @Nullable
    @Override
    public IBlockData getType(BlockPosition pos) {
        return this.wrapped.getType(pos);
    }

    @Override
    public Fluid getFluid(BlockPosition pos) {
        return this.wrapped.getFluid(pos);
    }

    @Override
    public int getMaxLightLevel() {
        return this.wrapped.getMaxLightLevel();
    }

    @Nullable
    @Override
    public IBlockData setType(BlockPosition pos, IBlockData state, boolean moved) {
        return null;
    }

    @Override
    public void setTileEntity(TileEntity blockEntity) {
    }

    @Override
    public void addEntity(Entity entity) {
    }

    @Override
    public void setStatus(ChunkStatus status) {
    }

    @Override
    public ChunkSection[] getSections() {
        return this.wrapped.getSections();
    }

    @Override
    public void setHeightmap(HeightMap.Type type, long[] heightmap) {
    }

    private HeightMap.Type fixType(HeightMap.Type type) {
        if (type == HeightMap.Type.WORLD_SURFACE_WG) {
            return HeightMap.Type.WORLD_SURFACE;
        } else {
            return type == HeightMap.Type.OCEAN_FLOOR_WG ? HeightMap.Type.OCEAN_FLOOR : type;
        }
    }

    @Override
    public int getHighestBlock(HeightMap.Type type, int x, int z) {
        return this.wrapped.getHighestBlock(this.fixType(type), x, z);
    }

    @Override
    public BlockPosition getHeighestPosition(HeightMap.Type types) {
        return this.wrapped.getHeighestPosition(this.fixType(types));
    }

    @Override
    public ChunkCoordIntPair getPos() {
        return this.wrapped.getPos();
    }

    @Nullable
    @Override
    public StructureStart<?> getStartForFeature(StructureGenerator<?> structure) {
        return this.wrapped.getStartForFeature(structure);
    }

    @Override
    public void setStartForFeature(StructureGenerator<?> structure, StructureStart<?> start) {
    }

    @Override
    public Map<StructureGenerator<?>, StructureStart<?>> getAllStarts() {
        return this.wrapped.getAllStarts();
    }

    @Override
    public void setAllStarts(Map<StructureGenerator<?>, StructureStart<?>> structureStarts) {
    }

    @Override
    public LongSet getReferencesForFeature(StructureGenerator<?> structure) {
        return this.wrapped.getReferencesForFeature(structure);
    }

    @Override
    public void addReferenceForFeature(StructureGenerator<?> structure, long reference) {
    }

    @Override
    public Map<StructureGenerator<?>, LongSet> getAllReferences() {
        return this.wrapped.getAllReferences();
    }

    @Override
    public void setAllReferences(Map<StructureGenerator<?>, LongSet> structureReferences) {
    }

    @Override
    public BiomeStorage getBiomeIndex() {
        return this.wrapped.getBiomeIndex();
    }

    @Override
    public void setNeedsSaving(boolean shouldSave) {
    }

    @Override
    public boolean isNeedsSaving() {
        return false;
    }

    @Override
    public ChunkStatus getChunkStatus() {
        return this.wrapped.getChunkStatus();
    }

    @Override
    public void removeTileEntity(BlockPosition pos) {
    }

    @Override
    public void markPosForPostprocessing(BlockPosition pos) {
    }

    @Override
    public void setBlockEntityNbt(NBTTagCompound nbt) {
    }

    @Nullable
    @Override
    public NBTTagCompound getBlockEntityNbt(BlockPosition pos) {
        return this.wrapped.getBlockEntityNbt(pos);
    }

    @Nullable
    @Override
    public NBTTagCompound getBlockEntityNbtForSaving(BlockPosition pos) {
        return this.wrapped.getBlockEntityNbtForSaving(pos);
    }

    @Override
    public void setBiomes(BiomeStorage biomes) {
    }

    @Override
    public Stream<BlockPosition> getLights() {
        return this.wrapped.getLights();
    }

    @Override
    public ProtoChunkTickList<Block> getBlockTicks() {
        return new ProtoChunkTickList<>((block) -> {
            return block.getBlockData().isAir();
        }, this.getPos(), this);
    }

    @Override
    public ProtoChunkTickList<FluidType> getLiquidTicks() {
        return new ProtoChunkTickList<>((fluid) -> {
            return fluid == FluidTypes.EMPTY;
        }, this.getPos(), this);
    }

    @Override
    public BitSet getCarvingMask(WorldGenStage.Features carver) {
        throw (UnsupportedOperationException)SystemUtils.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
    }

    @Override
    public BitSet getOrCreateCarvingMask(WorldGenStage.Features carver) {
        throw (UnsupportedOperationException)SystemUtils.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
    }

    public Chunk getWrapped() {
        return this.wrapped;
    }

    @Override
    public boolean isLightCorrect() {
        return this.wrapped.isLightCorrect();
    }

    @Override
    public void setLightCorrect(boolean lightOn) {
        this.wrapped.setLightCorrect(lightOn);
    }
}
