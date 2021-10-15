package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemFunctionSetTag extends LootItemFunctionConditional {
    final NBTTagCompound tag;

    LootItemFunctionSetTag(LootItemCondition[] conditions, NBTTagCompound compoundTag) {
        super(conditions);
        this.tag = compoundTag;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_NBT;
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        stack.getOrCreateTag().merge(this.tag);
        return stack;
    }

    public static LootItemFunctionConditional.Builder<?> setTag(NBTTagCompound nbt) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionSetTag(conditions, nbt);
        });
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionSetTag> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionSetTag object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("tag", object.tag.toString());
        }

        @Override
        public LootItemFunctionSetTag deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            try {
                NBTTagCompound compoundTag = MojangsonParser.parse(ChatDeserializer.getAsString(jsonObject, "tag"));
                return new LootItemFunctionSetTag(lootItemConditions, compoundTag);
            } catch (CommandSyntaxException var5) {
                throw new JsonSyntaxException(var5.getMessage());
            }
        }
    }
}
