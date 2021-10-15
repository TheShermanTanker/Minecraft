package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.item.ItemEnchantedBook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.WeightedRandomEnchant;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootItemFunctionEnchant extends LootItemFunctionConditional {
    private static final Logger LOGGER = LogManager.getLogger();
    final List<Enchantment> enchantments;

    LootItemFunctionEnchant(LootItemCondition[] conditions, Collection<Enchantment> collection) {
        super(conditions);
        this.enchantments = ImmutableList.copyOf(collection);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.ENCHANT_RANDOMLY;
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        Random random = context.getRandom();
        Enchantment enchantment;
        if (this.enchantments.isEmpty()) {
            boolean bl = stack.is(Items.BOOK);
            List<Enchantment> list = IRegistry.ENCHANTMENT.stream().filter(Enchantment::isDiscoverable).filter((enchantment) -> {
                return bl || enchantment.canEnchant(stack);
            }).collect(Collectors.toList());
            if (list.isEmpty()) {
                LOGGER.warn("Couldn't find a compatible enchantment for {}", (Object)stack);
                return stack;
            }

            enchantment = list.get(random.nextInt(list.size()));
        } else {
            enchantment = this.enchantments.get(random.nextInt(this.enchantments.size()));
        }

        return enchantItem(stack, enchantment, random);
    }

    private static ItemStack enchantItem(ItemStack stack, Enchantment enchantment, Random random) {
        int i = MathHelper.nextInt(random, enchantment.getStartLevel(), enchantment.getMaxLevel());
        if (stack.is(Items.BOOK)) {
            stack = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantedBook.addEnchantment(stack, new WeightedRandomEnchant(enchantment, i));
        } else {
            stack.addEnchantment(enchantment, i);
        }

        return stack;
    }

    public static LootItemFunctionEnchant.Builder randomEnchantment() {
        return new LootItemFunctionEnchant.Builder();
    }

    public static LootItemFunctionConditional.Builder<?> randomApplicableEnchantment() {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionEnchant(conditions, ImmutableList.of());
        });
    }

    public static class Builder extends LootItemFunctionConditional.Builder<LootItemFunctionEnchant.Builder> {
        private final Set<Enchantment> enchantments = Sets.newHashSet();

        @Override
        protected LootItemFunctionEnchant.Builder getThis() {
            return this;
        }

        public LootItemFunctionEnchant.Builder withEnchantment(Enchantment enchantment) {
            this.enchantments.add(enchantment);
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new LootItemFunctionEnchant(this.getConditions(), this.enchantments);
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionEnchant> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionEnchant object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            if (!object.enchantments.isEmpty()) {
                JsonArray jsonArray = new JsonArray();

                for(Enchantment enchantment : object.enchantments) {
                    MinecraftKey resourceLocation = IRegistry.ENCHANTMENT.getKey(enchantment);
                    if (resourceLocation == null) {
                        throw new IllegalArgumentException("Don't know how to serialize enchantment " + enchantment);
                    }

                    jsonArray.add(new JsonPrimitive(resourceLocation.toString()));
                }

                json.add("enchantments", jsonArray);
            }

        }

        @Override
        public LootItemFunctionEnchant deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            List<Enchantment> list = Lists.newArrayList();
            if (jsonObject.has("enchantments")) {
                for(JsonElement jsonElement : ChatDeserializer.getAsJsonArray(jsonObject, "enchantments")) {
                    String string = ChatDeserializer.convertToString(jsonElement, "enchantment");
                    Enchantment enchantment = IRegistry.ENCHANTMENT.getOptional(new MinecraftKey(string)).orElseThrow(() -> {
                        return new JsonSyntaxException("Unknown enchantment '" + string + "'");
                    });
                    list.add(enchantment);
                }
            }

            return new LootItemFunctionEnchant(lootItemConditions, list);
        }
    }
}
