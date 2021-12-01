package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;

public class ProtoChunk extends IChunkAccess {
    @Nullable
    private volatile LightEngine lightEngine;
    private volatile ChunkStatus status = ChunkStatus.EMPTY;
    private final List<NBTTagCompound> entities = Lists.newArrayList();
    private final List<BlockPosition> lights = Lists.newArrayList();
    private final Map<WorldGenStage.Features, CarvingMask> carvingMasks = new Object2ObjectArrayMap<>();
    @Nullable
    private BelowZeroRetrogen belowZeroRetrogen;
    private final ProtoChunkTicks<Block> blockTicks;
    private final ProtoChunkTicks<FluidType> fluidTicks;

    public ProtoChunk(ChunkCoordIntPair pos, ChunkConverter upgradeData, IWorldHeightAccess world, IRegistry<BiomeBase> biomeRegistry, @Nullable BlendingData blendingData) {
        this(pos, upgradeData, (ChunkSection[])null, new ProtoChunkTicks<>(), new ProtoChunkTicks<>(), world, biomeRegistry, blendingData);
    }

    public ProtoChunk(ChunkCoordIntPair pos, ChunkConverter upgradeData, @Nullable ChunkSection[] sections, ProtoChunkTicks<Block> blockTickScheduler, ProtoChunkTicks<FluidType> fluidTickScheduler, IWorldHeightAccess world, IRegistry<BiomeBase> biomeRegistry, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, world, biomeRegistry, 0L, sections, blendingData);
        this.blockTicks = blockTickScheduler;
        this.fluidTicks = fluidTickScheduler;
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public TickContainerAccess<FluidType> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public ChunkAccess$TicksToSave getTicksForSerialization() {
        return new ChunkAccess$TicksToSave(this.blockTicks, this.fluidTicks);
    }

    @Override
    public IBlockData getType(BlockPosition pos) {
        int i = pos.getY();
        if (this.isOutsideBuildHeight(i)) {
            return Blocks.VOID_AIR.getBlockData();
        } else {
            ChunkSection levelChunkSection = this.getSection(this.getSectionIndex(i));
            return levelChunkSection.hasOnlyAir() ? Blocks.AIR.getBlockData() : levelChunkSection.getType(pos.getX() & 15, i & 15, pos.getZ() & 15);
        }
    }

    @Override
    public Fluid getFluid(BlockPosition pos) {
        int i = pos.getY();
        if (this.isOutsideBuildHeight(i)) {
            return FluidTypes.EMPTY.defaultFluidState();
        } else {
            ChunkSection levelChunkSection = this.getSection(this.getSectionIndex(i));
            return levelChunkSection.hasOnlyAir() ? FluidTypes.EMPTY.defaultFluidState() : levelChunkSection.getFluidState(pos.getX() & 15, i & 15, pos.getZ() & 15);
        }
    }

    @Override
    public Stream<BlockPosition> getLights() {
        return this.lights.stream();
    }

    public ShortList[] getPackedLights() {
        ShortList[] shortLists = new ShortList[this.getSectionsCount()];

        for(BlockPosition blockPos : this.lights) {
            IChunkAccess.getOrCreateOffsetList(shortLists, this.getSectionIndex(blockPos.getY())).add(packOffsetCoordinates(blockPos));
        }

        return shortLists;
    }

    public void addLight(short chunkSliceRel, int sectionY) {
        this.addLight(unpackOffsetCoordinates(chunkSliceRel, this.getSectionYFromSectionIndex(sectionY), this.chunkPos));
    }

    public void addLight(BlockPosition pos) {
        this.lights.add(pos.immutableCopy());
    }

