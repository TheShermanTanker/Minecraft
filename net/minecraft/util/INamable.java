package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface INamable {
    String getSerializedName();

    static <E extends Enum<E> & INamable> Codec<E> fromEnum(Supplier<E[]> enumValues, Function<? super String, ? extends E> fromString) {
        E[] enums = (Enum[])enumValues.get();
        return fromStringResolver((object) -> {
            return object.ordinal();
        }, (ordinal) -> {
            return enums[ordinal];
        }, fromString);
    }

    static <E extends INamable> Codec<E> fromStringResolver(ToIntFunction<E> compressedEncoder, IntFunction<E> compressedDecoder, Function<? super String, ? extends E> decoder) {
        return new Codec<E>() {
            @Override
            public <T> DataResult<T> encode(E stringRepresentable, DynamicOps<T> dynamicOps, T object) {
                return dynamicOps.compressMaps() ? dynamicOps.mergeToPrimitive(object, dynamicOps.createInt(compressedEncoder.applyAsInt(stringRepresentable))) : dynamicOps.mergeToPrimitive(object, dynamicOps.createString(stringRepresentable.getSerializedName()));
            }

            @Override
            public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> dynamicOps, T object) {
                return dynamicOps.compressMaps() ? dynamicOps.getNumberValue(object).flatMap((id) -> {
                    return Optional.ofNullable(compressedDecoder.apply(id.intValue())).map(DataResult::success).orElseGet(() -> {
                        return DataResult.error("Unknown element id: " + id);
                    });
                }).map((stringRepresentable) -> {
                    return Pair.of(stringRepresentable, dynamicOps.empty());
                }) : dynamicOps.getStringValue(object).flatMap((name) -> {
                    return Optional.ofNullable(decoder.apply(name)).map(DataResult::success).orElseGet(() -> {
                        return DataResult.error("Unknown element name: " + name);
                    });
                }).map((stringRepresentable) -> {
                    return Pair.of(stringRepresentable, dynamicOps.empty());
                });
            }

            @Override
            public String toString() {
                return "StringRepresentable[" + compressedEncoder + "]";
            }
        };
    }

    static Keyable keys(INamable[] values) {
        return new Keyable() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
                return dynamicOps.compressMaps() ? IntStream.range(0, values.length).mapToObj(dynamicOps::createInt) : Arrays.stream(values).map(INamable::getSerializedName).map(dynamicOps::createString);
            }
        };
    }
}
