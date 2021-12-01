package net.minecraft.world.level.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import java.util.Objects;
import java.util.Spliterators;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.Consumer;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.phys.AxisAlignedBB;

public class EntitySectionStorage<T extends EntityAccess> {
    private final Class<T> entityClass;
    private final Long2ObjectFunction<Visibility> intialSectionVisibility;
    private final Long2ObjectMap<EntitySection<T>> sections = new Long2ObjectOpenHashMap<>();
    private final LongSortedSet sectionIds = new LongAVLTreeSet();

    public EntitySectionStorage(Class<T> entityClass, Long2ObjectFunction<Visibility> chunkStatusDiscriminator) {
        this.entityClass = entityClass;
        this.intialSectionVisibility = chunkStatusDiscriminator;
    }

    public void forEachAccessibleNonEmptySection(AxisAlignedBB box, Consumer<EntitySection<T>> action) {
        int i = SectionPosition.posToSectionCoord(box.minX - 2.0D);
        int j = SectionPosition.posToSectionCoord(box.minY - 2.0D);
        int k = SectionPosition.posToSectionCoord(box.minZ - 2.0D);
        int l = SectionPosition.posToSectionCoord(box.maxX + 2.0D);
        int m = SectionPosition.posToSectionCoord(box.maxY + 2.0D);
        int n = SectionPosition.posToSectionCoord(box.maxZ + 2.0D);

        for(int o = i; o <= l; ++o) {
            long p = SectionPosition.asLong(o, 0, 0);
            long q = SectionPosition.asLong(o, -1, -1);
            LongIterator longIterator = this.sectionIds.subSet(p, q + 1L).iterator();

            while(longIterator.hasNext()) {
                long r = longIterator.nextLong();
                int s = SectionPosition.y(r);
                int t = SectionPosition.z(r);
                if (s >= j && s <= m && t >= k && t <= n) {
                    EntitySection<T> entitySection = this.sections.get(r);
                    if (entitySection != null && !entitySection.isEmpty() && entitySection.getStatus().isAccessible()) {
                        action.accept(entitySection);
                    }
                }
            }
        }

    }

    public LongStream getExistingSectionPositionsInChunk(long chunkPos) {
        int i = ChunkCoordIntPair.getX(chunkPos);
        int j = ChunkCoordIntPair.getZ(chunkPos);
        LongSortedSet longSortedSet = this.getChunkSections(i, j);
        if (longSortedSet.isEmpty()) {
            return LongStream.empty();
        } else {
            OfLong ofLong = longSortedSet.iterator();
            return StreamSupport.longStream(Spliterators.spliteratorUnknownSize(ofLong, 1301), false);
        }
    }

    private LongSortedSet getChunkSections(int chunkX, int chunkZ) {
        long l = SectionPosition.asLong(chunkX, 0, chunkZ);
        long m = SectionPosition.asLong(chunkX, -1, chunkZ);
        return this.sectionIds.subSet(l, m + 1L);
    }

    public Stream<EntitySection<T>> getExistingSectionsInChunk(long chunkPos) {
        return this.getExistingSectionPositionsInChunk(chunkPos).mapToObj(this.sections::get).filter(Objects::nonNull);
    }

    private static long getChunkKeyFromSectionKey(long sectionPos) {
        return ChunkCoordIntPair.pair(SectionPosition.x(sectionPos), SectionPosition.z(sectionPos));
    }

    public EntitySection<T> getOrCreateSection(long sectionPos) {
        return this.sections.computeIfAbsent(sectionPos, this::createSection);
    }

    @Nullable
    public EntitySection<T> getSection(long sectionPos) {
        return this.sections.get(sectionPos);
    }

    private EntitySection<T> createSection(long sectionPos) {
        long l = getChunkKeyFromSectionKey(sectionPos);
        Visibility visibility = this.intialSectionVisibility.get(l);
        this.sectionIds.add(sectionPos);
        return new EntitySection<>(this.entityClass, visibility);
    }

    public LongSet getAllChunksWithExistingSections() {
        LongSet longSet = new LongOpenHashSet();
        this.sections.keySet().forEach((sectionPos) -> {
            longSet.add(getChunkKeyFromSectionKey(sectionPos));
        });
        return longSet;
    }

    public void getEntities(AxisAlignedBB box, Consumer<T> action) {
        this.forEachAccessibleNonEmptySection(box, (section) -> {
            section.getEntities(box, action);
        });
    }

    public <U extends T> void getEntities(EntityTypeTest<T, U> filter, AxisAlignedBB box, Consumer<U> action) {
        this.forEachAccessibleNonEmptySection(box, (section) -> {
            section.getEntities(filter, box, action);
        });
    }

    public void remove(long sectionPos) {
        this.sections.remove(sectionPos);
        this.sectionIds.remove(sectionPos);
    }

    @VisibleForDebug
    public int count() {
        return this.sectionIds.size();
    }
}
