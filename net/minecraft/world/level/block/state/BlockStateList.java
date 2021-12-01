package net.minecraft.world.level.block.state;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class BlockStateList<O, S extends IBlockDataHolder<O, S>> {
    static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");
    private final O owner;
    private final ImmutableSortedMap<String, IBlockState<?>> propertiesByName;
    private final ImmutableList<S> states;

    protected BlockStateList(Function<O, S> defaultStateGetter, O owner, BlockStateList.Factory<O, S> factory, Map<String, IBlockState<?>> propertiesMap) {
        this.owner = owner;
        this.propertiesByName = ImmutableSortedMap.copyOf(propertiesMap);
        Supplier<S> supplier = () -> {
            return defaultStateGetter.apply(owner);
        };
        MapCodec<S> mapCodec = MapCodec.of(Encoder.empty(), Decoder.unit(supplier));

        for(Entry<String, IBlockState<?>> entry : this.propertiesByName.entrySet()) {
            mapCodec = appendPropertyCodec(mapCodec, supplier, entry.getKey(), entry.getValue());
        }

        MapCodec<S> mapCodec2 = mapCodec;
        Map<Map<IBlockState<?>, Comparable<?>>, S> map = Maps.newLinkedHashMap();
        List<S> list = Lists.newArrayList();
        Stream<List<Pair<IBlockState<?>, Comparable<?>>>> stream = Stream.of(Collections.emptyList());

        for(IBlockState<?> property : this.propertiesByName.values()) {
            stream = stream.flatMap((listx) -> {
                return property.getValues().stream().map((comparable) -> {
                    List<Pair<IBlockState<?>, Comparable<?>>> list2 = Lists.newArrayList(listx);
                    list2.add(Pair.of(property, comparable));
                    return list2;
                });
            });
        }

        stream.forEach((list2) -> {
            ImmutableMap<IBlockState<?>, Comparable<?>> immutableMap = list2.stream().collect(ImmutableMap.toImmutableMap(Pair::getFirst, Pair::getSecond));
            S stateHolder = factory.create(owner, immutableMap, mapCodec2);
            map.put(immutableMap, stateHolder);
            list.add(stateHolder);
        });

        for(S stateHolder : list) {
            stateHolder.populateNeighbours(map);
        }

        this.states = ImmutableList.copyOf(list);
    }

    private static <S extends IBlockDataHolder<?, S>, T extends Comparable<T>> MapCodec<S> appendPropertyCodec(MapCodec<S> mapCodec, Supplier<S> defaultStateGetter, String key, IBlockState<T> property) {
        return Codec.mapPair(mapCodec, property.valueCodec().fieldOf(key).orElseGet((string) -> {
        }, () -> {
            return property.value(defaultStateGetter.get());
        })).xmap((pair) -> {
            return pair.getFirst().set(property, pair.getSecond().value());
        }, (stateHolder) -> {
            return Pair.of(stateHolder, property.value(stateHolder));
        });
    }

    public ImmutableList<S> getPossibleStates() {
        return this.states;
    }

    public S getBlockData() {
        return this.states.get(0);
    }

    public O getBlock() {
        return this.owner;
    }

    public Collection<IBlockState<?>> getProperties() {
        return this.propertiesByName.values();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("block", this.owner).add("properties", this.propertiesByName.values().stream().map(IBlockState::getName).collect(Collectors.toList())).toString();
    }

    @Nullable
    public IBlockState<?> getProperty(String name) {
        return this.propertiesByName.get(name);
    }

    public static class Builder<O, S extends IBlockDataHolder<O, S>> {
        private final O owner;
        private final Map<String, IBlockState<?>> properties = Maps.newHashMap();

        public Builder(O owner) {
            this.owner = owner;
        }

        public BlockStateList.Builder<O, S> add(IBlockState<?>... properties) {
            for(IBlockState<?> property : properties) {
                this.validateProperty(property);
                this.properties.put(property.getName(), property);
            }

            return this;
        }

        private <T extends Comparable<T>> void validateProperty(IBlockState<T> property) {
            String string = property.getName();
            if (!BlockStateList.NAME_PATTERN.matcher(string).matches()) {
                throw new IllegalArgumentException(this.owner + " has invalidly named property: " + string);
            } else {
                Collection<T> collection = property.getValues();
                if (collection.size() <= 1) {
                    throw new IllegalArgumentException(this.owner + " attempted use property " + string + " with <= 1 possible values");
                } else {
                    for(T comparable : collection) {
                        String string2 = property.getName(comparable);
                        if (!BlockStateList.NAME_PATTERN.matcher(string2).matches()) {
                            throw new IllegalArgumentException(this.owner + " has property: " + string + " with invalidly named value: " + string2);
                        }
                    }

                    if (this.properties.containsKey(string)) {
                        throw new IllegalArgumentException(this.owner + " has duplicate property: " + string);
                    }
                }
            }
        }

        public BlockStateList<O, S> create(Function<O, S> defaultStateGetter, BlockStateList.Factory<O, S> factory) {
            return new BlockStateList<>(defaultStateGetter, this.owner, factory, this.properties);
        }
    }

    public interface Factory<O, S> {
        S create(O owner, ImmutableMap<IBlockState<?>, Comparable<?>> entries, MapCodec<S> codec);
    }
}
