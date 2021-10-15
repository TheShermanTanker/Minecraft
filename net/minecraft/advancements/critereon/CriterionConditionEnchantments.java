package net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.enchantment.Enchantment;

public class CriterionConditionEnchantments {
    public static final CriterionConditionEnchantments ANY = new CriterionConditionEnchantments();
    public static final CriterionConditionEnchantments[] NONE = new CriterionConditionEnchantments[0];
    private final Enchantment enchantment;
    private final CriterionConditionValue.IntegerRange level;

    public CriterionConditionEnchantments() {
        this.enchantment = null;
        this.level = CriterionConditionValue.IntegerRange.ANY;
    }

    public CriterionConditionEnchantments(@Nullable Enchantment enchantment, CriterionConditionValue.IntegerRange levels) {
        this.enchantment = enchantment;
        this.level = levels;
    }

    public boolean containedIn(Map<Enchantment, Integer> enchantments) {
        if (this.enchantment != null) {
            if (!enchantments.containsKey(this.enchantment)) {
                return false;
            }

            int i = enchantments.get(this.enchantment);
            if (this.level != null && !this.level.matches(i)) {
                return false;
            }
        } else if (this.level != null) {
            for(Integer integer : enchantments.values()) {
                if (this.level.matches(integer)) {
                    return true;
                }
            }

            return false;
        }

        return true;
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.enchantment != null) {
                jsonObject.addProperty("enchantment", IRegistry.ENCHANTMENT.getKey(this.enchantment).toString());
            }

            jsonObject.add("levels", this.level.serializeToJson());
            return jsonObject;
        }
    }

    public static CriterionConditionEnchantments fromJson(@Nullable JsonElement el) {
        if (el != null && !el.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(el, "enchantment");
            Enchantment enchantment = null;
            if (jsonObject.has("enchantment")) {
                MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "enchantment"));
                enchantment = IRegistry.ENCHANTMENT.getOptional(resourceLocation).orElseThrow(() -> {
                    return new JsonSyntaxException("Unknown enchantment '" + resourceLocation + "'");
                });
            }

            CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("levels"));
            return new CriterionConditionEnchantments(enchantment, ints);
        } else {
            return ANY;
        }
    }

    public static CriterionConditionEnchantments[] fromJsonArray(@Nullable JsonElement el) {
        if (el != null && !el.isJsonNull()) {
            JsonArray jsonArray = ChatDeserializer.convertToJsonArray(el, "enchantments");
            CriterionConditionEnchantments[] enchantmentPredicates = new CriterionConditionEnchantments[jsonArray.size()];

            for(int i = 0; i < enchantmentPredicates.length; ++i) {
                enchantmentPredicates[i] = fromJson(jsonArray.get(i));
            }

            return enchantmentPredicates;
        } else {
            return NONE;
        }
    }
}
