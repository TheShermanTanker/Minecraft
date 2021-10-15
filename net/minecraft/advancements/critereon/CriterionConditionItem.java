package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsInstance;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemEnchantedBook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.IMaterial;

public class CriterionConditionItem {
    public static final CriterionConditionItem ANY = new CriterionConditionItem();
    @Nullable
    private final Tag<Item> tag;
    @Nullable
    private final Set<Item> items;
    private final CriterionConditionValue.IntegerRange count;
    private final CriterionConditionValue.IntegerRange durability;
    private final CriterionConditionEnchantments[] enchantments;
    private final CriterionConditionEnchantments[] storedEnchantments;
    @Nullable
    private final PotionRegistry potion;
    private final CriterionConditionNBT nbt;

    public CriterionConditionItem() {
        this.tag = null;
        this.items = null;
        this.potion = null;
        this.count = CriterionConditionValue.IntegerRange.ANY;
        this.durability = CriterionConditionValue.IntegerRange.ANY;
        this.enchantments = CriterionConditionEnchantments.NONE;
        this.storedEnchantments = CriterionConditionEnchantments.NONE;
        this.nbt = CriterionConditionNBT.ANY;
    }

    public CriterionConditionItem(@Nullable Tag<Item> tag, @Nullable Set<Item> items, CriterionConditionValue.IntegerRange count, CriterionConditionValue.IntegerRange durability, CriterionConditionEnchantments[] enchantments, CriterionConditionEnchantments[] storedEnchantments, @Nullable PotionRegistry potion, CriterionConditionNBT nbt) {
        this.tag = tag;
        this.items = items;
        this.count = count;
        this.durability = durability;
        this.enchantments = enchantments;
        this.storedEnchantments = storedEnchantments;
        this.potion = potion;
        this.nbt = nbt;
    }

    public boolean matches(ItemStack stack) {
        if (this == ANY) {
            return true;
        } else if (this.tag != null && !stack.is(this.tag)) {
            return false;
        } else if (this.items != null && !this.items.contains(stack.getItem())) {
            return false;
        } else if (!this.count.matches(stack.getCount())) {
            return false;
        } else if (!this.durability.isAny() && !stack.isDamageableItem()) {
            return false;
        } else if (!this.durability.matches(stack.getMaxDamage() - stack.getDamage())) {
            return false;
        } else if (!this.nbt.matches(stack)) {
            return false;
        } else {
            if (this.enchantments.length > 0) {
                Map<Enchantment, Integer> map = EnchantmentManager.deserializeEnchantments(stack.getEnchantments());

                for(CriterionConditionEnchantments enchantmentPredicate : this.enchantments) {
                    if (!enchantmentPredicate.containedIn(map)) {
                        return false;
                    }
                }
            }

            if (this.storedEnchantments.length > 0) {
                Map<Enchantment, Integer> map2 = EnchantmentManager.deserializeEnchantments(ItemEnchantedBook.getEnchantments(stack));

                for(CriterionConditionEnchantments enchantmentPredicate2 : this.storedEnchantments) {
                    if (!enchantmentPredicate2.containedIn(map2)) {
                        return false;
                    }
                }
            }

            PotionRegistry potion = PotionUtil.getPotion(stack);
            return this.potion == null || this.potion == potion;
        }
    }

    public static CriterionConditionItem fromJson(@Nullable JsonElement el) {
        if (el != null && !el.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(el, "item");
            CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("count"));
            CriterionConditionValue.IntegerRange ints2 = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("durability"));
            if (jsonObject.has("data")) {
                throw new JsonParseException("Disallowed data tag found");
            } else {
                CriterionConditionNBT nbtPredicate = CriterionConditionNBT.fromJson(jsonObject.get("nbt"));
                Set<Item> set = null;
                JsonArray jsonArray = ChatDeserializer.getAsJsonArray(jsonObject, "items", (JsonArray)null);
                if (jsonArray != null) {
                    ImmutableSet.Builder<Item> builder = ImmutableSet.builder();

                    for(JsonElement jsonElement : jsonArray) {
                        MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.convertToString(jsonElement, "item"));
                        builder.add(IRegistry.ITEM.getOptional(resourceLocation).orElseThrow(() -> {
                            return new JsonSyntaxException("Unknown item id '" + resourceLocation + "'");
                        }));
                    }

                    set = builder.build();
                }

