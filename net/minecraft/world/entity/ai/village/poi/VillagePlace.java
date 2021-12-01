package net.minecraft.world.entity.ai.village.poi;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.server.level.LightEngineGraphSection;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.storage.RegionFileSection;

public class VillagePlace extends RegionFileSection<VillagePlaceSection> {
    public static final int MAX_VILLAGE_DISTANCE = 6;
    public static final int VILLAGE_SECTION_SIZE = 1;
    private final VillagePlace.DistanceTracker distanceTracker;
    private final LongSet loadedChunks = new LongOpenHashSet();

    public VillagePlace(Path path, DataFixer dataFixer, boolean dsync, IWorldHeightAccess world) {
        super(path, VillagePlaceSection::codec, VillagePlaceSection::new, dataFixer, DataFixTypes.POI_CHUNK, dsync, world);
        this.distanceTracker = new VillagePlace.DistanceTracker();
    }

    public void add(BlockPosition pos, VillagePlaceType type) {
        this.getOrCreate(SectionPosition.asLong(pos)).add(pos, type);
    }

    public void remove(BlockPosition pos) {
        this.getOrLoad(SectionPosition.asLong(pos)).ifPresent((poiSet) -> {
            poiSet.remove(pos);
        });
    }

    public long getCountInRange(Predicate<VillagePlaceType> typePredicate, BlockPosition pos, int radius, VillagePlace.Occupancy occupationStatus) {
        return this.getInRange(typePredicate, pos, radius, occupationStatus).count();
    }

    public boolean existsAtPosition(VillagePlaceType type, BlockPosition pos) {
        return this.exists(pos, type::equals);
    }

    public Stream<VillagePlaceRecord> getInSquare(Predicate<VillagePlaceType> typePredicate, BlockPosition pos, int radius, VillagePlace.Occupancy occupationStatus) {
        int i = Math.floorDiv(radius, 16) + 1;
        return ChunkCoordIntPair.rangeClosed(new ChunkCoordIntPair(pos), i).flatMap((chunkPos) -> {
            return this.getInChunk(typePredicate, chunkPos, occupationStatus);
        }).filter((poi) -> {
            BlockPosition blockPos2 = poi.getPos();
            return Math.abs(blockPos2.getX() - pos.getX()) <= radius && Math.abs(blockPos2.getZ() - pos.getZ()) <= radius;
        });
    }

    public Stream<VillagePlaceRecord> getInRange(Predicate<VillagePlaceType> typePredicate, BlockPosition pos, int radius, VillagePlace.Occupancy occupationStatus) {
        int i = radius * radius;
        return this.getInSquare(typePredicate, pos, radius, occupationStatus).filter((poi) -> {
            return poi.getPos().distSqr(pos) <= (double)i;
        });
    }

    @VisibleForDebug
    public Stream<VillagePlaceRecord> getInChunk(Predicate<VillagePlaceType> typePredicate, ChunkCoordIntPair chunkPos, VillagePlace.Occupancy occupationStatus) {
        return IntStream.range(this.levelHeightAccessor.getMinSection(), this.levelHeightAccessor.getMaxSection()).boxed().map((integer) -> {
            return this.getOrLoad(SectionPosition.of(chunkPos, integer).asLong());
        }).filter(Optional::isPresent).flatMap((optional) -> {
            return optional.get().getRecords(typePredicate, occupationStatus);
        });
    }

    public Stream<BlockPosition> findAll(Predicate<VillagePlaceType> typePredicate, Predicate<BlockPosition> posPredicate, BlockPosition pos, int radius, VillagePlace.Occupancy occupationStatus) {
        return this.getInRange(typePredicate, pos, radius, occupationStatus).map(VillagePlaceRecord::getPos).filter(posPredicate);
    }

    public Stream<BlockPosition> findAllClosestFirst(Predicate<VillagePlaceType> typePredicate, Predicate<BlockPosition> posPredicate, BlockPosition pos, int radius, VillagePlace.Occupancy occupationStatus) {
        return this.findAll(typePredicate, posPredicate, pos, radius, occupationStatus).sorted(Comparator.comparingDouble((blockPos2) -> {
            return blockPos2.distSqr(pos);
        }));
    }

