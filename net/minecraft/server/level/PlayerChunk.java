package net.minecraft.server.level;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutLightUpdate;
import net.minecraft.network.protocol.game.PacketPlayOutMultiBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.util.DebugBuffer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunkExtension;
import net.minecraft.world.level.lighting.LightEngine;

public class PlayerChunk {
    public static final Either<IChunkAccess, PlayerChunk.Failure> UNLOADED_CHUNK = Either.right(PlayerChunk.Failure.UNLOADED);
    public static final CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> UNLOADED_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_CHUNK);
    public static final Either<Chunk, PlayerChunk.Failure> UNLOADED_LEVEL_CHUNK = Either.right(PlayerChunk.Failure.UNLOADED);
    private static final CompletableFuture<Either<Chunk, PlayerChunk.Failure>> UNLOADED_LEVEL_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_LEVEL_CHUNK);
    private static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.getStatusList();
    private static final PlayerChunk.State[] FULL_CHUNK_STATUSES = PlayerChunk.State.values();
    private static final int BLOCKS_BEFORE_RESEND_FUDGE = 64;
    private final AtomicReferenceArray<CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>>> futures = new AtomicReferenceArray<>(CHUNK_STATUSES.size());
    private final IWorldHeightAccess levelHeightAccessor;
    private volatile CompletableFuture<Either<Chunk, PlayerChunk.Failure>> fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private volatile CompletableFuture<Either<Chunk, PlayerChunk.Failure>> tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private volatile CompletableFuture<Either<Chunk, PlayerChunk.Failure>> entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private CompletableFuture<IChunkAccess> chunkToSave = CompletableFuture.completedFuture((IChunkAccess)null);
    @Nullable
    private final DebugBuffer<PlayerChunk.ChunkSaveDebug> chunkToSaveHistory = null;
    public int oldTicketLevel;
    private int ticketLevel;
    private int queueLevel;
    public final ChunkCoordIntPair pos;
    private boolean hasChangedSections;
    private final ShortSet[] changedBlocksPerSection;
    private final BitSet blockChangedLightSectionFilter = new BitSet();
    private final BitSet skyChangedLightSectionFilter = new BitSet();
    private final LightEngine lightEngine;
    private final PlayerChunk.LevelChangeListener onLevelChange;
    public final PlayerChunk.PlayerProvider playerProvider;
    private boolean wasAccessibleSinceLastSave;
    private boolean resendLight;
    private CompletableFuture<Void> pendingFullStateConfirmation = CompletableFuture.completedFuture((Void)null);

    public PlayerChunk(ChunkCoordIntPair pos, int level, IWorldHeightAccess world, LightEngine lightingProvider, PlayerChunk.LevelChangeListener levelUpdateListener, PlayerChunk.PlayerProvider playersWatchingChunkProvider) {
        this.pos = pos;
        this.levelHeightAccessor = world;
        this.lightEngine = lightingProvider;
        this.onLevelChange = levelUpdateListener;
        this.playerProvider = playersWatchingChunkProvider;
        this.oldTicketLevel = PlayerChunkMap.MAX_CHUNK_DISTANCE + 1;
        this.ticketLevel = this.oldTicketLevel;
        this.queueLevel = this.oldTicketLevel;
        this.setTicketLevel(level);
        this.changedBlocksPerSection = new ShortSet[world.getSectionsCount()];
    }

    public CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> getStatusFutureUnchecked(ChunkStatus leastStatus) {
        CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture = this.futures.get(leastStatus.getIndex());
        return completableFuture == null ? UNLOADED_CHUNK_FUTURE : completableFuture;
    }

    public CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> getFutureIfPresent(ChunkStatus leastStatus) {
        return getChunkStatus(this.ticketLevel).isOrAfter(leastStatus) ? this.getStatusFutureUnchecked(leastStatus) : UNLOADED_CHUNK_FUTURE;
    }

    public CompletableFuture<Either<Chunk, PlayerChunk.Failure>> getTickingChunkFuture() {
        return this.tickingChunkFuture;
    }

    public CompletableFuture<Either<Chunk, PlayerChunk.Failure>> getEntityTickingChunkFuture() {
        return this.entityTickingChunkFuture;
    }

    public CompletableFuture<Either<Chunk, PlayerChunk.Failure>> getFullChunkFuture() {
        return this.fullChunkFuture;
    }

    @Nullable
    public Chunk getChunk() {
        CompletableFuture<Either<Chunk, PlayerChunk.Failure>> completableFuture = this.getTickingChunkFuture();
        Either<Chunk, PlayerChunk.Failure> either = completableFuture.getNow((Either<Chunk, PlayerChunk.Failure>)null);
        return either == null ? null : either.left().orElse((Chunk)null);
    }

    @Nullable
    public ChunkStatus getLastAvailableStatus() {
        for(int i = CHUNK_STATUSES.size() - 1; i >= 0; --i) {
            ChunkStatus chunkStatus = CHUNK_STATUSES.get(i);
            CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture = this.getStatusFutureUnchecked(chunkStatus);
            if (completableFuture.getNow(UNLOADED_CHUNK).left().isPresent()) {
                return chunkStatus;
            }
        }

        return null;
    }

    @Nullable
    public IChunkAccess getLastAvailable() {
        for(int i = CHUNK_STATUSES.size() - 1; i >= 0; --i) {
            ChunkStatus chunkStatus = CHUNK_STATUSES.get(i);
            CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture = this.getStatusFutureUnchecked(chunkStatus);
            if (!completableFuture.isCompletedExceptionally()) {
                Optional<IChunkAccess> optional = completableFuture.getNow(UNLOADED_CHUNK).left();
                if (optional.isPresent()) {
                    return optional.get();
                }
            }
        }

        return null;
    }

    public CompletableFuture<IChunkAccess> getChunkSave() {
        return this.chunkToSave;
    }

    public void blockChanged(BlockPosition pos) {
        Chunk levelChunk = this.getChunk();
        if (levelChunk != null) {
            int i = this.levelHeightAccessor.getSectionIndex(pos.getY());
            if (this.changedBlocksPerSection[i] == null) {
                this.hasChangedSections = true;
                this.changedBlocksPerSection[i] = new ShortArraySet();
            }

            this.changedBlocksPerSection[i].add(SectionPosition.sectionRelativePos(pos));
        }
    }

    public void sectionLightChanged(EnumSkyBlock lightType, int y) {
        Chunk levelChunk = this.getChunk();
        if (levelChunk != null) {
            levelChunk.setNeedsSaving(true);
            int i = this.lightEngine.getMinLightSection();
            int j = this.lightEngine.getMaxLightSection();
            if (y >= i && y <= j) {
                int k = y - i;
                if (lightType == EnumSkyBlock.SKY) {
                    this.skyChangedLightSectionFilter.set(k);
                } else {
                    this.blockChangedLightSectionFilter.set(k);
                }

            }
        }
    }

    public void broadcastChanges(Chunk chunk) {
        if (this.hasChangedSections || !this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
            World level = chunk.getWorld();
            int i = 0;

            for(int j = 0; j < this.changedBlocksPerSection.length; ++j) {
                i += this.changedBlocksPerSection[j] != null ? this.changedBlocksPerSection[j].size() : 0;
            }

            this.resendLight |= i >= 64;
            if (!this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
                this.broadcast(new PacketPlayOutLightUpdate(chunk.getPos(), this.lightEngine, this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter, true), !this.resendLight);
                this.skyChangedLightSectionFilter.clear();
                this.blockChangedLightSectionFilter.clear();
            }

            for(int k = 0; k < this.changedBlocksPerSection.length; ++k) {
                ShortSet shortSet = this.changedBlocksPerSection[k];
                if (shortSet != null) {
                    int l = this.levelHeightAccessor.getSectionYFromSectionIndex(k);
                    SectionPosition sectionPos = SectionPosition.of(chunk.getPos(), l);
                    if (shortSet.size() == 1) {
                        BlockPosition blockPos = sectionPos.relativeToBlockPos(shortSet.iterator().nextShort());
                        IBlockData blockState = level.getType(blockPos);
                        this.broadcast(new PacketPlayOutBlockChange(blockPos, blockState), false);
                        this.broadcastBlockEntityIfNeeded(level, blockPos, blockState);
                    } else {
                        ChunkSection levelChunkSection = chunk.getSections()[k];
                        PacketPlayOutMultiBlockChange clientboundSectionBlocksUpdatePacket = new PacketPlayOutMultiBlockChange(sectionPos, shortSet, levelChunkSection, this.resendLight);
                        this.broadcast(clientboundSectionBlocksUpdatePacket, false);
                        clientboundSectionBlocksUpdatePacket.runUpdates((pos, state) -> {
                            this.broadcastBlockEntityIfNeeded(level, pos, state);
                        });
                    }

                    this.changedBlocksPerSection[k] = null;
                }
            }

            this.hasChangedSections = false;
        }
    }

    private void broadcastBlockEntityIfNeeded(World world, BlockPosition pos, IBlockData state) {
        if (state.isTileEntity()) {
            this.broadcastBlockEntity(world, pos);
        }

    }

    private void broadcastBlockEntity(World world, BlockPosition pos) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity != null) {
            PacketPlayOutTileEntityData clientboundBlockEntityDataPacket = blockEntity.getUpdatePacket();
            if (clientboundBlockEntityDataPacket != null) {
                this.broadcast(clientboundBlockEntityDataPacket, false);
            }
        }

    }

    public void broadcast(Packet<?> packet, boolean onlyOnWatchDistanceEdge) {
        this.playerProvider.getPlayers(this.pos, onlyOnWatchDistanceEdge).forEach((serverPlayer) -> {
            serverPlayer.connection.sendPacket(packet);
        });
    }

    public CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> getOrScheduleFuture(ChunkStatus targetStatus, PlayerChunkMap chunkStorage) {
        int i = targetStatus.getIndex();
        CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture = this.futures.get(i);
        if (completableFuture != null) {
            Either<IChunkAccess, PlayerChunk.Failure> either = completableFuture.getNow((Either<IChunkAccess, PlayerChunk.Failure>)null);
            boolean bl = either != null && either.right().isPresent();
            if (!bl) {
                return completableFuture;
            }
        }

        if (getChunkStatus(this.ticketLevel).isOrAfter(targetStatus)) {
            CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture2 = chunkStorage.schedule(this, targetStatus);
            this.updateChunkToSave(completableFuture2, "schedule " + targetStatus);
            this.futures.set(i, completableFuture2);
            return completableFuture2;
        } else {
            return completableFuture == null ? UNLOADED_CHUNK_FUTURE : completableFuture;
        }
    }

    private void updateChunkToSave(CompletableFuture<? extends Either<? extends IChunkAccess, PlayerChunk.Failure>> then, String thenDesc) {
        if (this.chunkToSaveHistory != null) {
            this.chunkToSaveHistory.push(new PlayerChunk.ChunkSaveDebug(Thread.currentThread(), then, thenDesc));
        }

        this.chunkToSave = this.chunkToSave.thenCombine(then, (chunkAccess, either) -> {
            return either.map((chunkAccessx) -> {
                return chunkAccessx;
            }, (chunkLoadingFailure) -> {
                return chunkAccess;
            });
        });
    }

    public PlayerChunk.State getFullStatus() {
        return getChunkState(this.ticketLevel);
    }

    public ChunkCoordIntPair getPos() {
        return this.pos;
    }

    public int getTicketLevel() {
        return this.ticketLevel;
    }

    public int getQueueLevel() {
        return this.queueLevel;
    }

    private void setQueueLevel(int level) {
        this.queueLevel = level;
    }

    public void setTicketLevel(int level) {
        this.ticketLevel = level;
    }

    private void scheduleFullChunkPromotion(PlayerChunkMap chunkMap, CompletableFuture<Either<Chunk, PlayerChunk.Failure>> completableFuture, Executor executor, PlayerChunk.State fullChunkStatus) {
        this.pendingFullStateConfirmation.cancel(false);
        CompletableFuture<Void> completableFuture2 = new CompletableFuture<>();
        completableFuture2.thenRunAsync(() -> {
            chunkMap.onFullChunkStatusChange(this.pos, fullChunkStatus);
        }, executor);
        this.pendingFullStateConfirmation = completableFuture2;
        completableFuture.thenAccept((either) -> {
            either.ifLeft((levelChunk) -> {
                completableFuture2.complete((Void)null);
            });
        });
    }

    private void demoteFullChunk(PlayerChunkMap chunkMap, PlayerChunk.State fullChunkStatus) {
        this.pendingFullStateConfirmation.cancel(false);
        chunkMap.onFullChunkStatusChange(this.pos, fullChunkStatus);
    }

    protected void updateFutures(PlayerChunkMap chunkStorage, Executor executor) {
        ChunkStatus chunkStatus = getChunkStatus(this.oldTicketLevel);
        ChunkStatus chunkStatus2 = getChunkStatus(this.ticketLevel);
        boolean bl = this.oldTicketLevel <= PlayerChunkMap.MAX_CHUNK_DISTANCE;
        boolean bl2 = this.ticketLevel <= PlayerChunkMap.MAX_CHUNK_DISTANCE;
        PlayerChunk.State fullChunkStatus = getChunkState(this.oldTicketLevel);
        PlayerChunk.State fullChunkStatus2 = getChunkState(this.ticketLevel);
        if (bl) {
            Either<IChunkAccess, PlayerChunk.Failure> either = Either.right(new PlayerChunk.Failure() {
                @Override
                public String toString() {
                    return "Unloaded ticket level " + PlayerChunk.this.pos;
                }
            });

            for(int i = bl2 ? chunkStatus2.getIndex() + 1 : 0; i <= chunkStatus.getIndex(); ++i) {
                CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture = this.futures.get(i);
                if (completableFuture == null) {
                    this.futures.set(i, CompletableFuture.completedFuture(either));
                }
            }
        }

        boolean bl3 = fullChunkStatus.isAtLeast(PlayerChunk.State.BORDER);
        boolean bl4 = fullChunkStatus2.isAtLeast(PlayerChunk.State.BORDER);
        this.wasAccessibleSinceLastSave |= bl4;
        if (!bl3 && bl4) {
            this.fullChunkFuture = chunkStorage.prepareAccessibleChunk(this);
            this.scheduleFullChunkPromotion(chunkStorage, this.fullChunkFuture, executor, PlayerChunk.State.BORDER);
            this.updateChunkToSave(this.fullChunkFuture, "full");
        }

        if (bl3 && !bl4) {
            CompletableFuture<Either<Chunk, PlayerChunk.Failure>> completableFuture2 = this.fullChunkFuture;
            this.fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
            this.updateChunkToSave(completableFuture2.thenApply((either) -> {
                return either.ifLeft(chunkStorage::packTicks);
            }), "unfull");
        }

        boolean bl5 = fullChunkStatus.isAtLeast(PlayerChunk.State.TICKING);
        boolean bl6 = fullChunkStatus2.isAtLeast(PlayerChunk.State.TICKING);
        if (!bl5 && bl6) {
            this.tickingChunkFuture = chunkStorage.prepareTickingChunk(this);
            this.scheduleFullChunkPromotion(chunkStorage, this.tickingChunkFuture, executor, PlayerChunk.State.TICKING);
            this.updateChunkToSave(this.tickingChunkFuture, "ticking");
        }

        if (bl5 && !bl6) {
            this.tickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        boolean bl7 = fullChunkStatus.isAtLeast(PlayerChunk.State.ENTITY_TICKING);
        boolean bl8 = fullChunkStatus2.isAtLeast(PlayerChunk.State.ENTITY_TICKING);
        if (!bl7 && bl8) {
            if (this.entityTickingChunkFuture != UNLOADED_LEVEL_CHUNK_FUTURE) {
                throw (IllegalStateException)SystemUtils.pauseInIde(new IllegalStateException());
            }

            this.entityTickingChunkFuture = chunkStorage.prepareEntityTickingChunk(this.pos);
            this.scheduleFullChunkPromotion(chunkStorage, this.entityTickingChunkFuture, executor, PlayerChunk.State.ENTITY_TICKING);
            this.updateChunkToSave(this.entityTickingChunkFuture, "entity ticking");
        }

        if (bl7 && !bl8) {
            this.entityTickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        if (!fullChunkStatus2.isAtLeast(fullChunkStatus)) {
            this.demoteFullChunk(chunkStorage, fullChunkStatus2);
        }

        this.onLevelChange.onLevelChange(this.pos, this::getQueueLevel, this.ticketLevel, this::setQueueLevel);
        this.oldTicketLevel = this.ticketLevel;
    }

    public static ChunkStatus getChunkStatus(int level) {
        return level < 33 ? ChunkStatus.FULL : ChunkStatus.getStatusAroundFullChunk(level - 33);
    }

    public static PlayerChunk.State getChunkState(int distance) {
        return FULL_CHUNK_STATUSES[MathHelper.clamp(33 - distance + 1, 0, FULL_CHUNK_STATUSES.length - 1)];
    }

    public boolean hasBeenLoaded() {
        return this.wasAccessibleSinceLastSave;
    }

    public void refreshAccessibility() {
        this.wasAccessibleSinceLastSave = getChunkState(this.ticketLevel).isAtLeast(PlayerChunk.State.BORDER);
    }

    public void replaceProtoChunk(ProtoChunkExtension chunk) {
        for(int i = 0; i < this.futures.length(); ++i) {
            CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture = this.futures.get(i);
            if (completableFuture != null) {
                Optional<IChunkAccess> optional = completableFuture.getNow(UNLOADED_CHUNK).left();
                if (optional.isPresent() && optional.get() instanceof ProtoChunk) {
                    this.futures.set(i, CompletableFuture.completedFuture(Either.left(chunk)));
                }
            }
        }

        this.updateChunkToSave(CompletableFuture.completedFuture(Either.left(chunk.getWrapped())), "replaceProto");
    }

    static final class ChunkSaveDebug {
        private final Thread thread;
        private final CompletableFuture<? extends Either<? extends IChunkAccess, PlayerChunk.Failure>> future;
        private final String source;

        ChunkSaveDebug(Thread thread, CompletableFuture<? extends Either<? extends IChunkAccess, PlayerChunk.Failure>> action, String actionDesc) {
            this.thread = thread;
            this.future = action;
            this.source = actionDesc;
        }
    }

    public interface Failure {
        PlayerChunk.Failure UNLOADED = new PlayerChunk.Failure() {
            @Override
            public String toString() {
                return "UNLOADED";
            }
        };
    }

    @FunctionalInterface
    public interface LevelChangeListener {
        void onLevelChange(ChunkCoordIntPair pos, IntSupplier levelGetter, int targetLevel, IntConsumer levelSetter);
    }

    public interface PlayerProvider {
        Stream<EntityPlayer> getPlayers(ChunkCoordIntPair chunkPos, boolean onlyOnWatchDistanceEdge);
    }

    public static enum State {
        INACCESSIBLE,
        BORDER,
        TICKING,
        ENTITY_TICKING;

        public boolean isAtLeast(PlayerChunk.State levelType) {
            return this.ordinal() >= levelType.ordinal();
        }
    }
}
