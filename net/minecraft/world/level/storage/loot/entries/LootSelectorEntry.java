package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionUser;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.apache.commons.lang3.ArrayUtils;

public abstract class LootSelectorEntry extends LootEntryAbstract {
    public static final int DEFAULT_WEIGHT = 1;
    public static final int DEFAULT_QUALITY = 0;
    protected final int weight;
    protected final int quality;
    protected final LootItemFunction[] functions;
    final BiFunction<ItemStack, LootTableInfo, ItemStack> compositeFunction;
    private final LootEntry entry = new LootSelectorEntry.EntryBase() {
        @Override
        public void createItemStack(Consumer<ItemStack> lootConsumer, LootTableInfo context) {
            LootSelectorEntry.this.createItemStack(LootItemFunction.decorate(LootSelectorEntry.this.compositeFunction, lootConsumer, context), context);
        }
    };

    protected LootSelectorEntry(int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions) {
        super(conditions);
        this.weight = weight;
        this.quality = quality;
        this.functions = functions;
        this.compositeFunction = LootItemFunctions.compose(functions);
    }

    @Override
    public void validate(LootCollector reporter) {
        super.validate(reporter);

        for(int i = 0; i < this.functions.length; ++i) {
            this.functions[i].validate(reporter.forChild(".functions[" + i + "]"));
        }

    }

    protected abstract void createItemStack(Consumer<ItemStack> lootConsumer, LootTableInfo context);

    @Override
    public boolean expand(LootTableInfo context, Consumer<LootEntry> choiceConsumer) {
        if (this.canRun(context)) {
            choiceConsumer.accept(this.entry);
            return true;
        } else {
            return false;
        }
    }

    public static LootSelectorEntry.Builder<?> simpleBuilder(LootSelectorEntry.EntryConstructor factory) {
        return new LootSelectorEntry.DummyBuilder(factory);
    }

    public abstract static class Builder<T extends LootSelectorEntry.Builder<T>> extends LootEntryAbstract.Builder<T> implements LootItemFunctionUser<T> {
        protected int weight = 1;
        protected int quality = 0;
        private final List<LootItemFunction> functions = Lists.newArrayList();

        @Override
        public T apply(LootItemFunction.Builder builder) {
            this.functions.add(builder.build());
            return this.getThis();
        }

        protected LootItemFunction[] getFunctions() {
            return this.functions.toArray(new LootItemFunction[0]);
        }

        public T setWeight(int weight) {
            this.weight = weight;
            return this.getThis();
        }

        public T setQuality(int quality) {
            this.quality = quality;
            return this.getThis();
        }
    }

    static class DummyBuilder extends LootSelectorEntry.Builder<LootSelectorEntry.DummyBuilder> {
        private final LootSelectorEntry.EntryConstructor constructor;

        public DummyBuilder(LootSelectorEntry.EntryConstructor factory) {
            this.constructor = factory;
        }

        @Override
        protected LootSelectorEntry.DummyBuilder getThis() {
            return this;
        }

        @Override
        public LootEntryAbstract build() {
            return this.constructor.build(this.weight, this.quality, this.getConditions(), this.getFunctions());
        }
    }

    protected abstract class EntryBase implements LootEntry {
        @Override
        public int getWeight(float luck) {
            return Math.max(MathHelper.floor((float)LootSelectorEntry.this.weight + (float)LootSelectorEntry.this.quality * luck), 0);
        }
    }

    @FunctionalInterface
    protected interface EntryConstructor {
        LootSelectorEntry build(int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions);
    }

    public abstract static class Serializer<T extends LootSelectorEntry> extends LootEntryAbstract.Serializer<T> {
        @Override
        public void serializeType(JsonObject json, T entry, JsonSerializationContext context) {
            if (entry.weight != 1) {
                json.addProperty("weight", entry.weight);
            }

            if (entry.quality != 0) {
                json.addProperty("quality", entry.quality);
            }

            if (!ArrayUtils.isEmpty((Object[])entry.functions)) {
                json.add("functions", context.serialize(entry.functions));
            }

        }

        @Override
        public final T deserializeCustom(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            int i = ChatDeserializer.getAsInt(jsonObject, "weight", 1);
            int j = ChatDeserializer.getAsInt(jsonObject, "quality", 0);
            LootItemFunction[] lootItemFunctions = ChatDeserializer.getAsObject(jsonObject, "functions", new LootItemFunction[0], jsonDeserializationContext, LootItemFunction[].class);
            return this.deserialize(jsonObject, jsonDeserializationContext, i, j, lootItemConditions, lootItemFunctions);
        }

        protected abstract T deserialize(JsonObject entryJson, JsonDeserializationContext context, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions);
    }
}
