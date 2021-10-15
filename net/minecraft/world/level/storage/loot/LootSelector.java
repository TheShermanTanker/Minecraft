package net.minecraft.world.level.storage.loot;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootEntry;
import net.minecraft.world.level.storage.loot.entries.LootEntryAbstract;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionUser;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionUser;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableInt;

public class LootSelector {
    final LootEntryAbstract[] entries;
    final LootItemCondition[] conditions;
    private final Predicate<LootTableInfo> compositeCondition;
    final LootItemFunction[] functions;
    private final BiFunction<ItemStack, LootTableInfo, ItemStack> compositeFunction;
    final NumberProvider rolls;
    final NumberProvider bonusRolls;

    LootSelector(LootEntryAbstract[] entries, LootItemCondition[] conditions, LootItemFunction[] functions, NumberProvider rolls, NumberProvider bonusRolls) {
        this.entries = entries;
        this.conditions = conditions;
        this.compositeCondition = LootItemConditions.andConditions(conditions);
        this.functions = functions;
        this.compositeFunction = LootItemFunctions.compose(functions);
        this.rolls = rolls;
        this.bonusRolls = bonusRolls;
    }

    private void addRandomItem(Consumer<ItemStack> lootConsumer, LootTableInfo context) {
        Random random = context.getRandom();
        List<LootEntry> list = Lists.newArrayList();
        MutableInt mutableInt = new MutableInt();

        for(LootEntryAbstract lootPoolEntryContainer : this.entries) {
            lootPoolEntryContainer.expand(context, (choice) -> {
                int i = choice.getWeight(context.getLuck());
                if (i > 0) {
                    list.add(choice);
                    mutableInt.add(i);
                }

            });
        }

        int i = list.size();
        if (mutableInt.intValue() != 0 && i != 0) {
            if (i == 1) {
                list.get(0).createItemStack(lootConsumer, context);
            } else {
                int j = random.nextInt(mutableInt.intValue());

                for(LootEntry lootPoolEntry : list) {
                    j -= lootPoolEntry.getWeight(context.getLuck());
                    if (j < 0) {
                        lootPoolEntry.createItemStack(lootConsumer, context);
                        return;
                    }
                }

            }
        }
    }

    public void addRandomItems(Consumer<ItemStack> lootConsumer, LootTableInfo context) {
        if (this.compositeCondition.test(context)) {
            Consumer<ItemStack> consumer = LootItemFunction.decorate(this.compositeFunction, lootConsumer, context);
            int i = this.rolls.getInt(context) + MathHelper.floor(this.bonusRolls.getFloat(context) * context.getLuck());

            for(int j = 0; j < i; ++j) {
                this.addRandomItem(consumer, context);
            }

        }
    }

    public void validate(LootCollector reporter) {
        for(int i = 0; i < this.conditions.length; ++i) {
            this.conditions[i].validate(reporter.forChild(".condition[" + i + "]"));
        }

        for(int j = 0; j < this.functions.length; ++j) {
            this.functions[j].validate(reporter.forChild(".functions[" + j + "]"));
        }

        for(int k = 0; k < this.entries.length; ++k) {
            this.entries[k].validate(reporter.forChild(".entries[" + k + "]"));
        }

        this.rolls.validate(reporter.forChild(".rolls"));
        this.bonusRolls.validate(reporter.forChild(".bonusRolls"));
    }

    public static LootSelector.Builder lootPool() {
        return new LootSelector.Builder();
    }

    public static class Builder implements LootItemFunctionUser<LootSelector.Builder>, LootItemConditionUser<LootSelector.Builder> {
        private final List<LootEntryAbstract> entries = Lists.newArrayList();
        private final List<LootItemCondition> conditions = Lists.newArrayList();
        private final List<LootItemFunction> functions = Lists.newArrayList();
        private NumberProvider rolls = ConstantValue.exactly(1.0F);
        private NumberProvider bonusRolls = ConstantValue.exactly(0.0F);

        public LootSelector.Builder setRolls(NumberProvider rolls) {
            this.rolls = rolls;
            return this;
        }

        @Override
        public LootSelector.Builder unwrap() {
            return this;
        }

        public LootSelector.Builder setBonusRolls(NumberProvider bonusRolls) {
            this.bonusRolls = bonusRolls;
            return this;
        }

        public LootSelector.Builder add(LootEntryAbstract.Builder<?> entry) {
            this.entries.add(entry.build());
            return this;
        }

        @Override
        public LootSelector.Builder when(LootItemCondition.Builder builder) {
            this.conditions.add(builder.build());
            return this;
        }

        @Override
        public LootSelector.Builder apply(LootItemFunction.Builder builder) {
            this.functions.add(builder.build());
            return this;
        }

        public LootSelector build() {
            if (this.rolls == null) {
                throw new IllegalArgumentException("Rolls not set");
            } else {
                return new LootSelector(this.entries.toArray(new LootEntryAbstract[0]), this.conditions.toArray(new LootItemCondition[0]), this.functions.toArray(new LootItemFunction[0]), this.rolls, this.bonusRolls);
            }
        }
    }

    public static class Serializer implements JsonDeserializer<LootSelector>, JsonSerializer<LootSelector> {
        @Override
        public LootSelector deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(jsonElement, "loot pool");
            LootEntryAbstract[] lootPoolEntryContainers = ChatDeserializer.getAsObject(jsonObject, "entries", jsonDeserializationContext, LootEntryAbstract[].class);
            LootItemCondition[] lootItemConditions = ChatDeserializer.getAsObject(jsonObject, "conditions", new LootItemCondition[0], jsonDeserializationContext, LootItemCondition[].class);
            LootItemFunction[] lootItemFunctions = ChatDeserializer.getAsObject(jsonObject, "functions", new LootItemFunction[0], jsonDeserializationContext, LootItemFunction[].class);
            NumberProvider numberProvider = ChatDeserializer.getAsObject(jsonObject, "rolls", jsonDeserializationContext, NumberProvider.class);
            NumberProvider numberProvider2 = ChatDeserializer.getAsObject(jsonObject, "bonus_rolls", ConstantValue.exactly(0.0F), jsonDeserializationContext, NumberProvider.class);
            return new LootSelector(lootPoolEntryContainers, lootItemConditions, lootItemFunctions, numberProvider, numberProvider2);
        }

        @Override
        public JsonElement serialize(LootSelector lootPool, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("rolls", jsonSerializationContext.serialize(lootPool.rolls));
            jsonObject.add("bonus_rolls", jsonSerializationContext.serialize(lootPool.bonusRolls));
            jsonObject.add("entries", jsonSerializationContext.serialize(lootPool.entries));
            if (!ArrayUtils.isEmpty((Object[])lootPool.conditions)) {
                jsonObject.add("conditions", jsonSerializationContext.serialize(lootPool.conditions));
            }

            if (!ArrayUtils.isEmpty((Object[])lootPool.functions)) {
                jsonObject.add("functions", jsonSerializationContext.serialize(lootPool.functions));
            }

            return jsonObject;
        }
    }
}
