package net.minecraft.network.syncher;

import com.google.common.collect.Lists;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataWatcher {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Object2IntMap<Class<? extends Entity>> ENTITY_ID_POOL = new Object2IntOpenHashMap<>();
    private static final int EOF_MARKER = 255;
    private static final int MAX_ID_VALUE = 254;
    private final Entity entity;
    private final Int2ObjectMap<DataWatcher.Item<?>> itemsById = new Int2ObjectOpenHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private boolean isEmpty = true;
    private boolean isDirty;

    public DataWatcher(Entity trackedEntity) {
        this.entity = trackedEntity;
    }

    public static <T> DataWatcherObject<T> defineId(Class<? extends Entity> entityClass, DataWatcherSerializer<T> dataHandler) {
        if (LOGGER.isDebugEnabled()) {
            try {
                Class<?> class_ = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
                if (!class_.equals(entityClass)) {
                    LOGGER.debug("defineId called for: {} from {}", entityClass, class_, new RuntimeException());
                }
            } catch (ClassNotFoundException var5) {
            }
        }

        int i;
        if (ENTITY_ID_POOL.containsKey(entityClass)) {
            i = ENTITY_ID_POOL.getInt(entityClass) + 1;
        } else {
            int j = 0;
            Class<?> class2 = entityClass;

            while(class2 != Entity.class) {
                class2 = class2.getSuperclass();
                if (ENTITY_ID_POOL.containsKey(class2)) {
                    j = ENTITY_ID_POOL.getInt(class2) + 1;
                    break;
                }
            }

            i = j;
        }

        if (i > 254) {
            throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is 254)");
        } else {
            ENTITY_ID_POOL.put(entityClass, i);
            return dataHandler.createAccessor(i);
        }
    }

    public <T> void register(DataWatcherObject<T> key, T initialValue) {
        int i = key.getId();
        if (i > 254) {
            throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is 254)");
        } else if (this.itemsById.containsKey(i)) {
            throw new IllegalArgumentException("Duplicate id value for " + i + "!");
        } else if (DataWatcherRegistry.getSerializedId(key.getSerializer()) < 0) {
            throw new IllegalArgumentException("Unregistered serializer " + key.getSerializer() + " for " + i + "!");
        } else {
            this.registerObject(key, initialValue);
        }
    }

    private <T> void registerObject(DataWatcherObject<T> entityDataAccessor, T object) {
        DataWatcher.Item<T> dataItem = new DataWatcher.Item<>(entityDataAccessor, object);
        this.lock.writeLock().lock();
        this.itemsById.put(entityDataAccessor.getId(), dataItem);
        this.isEmpty = false;
        this.lock.writeLock().unlock();
    }

    private <T> DataWatcher.Item<T> getItem(DataWatcherObject<T> entityDataAccessor) {
        this.lock.readLock().lock();

        DataWatcher.Item<T> dataItem;
        try {
            dataItem = this.itemsById.get(entityDataAccessor.getId());
        } catch (Throwable var9) {
            CrashReport crashReport = CrashReport.forThrowable(var9, "Getting synched entity data");
            CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Synched entity data");
            crashReportCategory.setDetail("Data ID", entityDataAccessor);
            throw new ReportedException(crashReport);
        } finally {
            this.lock.readLock().unlock();
        }

        return dataItem;
    }

    public <T> T get(DataWatcherObject<T> data) {
        return this.getItem(data).getValue();
    }

    public <T> void set(DataWatcherObject<T> key, T value) {
        DataWatcher.Item<T> dataItem = this.getItem(key);
        if (ObjectUtils.notEqual(value, dataItem.getValue())) {
            dataItem.setValue(value);
            this.entity.onSyncedDataUpdated(key);
            dataItem.setDirty(true);
            this.isDirty = true;
        }

    }

    public boolean isDirty() {
        return this.isDirty;
    }

    public static void pack(@Nullable List<DataWatcher.Item<?>> entries, PacketDataSerializer buf) {
        if (entries != null) {
            for(DataWatcher.Item<?> dataItem : entries) {
                writeDataItem(buf, dataItem);
            }
        }

        buf.writeByte(255);
    }

    @Nullable
    public List<DataWatcher.Item<?>> packDirty() {
        List<DataWatcher.Item<?>> list = null;
        if (this.isDirty) {
            this.lock.readLock().lock();

            for(DataWatcher.Item<?> dataItem : this.itemsById.values()) {
                if (dataItem.isDirty()) {
                    dataItem.setDirty(false);
                    if (list == null) {
                        list = Lists.newArrayList();
                    }

                    list.add(dataItem.copy());
                }
            }

            this.lock.readLock().unlock();
        }

        this.isDirty = false;
        return list;
    }

    @Nullable
    public List<DataWatcher.Item<?>> getAll() {
        List<DataWatcher.Item<?>> list = null;
        this.lock.readLock().lock();

        for(DataWatcher.Item<?> dataItem : this.itemsById.values()) {
            if (list == null) {
                list = Lists.newArrayList();
            }

            list.add(dataItem.copy());
        }

        this.lock.readLock().unlock();
        return list;
    }

    private static <T> void writeDataItem(PacketDataSerializer buf, DataWatcher.Item<T> entry) {
        DataWatcherObject<T> entityDataAccessor = entry.getAccessor();
        int i = DataWatcherRegistry.getSerializedId(entityDataAccessor.getSerializer());
        if (i < 0) {
            throw new EncoderException("Unknown serializer type " + entityDataAccessor.getSerializer());
        } else {
            buf.writeByte(entityDataAccessor.getId());
            buf.writeVarInt(i);
            entityDataAccessor.getSerializer().write(buf, entry.getValue());
        }
    }

    @Nullable
    public static List<DataWatcher.Item<?>> unpack(PacketDataSerializer buf) {
        List<DataWatcher.Item<?>> list = null;

        int i;
        while((i = buf.readUnsignedByte()) != 255) {
            if (list == null) {
                list = Lists.newArrayList();
            }

            int j = buf.readVarInt();
            DataWatcherSerializer<?> entityDataSerializer = DataWatcherRegistry.getSerializer(j);
            if (entityDataSerializer == null) {
                throw new DecoderException("Unknown serializer type " + j);
            }

            list.add(genericHelper(buf, i, entityDataSerializer));
        }

        return list;
    }

    private static <T> DataWatcher.Item<T> genericHelper(PacketDataSerializer buf, int i, DataWatcherSerializer<T> entityDataSerializer) {
        return new DataWatcher.Item<>(entityDataSerializer.createAccessor(i), entityDataSerializer.read(buf));
    }

    public void assignValues(List<DataWatcher.Item<?>> entries) {
        this.lock.writeLock().lock();

        try {
            for(DataWatcher.Item<?> dataItem : entries) {
                DataWatcher.Item<?> dataItem2 = this.itemsById.get(dataItem.getAccessor().getId());
                if (dataItem2 != null) {
                    this.assignValue(dataItem2, dataItem);
                    this.entity.onSyncedDataUpdated(dataItem.getAccessor());
                }
            }
        } finally {
            this.lock.writeLock().unlock();
        }

        this.isDirty = true;
    }

    private <T> void assignValue(DataWatcher.Item<T> to, DataWatcher.Item<?> from) {
        if (!Objects.equals(from.accessor.getSerializer(), to.accessor.getSerializer())) {
            throw new IllegalStateException(String.format("Invalid entity data item type for field %d on entity %s: old=%s(%s), new=%s(%s)", to.accessor.getId(), this.entity, to.value, to.value.getClass(), from.value, from.value.getClass()));
        } else {
            to.setValue((T)from.getValue());
        }
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    public void clearDirty() {
        this.isDirty = false;
        this.lock.readLock().lock();

        for(DataWatcher.Item<?> dataItem : this.itemsById.values()) {
            dataItem.setDirty(false);
        }

        this.lock.readLock().unlock();
    }

    public static class Item<T> {
        final DataWatcherObject<T> accessor;
        T value;
        private boolean dirty;

        public Item(DataWatcherObject<T> data, T value) {
            this.accessor = data;
            this.value = value;
            this.dirty = true;
        }

        public DataWatcherObject<T> getAccessor() {
            return this.accessor;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public T getValue() {
            return this.value;
        }

        public boolean isDirty() {
            return this.dirty;
        }

        public void setDirty(boolean dirty) {
            this.dirty = dirty;
        }

        public DataWatcher.Item<T> copy() {
            return new DataWatcher.Item<>(this.accessor, this.accessor.getSerializer().copy(this.value));
        }
    }
}
