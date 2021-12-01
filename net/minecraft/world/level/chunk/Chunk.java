package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;
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
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.PlayerChunk;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockFluids;
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
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Chunk extends IChunkAccess {
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
    private final Map<BlockPosition, Chunk.RebindableTickingBlockEntityWrapper> tickersInLevel = Maps.newHashMap();
    public boolean loaded;
    private boolean clientLightReady = false;
    public final World level;
    @Nullable
    private Supplier<PlayerChunk.State> fullStatus;
    @Nullable
    private LevelChunk$PostLoadProcessor postLoad;
    private final Int2ObjectMap<GameEventDispatcher> gameEventDispatcherSections;
    private final LevelChunkTicks<Block> blockTicks;
    private final LevelChunkTicks<FluidType> fluidTicks;

    public Chunk(World world, ChunkCoordIntPair pos) {
        this(world, pos, ChunkConverter.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, (ChunkSection[])null, (LevelChunk$PostLoadProcessor)null, (BlendingData)null);
    }

    public Chunk(World world, ChunkCoordIntPair pos, ChunkConverter upgradeData, LevelChunkTicks<Block> blockTickScheduler, LevelChunkTicks<FluidType> fluidTickScheduler, long inhabitedTime, @Nullable ChunkSection[] sectionArrayInitializer, @Nullable LevelChunk$PostLoadProcessor entityLoader, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, world, world.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY), inhabitedTime, sectionArrayInitializer, blendingData);
        this.level = world;
        this.gameEventDispatcherSections = new Int2ObjectOpenHashMap<>();

        for(HeightMap.Type types : HeightMap.Type.values()) {
            if (ChunkStatus.FULL.heightmapsAfter().contains(types)) {
                this.heightmaps.put(types, new HeightMap(this, types));
            }
        }

        this.postLoad = entityLoader;
        this.blockTicks = blockTickScheduler;
        this.fluidTicks = fluidTickScheduler;
    }

    public Chunk(WorldServer world, ProtoChunk protoChunk, @Nullable LevelChunk$PostLoadProcessor entityLoader) {
        this(world, protoChunk.getPos(), protoChunk.getUpgradeData(), protoChunk.unpackBlockTicks(), protoChunk.unpackFluidTicks(), protoChunk.getInhabitedTime(), protoChunk.getSections(), entityLoader, protoChunk.getBlendingData());

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
    public GameEventDispatcher getEventDispatcher(int ySectionCoord) {
        return this.gameEventDispatcherSections.computeIfAbsent(ySectionCoord, (sectionCoord) -> {
            return new EuclideanGameEventDispatcher(this.level);
        });
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
                    if (!levelChunkSection.hasOnlyAir()) {
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
                if (!levelChunkSection.hasOnlyAir()) {
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
        ChunkSection levelChunkSection = this.getSection(this.getSectionIndex(i));
        boolean bl = levelChunkSection.hasOnlyAir();
        if (bl && state.isAir()) {
            return null;
        } else {
            int j = pos.getX() & 15;
            int k = i & 15;
            int l = pos.getZ() & 15;
            IBlockData blockState = levelChunkSection.setType(j, k, l, state);
            if (blockState == state) {
                return null;
            } else {
                Block block = state.getBlock();
                this.heightmaps.get(HeightMap.Type.MOTION_BLOCKING).update(j, i, l, state);
                this.heightmaps.get(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES).update(j, i, l, state);
                this.heightmaps.get(HeightMap.Type.OCEAN_FLOOR).update(j, i, l, state);
                this.heightmaps.get(HeightMap.Type.WORLD_SURFACE).update(j, i, l, state);
                boolean bl2 = levelChunkSection.hasOnlyAir();
                if (bl != bl2) {
                    this.level.getChunkProvider().getLightEngine().updateSectionStatus(pos, bl2);
                }

                boolean bl3 = blockState.isTileEntity();
                if (!this.level.isClientSide) {
                    blockState.remove(this.level, pos, state, moved);
                } else if (!blockState.is(block) && bl3) {
                    this.removeTileEntity(pos);
                }

                if (!levelChunkSection.getType(j, k, l).is(block)) {
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
    }

    /** @deprecated */
    @Deprecated
    @Override
    public void addEntity(Entity entity) {
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
        } else {
            World var3 = this.level;
            if (!(var3 instanceof WorldServer)) {
                return true;
            } else {
                WorldServer serverLevel = (WorldServer)var3;
                return this.getState().isAtLeast(PlayerChunk.State.TICKING) && serverLevel.areEntitiesLoaded(ChunkCoordIntPair.asLong(pos));
            }
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

    @Nullable
    @Override
    public NBTTagCompound getBlockEntityNbtForSaving(BlockPosition pos) {
        TileEntity blockEntity = this.getTileEntity(pos);
        if (blockEntity != null && !blockEntity.isRemoved()) {
            NBTTagCompound compoundTag = blockEntity.saveWithFullMetadata();
            compoundTag.setBoolean("keepPacked", false);
            return compoundTag;
        } else {
            NBTTagCompound compoundTag2 = this.pendingBlockEntities.get(pos);
            if (compoundTag2 != null) {
                compoundTag2 = compoundTag2.copy();
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
            this.postLoad.run(this);
            this.postLoad = null;
        }

    }

    public boolean isEmpty() {
        return false;
    }

    public void replaceWithPacketData(PacketDataSerializer buf, NBTTagCompound nbt, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer) {
        this.clearAllBlockEntities();

        for(ChunkSection levelChunkSection : this.sections) {
            levelChunkSection.read(buf);
        }

        for(HeightMap.Type types : HeightMap.Type.values()) {
            String string = types.getSerializationKey();
            if (nbt.hasKeyOfType(string, 12)) {
                this.setHeightmap(types, nbt.getLongArray(string));
            }
        }

        consumer.accept((pos, blockEntityType, nbtx) -> {
            TileEntity blockEntity = this.getBlockEntity(pos, Chunk.EnumTileEntityState.IMMEDIATE);
            if (blockEntity != null && nbtx != null && blockEntity.getTileType() == blockEntityType) {
                blockEntity.load(nbtx);
            }

        });
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public World getWorld() {
        return this.level;
    }

    public Map<BlockPosition, TileEntity> getTileEntities() {
        return this.blockEntities;
    }

    @Override
    public Stream<BlockPosition> getLights() {
        return StreamSupport.stream(BlockPosition.betweenClosed(this.chunkPos.getMinBlockX(), this.getMinBuildHeight(), this.chunkPos.getMinBlockZ(), this.chunkPos.getMaxBlockX(), this.getMaxBuildHeight() - 1, this.chunkPos.getMaxBlockZ()).spliterator(), false).filter((blockPos) -> {
            return this.getType(blockPos).getLightEmission() != 0;
        });
    }

    public void postProcessGeneration() {
        ChunkCoordIntPair chunkPos = this.getPos();

        for(int i = 0; i < this.postProcessing.length; ++i) {
            if (this.postProcessing[i] != null) {
                for(Short short_ : this.postProcessing[i]) {
                    BlockPosition blockPos = ProtoChunk.unpackOffsetCoordinates(short_, this.getSectionYFromSectionIndex(i), chunkPos);
                    IBlockData blockState = this.getType(blockPos);
                    Fluid fluidState = blockState.getFluid();
                    if (!fluidState.isEmpty()) {
                        fluidState.tick(this.level, blockPos);
                    }

                    if (!(blockState.getBlock() instanceof BlockFluids)) {
                        IBlockData blockState2 = Block.updateFromNeighbourShapes(blockState, this.level, blockPos);
                        this.level.setTypeAndData(blockPos, blockState2, 20);
                    }
                }

                this.postProcessing[i].clear();
            }
        }

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

    public void unpackTicks(long time) {
        this.blockTicks.unpack(time);
        this.fluidTicks.unpack(time);
    }

    public void registerTickContainerInLevel(WorldServer world) {
        world.getBlockTicks().addContainer(this.chunkPos, this.blockTicks);
        world.getFluidTicks().addContainer(this.chunkPos, this.fluidTicks);
    }

    public void unregisterTickContainerFromLevel(WorldServer world) {
        world.getBlockTicks().removeContainer(this.chunkPos);
        world.getFluidTicks().removeContainer(this.chunkPos);
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

    public void clearAllBlockEntities() {
        this.blockEntities.values().forEach(TileEntity::setRemoved);
        this.blockEntities.clear();
        this.tickersInLevel.values().forEach((ticker) -> {
            ticker.rebind(NULL_TICKER);
        });
        this.tickersInLevel.clear();
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

    public boolean isClientLightReady() {
        return this.clientLightReady;
    }

    public void setClientLightReady(boolean shouldRenderOnUpdate) {
        this.clientLightReady = shouldRenderOnUpdate;
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
