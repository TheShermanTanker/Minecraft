package net.minecraft.data.models.blockstates;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.properties.IBlockState;

public interface Condition extends Supplier<JsonElement> {
    void validate(BlockStateList<?, ?> stateManager);

    static Condition.TerminalCondition condition() {
        return new Condition.TerminalCondition();
    }

    static Condition and(Condition... conditions) {
        return new Condition.CompositeCondition(Condition.Operation.AND, Arrays.asList(conditions));
    }

    static Condition or(Condition... conditions) {
        return new Condition.CompositeCondition(Condition.Operation.OR, Arrays.asList(conditions));
    }

    public static class CompositeCondition implements Condition {
        private final Condition.Operation operation;
        private final List<Condition> subconditions;

        CompositeCondition(Condition.Operation operation, List<Condition> list) {
            this.operation = operation;
            this.subconditions = list;
        }

        @Override
        public void validate(BlockStateList<?, ?> stateManager) {
            this.subconditions.forEach((condition) -> {
                condition.validate(stateManager);
            });
        }

        @Override
        public JsonElement get() {
            JsonArray jsonArray = new JsonArray();
            this.subconditions.stream().map(Supplier::get).forEach(jsonArray::add);
            JsonObject jsonObject = new JsonObject();
            jsonObject.add(this.operation.id, jsonArray);
            return jsonObject;
        }
    }

    public static enum Operation {
        AND("AND"),
        OR("OR");

        final String id;

        private Operation(String name) {
            this.id = name;
        }
    }

    public static class TerminalCondition implements Condition {
        private final Map<IBlockState<?>, String> terms = Maps.newHashMap();

        private static <T extends Comparable<T>> String joinValues(IBlockState<T> property, Stream<T> valueStream) {
            return valueStream.map(property::getName).collect(Collectors.joining("|"));
        }

        private static <T extends Comparable<T>> String getTerm(IBlockState<T> property, T value, T[] otherValues) {
            return joinValues(property, Stream.concat(Stream.of(value), Stream.of(otherValues)));
        }

        private <T extends Comparable<T>> void putValue(IBlockState<T> property, String value) {
            String string = this.terms.put(property, value);
            if (string != null) {
                throw new IllegalStateException("Tried to replace " + property + " value from " + string + " to " + value);
            }
        }

        public final <T extends Comparable<T>> Condition.TerminalCondition term(IBlockState<T> property, T value) {
            this.putValue(property, property.getName(value));
            return this;
        }

        @SafeVarargs
        public final <T extends Comparable<T>> Condition.TerminalCondition term(IBlockState<T> property, T value, T... otherValues) {
            this.putValue(property, getTerm(property, value, otherValues));
            return this;
        }

        public final <T extends Comparable<T>> Condition.TerminalCondition negatedTerm(IBlockState<T> property, T comparable) {
            this.putValue(property, "!" + property.getName(comparable));
            return this;
        }

        @SafeVarargs
        public final <T extends Comparable<T>> Condition.TerminalCondition negatedTerm(IBlockState<T> property, T comparable, T... comparables) {
            this.putValue(property, "!" + getTerm(property, comparable, comparables));
            return this;
        }

        @Override
        public JsonElement get() {
            JsonObject jsonObject = new JsonObject();
            this.terms.forEach((property, string) -> {
                jsonObject.addProperty(property.getName(), string);
            });
            return jsonObject;
        }

        @Override
        public void validate(BlockStateList<?, ?> stateManager) {
            List<IBlockState<?>> list = this.terms.keySet().stream().filter((property) -> {
                return stateManager.getProperty(property.getName()) != property;
            }).collect(Collectors.toList());
            if (!list.isEmpty()) {
                throw new IllegalStateException("Properties " + list + " are missing from " + stateManager);
            }
        }
    }
}
