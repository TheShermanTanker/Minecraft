package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemEnchantedBook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.WeightedRandomEnchant;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class SetEnchantmentsFunction extends LootItemFunctionConditional {
    final Map<Enchantment, NumberProvider> enchantments;
    final boolean add;

    SetEnchantmentsFunction(LootItemCondition[] conditions, Map<Enchantment, NumberProvider> map, boolean bl) {
        super(conditions);
        this.enchantments = ImmutableMap.copyOf(map);
        this.add = bl;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_ENCHANTMENTS;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.enchantments.values().stream().flatMap((numberProvider) -> {
            return numberProvider.getReferencedContextParams().stream();
        }).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        Object2IntMap<Enchantment> object2IntMap = new Object2IntOpenHashMap<>();
        this.enchantments.forEach((enchantment, numberProvider) -> {
            object2IntMap.put(enchantment, numberProvider.getInt(context));
        });
        if (stack.getItem() == Items.BOOK) {
            ItemStack itemStack = new ItemStack(Items.ENCHANTED_BOOK);
            object2IntMap.forEach((enchantment, level) -> {
                ItemEnchantedBook.addEnchantment(itemStack, new WeightedRandomEnchant(enchantment, level));
            });
            return itemStack;
        } else {
            Map<Enchantment, Integer> map = EnchantmentManager.getEnchantments(stack);
            if (this.add) {
                object2IntMap.forEach((enchantment, level) -> {
                    updateEnchantment(map, enchantment, Math.max(map.getOrDefault(enchantment, 0) + level, 0));
                });
            } else {
                object2IntMap.forEach((enchantment, level) -> {
                    updateEnchantment(map, enchantment, Math.max(level, 0));
                });
            }

            EnchantmentManager.setEnchantments(map, stack);
            return stack;
        }
    }

    private static void updateEnchantment(Map<Enchantment, Integer> map, Enchantment enchantment, int level) {
        if (level == 0) {
            map.remove(enchantment);
        } else {
            map.put(enchantment, level);
        }

    }

    public static class Builder extends LootItemFunctionConditional.Builder<SetEnchantmentsFunction.Builder> {
        private final Map<Enchantment, NumberProvider> enchantments = Maps.newHashMap();
        private final boolean add;

        public Builder() {
            this(false);
        }

        public Builder(boolean add) {
            this.add = add;
        }

        @Override
        protected SetEnchantmentsFunction.Builder getThis() {
            return this;
        }

        public SetEnchantmentsFunction.Builder withEnchantment(Enchantment enchantment, NumberProvider level) {
            this.enchantments.put(enchantment, level);
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetEnchantmentsFunction(this.getConditions(), this.enchantments, this.add);
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<SetEnchantmentsFunction> {
        @Override
        public void serialize(JsonObject json, SetEnchantmentsFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            JsonObject jsonObject = new JsonObject();
            object.enchantments.forEach((enchantment, numberProvider) -> {
                MinecraftKey resourceLocation = IRegistry.ENCHANTMENT.getKey(enchantment);
                if (resourceLocation == null) {
                    throw new IllegalArgumentException("Don't know how to serialize enchantment " + enchantment);
                } else {
                    jsonObject.add(resourceLocation.toString(), context.serialize(numberProvider));
                }
            });
            json.add("enchantments", jsonObject);
            json.addProperty("add", object.add);
        }

        @Override
        public SetEnchantmentsFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            Map<Enchantment, NumberProvider> map = Maps.newHashMap();
            if (jsonObject.has("enchantments")) {
                JsonObject jsonObject2 = ChatDeserializer.getAsJsonObject(jsonObject, "enchantments");

                for(Entry<String, JsonElement> entry : jsonObject2.entrySet()) {
                    String string = entry.getKey();
                    JsonElement jsonElement = entry.getValue();
                    Enchantment enchantment = IRegistry.ENCHANTMENT.getOptional(new MinecraftKey(string)).orElseThrow(() -> {
                        return new JsonSyntaxException("Unknown enchantment '" + string + "'");
                    });
                    NumberProvider numberProvider = jsonDeserializationContext.deserialize(jsonElement, NumberProvider.class);
                    map.put(enchantment, numberProvider);
                }
            }

            boolean bl = ChatDeserializer.getAsBoolean(jsonObject, "add", false);
            return new SetEnchantmentsFunction(lootItemConditions, map, bl);
        }
    }
}
