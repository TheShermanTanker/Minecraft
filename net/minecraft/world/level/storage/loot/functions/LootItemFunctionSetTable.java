package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemFunctionSetTable extends LootItemFunctionConditional {
    final MinecraftKey name;
    final long seed;

    LootItemFunctionSetTable(LootItemCondition[] conditions, MinecraftKey resourceLocation, long l) {
        super(conditions);
        this.name = resourceLocation;
        this.seed = l;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_LOOT_TABLE;
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        if (stack.isEmpty()) {
            return stack;
        } else {
            NBTTagCompound compoundTag = new NBTTagCompound();
            compoundTag.setString("LootTable", this.name.toString());
            if (this.seed != 0L) {
                compoundTag.setLong("LootTableSeed", this.seed);
            }

            stack.getOrCreateTag().set("BlockEntityTag", compoundTag);
            return stack;
        }
    }

    @Override
    public void validate(LootCollector reporter) {
        if (reporter.hasVisitedTable(this.name)) {
            reporter.reportProblem("Table " + this.name + " is recursively called");
        } else {
            super.validate(reporter);
            LootTable lootTable = reporter.resolveLootTable(this.name);
            if (lootTable == null) {
                reporter.reportProblem("Unknown loot table called " + this.name);
            } else {
                lootTable.validate(reporter.enterTable("->{" + this.name + "}", this.name));
            }

        }
    }

    public static LootItemFunctionConditional.Builder<?> withLootTable(MinecraftKey id) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionSetTable(conditions, id, 0L);
        });
    }

    public static LootItemFunctionConditional.Builder<?> withLootTable(MinecraftKey id, long seed) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionSetTable(conditions, id, seed);
        });
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionSetTable> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionSetTable object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("name", object.name.toString());
            if (object.seed != 0L) {
                json.addProperty("seed", object.seed);
            }

        }

        @Override
        public LootItemFunctionSetTable deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "name"));
            long l = ChatDeserializer.getAsLong(jsonObject, "seed", 0L);
            return new LootItemFunctionSetTable(lootItemConditions, resourceLocation, l);
        }
    }
}
