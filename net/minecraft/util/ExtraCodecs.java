package net.minecraft.util;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExtraCodecs {
    public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, (v) -> {
        return "Value must be non-negative: " + v;
    });
    public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, (v) -> {
        return "Value must be positive: " + v;
    });

    public static <F, S> Codec<Either<F, S>> xor(Codec<F> first, Codec<S> second) {
        return new ExtraCodecs.XorCodec<>(first, second);
    }

    private static <N extends Number & Comparable<N>> Function<N, DataResult<N>> checkRangeWithMessage(N min, N max, Function<N, String> messageFactory) {
        return (value) -> {
            return value.compareTo(min) >= 0 && value.compareTo(max) <= 0 ? DataResult.success(value) : DataResult.error(messageFactory.apply(value));
        };
    }

    private static Codec<Integer> intRangeWithMessage(int min, int max, Function<Integer, String> messageFactory) {
        Function<Integer, DataResult<Integer>> function = checkRangeWithMessage(min, max, messageFactory);
        return Codec.INT.flatXmap(function, function);
    }

    public static <T> Function<List<T>, DataResult<List<T>>> nonEmptyListCheck() {
        return (list) -> {
            return list.isEmpty() ? DataResult.error("List must have contents") : DataResult.success(list);
        };
    }

    public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> originalCodec) {
        return originalCodec.flatXmap(nonEmptyListCheck(), nonEmptyListCheck());
    }

    public static <T> Function<List<Supplier<T>>, DataResult<List<Supplier<T>>>> nonNullSupplierListCheck() {
        return (suppliers) -> {
            List<String> list = Lists.newArrayList();

            for(int i = 0; i < suppliers.size(); ++i) {
                Supplier<T> supplier = suppliers.get(i);

                try {
                    if (supplier.get() == null) {
                        list.add("Missing value [" + i + "] : " + supplier);
                    }
                } catch (Exception var5) {
                    list.add("Invalid value [" + i + "]: " + supplier + ", message: " + var5.getMessage());
                }
            }

            return !list.isEmpty() ? DataResult.error(String.join("; ", list)) : DataResult.success(suppliers, Lifecycle.stable());
        };
    }

    public static <T> Function<Supplier<T>, DataResult<Supplier<T>>> nonNullSupplierCheck() {
        return (supplier) -> {
            try {
                if (supplier.get() == null) {
                    return DataResult.error("Missing value: " + supplier);
                }
            } catch (Exception var2) {
                return DataResult.error("Invalid value: " + supplier + ", message: " + var2.getMessage());
            }

            return DataResult.success(supplier, Lifecycle.stable());
        };
    }

    static final class XorCodec<F, S> implements Codec<Either<F, S>> {
        private final Codec<F> first;
        private final Codec<S> second;

        public XorCodec(Codec<F> first, Codec<S> second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> dynamicOps, T object) {
            DataResult<Pair<Either<F, S>, T>> dataResult = this.first.decode(dynamicOps, object).map((pair) -> {
                return pair.mapFirst(Either::left);
            });
            DataResult<Pair<Either<F, S>, T>> dataResult2 = this.second.decode(dynamicOps, object).map((pair) -> {
                return pair.mapFirst(Either::right);
            });
            Optional<Pair<Either<F, S>, T>> optional = dataResult.result();
            Optional<Pair<Either<F, S>, T>> optional2 = dataResult2.result();
            if (optional.isPresent() && optional2.isPresent()) {
                return DataResult.error("Both alternatives read successfully, can not pick the correct one; first: " + optional.get() + " second: " + optional2.get(), optional.get());
            } else {
                return optional.isPresent() ? dataResult : dataResult2;
            }
        }

        @Override
        public <T> DataResult<T> encode(Either<F, S> either, DynamicOps<T> dynamicOps, T object) {
            return either.map((left) -> {
                return this.first.encode(left, dynamicOps, object);
            }, (right) -> {
                return this.second.encode(right, dynamicOps, object);
            });
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                ExtraCodecs.XorCodec<?, ?> xorCodec = (ExtraCodecs.XorCodec)object;
                return Objects.equals(this.first, xorCodec.first) && Objects.equals(this.second, xorCodec.second);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.first, this.second);
        }

        @Override
        public String toString() {
            return "XorCodec[" + this.first + ", " + this.second + "]";
        }
    }
}
