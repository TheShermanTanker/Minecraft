package net.minecraft.world.level.block.state;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.properties.IBlockState;

public abstract class IBlockDataHolder<O, S> {
    public static final String NAME_TAG = "Name";
    public static final String PROPERTIES_TAG = "Properties";
    public static final Function<Entry<IBlockState<?>, Comparable<?>>, String> PROPERTY_ENTRY_TO_STRING_FUNCTION = new Function<Entry<IBlockState<?>, Comparable<?>>, String>() {
        @Override
        public String apply(@Nullable Entry<IBlockState<?>, Comparable<?>> entry) {
            if (entry == null) {
                return "<NULL>";
            } else {
                IBlockState<?> property = entry.getKey();
                return property.getName() + "=" + this.getName(property, entry.getValue());
            }
        }

        private <T extends Comparable<T>> String getName(IBlockState<T> property, Comparable<?> value) {
            return property.getName((T)value);
        }
    };
    protected final O owner;
    private final ImmutableMap<IBlockState<?>, Comparable<?>> values;
    private Table<IBlockState<?>, Comparable<?>, S> neighbours;
    protected final MapCodec<S> propertiesCodec;

    protected IBlockDataHolder(O owner, ImmutableMap<IBlockState<?>, Comparable<?>> entries, MapCodec<S> codec) {
        this.owner = owner;
        this.values = entries;
        this.propertiesCodec = codec;
    }

    public <T extends Comparable<T>> S cycle(IBlockState<T> property) {
        return this.set(property, findNextInCollection(property.getValues(), this.get(property)));
    }

    protected static <T> T findNextInCollection(Collection<T> values, T value) {
        Iterator<T> iterator = values.iterator();

        while(iterator.hasNext()) {
            if (iterator.next().equals(value)) {
                if (iterator.hasNext()) {
                    return iterator.next();
                }

                return values.iterator().next();
            }
        }

        return iterator.next();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.owner);
        if (!this.getStateMap().isEmpty()) {
            stringBuilder.append('[');
            stringBuilder.append(this.getStateMap().entrySet().stream().map(PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.joining(",")));
            stringBuilder.append(']');
        }

        return stringBuilder.toString();
    }

    public Collection<IBlockState<?>> getProperties() {
        return Collections.unmodifiableCollection(this.values.keySet());
    }

    public <T extends Comparable<T>> boolean hasProperty(IBlockState<T> property) {
        return this.values.containsKey(property);
    }

    public <T extends Comparable<T>> T get(IBlockState<T> property) {
        Comparable<?> comparable = this.values.get(property);
        if (comparable == null) {
            throw new IllegalArgumentException("Cannot get property " + property + " as it does not exist in " + this.owner);
        } else {
            return property.getType().cast(comparable);
        }
    }

    public <T extends Comparable<T>> Optional<T> getOptionalValue(IBlockState<T> property) {
        Comparable<?> comparable = this.values.get(property);
        return comparable == null ? Optional.empty() : Optional.of(property.getType().cast(comparable));
    }

    public <T extends Comparable<T>, V extends T> S set(IBlockState<T> property, V value) {
        Comparable<?> comparable = this.values.get(property);
        if (comparable == null) {
            throw new IllegalArgumentException("Cannot set property " + property + " as it does not exist in " + this.owner);
        } else if (comparable == value) {
            return (S)this;
        } else {
            S object = this.neighbours.get(property, value);
            if (object == null) {
                throw new IllegalArgumentException("Cannot set property " + property + " to " + value + " on " + this.owner + ", it is not an allowed value");
            } else {
                return object;
            }
        }
    }

    public void populateNeighbours(Map<Map<IBlockState<?>, Comparable<?>>, S> states) {
        if (this.neighbours != null) {
            throw new IllegalStateException();
        } else {
            Table<IBlockState<?>, Comparable<?>, S> table = HashBasedTable.create();

            for(Entry<IBlockState<?>, Comparable<?>> entry : this.values.entrySet()) {
                IBlockState<?> property = entry.getKey();

                for(Comparable<?> comparable : property.getValues()) {
                    if (comparable != entry.getValue()) {
                        table.put(property, comparable, states.get(this.makeNeighbourValues(property, comparable)));
                    }
                }
            }

            this.neighbours = (Table<IBlockState<?>, Comparable<?>, S>)(table.isEmpty() ? table : ArrayTable.create(table));
        }
    }

    private Map<IBlockState<?>, Comparable<?>> makeNeighbourValues(IBlockState<?> property, Comparable<?> value) {
        Map<IBlockState<?>, Comparable<?>> map = Maps.newHashMap(this.values);
        map.put(property, value);
        return map;
    }

    public ImmutableMap<IBlockState<?>, Comparable<?>> getStateMap() {
        return this.values;
    }

    protected static <O, S extends IBlockDataHolder<O, S>> Codec<S> codec(Codec<O> codec, Function<O, S> ownerToStateFunction) {
        return codec.dispatch("Name", (stateHolder) -> {
            return stateHolder.owner;
        }, (object) -> {
            S stateHolder = ownerToStateFunction.apply(object);
            return stateHolder.getStateMap().isEmpty() ? Codec.unit(stateHolder) : stateHolder.propertiesCodec.fieldOf("Properties").codec();
        });
    }
}