    public Optional<BlockPosition> find(Predicate<VillagePlaceType> typePredicate, Predicate<BlockPosition> posPredicate, BlockPosition pos, int radius, VillagePlace.Occupancy occupationStatus) {
        return this.findAll(typePredicate, posPredicate, pos, radius, occupationStatus).findFirst();
    }

    public Optional<BlockPosition> findClosest(Predicate<VillagePlaceType> typePredicate, BlockPosition pos, int radius, VillagePlace.Occupancy occupationStatus) {
        return this.getInRange(typePredicate, pos, radius, occupationStatus).map(VillagePlaceRecord::getPos).min(Comparator.comparingDouble((blockPos2) -> {
            return blockPos2.distSqr(pos);
        }));
    }

    public Optional<BlockPosition> findClosest(Predicate<VillagePlaceType> typePredicate, Predicate<BlockPosition> posPredicate, BlockPosition pos, int radius, VillagePlace.Occupancy occupationStatus) {
        return this.getInRange(typePredicate, pos, radius, occupationStatus).map(VillagePlaceRecord::getPos).filter(posPredicate).min(Comparator.comparingDouble((blockPos2) -> {
            return blockPos2.distSqr(pos);
        }));
    }

    public Optional<BlockPosition> take(Predicate<VillagePlaceType> typePredicate, Predicate<BlockPosition> positionPredicate, BlockPosition pos, int radius) {
        return this.getInRange(typePredicate, pos, radius, VillagePlace.Occupancy.HAS_SPACE).filter((poi) -> {
            return positionPredicate.test(poi.getPos());
        }).findFirst().map((poi) -> {
            poi.acquireTicket();
            return poi.getPos();
        });
    }

    public Optional<BlockPosition> getRandom(Predicate<VillagePlaceType> typePredicate, Predicate<BlockPosition> positionPredicate, VillagePlace.Occupancy occupationStatus, BlockPosition pos, int radius, Random random) {
        List<VillagePlaceRecord> list = this.getInRange(typePredicate, pos, radius, occupationStatus).collect(Collectors.toList());
        Collections.shuffle(list, random);
        return list.stream().filter((poi) -> {
            return positionPredicate.test(poi.getPos());
        }).findFirst().map(VillagePlaceRecord::getPos);
    }

    public boolean release(BlockPosition pos) {
        return this.getOrLoad(SectionPosition.asLong(pos)).map((poiSet) -> {
            return poiSet.release(pos);
        }).orElseThrow(() -> {
            return SystemUtils.pauseInIde(new IllegalStateException("POI never registered at " + pos));
        });
    }

    public boolean exists(BlockPosition pos, Predicate<VillagePlaceType> predicate) {
        return this.getOrLoad(SectionPosition.asLong(pos)).map((poiSet) -> {
            return poiSet.exists(pos, predicate);
        }).orElse(false);
    }

    public Optional<VillagePlaceType> getType(BlockPosition pos) {
        return this.getOrLoad(SectionPosition.asLong(pos)).flatMap((poiSet) -> {
            return poiSet.getType(pos);
        });
    }

    /** @deprecated */
    @Deprecated
    @VisibleForDebug
    public int getFreeTickets(BlockPosition pos) {
        return this.getOrLoad(SectionPosition.asLong(pos)).map((poiSet) -> {
            return poiSet.getFreeTickets(pos);
        }).orElse(0);
    }

    public int sectionsToVillage(SectionPosition pos) {
        this.distanceTracker.runAllUpdates();
        return this.distanceTracker.getLevel(pos.asLong());
    }

    boolean isVillageCenter(long pos) {
        Optional<VillagePlaceSection> optional = this.get(pos);
        return optional == null ? false : optional.map((poiSet) -> {
            return poiSet.getRecords(VillagePlaceType.ALL, VillagePlace.Occupancy.IS_OCCUPIED).count() > 0L;
        }).orElse(false);
    }

    @Override
    public void tick(BooleanSupplier shouldKeepTicking) {
        super.tick(shouldKeepTicking);
        this.distanceTracker.runAllUpdates();
    }

