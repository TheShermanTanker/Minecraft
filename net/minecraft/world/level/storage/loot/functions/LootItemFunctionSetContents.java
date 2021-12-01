package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.IRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.entries.LootEntryAbstract;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemFunctionSetContents extends LootItemFunctionConditional {
    final List<LootEntryAbstract> entries;
    final TileEntityTypes<?> type;

    LootItemFunctionSetContents(LootItemCondition[] conditions, TileEntityTypes<?> type, List<LootEntryAbstract> entries) {
        super(conditions);
        this.type = type;
        this.entries = ImmutableList.copyOf(entries);
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
            NBTTagCompound compoundTag2 = ItemBlock.getBlockEntityData(stack);
            if (compoundTag2 == null) {
                compoundTag2 = compoundTag;
            } else {
                compoundTag2.merge(compoundTag);
            }

            ItemBlock.setBlockEntityData(stack, this.type, compoundTag2);
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

    public static LootItemFunctionSetContents.Builder setContents(TileEntityTypes<?> type) {
        return new LootItemFunctionSetContents.Builder(type);
    }

    public static class Builder extends LootItemFunctionConditional.Builder<LootItemFunctionSetContents.Builder> {
        private final List<LootEntryAbstract> entries = Lists.newArrayList();
        private final TileEntityTypes<?> type;

        public Builder(TileEntityTypes<?> type) {
            this.type = type;
        }

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
            return new LootItemFunctionSetContents(this.getConditions(), this.type, this.entries);
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionSetContents> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionSetContents object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("type", IRegistry.BLOCK_ENTITY_TYPE.getKey(object.type).toString());
            json.add("entries", context.serialize(object.entries));
        }

        @Override
        public LootItemFunctionSetContents deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            LootEntryAbstract[] lootPoolEntryContainers = ChatDeserializer.getAsObject(jsonObject, "entries", jsonDeserializationContext, LootEntryAbstract[].class);
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "type"));
            TileEntityTypes<?> blockEntityType = IRegistry.BLOCK_ENTITY_TYPE.getOptional(resourceLocation).orElseThrow(() -> {
                return new JsonSyntaxException("Unknown block entity type id '" + resourceLocation + "'");
            });
            return new LootItemFunctionSetContents(lootItemConditions, blockEntityType, Arrays.asList(lootPoolEntryContainers));
        }
    }
}
