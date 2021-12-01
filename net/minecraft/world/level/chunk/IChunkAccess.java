package net.minecraft.world.level.chunk;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GeneratorSettingBase;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseSampler;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.ticks.TickContainerAccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class IChunkAccess implements IBlockAccess, BiomeManager.Provider, IStructureAccess {
    private static final Logger LOGGER = LogManager.getLogger();
    protected final ShortList[] postProcessing;
    protected volatile boolean unsaved;
    private volatile boolean isLightCorrect;
    protected final ChunkCoordIntPair chunkPos;
    private long inhabitedTime;
    /** @deprecated */
    @Nullable
    @Deprecated
    private BiomeBase carverBiome;
    @Nullable
    protected NoiseChunk noiseChunk;
    protected final ChunkConverter upgradeData;
    @Nullable
    protected BlendingData blendingData;
    public final Map<HeightMap.Type, HeightMap> heightmaps = Maps.newEnumMap(HeightMap.Type.class);
    private final Map<StructureGenerator<?>, StructureStart<?>> structureStarts = Maps.newHashMap();
    private final Map<StructureGenerator<?>, LongSet> structuresRefences = Maps.newHashMap();
    protected final Map<BlockPosition, NBTTagCompound> pendingBlockEntities = Maps.newHashMap();
    public final Map<BlockPosition, TileEntity> blockEntities = Maps.newHashMap();
    protected final IWorldHeightAccess levelHeightAccessor;
    protected final ChunkSection[] sections;

    public IChunkAccess(ChunkCoordIntPair pos, ChunkConverter upgradeData, IWorldHeightAccess heightLimitView, IRegistry<BiomeBase> biome, long inhabitedTime, @Nullable ChunkSection[] sectionArrayInitializer, @Nullable BlendingData blendingData) {
        this.chunkPos = pos;
        this.upgradeData = upgradeData;
        this.levelHeightAccessor = heightLimitView;
        this.sections = new ChunkSection[heightLimitView.getSectionsCount()];
        this.inhabitedTime = inhabitedTime;
        this.postProcessing = new ShortList[heightLimitView.getSectionsCount()];
        this.blendingData = blendingData;
        if (sectionArrayInitializer != null) {
            if (this.sections.length == sectionArrayInitializer.length) {
                System.arraycopy(sectionArrayInitializer, 0, this.sections, 0, this.sections.length);
            } else {
                LOGGER.warn("Could not set level chunk sections, array length is {} instead of {}", sectionArrayInitializer.length, this.sections.length);
            }
        }

        replaceMissingSections(heightLimitView, biome, this.sections);
    }

    private static void replaceMissingSections(IWorldHeightAccess world, IRegistry<BiomeBase> biome, ChunkSection[] sectionArray) {
        for(int i = 0; i < sectionArray.length; ++i) {
            if (sectionArray[i] == null) {
                sectionArray[i] = new ChunkSection(world.getSectionYFromSectionIndex(i), biome);
            }
        }

    }

    public GameEventDispatcher getEventDispatcher(int ySectionCoord) {
        return GameEventDispatcher.NOOP;
    }

    @Nullable
    public abstract IBlockData setType(BlockPosition pos, IBlockData state, boolean moved);

    public abstract void setTileEntity(TileEntity blockEntity);

    public abstract void addEntity(Entity entity);

    @Nullable
    public ChunkSection getHighestSection() {
        ChunkSection[] levelChunkSections = this.getSections();

        for(int i = levelChunkSections.length - 1; i >= 0; --i) {
            ChunkSection levelChunkSection = levelChunkSections[i];
            if (!levelChunkSection.hasOnlyAir()) {
                return levelChunkSection;
            }
        }

        return null;
    }

    public int getHighestSectionPosition() {
        ChunkSection levelChunkSection = this.getHighestSection();
        return levelChunkSection == null ? this.getMinBuildHeight() : levelChunkSection.getYPosition();
    }

    public Set<BlockPosition> getBlockEntitiesPos() {
        Set<BlockPosition> set = Sets.newHashSet(this.pendingBlockEntities.keySet());
        set.addAll(this.blockEntities.keySet());
        return set;
    }

    public ChunkSection[] getSections() {
        return this.sections;
    }

    public ChunkSection getSection(int yIndex) {
        return this.getSections()[yIndex];
    }

    public Collection<Entry<HeightMap.Type, HeightMap>> getHeightmaps() {
        return Collections.unmodifiableSet(this.heightmaps.entrySet());
    }

    public void setHeightmap(HeightMap.Type type, long[] heightmap) {
        this.getOrCreateHeightmapUnprimed(type).setRawData(this, type, heightmap);
    }

    public HeightMap getOrCreateHeightmapUnprimed(HeightMap.Type type) {
        return this.heightmaps.computeIfAbsent(type, (types) -> {
            return new HeightMap(this, types);
        });
    }

    public boolean hasPrimedHeightmap(HeightMap.Type type) {
        return this.heightmaps.get(type) != null;
    }

    public int getHighestBlock(HeightMap.Type type, int x, int z) {
        HeightMap heightmap = this.heightmaps.get(type);
        if (heightmap == null) {
            if (SharedConstants.IS_RUNNING_IN_IDE && this instanceof Chunk) {
                LOGGER.error("Unprimed heightmap: " + type + " " + x + " " + z);
            }

            HeightMap.primeHeightmaps(this, EnumSet.of(type));
            heightmap = this.heightmaps.get(type);
        }

        return heightmap.getFirstAvailable(x & 15, z & 15) - 1;
    }

    public ChunkCoordIntPair getPos() {
        return this.chunkPos;
    }

    @Nullable
    @Override
    public StructureStart<?> getStartForFeature(StructureGenerator<?> structure) {
        return this.structureStarts.get(structure);
    }

    @Override
    public void setStartForFeature(StructureGenerator<?> structure, StructureStart<?> start) {
        this.structureStarts.put(structure, start);
        this.unsaved = true;
    }

    public Map<StructureGenerator<?>, StructureStart<?>> getAllStarts() {
        return Collections.unmodifiableMap(this.structureStarts);
    }

    public void setAllStarts(Map<StructureGenerator<?>, StructureStart<?>> structureStarts) {
        this.structureStarts.clear();
        this.structureStarts.putAll(structureStarts);
        this.unsaved = true;
    }

    @Override
    public LongSet getReferencesForFeature(StructureGenerator<?> structure) {
        return this.structuresRefences.computeIfAbsent(structure, (structureFeature) -> {
            return new LongOpenHashSet();
        });
    }

    @Override
    public void addReferenceForFeature(StructureGenerator<?> structure, long reference) {
        this.structuresRefences.computeIfAbsent(structure, (structureFeature) -> {
            return new LongOpenHashSet();
        }).add(reference);
        this.unsaved = true;
    }

    @Override
    public Map<StructureGenerator<?>, LongSet> getAllReferences() {
        return Collections.unmodifiableMap(this.structuresRefences);
    }

    @Override
    public void setAllReferences(Map<StructureGenerator<?>, LongSet> structureReferences) {
        this.structuresRefences.clear();
        this.structuresRefences.putAll(structureReferences);
        this.unsaved = true;
    }

    public boolean isYSpaceEmpty(int lowerHeight, int upperHeight) {
        if (lowerHeight < this.getMinBuildHeight()) {
            lowerHeight = this.getMinBuildHeight();
        }

        if (upperHeight >= this.getMaxBuildHeight()) {
            upperHeight = this.getMaxBuildHeight() - 1;
        }

        for(int i = lowerHeight; i <= upperHeight; i += 16) {
            if (!this.getSection(this.getSectionIndex(i)).hasOnlyAir()) {
                return false;
            }
        }

        return true;
    }

    public void setNeedsSaving(boolean shouldSave) {
        this.unsaved = shouldSave;
    }

    public boolean isNeedsSaving() {
        return this.unsaved;
    }

    public abstract ChunkStatus getChunkStatus();

    public abstract void removeTileEntity(BlockPosition pos);

    public void markPosForPostprocessing(BlockPosition pos) {
        LogManager.getLogger().warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", (Object)pos);
    }

    public ShortList[] getPostProcessing() {
        return this.postProcessing;
    }

    public void addPackedPostProcess(short packedPos, int index) {
        getOrCreateOffsetList(this.getPostProcessing(), index).add(packedPos);
    }

    public void setBlockEntityNbt(NBTTagCompound nbt) {
        this.pendingBlockEntities.put(TileEntity.getPosFromTag(nbt), nbt);
    }

    @Nullable
    public NBTTagCompound getBlockEntityNbt(BlockPosition pos) {
        return this.pendingBlockEntities.get(pos);
    }

    @Nullable
    public abstract NBTTagCompound getBlockEntityNbtForSaving(BlockPosition pos);

    public abstract Stream<BlockPosition> getLights();

    public abstract TickContainerAccess<Block> getBlockTicks();

    public abstract TickContainerAccess<FluidType> getFluidTicks();

    public abstract ChunkAccess$TicksToSave getTicksForSerialization();

    public ChunkConverter getUpgradeData() {
        return this.upgradeData;
    }

    public boolean isOldNoiseGeneration() {
        return this.blendingData != null && this.blendingData.oldNoise();
    }

    @Nullable
    public BlendingData getBlendingData() {
        return this.blendingData;
    }

    public void setBlendingData(BlendingData blender) {
        this.blendingData = blender;
    }

    public long getInhabitedTime() {
        return this.inhabitedTime;
    }

    public void incrementInhabitedTime(long delta) {
        this.inhabitedTime += delta;
    }

    public void setInhabitedTime(long inhabitedTime) {
        this.inhabitedTime = inhabitedTime;
    }

    public static ShortList getOrCreateOffsetList(ShortList[] lists, int index) {
        if (lists[index] == null) {
            lists[index] = new ShortArrayList();
        }

        return lists[index];
    }

    public boolean isLightCorrect() {
        return this.isLightCorrect;
    }

    public void setLightCorrect(boolean lightOn) {
        this.isLightCorrect = lightOn;
        this.setNeedsSaving(true);
    }

    @Override
    public int getMinBuildHeight() {
        return this.levelHeightAccessor.getMinBuildHeight();
    }

    @Override
    public int getHeight() {
        return this.levelHeightAccessor.getHeight();
    }

    public NoiseChunk getOrCreateNoiseChunk(NoiseSampler noiseColumnSampler, Supplier<NoiseChunk.NoiseFiller> columnSampler, GeneratorSettingBase chunkGeneratorSettings, Aquifer.FluidPicker fluidLevelSampler, Blender blender) {
        if (this.noiseChunk == null) {
            this.noiseChunk = NoiseChunk.forChunk(this, noiseColumnSampler, columnSampler, chunkGeneratorSettings, fluidLevelSampler, blender);
        }

        return this.noiseChunk;
    }

    /** @deprecated */
    @Deprecated
    public BiomeBase carverBiome(Supplier<BiomeBase> biomeSupplier) {
        if (this.carverBiome == null) {
            this.carverBiome = biomeSupplier.get();
        }

        return this.carverBiome;
    }

    @Override
    public BiomeBase getBiome(int biomeX, int biomeY, int biomeZ) {
        try {
            int i = QuartPos.fromBlock(this.getMinBuildHeight());
            int j = i + QuartPos.fromBlock(this.getHeight()) - 1;
            int k = MathHelper.clamp(biomeY, i, j);
            int l = this.getSectionIndex(QuartPos.toBlock(k));
            return this.sections[l].getNoiseBiome(biomeX & 3, k & 3, biomeZ & 3);
        } catch (Throwable var8) {
            CrashReport crashReport = CrashReport.forThrowable(var8, "Getting biome");
            CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Biome being got");
            crashReportCategory.setDetail("Location", () -> {
                return CrashReportSystemDetails.formatLocation(this, biomeX, biomeY, biomeZ);
            });
            throw new ReportedException(crashReport);
        }
    }

    public void fillBiomesFromNoise(BiomeResolver biomeSupplier, Climate.Sampler sampler) {
        ChunkCoordIntPair chunkPos = this.getPos();
        int i = QuartPos.fromBlock(chunkPos.getMinBlockX());
        int j = QuartPos.fromBlock(chunkPos.getMinBlockZ());
        IWorldHeightAccess levelHeightAccessor = this.getHeightAccessorForGeneration();

        for(int k = levelHeightAccessor.getMinSection(); k < levelHeightAccessor.getMaxSection(); ++k) {
            ChunkSection levelChunkSection = this.getSection(this.getSectionIndexFromSectionY(k));
            levelChunkSection.fillBiomesFromNoise(biomeSupplier, sampler, i, j);
        }

    }

    public boolean hasAnyStructureReferences() {
        return !this.getAllReferences().isEmpty();
    }

    @Nullable
    public BelowZeroRetrogen getBelowZeroRetrogen() {
        return null;
    }

    public boolean isUpgrading() {
        return this.getBelowZeroRetrogen() != null;
    }

    public IWorldHeightAccess getHeightAccessorForGeneration() {
        return this;
    }
}
