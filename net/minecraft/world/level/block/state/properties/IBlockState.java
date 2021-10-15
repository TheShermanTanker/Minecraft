package net.minecraft.world.level.block.state.properties;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.world.level.block.state.IBlockDataHolder;

public abstract class IBlockState<T extends Comparable<T>> {
    private final Class<T> clazz;
    private final String name;
    private Integer hashCode;
    private final Codec<T> codec = Codec.STRING.comapFlatMap((value) -> {
        return this.getValue(value).map(DataResult::success).orElseGet(() -> {
            return DataResult.error("Unable to read property: " + this + " with value: " + value);
        });
    }, this::getName);
    private final Codec<IBlockState.Value<T>> valueCodec = this.codec.xmap(this::value, IBlockState.Value::value);

    protected IBlockState(String name, Class<T> type) {
        this.clazz = type;
        this.name = name;
    }

    public IBlockState.Value<T> value(T value) {
        return new IBlockState.Value<>(this, value);
    }

    public IBlockState.Value<T> value(IBlockDataHolder<?, ?> state) {
        return new IBlockState.Value<>(this, state.get(this));
    }

    public Stream<IBlockState.Value<T>> getAllValues() {
        return this.getValues().stream().map(this::value);
    }

    public Codec<T> codec() {
        return this.codec;
    }

    public Codec<IBlockState.Value<T>> valueCodec() {
        return this.valueCodec;
    }

    public String getName() {
        return this.name;
    }

    public Class<T> getType() {
        return this.clazz;
    }

    public abstract Collection<T> getValues();

    public abstract String getName(T value);

    public abstract Optional<T> getValue(String name);

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("name", this.name).add("clazz", this.clazz).add("values", this.getValues()).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof IBlockState)) {
            return false;
        } else {
            IBlockState<?> property = (IBlockState)object;
            return this.clazz.equals(property.clazz) && this.name.equals(property.name);
        }
    }

    @Override
    public final int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = this.generateHashCode();
        }

        return this.hashCode;
    }

    public int generateHashCode() {
        return 31 * this.clazz.hashCode() + this.name.hashCode();
    }

    public <U, S extends IBlockDataHolder<?, S>> DataResult<S> parseValue(DynamicOps<U> dynamicOps, S stateHolder, U object) {
        DataResult<T> dataResult = this.codec.parse(dynamicOps, object);
        return dataResult.map((comparable) -> {
            return stateHolder.set(this, comparable);
        }).setPartial(stateHolder);
    }

    public static final class Value<T extends Comparable<T>> {
        private final IBlockState<T> property;
        private final T value;

        Value(IBlockState<T> property, T value) {
            if (!property.getValues().contains(value)) {
                throw new IllegalArgumentException("Value " + value + " does not belong to property " + property);
            } else {
                this.property = property;
                this.value = value;
            }
        }

        public IBlockState<T> getProperty() {
            return this.property;
        }

        public T value() {
            return this.value;
        }

        @Override
        public String toString() {
            return this.property.getName() + "=" + this.property.getName(this.value);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof IBlockState.Value)) {
                return false;
            } else {
                IBlockState.Value<?> value = (IBlockState.Value)object;
                return this.property == value.property && this.value.equals(value.value);
            }
        }

        @Override
        public int hashCode() {
            int i = this.property.hashCode();
            return 31 * i + this.value.hashCode();
        }
    }
}
