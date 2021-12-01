package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.INamable;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.IBlockDataHolder;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.material.Fluid;

public class CriterionTriggerProperties {
    public static final CriterionTriggerProperties ANY = new CriterionTriggerProperties(ImmutableList.of());
    private final List<CriterionTriggerProperties.PropertyMatcher> properties;

    private static CriterionTriggerProperties.PropertyMatcher fromJson(String key, JsonElement json) {
        if (json.isJsonPrimitive()) {
            String string = json.getAsString();
            return new CriterionTriggerProperties.ExactPropertyMatcher(key, string);
        } else {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "value");
            String string2 = jsonObject.has("min") ? getStringOrNull(jsonObject.get("min")) : null;
            String string3 = jsonObject.has("max") ? getStringOrNull(jsonObject.get("max")) : null;
            return (CriterionTriggerProperties.PropertyMatcher)(string2 != null && string2.equals(string3) ? new CriterionTriggerProperties.ExactPropertyMatcher(key, string2) : new CriterionTriggerProperties.RangedPropertyMatcher(key, string2, string3));
        }
    }

    @Nullable
    private static String getStringOrNull(JsonElement json) {
        return json.isJsonNull() ? null : json.getAsString();
    }

    CriterionTriggerProperties(List<CriterionTriggerProperties.PropertyMatcher> conditions) {
        this.properties = ImmutableList.copyOf(conditions);
    }

    public <S extends IBlockDataHolder<?, S>> boolean matches(BlockStateList<?, S> stateManager, S container) {
        for(CriterionTriggerProperties.PropertyMatcher propertyMatcher : this.properties) {
            if (!propertyMatcher.match(stateManager, container)) {
                return false;
            }
        }

        return true;
    }

    public boolean matches(IBlockData state) {
        return this.matches(state.getBlock().getStates(), state);
    }

    public boolean matches(Fluid state) {
        return this.matches(state.getType().getStateDefinition(), state);
    }

    public void checkState(BlockStateList<?, ?> factory, Consumer<String> reporter) {
        this.properties.forEach((condition) -> {
            condition.checkState(factory, reporter);
        });
    }

    public static CriterionTriggerProperties fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "properties");
            List<CriterionTriggerProperties.PropertyMatcher> list = Lists.newArrayList();

            for(Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                list.add(fromJson(entry.getKey(), entry.getValue()));
            }

            return new CriterionTriggerProperties(list);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (!this.properties.isEmpty()) {
                this.properties.forEach((condition) -> {
                    jsonObject.add(condition.getName(), condition.toJson());
                });
            }

            return jsonObject;
        }
    }

    public static class Builder {
        private final List<CriterionTriggerProperties.PropertyMatcher> matchers = Lists.newArrayList();

        private Builder() {
        }

        public static CriterionTriggerProperties.Builder properties() {
            return new CriterionTriggerProperties.Builder();
        }

        public CriterionTriggerProperties.Builder hasProperty(IBlockState<?> property, String valueName) {
            this.matchers.add(new CriterionTriggerProperties.ExactPropertyMatcher(property.getName(), valueName));
            return this;
        }

        public CriterionTriggerProperties.Builder hasProperty(IBlockState<Integer> property, int value) {
            return this.hasProperty(property, Integer.toString(value));
        }

        public CriterionTriggerProperties.Builder hasProperty(IBlockState<Boolean> property, boolean value) {
            return this.hasProperty(property, Boolean.toString(value));
        }

        public <T extends Comparable<T> & INamable> CriterionTriggerProperties.Builder hasProperty(IBlockState<T> property, T value) {
            return this.hasProperty(property, value.getSerializedName());
        }

        public CriterionTriggerProperties build() {
            return new CriterionTriggerProperties(this.matchers);
        }
    }

    static class ExactPropertyMatcher extends CriterionTriggerProperties.PropertyMatcher {
        private final String value;

        public ExactPropertyMatcher(String key, String value) {
            super(key);
            this.value = value;
        }

        @Override
        protected <T extends Comparable<T>> boolean match(IBlockDataHolder<?, ?> state, IBlockState<T> property) {
            T comparable = state.get(property);
            Optional<T> optional = property.getValue(this.value);
            return optional.isPresent() && comparable.compareTo(optional.get()) == 0;
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(this.value);
        }
    }

    abstract static class PropertyMatcher {
        private final String name;

        public PropertyMatcher(String key) {
            this.name = key;
        }

        public <S extends IBlockDataHolder<?, S>> boolean match(BlockStateList<?, S> stateManager, S state) {
            IBlockState<?> property = stateManager.getProperty(this.name);
            return property == null ? false : this.match(state, property);
        }

        protected abstract <T extends Comparable<T>> boolean match(IBlockDataHolder<?, ?> state, IBlockState<T> property);

        public abstract JsonElement toJson();

        public String getName() {
            return this.name;
        }

        public void checkState(BlockStateList<?, ?> factory, Consumer<String> reporter) {
            IBlockState<?> property = factory.getProperty(this.name);
            if (property == null) {
                reporter.accept(this.name);
            }

        }
    }

    static class RangedPropertyMatcher extends CriterionTriggerProperties.PropertyMatcher {
        @Nullable
        private final String minValue;
        @Nullable
        private final String maxValue;

        public RangedPropertyMatcher(String key, @Nullable String min, @Nullable String max) {
            super(key);
            this.minValue = min;
            this.maxValue = max;
        }

        @Override
        protected <T extends Comparable<T>> boolean match(IBlockDataHolder<?, ?> state, IBlockState<T> property) {
            T comparable = state.get(property);
            if (this.minValue != null) {
                Optional<T> optional = property.getValue(this.minValue);
                if (!optional.isPresent() || comparable.compareTo(optional.get()) < 0) {
                    return false;
                }
            }

            if (this.maxValue != null) {
                Optional<T> optional2 = property.getValue(this.maxValue);
                if (!optional2.isPresent() || comparable.compareTo(optional2.get()) > 0) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public JsonElement toJson() {
            JsonObject jsonObject = new JsonObject();
            if (this.minValue != null) {
                jsonObject.addProperty("min", this.minValue);
            }

            if (this.maxValue != null) {
                jsonObject.addProperty("max", this.maxValue);
            }

            return jsonObject;
        }
    }
}
