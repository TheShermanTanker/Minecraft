package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProtoChunk implements IChunkAccess {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ChunkCoordIntPair chunkPos;
    private volatile boolean isDirty;
    @Nullable
    private BiomeStorage biomes;
    @Nullable
    private volatile LightEngine lightEngine;
    private final Map<HeightMap.Type, HeightMap> heightmaps = Maps.newEnumMap(HeightMap.Type.class);
    private volatile ChunkStatus status = ChunkStatus.EMPTY;
    private final Map<BlockPosition, TileEntity> blockEntities = Maps.newHashMap();
    private final Map<BlockPosition, NBTTagCompound> blockEntityNbts = Maps.newHashMap();
    private final ChunkSection[] sections;
    private final List<NBTTagCompound> entities = Lists.newArrayList();
    private final List<BlockPosition> lights = Lists.newArrayList();
    private final ShortList[] postProcessing;
    private final Map<StructureGenerator<?>, StructureStart<?>> structureStarts = Maps.newHashMap();
    private final Map<StructureGenerator<?>, LongSet> structuresRefences = Maps.newHashMap();
    private final ChunkConverter upgradeData;
    private final ProtoChunkTickList<Block> blockTicks;
    private final ProtoChunkTickList<FluidType> liquidTicks;
    private final IWorldHeightAccess levelHeightAccessor;
    private long inhabitedTime;
    private final Map<WorldGenStage.Features, BitSet> carvingMasks = new Object2ObjectArrayMap<>();
    private volatile boolean isLightCorrect;

    public ProtoChunk(ChunkCoordIntPair pos, ChunkConverter upgradeData, IWorldHeightAccess world) {
        this(pos, upgradeData, (ChunkSection[])null, new ProtoChunkTickList<>((block) -> {
            return block == null || block.getBlockData().isAir();
        }, pos, world), new ProtoChunkTickList<>((fluid) -> {
            return fluid == null || fluid == FluidTypes.EMPTY;
        }, pos, world), world);
    }

    public ProtoChunk(ChunkCoordIntPair pos, ChunkConverter upgradeData, @Nullable ChunkSection[] levelChunkSections, ProtoChunkTickList<Block> blockTickScheduler, ProtoChunkTickList<FluidType> fluidTickScheduler, IWorldHeightAccess world) {
        this.chunkPos = pos;
        this.upgradeData = upgradeData;
        this.blockTicks = blockTickScheduler;
        this.liquidTicks = fluidTickScheduler;
        this.levelHeightAccessor = world;
        this.sections = new ChunkSection[world.getSectionsCount()];
        if (levelChunkSections != null) {
            if (this.sections.length == levelChunkSections.length) {
                System.arraycopy(levelChunkSections, 0, this.sections, 0, this.sections.length);
            } else {
                LOGGER.warn("Could not set level chunk sections, array length is {} instead of {}", levelChunkSections.length, this.sections.length);
            }
        }

        this.postProcessing = new ShortList[world.getSectionsCount()];
    }

    @Override
    public IBlockData getType(BlockPosition pos) {
        int i = pos.getY();
        if (this.isOutsideBuildHeight(i)) {
            return Blocks.VOID_AIR.getBlockData();
        } else {
            ChunkSection levelChunkSection = this.getSections()[this.getSectionIndex(i)];
            return ChunkSection.isEmpty(levelChunkSection) ? Blocks.AIR.getBlockData() : levelChunkSection.getType(pos.getX() & 15, i & 15, pos.getZ() & 15);
        }
    }

    @Override
    public Fluid getFluid(BlockPosition pos) {
        int i = pos.getY();
        if (this.isOutsideBuildHeight(i)) {
            return FluidTypes.EMPTY.defaultFluidState();
        } else {
            ChunkSection levelChunkSection = this.getSections()[this.getSectionIndex(i)];
            return ChunkSection.isEmpty(levelChunkSection) ? FluidTypes.EMPTY.defaultFluidState() : levelChunkSection.getFluidState(pos.getX() & 15, i & 15, pos.getZ() & 15);
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
            if (this.sections[l] == Chunk.EMPTY_SECTION && state.is(Blocks.AIR)) {
                return state;
            } else {
                if (state.getLightEmission() > 0) {
                    this.lights.add(new BlockPosition((i & 15) + this.getPos().getMinBlockX(), j, (k & 15) + this.getPos().getMinBlockZ()));
                }

                ChunkSection levelChunkSection = this.getOrCreateSection(l);
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

    @Override
    public Set<BlockPosition> getBlockEntitiesPos() {
        Set<BlockPosition> set = Sets.newHashSet(this.blockEntityNbts.keySet());
        set.addAll(this.blockEntities.keySet());
        return set;
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

    public List<NBTTagCompound> getEntities() {
        return this.entities;
    }

    public void setBiomes(BiomeStorage biomes) {
        this.biomes = biomes;
    }

    @Nullable
    @Override
    public BiomeStorage getBiomeIndex() {
        return this.biomes;
    }

    @Override
    public void setNeedsSaving(boolean shouldSave) {
        this.isDirty = shouldSave;
    }

    @Override
    public boolean isNeedsSaving() {
        return this.isDirty;
    }

    @Override
    public ChunkStatus getChunkStatus() {
        return this.status;
    }

    public void setStatus(ChunkStatus status) {
        this.status = status;
        this.setNeedsSaving(true);
    }

    @Override
    public ChunkSection[] getSections() {
        return this.sections;
    }

    @Override
    public Collection<Entry<HeightMap.Type, HeightMap>> getHeightmaps() {
        return Collections.unmodifiableSet(this.heightmaps.entrySet());
    }

    @Override
    public HeightMap getOrCreateHeightmapUnprimed(HeightMap.Type type) {
        return this.heightmaps.computeIfAbsent(type, (typex) -> {
            return new HeightMap(this, typex);
        });
    }

    @Override
    public int getHighestBlock(HeightMap.Type type, int x, int z) {
        HeightMap heightmap = this.heightmaps.get(type);
        if (heightmap == null) {
            HeightMap.primeHeightmaps(this, EnumSet.of(type));
            heightmap = this.heightmaps.get(type);
        }

        return heightmap.getFirstAvailable(x & 15, z & 15) - 1;
    }

    @Override
    public BlockPosition getHeighestPosition(HeightMap.Type types) {
        int i = this.getMinBuildHeight();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int j = this.chunkPos.getMinBlockX(); j <= this.chunkPos.getMaxBlockX(); ++j) {
            for(int k = this.chunkPos.getMinBlockZ(); k <= this.chunkPos.getMaxBlockZ(); ++k) {
                int l = this.getHighestBlock(types, j & 15, k & 15);
                if (l > i) {
                    i = l;
                    mutableBlockPos.set(j, l, k);
                }
            }
        }

        return mutableBlockPos.immutableCopy();
    }

    @Override
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
        this.isDirty = true;
    }

    @Override
    public Map<StructureGenerator<?>, StructureStart<?>> getAllStarts() {
        return Collections.unmodifiableMap(this.structureStarts);
    }

    @Override
    public void setAllStarts(Map<StructureGenerator<?>, StructureStart<?>> structureStarts) {
        this.structureStarts.clear();
        this.structureStarts.putAll(structureStarts);
        this.isDirty = true;
    }

    @Override
    public LongSet getReferencesForFeature(StructureGenerator<?> structure) {
        return this.structuresRefences.computeIfAbsent(structure, (structurex) -> {
            return new LongOpenHashSet();
        });
    }

    @Override
    public void addReferenceForFeature(StructureGenerator<?> structure, long reference) {
        this.structuresRefences.computeIfAbsent(structure, (structurex) -> {
            return new LongOpenHashSet();
        }).add(reference);
        this.isDirty = true;
    }

    @Override
    public Map<StructureGenerator<?>, LongSet> getAllReferences() {
        return Collections.unmodifiableMap(this.structuresRefences);
    }

    @Override
    public void setAllReferences(Map<StructureGenerator<?>, LongSet> structureReferences) {
        this.structuresRefences.clear();
        this.structuresRefences.putAll(structureReferences);
        this.isDirty = true;
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
    public ShortList[] getPostProcessing() {
        return this.postProcessing;
    }

    @Override
    public void addPackedPostProcess(short packedPos, int index) {
        IChunkAccess.getOrCreateOffsetList(this.postProcessing, index).add(packedPos);
    }

    @Override
    public ProtoChunkTickList<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public ProtoChunkTickList<FluidType> getLiquidTicks() {
        return this.liquidTicks;
    }

    @Override
    public ChunkConverter getUpgradeData() {
        return this.upgradeData;
    }

    @Override
    public void setInhabitedTime(long inhabitedTime) {
        this.inhabitedTime = inhabitedTime;
    }

    @Override
    public long getInhabitedTime() {
        return this.inhabitedTime;
    }

    @Override
    public void setBlockEntityNbt(NBTTagCompound nbt) {
        this.blockEntityNbts.put(new BlockPosition(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z")), nbt);
    }

    public Map<BlockPosition, NBTTagCompound> getBlockEntityNbts() {
        return Collections.unmodifiableMap(this.blockEntityNbts);
    }

    @Override
    public NBTTagCompound getBlockEntityNbt(BlockPosition pos) {
        return this.blockEntityNbts.get(pos);
    }

    @Nullable
    @Override
    public NBTTagCompound getBlockEntityNbtForSaving(BlockPosition pos) {
        TileEntity blockEntity = this.getTileEntity(pos);
        return blockEntity != null ? blockEntity.save(new NBTTagCompound()) : this.blockEntityNbts.get(pos);
    }

    @Override
    public void removeTileEntity(BlockPosition pos) {
        this.blockEntities.remove(pos);
        this.blockEntityNbts.remove(pos);
    }

    @Nullable
    public BitSet getCarvingMask(WorldGenStage.Features carver) {
        return this.carvingMasks.get(carver);
    }

    public BitSet getOrCreateCarvingMask(WorldGenStage.Features carver) {
        return this.carvingMasks.computeIfAbsent(carver, (carverx) -> {
            return new BitSet(65536);
        });
    }

    public void setCarvingMask(WorldGenStage.Features carver, BitSet mask) {
        this.carvingMasks.put(carver, mask);
    }

    public void setLightEngine(LightEngine lightingProvider) {
        this.lightEngine = lightingProvider;
    }

    @Override
    public boolean isLightCorrect() {
        return this.isLightCorrect;
    }

    @Override
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
}
