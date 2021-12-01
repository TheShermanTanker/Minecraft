package net.minecraft.world.level.entity;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.util.EntitySlice;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.phys.AxisAlignedBB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntitySection<T extends EntityAccess> {
    protected static final Logger LOGGER = LogManager.getLogger();
    private final EntitySlice<T> storage;
    private Visibility chunkStatus;

    public EntitySection(Class<T> entityClass, Visibility status) {
        this.chunkStatus = status;
        this.storage = new EntitySlice<>(entityClass);
    }

    public void add(T entity) {
        this.storage.add(entity);
    }

    public boolean remove(T entity) {
        return this.storage.remove(entity);
    }

    public void getEntities(AxisAlignedBB box, Consumer<T> action) {
        for(T entityAccess : this.storage) {
            if (entityAccess.getBoundingBox().intersects(box)) {
                action.accept(entityAccess);
            }
        }

    }

    public <U extends T> void getEntities(EntityTypeTest<T, U> type, AxisAlignedBB box, Consumer<? super U> action) {
        Collection<? extends T> collection = this.storage.find(type.getBaseClass());
        if (!collection.isEmpty()) {
            for(T entityAccess : collection) {
                U entityAccess2 = (U)((EntityAccess)type.tryCast(entityAccess));
                if (entityAccess2 != null && entityAccess.getBoundingBox().intersects(box)) {
                    action.accept((T)entityAccess2);
                }
            }

        }
    }

    public boolean isEmpty() {
        return this.storage.isEmpty();
    }

    public Stream<T> getEntities() {
        return this.storage.stream();
    }

    public Visibility getStatus() {
        return this.chunkStatus;
    }

    public Visibility updateChunkStatus(Visibility status) {
        Visibility visibility = this.chunkStatus;
        this.chunkStatus = status;
        return visibility;
    }

    @VisibleForDebug
    public int size() {
        return this.storage.size();
    }
}
