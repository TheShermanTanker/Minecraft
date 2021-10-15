package net.minecraft.network.syncher;

public class DataWatcherObject<T> {
    private final int id;
    private final DataWatcherSerializer<T> serializer;

    public DataWatcherObject(int id, DataWatcherSerializer<T> dataType) {
        this.id = id;
        this.serializer = dataType;
    }

    public int getId() {
        return this.id;
    }

    public DataWatcherSerializer<T> getSerializer() {
        return this.serializer;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            DataWatcherObject<?> entityDataAccessor = (DataWatcherObject)object;
            return this.id == entityDataAccessor.id;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public String toString() {
        return "<entity data: " + this.id + ">";
    }
}
