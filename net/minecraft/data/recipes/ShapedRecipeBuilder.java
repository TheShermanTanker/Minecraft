package net.minecraft.data.recipes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionInstance;
import net.minecraft.advancements.critereon.CriterionTriggerRecipeUnlocked;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.IMaterial;

public class ShapedRecipeBuilder implements RecipeBuilder {
    private final Item result;
    private final int count;
    private final List<String> rows = Lists.newArrayList();
    private final Map<Character, RecipeItemStack> key = Maps.newLinkedHashMap();
    private final Advancement.SerializedAdvancement advancement = Advancement.SerializedAdvancement.advancement();
    @Nullable
    private String group;

    public ShapedRecipeBuilder(IMaterial output, int outputCount) {
        this.result = output.getItem();
        this.count = outputCount;
    }

    public static ShapedRecipeBuilder shaped(IMaterial output) {
        return shaped(output, 1);
    }

    public static ShapedRecipeBuilder shaped(IMaterial output, int outputCount) {
        return new ShapedRecipeBuilder(output, outputCount);
    }

    public ShapedRecipeBuilder define(Character c, Tag<Item> tag) {
        return this.define(c, RecipeItemStack.of(tag));
    }

    public ShapedRecipeBuilder define(Character c, IMaterial itemProvider) {
        return this.define(c, RecipeItemStack.of(itemProvider));
    }

    public ShapedRecipeBuilder define(Character c, RecipeItemStack ingredient) {
        if (this.key.containsKey(c)) {
            throw new IllegalArgumentException("Symbol '" + c + "' is already defined!");
        } else if (c == ' ') {
            throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
        } else {
            this.key.put(c, ingredient);
            return this;
        }
    }

    public ShapedRecipeBuilder pattern(String patternStr) {
        if (!this.rows.isEmpty() && patternStr.length() != this.rows.get(0).length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        } else {
            this.rows.add(patternStr);
            return this;
        }
    }

    @Override
    public ShapedRecipeBuilder unlockedBy(String string, CriterionInstance criterionTriggerInstance) {
        this.advancement.addCriterion(string, criterionTriggerInstance);
        return this;
    }

    @Override
    public ShapedRecipeBuilder group(@Nullable String string) {
        this.group = string;
        return this;
    }

    @Override
    public Item getResult() {
        return this.result;
    }

    @Override
    public void save(Consumer<FinishedRecipe> exporter, MinecraftKey recipeId) {
        this.ensureValid(recipeId);
        this.advancement.parent(new MinecraftKey("recipes/root")).addCriterion("has_the_recipe", CriterionTriggerRecipeUnlocked.unlocked(recipeId)).rewards(AdvancementRewards.Builder.recipe(recipeId)).requirements(AdvancementRequirements.OR);
        exporter.accept(new ShapedRecipeBuilder.Result(recipeId, this.result, this.count, this.group == null ? "" : this.group, this.rows, this.key, this.advancement, new MinecraftKey(recipeId.getNamespace(), "recipes/" + this.result.getItemCategory().getRecipeFolderName() + "/" + recipeId.getKey())));
    }

    private void ensureValid(MinecraftKey recipeId) {
        if (this.rows.isEmpty()) {
            throw new IllegalStateException("No pattern is defined for shaped recipe " + recipeId + "!");
        } else {
            Set<Character> set = Sets.newHashSet(this.key.keySet());
            set.remove(' ');

            for(String string : this.rows) {
                for(int i = 0; i < string.length(); ++i) {
                    char c = string.charAt(i);
                    if (!this.key.containsKey(c) && c != ' ') {
                        throw new IllegalStateException("Pattern in recipe " + recipeId + " uses undefined symbol '" + c + "'");
                    }

                    set.remove(c);
                }
            }

            if (!set.isEmpty()) {
                throw new IllegalStateException("Ingredients are defined but not used in pattern for recipe " + recipeId);
            } else if (this.rows.size() == 1 && this.rows.get(0).length() == 1) {
                throw new IllegalStateException("Shaped recipe " + recipeId + " only takes in a single item - should it be a shapeless recipe instead?");
            } else if (this.advancement.getCriteria().isEmpty()) {
                throw new IllegalStateException("No way of obtaining recipe " + recipeId);
            }
        }
    }

    static class Result implements FinishedRecipe {
        private final MinecraftKey id;
        private final Item result;
        private final int count;
        private final String group;
        private final List<String> pattern;
        private final Map<Character, RecipeItemStack> key;
        private final Advancement.SerializedAdvancement advancement;
        private final MinecraftKey advancementId;

        public Result(MinecraftKey recipeId, Item output, int resultCount, String group, List<String> pattern, Map<Character, RecipeItemStack> inputs, Advancement.SerializedAdvancement builder, MinecraftKey advancementId) {
            this.id = recipeId;
            this.result = output;
            this.count = resultCount;
            this.group = group;
            this.pattern = pattern;
            this.key = inputs;
            this.advancement = builder;
            this.advancementId = advancementId;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            if (!this.group.isEmpty()) {
                json.addProperty("group", this.group);
            }

            JsonArray jsonArray = new JsonArray();

            for(String string : this.pattern) {
                jsonArray.add(string);
            }

            json.add("pattern", jsonArray);
            JsonObject jsonObject = new JsonObject();

            for(Entry<Character, RecipeItemStack> entry : this.key.entrySet()) {
                jsonObject.add(String.valueOf(entry.getKey()), entry.getValue().toJson());
            }

            json.add("key", jsonObject);
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("item", IRegistry.ITEM.getKey(this.result).toString());
            if (this.count > 1) {
                jsonObject2.addProperty("count", this.count);
            }

            json.add("result", jsonObject2);
        }

        @Override
        public RecipeSerializer<?> getType() {
            return RecipeSerializer.SHAPED_RECIPE;
        }

        @Override
        public MinecraftKey getId() {
            return this.id;
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return this.advancement.serializeToJson();
        }

        @Nullable
        @Override
        public MinecraftKey getAdvancementId() {
            return this.advancementId;
        }
    }
}
