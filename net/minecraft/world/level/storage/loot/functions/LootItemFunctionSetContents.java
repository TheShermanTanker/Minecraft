package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.entries.LootEntryAbstract;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemFunctionSetContents extends LootItemFunctionConditional {
    final List<LootEntryAbstract> entries;

    LootItemFunctionSetContents(LootItemCondition[] conditions, List<LootEntryAbstract> list) {
        super(conditions);
        this.entries = ImmutableList.copyOf(list);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_CONTENTS;
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        if (stack.isEmpty()) {
            return stack;
        } else {
            NonNullList<ItemStack> nonNullList = NonNullList.create();
            this.entries.forEach((entry) -> {
                entry.expand(context, (choice) -> {
                    choice.createItemStack(LootTable.createStackSplitter(nonNullList::add), context);
                });
            });
            NBTTagCompound compoundTag = new NBTTagCompound();
            ContainerUtil.saveAllItems(compoundTag, nonNullList);
            NBTTagCompound compoundTag2 = stack.getOrCreateTag();
            compoundTag2.set("BlockEntityTag", compoundTag.merge(compoundTag2.getCompound("BlockEntityTag")));
            return stack;
        }
    }

    @Override
    public void validate(LootCollector reporter) {
        super.validate(reporter);

        for(int i = 0; i < this.entries.size(); ++i) {
            this.entries.get(i).validate(reporter.forChild(".entry[" + i + "]"));
        }

    }

    public static LootItemFunctionSetContents.Builder setContents() {
        return new LootItemFunctionSetContents.Builder();
    }

    public static class Builder extends LootItemFunctionConditional.Builder<LootItemFunctionSetContents.Builder> {
        private final List<LootEntryAbstract> entries = Lists.newArrayList();

        @Override
        protected LootItemFunctionSetContents.Builder getThis() {
            return this;
        }

        public LootItemFunctionSetContents.Builder withEntry(LootEntryAbstract.Builder<?> entryBuilder) {
            this.entries.add(entryBuilder.build());
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new LootItemFunctionSetContents(this.getConditions(), this.entries);
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionSetContents> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionSetContents object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("entries", context.serialize(object.entries));
        }

        @Override
        public LootItemFunctionSetContents deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            LootEntryAbstract[] lootPoolEntryContainers = ChatDeserializer.getAsObject(jsonObject, "entries", jsonDeserializationContext, LootEntryAbstract[].class);
            return new LootItemFunctionSetContents(lootItemConditions, Arrays.asList(lootPoolEntryContainers));
        }
    }
}