    @Override
    protected void setDirty(long pos) {
        super.setDirty(pos);
        this.distanceTracker.update(pos, this.distanceTracker.getLevelFromSource(pos), false);
    }

    @Override
    protected void onSectionLoad(long pos) {
        this.distanceTracker.update(pos, this.distanceTracker.getLevelFromSource(pos), false);
    }

    public void checkConsistencyWithBlocks(ChunkCoordIntPair chunkPos, ChunkSection chunkSection) {
        SectionPosition sectionPos = SectionPosition.of(chunkPos, SectionPosition.blockToSectionCoord(chunkSection.getYPosition()));
        SystemUtils.ifElse(this.getOrLoad(sectionPos.asLong()), (poiSet) -> {
            poiSet.refresh((biConsumer) -> {
                if (mayHavePoi(chunkSection)) {
                    this.updateFromSection(chunkSection, sectionPos, biConsumer);
                }

            });
        }, () -> {
            if (mayHavePoi(chunkSection)) {
                VillagePlaceSection poiSection = this.getOrCreate(sectionPos.asLong());
                this.updateFromSection(chunkSection, sectionPos, poiSection::add);
            }

        });
    }

    private static boolean mayHavePoi(ChunkSection chunkSection) {
        return chunkSection.maybeHas(VillagePlaceType.ALL_STATES::contains);
    }

    private void updateFromSection(ChunkSection chunkSection, SectionPosition sectionPos, BiConsumer<BlockPosition, VillagePlaceType> biConsumer) {
        sectionPos.blocksInside().forEach((pos) -> {
            IBlockData blockState = chunkSection.getType(SectionPosition.sectionRelative(pos.getX()), SectionPosition.sectionRelative(pos.getY()), SectionPosition.sectionRelative(pos.getZ()));
            VillagePlaceType.forState(blockState).ifPresent((poiType) -> {
                biConsumer.accept(pos, poiType);
            });
        });
    }

    public void ensureLoadedAndValid(IWorldReader world, BlockPosition pos, int radius) {
        SectionPosition.aroundChunk(new ChunkCoordIntPair(pos), Math.floorDiv(radius, 16), this.levelHeightAccessor.getMinSection(), this.levelHeightAccessor.getMaxSection()).map((sectionPos) -> {
            return Pair.of(sectionPos, this.getOrLoad(sectionPos.asLong()));
        }).filter((pair) -> {
            return !pair.getSecond().map(VillagePlaceSection::isValid).orElse(false);
        }).map((pair) -> {
            return pair.getFirst().chunk();
        }).filter((chunkPos) -> {
            return this.loadedChunks.add(chunkPos.pair());
        }).forEach((chunkPos) -> {
            world.getChunkAt(chunkPos.x, chunkPos.z, ChunkStatus.EMPTY);
        });
    }

    final class DistanceTracker extends LightEngineGraphSection {
        private final Long2ByteMap levels = new Long2ByteOpenHashMap();

        protected DistanceTracker() {
            super(7, 16, 256);
            this.levels.defaultReturnValue((byte)7);
        }

        @Override
        protected int getLevelFromSource(long id) {
            return VillagePlace.this.isVillageCenter(id) ? 0 : 7;
        }

        @Override
        protected int getLevel(long id) {
            return this.levels.get(id);
        }

        @Override
        protected void setLevel(long id, int level) {
            if (level > 6) {
                this.levels.remove(id);
            } else {
                this.levels.put(id, (byte)level);
            }

        }

        public void runAllUpdates() {
            super.runUpdates(Integer.MAX_VALUE);
        }
    }

    public static enum Occupancy {
        HAS_SPACE(VillagePlaceRecord::hasSpace),
        IS_OCCUPIED(VillagePlaceRecord::isOccupied),
        ANY((poiRecord) -> {
            return true;
        });

        private final Predicate<? super VillagePlaceRecord> test;

        private Occupancy(Predicate<? super VillagePlaceRecord> predicate) {
            this.test = predicate;
        }

        public Predicate<? super VillagePlaceRecord> getTest() {
            return this.test;
        }
    }
}
