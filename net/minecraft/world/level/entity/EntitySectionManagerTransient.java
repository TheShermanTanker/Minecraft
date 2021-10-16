package net.minecraft.world.level.entity;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkCoordIntPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntitySectionManagerTransient<T extends EntityAccess> {
    static final Logger LOGGER = LogManager.getLogger();
    final IWorldCallback<T> callbacks;
    final EntityLookup<T> entityStorage;
    final EntitySectionStorage<T> sectionStorage;
    private final LongSet tickingChunks = new LongOpenHashSet();
    private final IWorldEntityAccess<T> entityGetter;

    public EntitySectionManagerTransient(Class<T> entityClass, IWorldCallback<T> handler) {
        this.entityStorage = new EntityLookup<>();
        this.sectionStorage = new EntitySectionStorage<>(entityClass, (pos) -> {
            return this.tickingChunks.contains(pos) ? Visibility.TICKING : Visibility.TRACKED;
        });
        this.callbacks = handler;
        this.entityGetter = new WorldEntityAccess<>(this.entityStorage, this.sectionStorage);
    }

    public void startTicking(ChunkCoordIntPair pos) {
        long l = pos.pair();
        this.tickingChunks.add(l);
        this.sectionStorage.getExistingSectionsInChunk(l).forEach((sections) -> {
            Visibility visibility = sections.updateChunkStatus(Visibility.TICKING);
            if (!visibility.isTicking()) {
                sections.getEntities().filter((e) -> {
                    return !e.isAlwaysTicking();
                }).forEach(this.callbacks::onTickingStart);
            }

        });
    }

    public void stopTicking(ChunkCoordIntPair pos) {
        long l = pos.pair();
        this.tickingChunks.remove(l);
        this.sectionStorage.getExistingSectionsInChunk(l).forEach((sections) -> {
            Visibility visibility = sections.updateChunkStatus(Visibility.TRACKED);
            if (visibility.isTicking()) {
                sections.getEntities().filter((e) -> {
                    return !e.isAlwaysTicking();
                }).forEach(this.callbacks::onTickingEnd);
            }

        });
    }

    public IWorldEntityAccess<T> getEntityGetter() {
        return this.entityGetter;
    }

    public void addEntity(T entity) {
        this.entityStorage.add(entity);
        long l = SectionPosition.asLong(entity.getChunkCoordinates());
        EntitySection<T> entitySection = this.sectionStorage.getOrCreateSection(l);
        entitySection.add(entity);
        entity.setWorldCallback(new EntitySectionManagerTransient.Callback(entity, l, entitySection));
        this.callbacks.onCreated(entity);
        this.callbacks.onTrackingStart(entity);
        if (entity.isAlwaysTicking() || entitySection.getStatus().isTicking()) {
            this.callbacks.onTickingStart(entity);
        }

    }

    @VisibleForDebug
    public int count() {
        return this.entityStorage.count();
    }

    void removeSectionIfEmpty(long packedChunkSection, EntitySection<T> entities) {
        if (entities.isEmpty()) {
            this.sectionStorage.remove(packedChunkSection);
        }

    }

    @VisibleForDebug
    public String gatherStats() {
        return this.entityStorage.count() + "," + this.sectionStorage.count() + "," + this.tickingChunks.size();
    }

    class Callback implements IEntityCallback {
        private final T entity;
        private long currentSectionKey;
        private EntitySection<T> currentSection;

        Callback(T entity, long pos, EntitySection<T> section) {
            this.entity = entity;
            this.currentSectionKey = pos;
            this.currentSection = section;
        }

        @Override
        public void onMove() {
            BlockPosition blockPos = this.entity.getChunkCoordinates();
            long l = SectionPosition.asLong(blockPos);
            if (l != this.currentSectionKey) {
                Visibility visibility = this.currentSection.getStatus();
                if (!this.currentSection.remove(this.entity)) {
                    EntitySectionManagerTransient.LOGGER.warn("Entity {} wasn't found in section {} (moving to {})", this.entity, SectionPosition.of(this.currentSectionKey), l);
                }

                EntitySectionManagerTransient.this.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
                EntitySection<T> entitySection = EntitySectionManagerTransient.this.sectionStorage.getOrCreateSection(l);
                entitySection.add(this.entity);
                this.currentSection = entitySection;
                this.currentSectionKey = l;
                if (!this.entity.isAlwaysTicking()) {
                    boolean bl = visibility.isTicking();
                    boolean bl2 = entitySection.getStatus().isTicking();
                    if (bl && !bl2) {
                        EntitySectionManagerTransient.this.callbacks.onTickingEnd(this.entity);
                    } else if (!bl && bl2) {
                        EntitySectionManagerTransient.this.callbacks.onTickingStart(this.entity);
                    }
                }
            }

        }

        @Override
        public void onRemove(Entity.RemovalReason reason) {
            if (!this.currentSection.remove(this.entity)) {
                EntitySectionManagerTransient.LOGGER.warn("Entity {} wasn't found in section {} (destroying due to {})", this.entity, SectionPosition.of(this.currentSectionKey), reason);
            }

            Visibility visibility = this.currentSection.getStatus();
            if (visibility.isTicking() || this.entity.isAlwaysTicking()) {
                EntitySectionManagerTransient.this.callbacks.onTickingEnd(this.entity);
            }

            EntitySectionManagerTransient.this.callbacks.onTrackingEnd(this.entity);
            EntitySectionManagerTransient.this.callbacks.onDestroyed(this.entity);
            EntitySectionManagerTransient.this.entityStorage.remove(this.entity);
            this.entity.setWorldCallback(NULL);
            EntitySectionManagerTransient.this.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
        }
    }
}