                Tag<Item> tag = null;
                if (jsonObject.has("tag")) {
                    MinecraftKey resourceLocation2 = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "tag"));
                    tag = TagsInstance.getInstance().getTagOrThrow(IRegistry.ITEM_REGISTRY, resourceLocation2, (id) -> {
                        return new JsonSyntaxException("Unknown item tag '" + id + "'");
                    });
                }

                PotionRegistry potion = null;
                if (jsonObject.has("potion")) {
                    MinecraftKey resourceLocation3 = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "potion"));
                    potion = IRegistry.POTION.getOptional(resourceLocation3).orElseThrow(() -> {
                        return new JsonSyntaxException("Unknown potion '" + resourceLocation3 + "'");
                    });
                }

                CriterionConditionEnchantments[] enchantmentPredicates = CriterionConditionEnchantments.fromJsonArray(jsonObject.get("enchantments"));
                CriterionConditionEnchantments[] enchantmentPredicates2 = CriterionConditionEnchantments.fromJsonArray(jsonObject.get("stored_enchantments"));
                return new CriterionConditionItem(tag, set, ints, ints2, enchantmentPredicates, enchantmentPredicates2, potion, nbtPredicate);
            }
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.items != null) {
                JsonArray jsonArray = new JsonArray();

                for(Item item : this.items) {
                    jsonArray.add(IRegistry.ITEM.getKey(item).toString());
                }

                jsonObject.add("items", jsonArray);
            }

            if (this.tag != null) {
                jsonObject.addProperty("tag", TagsInstance.getInstance().getIdOrThrow(IRegistry.ITEM_REGISTRY, this.tag, () -> {
                    return new IllegalStateException("Unknown item tag");
                }).toString());
            }

            jsonObject.add("count", this.count.serializeToJson());
            jsonObject.add("durability", this.durability.serializeToJson());
            jsonObject.add("nbt", this.nbt.serializeToJson());
            if (this.enchantments.length > 0) {
                JsonArray jsonArray2 = new JsonArray();

                for(CriterionConditionEnchantments enchantmentPredicate : this.enchantments) {
                    jsonArray2.add(enchantmentPredicate.serializeToJson());
                }

                jsonObject.add("enchantments", jsonArray2);
            }

            if (this.storedEnchantments.length > 0) {
                JsonArray jsonArray3 = new JsonArray();

                for(CriterionConditionEnchantments enchantmentPredicate2 : this.storedEnchantments) {
                    jsonArray3.add(enchantmentPredicate2.serializeToJson());
                }

                jsonObject.add("stored_enchantments", jsonArray3);
            }

            if (this.potion != null) {
                jsonObject.addProperty("potion", IRegistry.POTION.getKey(this.potion).toString());
            }

            return jsonObject;
        }
    }

    public static CriterionConditionItem[] fromJsonArray(@Nullable JsonElement el) {
        if (el != null && !el.isJsonNull()) {
            JsonArray jsonArray = ChatDeserializer.convertToJsonArray(el, "items");
            CriterionConditionItem[] itemPredicates = new CriterionConditionItem[jsonArray.size()];

            for(int i = 0; i < itemPredicates.length; ++i) {
                itemPredicates[i] = fromJson(jsonArray.get(i));
            }

            return itemPredicates;
        } else {
            return new CriterionConditionItem[0];
        }
    }

    public static class Builder {
        private final List<CriterionConditionEnchantments> enchantments = Lists.newArrayList();
        private final List<CriterionConditionEnchantments> storedEnchantments = Lists.newArrayList();
        @Nullable
        private Set<Item> items;
        @Nullable
        private Tag<Item> tag;
        private CriterionConditionValue.IntegerRange count = CriterionConditionValue.IntegerRange.ANY;
        private CriterionConditionValue.IntegerRange durability = CriterionConditionValue.IntegerRange.ANY;
        @Nullable
        private PotionRegistry potion;
        private CriterionConditionNBT nbt = CriterionConditionNBT.ANY;

        private Builder() {
        }

        public static CriterionConditionItem.Builder item() {
            return new CriterionConditionItem.Builder();
        }

        public CriterionConditionItem.Builder of(IMaterial... items) {
            this.items = Stream.of(items).map(IMaterial::getItem).collect(ImmutableSet.toImmutableSet());
            return this;
        }

        public CriterionConditionItem.Builder of(Tag<Item> tag) {
            this.tag = tag;
            return this;
        }

        public CriterionConditionItem.Builder withCount(CriterionConditionValue.IntegerRange count) {
            this.count = count;
            return this;
        }

        public CriterionConditionItem.Builder hasDurability(CriterionConditionValue.IntegerRange durability) {
            this.durability = durability;
            return this;
        }

        public CriterionConditionItem.Builder isPotion(PotionRegistry potion) {
            this.potion = potion;
            return this;
        }

        public CriterionConditionItem.Builder hasNbt(NBTTagCompound nbt) {
            this.nbt = new CriterionConditionNBT(nbt);
            return this;
        }

        public CriterionConditionItem.Builder hasEnchantment(CriterionConditionEnchantments enchantment) {
            this.enchantments.add(enchantment);
            return this;
        }

        public CriterionConditionItem.Builder hasStoredEnchantment(CriterionConditionEnchantments enchantment) {
            this.storedEnchantments.add(enchantment);
            return this;
        }

        public CriterionConditionItem build() {
            return new CriterionConditionItem(this.tag, this.items, this.count, this.durability, this.enchantments.toArray(CriterionConditionEnchantments.NONE), this.storedEnchantments.toArray(CriterionConditionEnchantments.NONE), this.potion, this.nbt);
        }
    }
}
