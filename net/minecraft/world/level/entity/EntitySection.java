package net.minecraft.world.level.entity;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.util.EntitySlice;
import net.minecraft.util.VisibleForDebug;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntitySection<T> {
    protected static final Logger LOGGER = LogManager.getLogger();
    private final EntitySlice<T> storage;
    private Visibility chunkStatus;

    public EntitySection(Class<T> entityClass, Visibility status) {
        this.chunkStatus = status;
        this.storage = new EntitySlice<>(entityClass);
    }

    public void add(T obj) {
        this.storage.add(obj);
    }

    public boolean remove(T obj) {
        return this.storage.remove(obj);
    }

    public void getEntities(Predicate<? super T> predicate, Consumer<T> action) {
        for(T object : this.storage) {
            if (predicate.test(object)) {
                action.accept(object);
            }
        }

    }

    public <U extends T> void getEntities(EntityTypeTest<T, U> type, Predicate<? super U> filter, Consumer<? super U> action) {
        for(T object : this.storage.find(type.getBaseClass())) {
            U object2 = (U)type.tryCast(object);
            if (object2 != null && filter.test(object2)) {
                action.accept((T)object2);
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
