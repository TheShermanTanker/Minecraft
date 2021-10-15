package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.util.ChatDeserializer;

public abstract class CriterionConditionValue<T extends Number> {
    public static final SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(new ChatMessage("argument.range.empty"));
    public static final SimpleCommandExceptionType ERROR_SWAPPED = new SimpleCommandExceptionType(new ChatMessage("argument.range.swapped"));
    protected final T min;
    protected final T max;

    protected CriterionConditionValue(@Nullable T min, @Nullable T max) {
        this.min = min;
        this.max = max;
    }

    @Nullable
    public T getMin() {
        return this.min;
    }

    @Nullable
    public T getMax() {
        return this.max;
    }

    public boolean isAny() {
        return this.min == null && this.max == null;
    }

    public JsonElement serializeToJson() {
        if (this.isAny()) {
            return JsonNull.INSTANCE;
        } else if (this.min != null && this.min.equals(this.max)) {
            return new JsonPrimitive(this.min);
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.min != null) {
                jsonObject.addProperty("min", this.min);
            }

            if (this.max != null) {
                jsonObject.addProperty("max", this.max);
            }

            return jsonObject;
        }
    }

    protected static <T extends Number, R extends CriterionConditionValue<T>> R fromJson(@Nullable JsonElement json, R fallback, BiFunction<JsonElement, String, T> asNumber, CriterionConditionValue.BoundsFactory<T, R> factory) {
        if (json != null && !json.isJsonNull()) {
            if (ChatDeserializer.isNumberValue(json)) {
                T number = asNumber.apply(json, "value");
                return factory.create(number, number);
            } else {
                JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "value");
                T number2 = jsonObject.has("min") ? asNumber.apply(jsonObject.get("min"), "min") : null;
                T number3 = jsonObject.has("max") ? asNumber.apply(jsonObject.get("max"), "max") : null;
                return factory.create(number2, number3);
            }
        } else {
            return fallback;
        }
    }

    protected static <T extends Number, R extends CriterionConditionValue<T>> R fromReader(StringReader commandReader, CriterionConditionValue.BoundsFromReaderFactory<T, R> boundsFromReaderFactory, Function<String, T> converter, Supplier<DynamicCommandExceptionType> exceptionTypeSupplier, Function<T, T> mapper) throws CommandSyntaxException {
        if (!commandReader.canRead()) {
            throw ERROR_EMPTY.createWithContext(commandReader);
        } else {
            int i = commandReader.getCursor();

            try {
                T number = optionallyFormat(readNumber(commandReader, converter, exceptionTypeSupplier), mapper);
                T number2;
                if (commandReader.canRead(2) && commandReader.peek() == '.' && commandReader.peek(1) == '.') {
                    commandReader.skip();
                    commandReader.skip();
                    number2 = optionallyFormat(readNumber(commandReader, converter, exceptionTypeSupplier), mapper);
                    if (number == null && number2 == null) {
                        throw ERROR_EMPTY.createWithContext(commandReader);
                    }
                } else {
                    number2 = number;
                }

                if (number == null && number2 == null) {
                    throw ERROR_EMPTY.createWithContext(commandReader);
                } else {
                    return boundsFromReaderFactory.create(commandReader, number, number2);
                }
            } catch (CommandSyntaxException var8) {
                commandReader.setCursor(i);
                throw new CommandSyntaxException(var8.getType(), var8.getRawMessage(), var8.getInput(), i);
            }
        }
    }

    @Nullable
    private static <T extends Number> T readNumber(StringReader reader, Function<String, T> converter, Supplier<DynamicCommandExceptionType> exceptionTypeSupplier) throws CommandSyntaxException {
        int i = reader.getCursor();

        while(reader.canRead() && isAllowedInputChat(reader)) {
            reader.skip();
        }

        String string = reader.getString().substring(i, reader.getCursor());
        if (string.isEmpty()) {
            return (T)null;
        } else {
            try {
                return converter.apply(string);
            } catch (NumberFormatException var6) {
                throw exceptionTypeSupplier.get().createWithContext(reader, string);
            }
        }
    }

    private static boolean isAllowedInputChat(StringReader reader) {
        char c = reader.peek();
        if ((c < '0' || c > '9') && c != '-') {
            if (c != '.') {
                return false;
            } else {
                return !reader.canRead(2) || reader.peek(1) != '.';
            }
        } else {
            return true;
        }
    }

    @Nullable
    private static <T> T optionallyFormat(@Nullable T object, Function<T, T> function) {
        return (T)(object == null ? null : function.apply(object));
    }

    @FunctionalInterface
    protected interface BoundsFactory<T extends Number, R extends CriterionConditionValue<T>> {
        R create(@Nullable T min, @Nullable T max);
    }

    @FunctionalInterface
    protected interface BoundsFromReaderFactory<T extends Number, R extends CriterionConditionValue<T>> {
        R create(StringReader reader, @Nullable T min, @Nullable T max) throws CommandSyntaxException;
    }

    public static class DoubleRange extends CriterionConditionValue<Double> {
        public static final CriterionConditionValue.DoubleRange ANY = new CriterionConditionValue.DoubleRange((Double)null, (Double)null);
        private final Double minSq;
        private final Double maxSq;

        private static CriterionConditionValue.DoubleRange create(StringReader reader, @Nullable Double double_, @Nullable Double double2) throws CommandSyntaxException {
            if (double_ != null && double2 != null && double_ > double2) {
                throw ERROR_SWAPPED.createWithContext(reader);
            } else {
                return new CriterionConditionValue.DoubleRange(double_, double2);
            }
        }

        @Nullable
        private static Double squareOpt(@Nullable Double double_) {
            return double_ == null ? null : double_ * double_;
        }

        private DoubleRange(@Nullable Double min, @Nullable Double max) {
            super(min, max);
            this.minSq = squareOpt(min);
            this.maxSq = squareOpt(max);
        }

        public static CriterionConditionValue.DoubleRange exactly(double d) {
            return new CriterionConditionValue.DoubleRange(d, d);
        }

        public static CriterionConditionValue.DoubleRange between(double d, double e) {
            return new CriterionConditionValue.DoubleRange(d, e);
        }

        public static CriterionConditionValue.DoubleRange atLeast(double d) {
            return new CriterionConditionValue.DoubleRange(d, (Double)null);
        }

        public static CriterionConditionValue.DoubleRange atMost(double d) {
            return new CriterionConditionValue.DoubleRange((Double)null, d);
        }

        public boolean matches(double d) {
            if (this.min != null && this.min > d) {
                return false;
            } else {
                return this.max == null || !(this.max < d);
            }
        }

        public boolean matchesSqr(double value) {
            if (this.minSq != null && this.minSq > value) {
                return false;
            } else {
                return this.maxSq == null || !(this.maxSq < value);
            }
        }

        public static CriterionConditionValue.DoubleRange fromJson(@Nullable JsonElement element) {
            return fromJson(element, ANY, ChatDeserializer::convertToDouble, CriterionConditionValue.DoubleRange::new);
        }

        public static CriterionConditionValue.DoubleRange fromReader(StringReader reader) throws CommandSyntaxException {
            return fromReader(reader, (double_) -> {
                return double_;
            });
        }

        public static CriterionConditionValue.DoubleRange fromReader(StringReader reader, Function<Double, Double> mapper) throws CommandSyntaxException {
            return fromReader(reader, CriterionConditionValue.DoubleRange::create, Double::parseDouble, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidDouble, mapper);
        }
    }

    public static class IntegerRange extends CriterionConditionValue<Integer> {
        public static final CriterionConditionValue.IntegerRange ANY = new CriterionConditionValue.IntegerRange((Integer)null, (Integer)null);
        private final Long minSq;
        private final Long maxSq;

        private static CriterionConditionValue.IntegerRange create(StringReader reader, @Nullable Integer min, @Nullable Integer max) throws CommandSyntaxException {
            if (min != null && max != null && min > max) {
                throw ERROR_SWAPPED.createWithContext(reader);
            } else {
                return new CriterionConditionValue.IntegerRange(min, max);
            }
        }

        @Nullable
        private static Long squareOpt(@Nullable Integer value) {
            return value == null ? null : value.longValue() * value.longValue();
        }

        private IntegerRange(@Nullable Integer min, @Nullable Integer max) {
            super(min, max);
            this.minSq = squareOpt(min);
            this.maxSq = squareOpt(max);
        }

        public static CriterionConditionValue.IntegerRange exactly(int value) {
            return new CriterionConditionValue.IntegerRange(value, value);
        }

        public static CriterionConditionValue.IntegerRange between(int min, int max) {
            return new CriterionConditionValue.IntegerRange(min, max);
        }

        public static CriterionConditionValue.IntegerRange atLeast(int value) {
            return new CriterionConditionValue.IntegerRange(value, (Integer)null);
        }

        public static CriterionConditionValue.IntegerRange atMost(int value) {
            return new CriterionConditionValue.IntegerRange((Integer)null, value);
        }

        public boolean matches(int value) {
            if (this.min != null && this.min > value) {
                return false;
            } else {
                return this.max == null || this.max >= value;
            }
        }

        public boolean matchesSqr(long l) {
            if (this.minSq != null && this.minSq > l) {
                return false;
            } else {
                return this.maxSq == null || this.maxSq >= l;
            }
        }

        public static CriterionConditionValue.IntegerRange fromJson(@Nullable JsonElement element) {
            return fromJson(element, ANY, ChatDeserializer::convertToInt, CriterionConditionValue.IntegerRange::new);
        }

        public static CriterionConditionValue.IntegerRange fromReader(StringReader reader) throws CommandSyntaxException {
            return fromReader(reader, (integer) -> {
                return integer;
            });
        }

        public static CriterionConditionValue.IntegerRange fromReader(StringReader reader, Function<Integer, Integer> converter) throws CommandSyntaxException {
            return fromReader(reader, CriterionConditionValue.IntegerRange::create, Integer::parseInt, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt, converter);
        }
    }
}
