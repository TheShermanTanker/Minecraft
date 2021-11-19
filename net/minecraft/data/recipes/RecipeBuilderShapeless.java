package net.minecraft.data.recipes;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
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

public class RecipeBuilderShapeless implements IRecipeBuilder {
    private final Item result;
    private final int count;
    private final List<RecipeItemStack> ingredients = Lists.newArrayList();
    private final Advancement.SerializedAdvancement advancement = Advancement.SerializedAdvancement.advancement();
    @Nullable
    private String group;

    public RecipeBuilderShapeless(IMaterial output, int outputCount) {
        this.result = output.getItem();
        this.count = outputCount;
    }

    public static RecipeBuilderShapeless shapeless(IMaterial output) {
        return new RecipeBuilderShapeless(output, 1);
    }

    public static RecipeBuilderShapeless shapeless(IMaterial output, int outputCount) {
        return new RecipeBuilderShapeless(output, outputCount);
    }

    public RecipeBuilderShapeless requires(Tag<Item> tag) {
        return this.requires(RecipeItemStack.of(tag));
    }

    public RecipeBuilderShapeless requires(IMaterial itemProvider) {
        return this.requires(itemProvider, 1);
    }

    public RecipeBuilderShapeless requires(IMaterial itemProvider, int size) {
        for(int i = 0; i < size; ++i) {
            this.requires(RecipeItemStack.of(itemProvider));
        }

        return this;
    }

    public RecipeBuilderShapeless requires(RecipeItemStack ingredient) {
        return this.requires(ingredient, 1);
    }

    public RecipeBuilderShapeless requires(RecipeItemStack ingredient, int size) {
        for(int i = 0; i < size; ++i) {
            this.ingredients.add(ingredient);
        }

        return this;
    }

    @Override
    public RecipeBuilderShapeless unlockedBy(String string, CriterionInstance criterionTriggerInstance) {
        this.advancement.addCriterion(string, criterionTriggerInstance);
        return this;
    }

    @Override
    public RecipeBuilderShapeless group(@Nullable String string) {
        this.group = string;
        return this;
    }

    @Override
    public Item getResult() {
        return this.result;
    }

    @Override
    public void save(Consumer<IFinishedRecipe> exporter, MinecraftKey recipeId) {
        this.ensureValid(recipeId);
        this.advancement.parent(new MinecraftKey("recipes/root")).addCriterion("has_the_recipe", CriterionTriggerRecipeUnlocked.unlocked(recipeId)).rewards(AdvancementRewards.Builder.recipe(recipeId)).requirements(AdvancementRequirements.OR);
        exporter.accept(new RecipeBuilderShapeless.Result(recipeId, this.result, this.count, this.group == null ? "" : this.group, this.ingredients, this.advancement, new MinecraftKey(recipeId.getNamespace(), "recipes/" + this.result.getItemCategory().getRecipeFolderName() + "/" + recipeId.getKey())));
    }

    private void ensureValid(MinecraftKey recipeId) {
        if (this.advancement.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + recipeId);
        }
    }

    public static class Result implements IFinishedRecipe {
        private final MinecraftKey id;
        private final Item result;
        private final int count;
        private final String group;
        private final List<RecipeItemStack> ingredients;
        private final Advancement.SerializedAdvancement advancement;
        private final MinecraftKey advancementId;

        public Result(MinecraftKey recipeId, Item output, int outputCount, String group, List<RecipeItemStack> inputs, Advancement.SerializedAdvancement builder, MinecraftKey advancementId) {
            this.id = recipeId;
            this.result = output;
            this.count = outputCount;
            this.group = group;
            this.ingredients = inputs;
            this.advancement = builder;
            this.advancementId = advancementId;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            if (!this.group.isEmpty()) {
                json.addProperty("group", this.group);
            }

            JsonArray jsonArray = new JsonArray();

            for(RecipeItemStack ingredient : this.ingredients) {
                jsonArray.add(ingredient.toJson());
            }

            json.add("ingredients", jsonArray);
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("item", IRegistry.ITEM.getKey(this.result).toString());
            if (this.count > 1) {
                jsonObject.addProperty("count", this.count);
            }

            json.add("result", jsonObject);
        }

        @Override
        public RecipeSerializer<?> getType() {
            return RecipeSerializer.SHAPELESS_RECIPE;
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
