package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.server.level.PlayerChunk;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.TickListChunk;
import net.minecraft.world.level.TickListEmpty;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ITileEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.EuclideanGameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.levelgen.ChunkProviderDebug;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Chunk implements IChunkAccess {
    static final Logger LOGGER = LogManager.getLogger();
    private static final TickingBlockEntity NULL_TICKER = new TickingBlockEntity() {
        @Override
        public void tick() {
        }

        @Override
        public boolean isRemoved() {
            return true;
        }

        @Override
        public BlockPosition getPos() {
            return BlockPosition.ZERO;
        }

        @Override
        public String getType() {
            return "<null>";
        }
    };
    @Nullable
    public static final ChunkSection EMPTY_SECTION = null;
    private final ChunkSection[] sections;
    private BiomeStorage biomes;
    private final Map<BlockPosition, NBTTagCompound> pendingBlockEntities = Maps.newHashMap();
    private final Map<BlockPosition, Chunk.RebindableTickingBlockEntityWrapper> tickersInLevel = Maps.newHashMap();
    public boolean loaded;
    public final World level;
    public final Map<HeightMap.Type, HeightMap> heightmaps = Maps.newEnumMap(HeightMap.Type.class);
    private final ChunkConverter upgradeData;
    public final Map<BlockPosition, TileEntity> blockEntities = Maps.newHashMap();
    private final Map<StructureGenerator<?>, StructureStart<?>> structureStarts = Maps.newHashMap();
    private final Map<StructureGenerator<?>, LongSet> structuresRefences = Maps.newHashMap();
    private final ShortList[] postProcessing;
    private TickList<Block> blockTicks;
    private TickList<FluidType> liquidTicks;
    private volatile boolean unsaved;
    private long inhabitedTime;
    @Nullable
    private Supplier<PlayerChunk.State> fullStatus;
    @Nullable
    private Consumer<Chunk> postLoad;
    private final ChunkCoordIntPair chunkPos;
    private volatile boolean isLightCorrect;
    private final Int2ObjectMap<GameEventDispatcher> gameEventDispatcherSections;

    public Chunk(World world, ChunkCoordIntPair pos, BiomeStorage biomes) {
        this(world, pos, biomes, ChunkConverter.EMPTY, TickListEmpty.empty(), TickListEmpty.empty(), 0L, (ChunkSection[])null, (Consumer<Chunk>)null);
    }

    public Chunk(World world, ChunkCoordIntPair pos, BiomeStorage biomes, ChunkConverter upgradeData, TickList<Block> blockTickScheduler, TickList<FluidType> fluidTickScheduler, long inhabitedTime, @Nullable ChunkSection[] sections, @Nullable Consumer<Chunk> loadToWorldConsumer) {
        this.level = world;
        this.chunkPos = pos;
        this.upgradeData = upgradeData;
        this.gameEventDispatcherSections = new Int2ObjectOpenHashMap<>();

        for(HeightMap.Type types : HeightMap.Type.values()) {
            if (ChunkStatus.FULL.heightmapsAfter().contains(types)) {
                this.heightmaps.put(types, new HeightMap(this, types));
            }
        }

        this.biomes = biomes;
        this.blockTicks = blockTickScheduler;
        this.liquidTicks = fluidTickScheduler;
        this.inhabitedTime = inhabitedTime;
        this.postLoad = loadToWorldConsumer;
        this.sections = new ChunkSection[world.getSectionsCount()];
        if (sections != null) {
            if (this.sections.length == sections.length) {
                System.arraycopy(sections, 0, this.sections, 0, this.sections.length);
            } else {
                LOGGER.warn("Could not set level chunk sections, array length is {} instead of {}", sections.length, this.sections.length);
            }
        }

        this.postProcessing = new ShortList[world.getSectionsCount()];
    }

    public Chunk(WorldServer serverLevel, ProtoChunk protoChunk, @Nullable Consumer<Chunk> consumer) {
        this(serverLevel, protoChunk.getPos(), protoChunk.getBiomeIndex(), protoChunk.getUpgradeData(), protoChunk.getBlockTicks(), protoChunk.getLiquidTicks(), protoChunk.getInhabitedTime(), protoChunk.getSections(), consumer);

        for(TileEntity blockEntity : protoChunk.getBlockEntities().values()) {
            this.setTileEntity(blockEntity);
        }

        this.pendingBlockEntities.putAll(protoChunk.getBlockEntityNbts());

        for(int i = 0; i < protoChunk.getPostProcessing().length; ++i) {
            this.postProcessing[i] = protoChunk.getPostProcessing()[i];
        }

        this.setAllStarts(protoChunk.getAllStarts());
        this.setAllReferences(protoChunk.getAllReferences());

        for(Entry<HeightMap.Type, HeightMap> entry : protoChunk.getHeightmaps()) {
            if (ChunkStatus.FULL.heightmapsAfter().contains(entry.getKey())) {
                this.setHeightmap(entry.getKey(), entry.getValue().getRawData());
            }
        }

        this.setLightCorrect(protoChunk.isLightCorrect());
        this.unsaved = true;
    }

    @Override
    public GameEventDispatcher getEventDispatcher(int ySectionCoord) {
        return this.gameEventDispatcherSections.computeIfAbsent(ySectionCoord, (sectionCoord) -> {
            return new EuclideanGameEventDispatcher(this.level);
        });
    }

    @Override
    public HeightMap getOrCreateHeightmapUnprimed(HeightMap.Type type) {
        return this.heightmaps.computeIfAbsent(type, (typex) -> {
            return new HeightMap(this, typex);
        });
    }

    @Override
    public Set<BlockPosition> getBlockEntitiesPos() {
        Set<BlockPosition> set = Sets.newHashSet(this.pendingBlockEntities.keySet());
        set.addAll(this.blockEntities.keySet());
        return set;
    }

    @Override
    public ChunkSection[] getSections() {
        return this.sections;
    }

    @Override
    public IBlockData getType(BlockPosition pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        if (this.level.isDebugWorld()) {
            IBlockData blockState = null;
            if (j == 60) {
                blockState = Blocks.BARRIER.getBlockData();
            }

            if (j == 70) {
                blockState = ChunkProviderDebug.getBlockStateFor(i, k);
            }

            return blockState == null ? Blocks.AIR.getBlockData() : blockState;
        } else {
            try {
                int l = this.getSectionIndex(j);
                if (l >= 0 && l < this.sections.length) {
                    ChunkSection levelChunkSection = this.sections[l];
                    if (!ChunkSection.isEmpty(levelChunkSection)) {
                        return levelChunkSection.getType(i & 15, j & 15, k & 15);
                    }
                }

                return Blocks.AIR.getBlockData();
            } catch (Throwable var8) {
                CrashReport crashReport = CrashReport.forThrowable(var8, "Getting block state");
                CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Block being got");
                crashReportCategory.setDetail("Location", () -> {
                    return CrashReportSystemDetails.formatLocation(this, i, j, k);
                });
                throw new ReportedException(crashReport);
            }
        }
    }

    @Override
    public Fluid getFluid(BlockPosition pos) {
        return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
    }

    public Fluid getFluidState(int x, int y, int z) {
        try {
            int i = this.getSectionIndex(y);
            if (i >= 0 && i < this.sections.length) {
                ChunkSection levelChunkSection = this.sections[i];
                if (!ChunkSection.isEmpty(levelChunkSection)) {
                    return levelChunkSection.getFluidState(x & 15, y & 15, z & 15);
                }
            }

            return FluidTypes.EMPTY.defaultFluidState();
        } catch (Throwable var7) {
            CrashReport crashReport = CrashReport.forThrowable(var7, "Getting fluid state");
            CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Block being got");
            crashReportCategory.setDetail("Location", () -> {
                return CrashReportSystemDetails.formatLocation(this, x, y, z);
            });
            throw new ReportedException(crashReport);
        }
    }

    @Nullable
    @Override
    public IBlockData setType(BlockPosition pos, IBlockData state, boolean moved) {
        int i = pos.getY();
        int j = this.getSectionIndex(i);
        ChunkSection levelChunkSection = this.sections[j];
        if (levelChunkSection == EMPTY_SECTION) {
            if (state.isAir()) {
                return null;
            }

            levelChunkSection = new ChunkSection(SectionPosition.blockToSectionCoord(i));
            this.sections[j] = levelChunkSection;
        }

        boolean bl = levelChunkSection.isEmpty();
        int k = pos.getX() & 15;
        int l = i & 15;
        int m = pos.getZ() & 15;
        IBlockData blockState = levelChunkSection.setType(k, l, m, state);
        if (blockState == state) {
            return null;
        } else {
            Block block = state.getBlock();
            this.heightmaps.get(HeightMap.Type.MOTION_BLOCKING).update(k, i, m, state);
            this.heightmaps.get(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES).update(k, i, m, state);
            this.heightmaps.get(HeightMap.Type.OCEAN_FLOOR).update(k, i, m, state);
            this.heightmaps.get(HeightMap.Type.WORLD_SURFACE).update(k, i, m, state);
            boolean bl2 = levelChunkSection.isEmpty();
            if (bl != bl2) {
                this.level.getChunkProvider().getLightEngine().updateSectionStatus(pos, bl2);
            }

            boolean bl3 = blockState.isTileEntity();
            if (!this.level.isClientSide) {
                blockState.remove(this.level, pos, state, moved);
            } else if (!blockState.is(block) && bl3) {
                this.removeTileEntity(pos);
            }

            if (!levelChunkSection.getType(k, l, m).is(block)) {
                return null;
            } else {
                if (!this.level.isClientSide) {
                    state.onPlace(this.level, pos, blockState, moved);
                }

                if (state.isTileEntity()) {
                    TileEntity blockEntity = this.getBlockEntity(pos, Chunk.EnumTileEntityState.CHECK);
                    if (blockEntity == null) {
                        blockEntity = ((ITileEntity)block).createTile(pos, state);
                        if (blockEntity != null) {
                            this.addAndRegisterBlockEntity(blockEntity);
                        }
                    } else {
                        blockEntity.setBlockState(state);
                        this.updateBlockEntityTicker(blockEntity);
                    }
                }

                this.unsaved = true;
                return blockState;
            }
        }
    }

    @Deprecated
    @Override
    public void addEntity(Entity entity) {
    }

    @Override
    public int getHighestBlock(HeightMap.Type type, int x, int z) {
        return this.heightmaps.get(type).getFirstAvailable(x & 15, z & 15) - 1;
    }

    @Override
    public BlockPosition getHeighestPosition(HeightMap.Type types) {
        ChunkCoordIntPair chunkPos = this.getPos();
        int i = this.getMinBuildHeight();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int j = chunkPos.getMinBlockX(); j <= chunkPos.getMaxBlockX(); ++j) {
            for(int k = chunkPos.getMinBlockZ(); k <= chunkPos.getMaxBlockZ(); ++k) {
                int l = this.getHighestBlock(types, j & 15, k & 15);
                if (l > i) {
                    i = l;
                    mutableBlockPos.set(j, l, k);
                }
            }
        }

        return mutableBlockPos.immutableCopy();
    }

    @Nullable
    private TileEntity createBlockEntity(BlockPosition pos) {
        IBlockData blockState = this.getType(pos);
        return !blockState.isTileEntity() ? null : ((ITileEntity)blockState.getBlock()).createTile(pos, blockState);
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition pos) {
        return this.getBlockEntity(pos, Chunk.EnumTileEntityState.CHECK);
    }

    @Nullable
    public TileEntity getBlockEntity(BlockPosition pos, Chunk.EnumTileEntityState creationType) {
        TileEntity blockEntity = this.blockEntities.get(pos);
        if (blockEntity == null) {
            NBTTagCompound compoundTag = this.pendingBlockEntities.remove(pos);
            if (compoundTag != null) {
                TileEntity blockEntity2 = this.promotePendingBlockEntity(pos, compoundTag);
                if (blockEntity2 != null) {
                    return blockEntity2;
                }
            }
        }

        if (blockEntity == null) {
            if (creationType == Chunk.EnumTileEntityState.IMMEDIATE) {
                blockEntity = this.createBlockEntity(pos);
                if (blockEntity != null) {
                    this.addAndRegisterBlockEntity(blockEntity);
                }
            }
        } else if (blockEntity.isRemoved()) {
            this.blockEntities.remove(pos);
            return null;
        }

        return blockEntity;
    }

    public void addAndRegisterBlockEntity(TileEntity blockEntity) {
        this.setTileEntity(blockEntity);
        if (this.isInLevel()) {
            this.addGameEventListener(blockEntity);
            this.updateBlockEntityTicker(blockEntity);
        }

    }

    private boolean isInLevel() {
        return this.loaded || this.level.isClientSide();
    }

    boolean isTicking(BlockPosition pos) {
        if (!this.level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        } else if (!(this.level instanceof WorldServer)) {
            return true;
        } else {
            return this.getState().isAtLeast(PlayerChunk.State.TICKING) && ((WorldServer)this.level).areEntitiesLoaded(ChunkCoordIntPair.asLong(pos));
        }
    }

    @Override
    public void setTileEntity(TileEntity blockEntity) {
        BlockPosition blockPos = blockEntity.getPosition();
        if (this.getType(blockPos).isTileEntity()) {
            blockEntity.setWorld(this.level);
            blockEntity.clearRemoved();
            TileEntity blockEntity2 = this.blockEntities.put(blockPos.immutableCopy(), blockEntity);
            if (blockEntity2 != null && blockEntity2 != blockEntity) {
                blockEntity2.setRemoved();
            }

        }
    }

    @Override
    public void setBlockEntityNbt(NBTTagCompound nbt) {
        this.pendingBlockEntities.put(new BlockPosition(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z")), nbt);
    }

    @Nullable
    @Override
    public NBTTagCompound getBlockEntityNbtForSaving(BlockPosition pos) {
        TileEntity blockEntity = this.getTileEntity(pos);
        if (blockEntity != null && !blockEntity.isRemoved()) {
            NBTTagCompound compoundTag = blockEntity.save(new NBTTagCompound());
            compoundTag.setBoolean("keepPacked", false);
            return compoundTag;
        } else {
            NBTTagCompound compoundTag2 = this.pendingBlockEntities.get(pos);
            if (compoundTag2 != null) {
                compoundTag2 = compoundTag2.c();
                compoundTag2.setBoolean("keepPacked", true);
            }

            return compoundTag2;
        }
    }

    @Override
    public void removeTileEntity(BlockPosition pos) {
        if (this.isInLevel()) {
            TileEntity blockEntity = this.blockEntities.remove(pos);
            if (blockEntity != null) {
                this.removeGameEventListener(blockEntity);
                blockEntity.setRemoved();
            }
        }

        this.removeBlockEntityTicker(pos);
    }

    private <T extends TileEntity> void removeGameEventListener(T blockEntity) {
        if (!this.level.isClientSide) {
            Block block = blockEntity.getBlock().getBlock();
            if (block instanceof ITileEntity) {
                GameEventListener gameEventListener = ((ITileEntity)block).getListener(this.level, blockEntity);
                if (gameEventListener != null) {
                    int i = SectionPosition.blockToSectionCoord(blockEntity.getPosition().getY());
                    GameEventDispatcher gameEventDispatcher = this.getEventDispatcher(i);
                    gameEventDispatcher.unregister(gameEventListener);
                    if (gameEventDispatcher.isEmpty()) {
                        this.gameEventDispatcherSections.remove(i);
                    }
                }
            }

        }
    }

    private void removeBlockEntityTicker(BlockPosition pos) {
        Chunk.RebindableTickingBlockEntityWrapper rebindableTickingBlockEntityWrapper = this.tickersInLevel.remove(pos);
        if (rebindableTickingBlockEntityWrapper != null) {
            rebindableTickingBlockEntityWrapper.rebind(NULL_TICKER);
        }

    }

    public void addEntities() {
        if (this.postLoad != null) {
            this.postLoad.accept(this);
            this.postLoad = null;
        }

    }

    public void markDirty() {
        this.unsaved = true;
    }

    public boolean isEmpty() {
        return false;
    }

    @Override
    public ChunkCoordIntPair getPos() {
        return this.chunkPos;
    }

    public void replaceWithPacketData(@Nullable BiomeStorage biomes, PacketDataSerializer buf, NBTTagCompound nbt, BitSet bitSet) {
        boolean bl = biomes != null;
        if (bl) {
            this.blockEntities.values().forEach(this::onBlockEntityRemove);
            this.blockEntities.clear();
        } else {
            this.blockEntities.values().removeIf((blockEntity) -> {
                int i = this.getSectionIndex(blockEntity.getPosition().getY());
                if (bitSet.get(i)) {
                    blockEntity.setRemoved();
                    return true;
                } else {
                    return false;
                }
            });
        }

        for(int i = 0; i < this.sections.length; ++i) {
            ChunkSection levelChunkSection = this.sections[i];
            if (!bitSet.get(i)) {
                if (bl && levelChunkSection != EMPTY_SECTION) {
                    this.sections[i] = EMPTY_SECTION;
                }
            } else {
                if (levelChunkSection == EMPTY_SECTION) {
                    levelChunkSection = new ChunkSection(this.getSectionYFromSectionIndex(i));
                    this.sections[i] = levelChunkSection;
                }

                levelChunkSection.read(buf);
            }
        }

        if (biomes != null) {
            this.biomes = biomes;
        }

        for(HeightMap.Type types : HeightMap.Type.values()) {
            String string = types.getSerializationKey();
            if (nbt.hasKeyOfType(string, 12)) {
                this.setHeightmap(types, nbt.getLongArray(string));
            }
        }

    }

    private void onBlockEntityRemove(TileEntity blockEntity) {
        blockEntity.setRemoved();
        this.tickersInLevel.remove(blockEntity.getPosition());
    }

    @Override
    public BiomeStorage getBiomeIndex() {
        return this.biomes;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public World getWorld() {
        return this.level;
    }

    @Override
    public Collection<Entry<HeightMap.Type, HeightMap>> getHeightmaps() {
        return Collections.unmodifiableSet(this.heightmaps.entrySet());
    }

    public Map<BlockPosition, TileEntity> getTileEntities() {
        return this.blockEntities;
    }

    @Override
    public NBTTagCompound getBlockEntityNbt(BlockPosition pos) {
        return this.pendingBlockEntities.get(pos);
    }

    @Override
    public Stream<BlockPosition> getLights() {
        return StreamSupport.stream(BlockPosition.betweenClosed(this.chunkPos.getMinBlockX(), this.getMinBuildHeight(), this.chunkPos.getMinBlockZ(), this.chunkPos.getMaxBlockX(), this.getMaxBuildHeight() - 1, this.chunkPos.getMaxBlockZ()).spliterator(), false).filter((blockPos) -> {
            return this.getType(blockPos).getLightEmission() != 0;
        });
    }

    @Override
    public TickList<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public TickList<FluidType> getLiquidTicks() {
        return this.liquidTicks;
    }

    @Override
    public void setNeedsSaving(boolean shouldSave) {
        this.unsaved = shouldSave;
    }

    @Override
    public boolean isNeedsSaving() {
        return this.unsaved;
    }

    @Nullable
    @Override
    public StructureStart<?> getStartForFeature(StructureGenerator<?> structure) {
        return this.structureStarts.get(structure);
    }

    @Override
    public void setStartForFeature(StructureGenerator<?> structure, StructureStart<?> start) {
        this.structureStarts.put(structure, start);
    }

    @Override
    public Map<StructureGenerator<?>, StructureStart<?>> getAllStarts() {
        return this.structureStarts;
    }

    @Override
    public void setAllStarts(Map<StructureGenerator<?>, StructureStart<?>> structureStarts) {
        this.structureStarts.clear();
        this.structureStarts.putAll(structureStarts);
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
    }

    @Override
    public Map<StructureGenerator<?>, LongSet> getAllReferences() {
        return this.structuresRefences;
    }

    @Override
    public void setAllReferences(Map<StructureGenerator<?>, LongSet> structureReferences) {
        this.structuresRefences.clear();
        this.structuresRefences.putAll(structureReferences);
    }

    @Override
    public long getInhabitedTime() {
        return this.inhabitedTime;
    }

    @Override
    public void setInhabitedTime(long inhabitedTime) {
        this.inhabitedTime = inhabitedTime;
    }

    public void postProcessGeneration() {
        ChunkCoordIntPair chunkPos = this.getPos();

        for(int i = 0; i < this.postProcessing.length; ++i) {
            if (this.postProcessing[i] != null) {
                for(Short short_ : this.postProcessing[i]) {
                    BlockPosition blockPos = ProtoChunk.unpackOffsetCoordinates(short_, this.getSectionYFromSectionIndex(i), chunkPos);
                    IBlockData blockState = this.getType(blockPos);
                    IBlockData blockState2 = Block.updateFromNeighbourShapes(blockState, this.level, blockPos);
                    this.level.setTypeAndData(blockPos, blockState2, 20);
                }

                this.postProcessing[i].clear();
            }
        }

        this.unpackTicks();

        for(BlockPosition blockPos2 : ImmutableList.copyOf(this.pendingBlockEntities.keySet())) {
            this.getTileEntity(blockPos2);
        }

        this.pendingBlockEntities.clear();
        this.upgradeData.upgrade(this);
    }

    @Nullable
    private TileEntity promotePendingBlockEntity(BlockPosition pos, NBTTagCompound nbt) {
        IBlockData blockState = this.getType(pos);
        TileEntity blockEntity;
        if ("DUMMY".equals(nbt.getString("id"))) {
            if (blockState.isTileEntity()) {
                blockEntity = ((ITileEntity)blockState.getBlock()).createTile(pos, blockState);
            } else {
                blockEntity = null;
                LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", pos, blockState);
            }
        } else {
            blockEntity = TileEntity.create(pos, blockState, nbt);
        }

        if (blockEntity != null) {
            blockEntity.setWorld(this.level);
            this.addAndRegisterBlockEntity(blockEntity);
        } else {
            LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", blockState, pos);
        }

        return blockEntity;
    }

    @Override
    public ChunkConverter getUpgradeData() {
        return this.upgradeData;
    }

    @Override
    public ShortList[] getPostProcessing() {
        return this.postProcessing;
    }

    public void unpackTicks() {
        if (this.blockTicks instanceof ProtoChunkTickList) {
            ((ProtoChunkTickList)this.blockTicks).copyOut(this.level.getBlockTickList(), (pos) -> {
                return this.getType(pos).getBlock();
            });
            this.blockTicks = TickListEmpty.empty();
        } else if (this.blockTicks instanceof TickListChunk) {
            ((TickListChunk)this.blockTicks).copyOut(this.level.getBlockTickList());
            this.blockTicks = TickListEmpty.empty();
        }

        if (this.liquidTicks instanceof ProtoChunkTickList) {
            ((ProtoChunkTickList)this.liquidTicks).copyOut(this.level.getFluidTickList(), (pos) -> {
                return this.getFluid(pos).getType();
            });
            this.liquidTicks = TickListEmpty.empty();
        } else if (this.liquidTicks instanceof TickListChunk) {
            ((TickListChunk)this.liquidTicks).copyOut(this.level.getFluidTickList());
            this.liquidTicks = TickListEmpty.empty();
        }

    }

    public void packTicks(WorldServer world) {
        if (this.blockTicks == TickListEmpty.empty()) {
            this.blockTicks = new TickListChunk<>(IRegistry.BLOCK::getKey, world.getBlockTicks().fetchTicksInChunk(this.chunkPos, true, false), world.getTime());
            this.setNeedsSaving(true);
        }

        if (this.liquidTicks == TickListEmpty.empty()) {
            this.liquidTicks = new TickListChunk<>(IRegistry.FLUID::getKey, world.getLiquidTicks().fetchTicksInChunk(this.chunkPos, true, false), world.getTime());
            this.setNeedsSaving(true);
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

    @Override
    public ChunkStatus getChunkStatus() {
        return ChunkStatus.FULL;
    }

    public PlayerChunk.State getState() {
        return this.fullStatus == null ? PlayerChunk.State.BORDER : this.fullStatus.get();
    }

    public void setFullStatus(Supplier<PlayerChunk.State> levelTypeProvider) {
        this.fullStatus = levelTypeProvider;
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

    public void invalidateAllBlockEntities() {
        this.blockEntities.values().forEach(this::onBlockEntityRemove);
    }

    public void registerAllBlockEntitiesAfterLevelLoad() {
        this.blockEntities.values().forEach((blockEntity) -> {
            this.addGameEventListener(blockEntity);
            this.updateBlockEntityTicker(blockEntity);
        });
    }

    private <T extends TileEntity> void addGameEventListener(T blockEntity) {
        if (!this.level.isClientSide) {
            Block block = blockEntity.getBlock().getBlock();
            if (block instanceof ITileEntity) {
                GameEventListener gameEventListener = ((ITileEntity)block).getListener(this.level, blockEntity);
                if (gameEventListener != null) {
                    GameEventDispatcher gameEventDispatcher = this.getEventDispatcher(SectionPosition.blockToSectionCoord(blockEntity.getPosition().getY()));
                    gameEventDispatcher.register(gameEventListener);
                }
            }

        }
    }

    private <T extends TileEntity> void updateBlockEntityTicker(T blockEntity) {
        IBlockData blockState = blockEntity.getBlock();
        BlockEntityTicker<T> blockEntityTicker = blockState.getTicker(this.level, blockEntity.getTileType());
        if (blockEntityTicker == null) {
            this.removeBlockEntityTicker(blockEntity.getPosition());
        } else {
            this.tickersInLevel.compute(blockEntity.getPosition(), (pos, rebindableTickingBlockEntityWrapper) -> {
                TickingBlockEntity tickingBlockEntity = this.createTicker(blockEntity, blockEntityTicker);
                if (rebindableTickingBlockEntityWrapper != null) {
                    rebindableTickingBlockEntityWrapper.rebind(tickingBlockEntity);
                    return rebindableTickingBlockEntityWrapper;
                } else if (this.isInLevel()) {
                    Chunk.RebindableTickingBlockEntityWrapper rebindableTickingBlockEntityWrapper2 = new Chunk.RebindableTickingBlockEntityWrapper(tickingBlockEntity);
                    this.level.addBlockEntityTicker(rebindableTickingBlockEntityWrapper2);
                    return rebindableTickingBlockEntityWrapper2;
                } else {
                    return null;
                }
            });
        }

    }

    private <T extends TileEntity> TickingBlockEntity createTicker(T blockEntity, BlockEntityTicker<T> blockEntityTicker) {
        return new Chunk.BoundTickingBlockEntity<>(blockEntity, blockEntityTicker);
    }

    class BoundTickingBlockEntity<T extends TileEntity> implements TickingBlockEntity {
        private final T blockEntity;
        private final BlockEntityTicker<T> ticker;
        private boolean loggedInvalidBlockState;

        BoundTickingBlockEntity(T blockEntity, BlockEntityTicker<T> ticker) {
            this.blockEntity = blockEntity;
            this.ticker = ticker;
        }

        @Override
        public void tick() {
            if (!this.blockEntity.isRemoved() && this.blockEntity.hasWorld()) {
                BlockPosition blockPos = this.blockEntity.getPosition();
                if (Chunk.this.isTicking(blockPos)) {
                    try {
                        GameProfilerFiller profilerFiller = Chunk.this.level.getMethodProfiler();
                        profilerFiller.push(this::getType);
                        IBlockData blockState = Chunk.this.getType(blockPos);
                        if (this.blockEntity.getTileType().isValidBlock(blockState)) {
                            this.ticker.tick(Chunk.this.level, this.blockEntity.getPosition(), blockState, this.blockEntity);
                            this.loggedInvalidBlockState = false;
                        } else if (!this.loggedInvalidBlockState) {
                            this.loggedInvalidBlockState = true;
                            Chunk.LOGGER.warn("Block entity {} @ {} state {} invalid for ticking:", this::getType, this::getPos, () -> {
                                return blockState;
                            });
                        }

                        profilerFiller.exit();
                    } catch (Throwable var5) {
                        CrashReport crashReport = CrashReport.forThrowable(var5, "Ticking block entity");
                        CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Block entity being ticked");
                        this.blockEntity.fillCrashReportCategory(crashReportCategory);
                        throw new ReportedException(crashReport);
                    }
                }
            }

        }

        @Override
        public boolean isRemoved() {
            return this.blockEntity.isRemoved();
        }

        @Override
        public BlockPosition getPos() {
            return this.blockEntity.getPosition();
        }

        @Override
        public String getType() {
            return TileEntityTypes.getKey(this.blockEntity.getTileType()).toString();
        }

        @Override
        public String toString() {
            return "Level ticker for " + this.getType() + "@" + this.getPos();
        }
    }

    public static enum EnumTileEntityState {
        IMMEDIATE,
        QUEUED,
        CHECK;
    }

    class RebindableTickingBlockEntityWrapper implements TickingBlockEntity {
        private TickingBlockEntity ticker;

        RebindableTickingBlockEntityWrapper(TickingBlockEntity wrapped) {
            this.ticker = wrapped;
        }

        void rebind(TickingBlockEntity wrapped) {
            this.ticker = wrapped;
        }

        @Override
        public void tick() {
            this.ticker.tick();
        }

        @Override
        public boolean isRemoved() {
            return this.ticker.isRemoved();
        }

        @Override
        public BlockPosition getPos() {
            return this.ticker.getPos();
        }

        @Override
        public String getType() {
            return this.ticker.getType();
        }

        @Override
        public String toString() {
            return this.ticker.toString() + " <wrapped>";
        }
    }
}
