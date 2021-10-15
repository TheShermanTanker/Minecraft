package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.util.ChatDeserializer;

public class CriterionConditionRange {
    public static final CriterionConditionRange ANY = new CriterionConditionRange((Float)null, (Float)null);
    public static final SimpleCommandExceptionType ERROR_INTS_ONLY = new SimpleCommandExceptionType(new ChatMessage("argument.range.ints"));
    private final Float min;
    private final Float max;

    public CriterionConditionRange(@Nullable Float min, @Nullable Float max) {
        this.min = min;
        this.max = max;
    }

    public static CriterionConditionRange exactly(float value) {
        return new CriterionConditionRange(value, value);
    }

    public static CriterionConditionRange between(float min, float max) {
        return new CriterionConditionRange(min, max);
    }

    public static CriterionConditionRange atLeast(float value) {
        return new CriterionConditionRange(value, (Float)null);
    }

    public static CriterionConditionRange atMost(float value) {
        return new CriterionConditionRange((Float)null, value);
    }

    public boolean matches(float value) {
        if (this.min != null && this.max != null && this.min > this.max && this.min > value && this.max < value) {
            return false;
        } else if (this.min != null && this.min > value) {
            return false;
        } else {
            return this.max == null || !(this.max < value);
        }
    }

    public boolean matchesSqr(double value) {
        if (this.min != null && this.max != null && this.min > this.max && (double)(this.min * this.min) > value && (double)(this.max * this.max) < value) {
            return false;
        } else if (this.min != null && (double)(this.min * this.min) > value) {
            return false;
        } else {
            return this.max == null || !((double)(this.max * this.max) < value);
        }
    }

    @Nullable
    public Float getMin() {
        return this.min;
    }

    @Nullable
    public Float getMax() {
        return this.max;
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else if (this.min != null && this.max != null && this.min.equals(this.max)) {
            return new JsonPrimitive(this.min);
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.min != null) {
                jsonObject.addProperty("min", this.min);
            }

            if (this.max != null) {
                jsonObject.addProperty("max", this.min);
            }

            return jsonObject;
        }
    }

    public static CriterionConditionRange fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            if (ChatDeserializer.isNumberValue(json)) {
                float f = ChatDeserializer.convertToFloat(json, "value");
                return new CriterionConditionRange(f, f);
            } else {
                JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "value");
                Float float_ = jsonObject.has("min") ? ChatDeserializer.getAsFloat(jsonObject, "min") : null;
                Float float2 = jsonObject.has("max") ? ChatDeserializer.getAsFloat(jsonObject, "max") : null;
                return new CriterionConditionRange(float_, float2);
            }
        } else {
            return ANY;
        }
    }

    public static CriterionConditionRange fromReader(StringReader reader, boolean allowFloats) throws CommandSyntaxException {
        return fromReader(reader, allowFloats, (value) -> {
            return value;
        });
    }

    public static CriterionConditionRange fromReader(StringReader reader, boolean allowFloats, Function<Float, Float> transform) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw CriterionConditionValue.ERROR_EMPTY.createWithContext(reader);
        } else {
            int i = reader.getCursor();
            Float float_ = optionallyFormat(readNumber(reader, allowFloats), transform);
            Float float2;
            if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
                reader.skip();
                reader.skip();
                float2 = optionallyFormat(readNumber(reader, allowFloats), transform);
                if (float_ == null && float2 == null) {
                    reader.setCursor(i);
                    throw CriterionConditionValue.ERROR_EMPTY.createWithContext(reader);
                }
            } else {
                if (!allowFloats && reader.canRead() && reader.peek() == '.') {
                    reader.setCursor(i);
                    throw ERROR_INTS_ONLY.createWithContext(reader);
                }

                float2 = float_;
            }

            if (float_ == null && float2 == null) {
                reader.setCursor(i);
                throw CriterionConditionValue.ERROR_EMPTY.createWithContext(reader);
            } else {
                return new CriterionConditionRange(float_, float2);
            }
        }
    }

    @Nullable
    private static Float readNumber(StringReader reader, boolean allowFloats) throws CommandSyntaxException {
        int i = reader.getCursor();

        while(reader.canRead() && isAllowedNumber(reader, allowFloats)) {
            reader.skip();
        }

        String string = reader.getString().substring(i, reader.getCursor());
        if (string.isEmpty()) {
            return null;
        } else {
            try {
                return Float.parseFloat(string);
            } catch (NumberFormatException var5) {
                if (allowFloats) {
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().createWithContext(reader, string);
                } else {
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(reader, string);
                }
            }
        }
    }

    private static boolean isAllowedNumber(StringReader reader, boolean allowFloats) {
        char c = reader.peek();
        if ((c < '0' || c > '9') && c != '-') {
            if (allowFloats && c == '.') {
                return !reader.canRead(2) || reader.peek(1) != '.';
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    @Nullable
    private static Float optionallyFormat(@Nullable Float value, Function<Float, Float> function) {
        return value == null ? null : function.apply(value);
    }
}