    @Nullable
    @Override
    public IBlockData setType(BlockPosition pos, IBlockData state, boolean moved) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        if (j >= this.getMinBuildHeight() && j < this.getMaxBuildHeight()) {
            int l = this.getSectionIndex(j);
            if (this.sections[l].hasOnlyAir() && state.is(Blocks.AIR)) {
                return state;
            } else {
                if (state.getLightEmission() > 0) {
                    this.lights.add(new BlockPosition((i & 15) + this.getPos().getMinBlockX(), j, (k & 15) + this.getPos().getMinBlockZ()));
                }

                ChunkSection levelChunkSection = this.getSection(l);
                IBlockData blockState = levelChunkSection.setType(i & 15, j & 15, k & 15, state);
                if (this.status.isOrAfter(ChunkStatus.FEATURES) && state != blockState && (state.getLightBlock(this, pos) != blockState.getLightBlock(this, pos) || state.getLightEmission() != blockState.getLightEmission() || state.useShapeForLightOcclusion() || blockState.useShapeForLightOcclusion())) {
                    this.lightEngine.checkBlock(pos);
                }

                EnumSet<HeightMap.Type> enumSet = this.getChunkStatus().heightmapsAfter();
                EnumSet<HeightMap.Type> enumSet2 = null;

                for(HeightMap.Type types : enumSet) {
                    HeightMap heightmap = this.heightmaps.get(types);
                    if (heightmap == null) {
                        if (enumSet2 == null) {
                            enumSet2 = EnumSet.noneOf(HeightMap.Type.class);
                        }

                        enumSet2.add(types);
                    }
                }

                if (enumSet2 != null) {
                    HeightMap.primeHeightmaps(this, enumSet2);
                }

                for(HeightMap.Type types2 : enumSet) {
                    this.heightmaps.get(types2).update(i & 15, j, k & 15, state);
                }

                return blockState;
            }
        } else {
            return Blocks.VOID_AIR.getBlockData();
        }
    }

    @Override
    public void setTileEntity(TileEntity blockEntity) {
        this.blockEntities.put(blockEntity.getPosition(), blockEntity);
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition pos) {
        return this.blockEntities.get(pos);
    }

    public Map<BlockPosition, TileEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public void addEntity(NBTTagCompound entityTag) {
        this.entities.add(entityTag);
    }

    @Override
    public void addEntity(Entity entity) {
        if (!entity.isPassenger()) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            entity.save(compoundTag);
            this.addEntity(compoundTag);
        }
    }

    @Override
    public void setStartForFeature(StructureGenerator<?> structure, StructureStart<?> start) {
        BelowZeroRetrogen belowZeroRetrogen = this.getBelowZeroRetrogen();
        if (belowZeroRetrogen != null && start.isValid()) {
            StructureBoundingBox boundingBox = start.getBoundingBox();
            IWorldHeightAccess levelHeightAccessor = this.getHeightAccessorForGeneration();
            if (boundingBox.minY() < levelHeightAccessor.getMinBuildHeight() || boundingBox.maxY() >= levelHeightAccessor.getMaxBuildHeight()) {
                return;
            }
        }

        super.setStartForFeature(structure, start);
    }

    public List<NBTTagCompound> getEntities() {
        return this.entities;
    }

    @Override
    public ChunkStatus getChunkStatus() {
        return this.status;
    }

    public void setStatus(ChunkStatus status) {
        this.status = status;
        if (this.belowZeroRetrogen != null && status.isOrAfter(this.belowZeroRetrogen.targetStatus())) {
            this.setBelowZeroRetrogen((BelowZeroRetrogen)null);
        }

        this.setNeedsSaving(true);
    }

    @Override
    public BiomeBase getBiome(int biomeX, int biomeY, int biomeZ) {
        if (!this.getChunkStatus().isOrAfter(ChunkStatus.BIOMES) && (this.belowZeroRetrogen == null || !this.belowZeroRetrogen.targetStatus().isOrAfter(ChunkStatus.BIOMES))) {
            throw new IllegalStateException("Asking for biomes before we have biomes");
        } else {
            return super.getBiome(biomeX, biomeY, biomeZ);
        }
    }

    public static short packOffsetCoordinates(BlockPosition pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        int l = i & 15;
        int m = j & 15;
        int n = k & 15;
        return (short)(l | m << 4 | n << 8);
    }

    public static BlockPosition unpackOffsetCoordinates(short sectionRel, int sectionY, ChunkCoordIntPair chunkPos) {
        int i = SectionPosition.sectionToBlockCoord(chunkPos.x, sectionRel & 15);
        int j = SectionPosition.sectionToBlockCoord(sectionY, sectionRel >>> 4 & 15);
        int k = SectionPosition.sectionToBlockCoord(chunkPos.z, sectionRel >>> 8 & 15);
        return new BlockPosition(i, j, k);
    }

    @Override
    public void markPosForPostprocessing(BlockPosition pos) {
        if (!this.isOutsideWorld(pos)) {
            IChunkAccess.getOrCreateOffsetList(this.postProcessing, this.getSectionIndex(pos.getY())).add(packOffsetCoordinates(pos));
        }

    }

    @Override
    public void addPackedPostProcess(short packedPos, int index) {
        IChunkAccess.getOrCreateOffsetList(this.postProcessing, index).add(packedPos);
    }

    public Map<BlockPosition, NBTTagCompound> getBlockEntityNbts() {
        return Collections.unmodifiableMap(this.pendingBlockEntities);
    }

    @Nullable
    @Override
    public NBTTagCompound getBlockEntityNbtForSaving(BlockPosition pos) {
        TileEntity blockEntity = this.getTileEntity(pos);
        return blockEntity != null ? blockEntity.saveWithFullMetadata() : this.pendingBlockEntities.get(pos);
    }

    @Override
    public void removeTileEntity(BlockPosition pos) {
        this.blockEntities.remove(pos);
        this.pendingBlockEntities.remove(pos);
    }

    @Nullable
    public CarvingMask getCarvingMask(WorldGenStage.Features carver) {
        return this.carvingMasks.get(carver);
    }

    public CarvingMask getOrCreateCarvingMask(WorldGenStage.Features carver) {
        return this.carvingMasks.computeIfAbsent(carver, (carving) -> {
            return new CarvingMask(this.getHeight(), this.getMinBuildHeight());
        });
    }

    public void setCarvingMask(WorldGenStage.Features carver, CarvingMask carvingMask) {
        this.carvingMasks.put(carver, carvingMask);
    }

    public void setLightEngine(LightEngine lightingProvider) {
        this.lightEngine = lightingProvider;
    }

    public void setBelowZeroRetrogen(@Nullable BelowZeroRetrogen belowZeroRetrogen) {
        this.belowZeroRetrogen = belowZeroRetrogen;
    }

    @Nullable
    @Override
    public BelowZeroRetrogen getBelowZeroRetrogen() {
        return this.belowZeroRetrogen;
    }

    private static <T> LevelChunkTicks<T> unpackTicks(ProtoChunkTicks<T> tickScheduler) {
        return new LevelChunkTicks<>(tickScheduler.scheduledTicks());
    }

    public LevelChunkTicks<Block> unpackBlockTicks() {
        return unpackTicks(this.blockTicks);
    }

    public LevelChunkTicks<FluidType> unpackFluidTicks() {
        return unpackTicks(this.fluidTicks);
    }

    @Override
    public IWorldHeightAccess getHeightAccessorForGeneration() {
        return (IWorldHeightAccess)(this.isUpgrading() ? BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR : this);
    }
}
