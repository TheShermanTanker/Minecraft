package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.TickContainerAccess;

public class ProtoChunkExtension extends ProtoChunk {
    private final Chunk wrapped;
    private final boolean allowWrites;

    public ProtoChunkExtension(Chunk wrapped, boolean bl) {
        super(wrapped.getPos(), ChunkConverter.EMPTY, wrapped.levelHeightAccessor, wrapped.getWorld().registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY), wrapped.getBlendingData());
        this.wrapped = wrapped;
        this.allowWrites = bl;
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition pos) {
        return this.wrapped.getTileEntity(pos);
    }

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

    @Override
    public ChunkSection getSection(int yIndex) {
        return this.allowWrites ? this.wrapped.getSection(yIndex) : super.getSection(yIndex);
    }

    @Nullable
    @Override
    public IBlockData setType(BlockPosition pos, IBlockData state, boolean moved) {
        return this.allowWrites ? this.wrapped.setType(pos, state, moved) : null;
    }

    @Override
    public void setTileEntity(TileEntity blockEntity) {
        if (this.allowWrites) {
            this.wrapped.setTileEntity(blockEntity);
        }

    }

    @Override
    public void addEntity(Entity entity) {
        if (this.allowWrites) {
            this.wrapped.addEntity(entity);
        }

    }

    @Override
    public void setStatus(ChunkStatus status) {
        if (this.allowWrites) {
            super.setStatus(status);
        }

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
    public HeightMap getOrCreateHeightmapUnprimed(HeightMap.Type type) {
        return this.wrapped.getOrCreateHeightmapUnprimed(type);
    }

    @Override
    public int getHighestBlock(HeightMap.Type type, int x, int z) {
        return this.wrapped.getHighestBlock(this.fixType(type), x, z);
    }

    @Override
    public BiomeBase getBiome(int biomeX, int biomeY, int biomeZ) {
        return this.wrapped.getBiome(biomeX, biomeY, biomeZ);
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
    public Stream<BlockPosition> getLights() {
        return this.wrapped.getLights();
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return this.allowWrites ? this.wrapped.getBlockTicks() : BlackholeTickAccess.emptyContainer();
    }

    @Override
    public TickContainerAccess<FluidType> getFluidTicks() {
        return this.allowWrites ? this.wrapped.getFluidTicks() : BlackholeTickAccess.emptyContainer();
    }

    @Override
    public ChunkAccess$TicksToSave getTicksForSerialization() {
        return this.wrapped.getTicksForSerialization();
    }

    @Nullable
    @Override
    public BlendingData getBlendingData() {
        return this.wrapped.getBlendingData();
    }

    @Override
    public void setBlendingData(BlendingData blender) {
        this.wrapped.setBlendingData(blender);
    }

    @Override
    public CarvingMask getCarvingMask(WorldGenStage.Features carver) {
        if (this.allowWrites) {
            return super.getCarvingMask(carver);
        } else {
            throw (UnsupportedOperationException)SystemUtils.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
        }
    }

    @Override
    public CarvingMask getOrCreateCarvingMask(WorldGenStage.Features carver) {
        if (this.allowWrites) {
            return super.getOrCreateCarvingMask(carver);
        } else {
            throw (UnsupportedOperationException)SystemUtils.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
        }
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

    @Override
    public void fillBiomesFromNoise(BiomeResolver biomeSupplier, Climate.Sampler sampler) {
        if (this.allowWrites) {
            this.wrapped.fillBiomesFromNoise(biomeSupplier, sampler);
        }

    }
}
