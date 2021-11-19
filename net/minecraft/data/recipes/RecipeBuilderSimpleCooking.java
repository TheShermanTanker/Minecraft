package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionInstance;
import net.minecraft.advancements.critereon.CriterionTriggerRecipeUnlocked;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeCooking;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializerCooking;
import net.minecraft.world.level.IMaterial;

public class RecipeBuilderSimpleCooking implements IRecipeBuilder {
    private final Item result;
    private final RecipeItemStack ingredient;
    private final float experience;
    private final int cookingTime;
    private final Advancement.SerializedAdvancement advancement = Advancement.SerializedAdvancement.advancement();
    @Nullable
    private String group;
    private final RecipeSerializerCooking<?> serializer;

    private RecipeBuilderSimpleCooking(IMaterial output, RecipeItemStack input, float experience, int cookingTime, RecipeSerializerCooking<?> serializer) {
        this.result = output.getItem();
        this.ingredient = input;
        this.experience = experience;
        this.cookingTime = cookingTime;
        this.serializer = serializer;
    }

    public static RecipeBuilderSimpleCooking cooking(RecipeItemStack ingredient, IMaterial result, float experience, int cookingTime, RecipeSerializerCooking<?> serializer) {
        return new RecipeBuilderSimpleCooking(result, ingredient, experience, cookingTime, serializer);
    }

    public static RecipeBuilderSimpleCooking campfireCooking(RecipeItemStack result, IMaterial ingredient, float experience, int cookingTime) {
        return cooking(result, ingredient, experience, cookingTime, RecipeSerializer.CAMPFIRE_COOKING_RECIPE);
    }

    public static RecipeBuilderSimpleCooking blasting(RecipeItemStack ingredient, IMaterial result, float experience, int cookingTime) {
        return cooking(ingredient, result, experience, cookingTime, RecipeSerializer.BLASTING_RECIPE);
    }

    public static RecipeBuilderSimpleCooking smelting(RecipeItemStack ingredient, IMaterial result, float experience, int cookingTime) {
        return cooking(ingredient, result, experience, cookingTime, RecipeSerializer.SMELTING_RECIPE);
    }

    public static RecipeBuilderSimpleCooking smoking(RecipeItemStack result, IMaterial ingredient, float experience, int cookingTime) {
        return cooking(result, ingredient, experience, cookingTime, RecipeSerializer.SMOKING_RECIPE);
    }

    @Override
    public RecipeBuilderSimpleCooking unlockedBy(String string, CriterionInstance criterionTriggerInstance) {
        this.advancement.addCriterion(string, criterionTriggerInstance);
        return this;
    }

    @Override
    public RecipeBuilderSimpleCooking group(@Nullable String string) {
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
        exporter.accept(new RecipeBuilderSimpleCooking.Result(recipeId, this.group == null ? "" : this.group, this.ingredient, this.result, this.experience, this.cookingTime, this.advancement, new MinecraftKey(recipeId.getNamespace(), "recipes/" + this.result.getItemCategory().getRecipeFolderName() + "/" + recipeId.getKey()), this.serializer));
    }

    private void ensureValid(MinecraftKey recipeId) {
        if (this.advancement.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + recipeId);
        }
    }

    public static class Result implements IFinishedRecipe {
        private final MinecraftKey id;
        private final String group;
        private final RecipeItemStack ingredient;
        private final Item result;
        private final float experience;
        private final int cookingTime;
        private final Advancement.SerializedAdvancement advancement;
        private final MinecraftKey advancementId;
        private final RecipeSerializer<? extends RecipeCooking> serializer;

        public Result(MinecraftKey recipeId, String group, RecipeItemStack input, Item result, float experience, int cookingTime, Advancement.SerializedAdvancement builder, MinecraftKey advancementId, RecipeSerializer<? extends RecipeCooking> serializer) {
            this.id = recipeId;
            this.group = group;
            this.ingredient = input;
            this.result = result;
            this.experience = experience;
            this.cookingTime = cookingTime;
            this.advancement = builder;
            this.advancementId = advancementId;
            this.serializer = serializer;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            if (!this.group.isEmpty()) {
                json.addProperty("group", this.group);
            }

            json.add("ingredient", this.ingredient.toJson());
            json.addProperty("result", IRegistry.ITEM.getKey(this.result).toString());
            json.addProperty("experience", this.experience);
            json.addProperty("cookingtime", this.cookingTime);
        }

        @Override
        public RecipeSerializer<?> getType() {
            return this.serializer;
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
