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
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.IMaterial;

public class SingleItemRecipeBuilder implements RecipeBuilder {
    private final Item result;
    private final RecipeItemStack ingredient;
    private final int count;
    private final Advancement.SerializedAdvancement advancement = Advancement.SerializedAdvancement.advancement();
    @Nullable
    private String group;
    private final RecipeSerializer<?> type;

    public SingleItemRecipeBuilder(RecipeSerializer<?> serializer, RecipeItemStack input, IMaterial output, int outputCount) {
        this.type = serializer;
        this.result = output.getItem();
        this.ingredient = input;
        this.count = outputCount;
    }

    public static SingleItemRecipeBuilder stonecutting(RecipeItemStack input, IMaterial output) {
        return new SingleItemRecipeBuilder(RecipeSerializer.STONECUTTER, input, output, 1);
    }

    public static SingleItemRecipeBuilder stonecutting(RecipeItemStack input, IMaterial output, int outputCount) {
        return new SingleItemRecipeBuilder(RecipeSerializer.STONECUTTER, input, output, outputCount);
    }

    @Override
    public SingleItemRecipeBuilder unlockedBy(String string, CriterionInstance criterionTriggerInstance) {
        this.advancement.addCriterion(string, criterionTriggerInstance);
        return this;
    }

    @Override
    public SingleItemRecipeBuilder group(@Nullable String string) {
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
        exporter.accept(new SingleItemRecipeBuilder.Result(recipeId, this.type, this.group == null ? "" : this.group, this.ingredient, this.result, this.count, this.advancement, new MinecraftKey(recipeId.getNamespace(), "recipes/" + this.result.getItemCategory().getRecipeFolderName() + "/" + recipeId.getKey())));
    }

    private void ensureValid(MinecraftKey recipeId) {
        if (this.advancement.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + recipeId);
        }
    }

    public static class Result implements FinishedRecipe {
        private final MinecraftKey id;
        private final String group;
        private final RecipeItemStack ingredient;
        private final Item result;
        private final int count;
        private final Advancement.SerializedAdvancement advancement;
        private final MinecraftKey advancementId;
        private final RecipeSerializer<?> type;

        public Result(MinecraftKey recipeId, RecipeSerializer<?> serializer, String group, RecipeItemStack input, Item output, int outputCount, Advancement.SerializedAdvancement builder, MinecraftKey advancementId) {
            this.id = recipeId;
            this.type = serializer;
            this.group = group;
            this.ingredient = input;
            this.result = output;
            this.count = outputCount;
            this.advancement = builder;
            this.advancementId = advancementId;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            if (!this.group.isEmpty()) {
                json.addProperty("group", this.group);
            }

            json.add("ingredient", this.ingredient.toJson());
            json.addProperty("result", IRegistry.ITEM.getKey(this.result).toString());
            json.addProperty("count", this.count);
        }

        @Override
        public MinecraftKey getId() {
            return this.id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return this.type;
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
