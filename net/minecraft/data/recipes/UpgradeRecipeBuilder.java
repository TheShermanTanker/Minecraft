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

public class UpgradeRecipeBuilder {
    private final RecipeItemStack base;
    private final RecipeItemStack addition;
    private final Item result;
    private final Advancement.SerializedAdvancement advancement = Advancement.SerializedAdvancement.advancement();
    private final RecipeSerializer<?> type;

    public UpgradeRecipeBuilder(RecipeSerializer<?> serializer, RecipeItemStack base, RecipeItemStack addition, Item result) {
        this.type = serializer;
        this.base = base;
        this.addition = addition;
        this.result = result;
    }

    public static UpgradeRecipeBuilder smithing(RecipeItemStack base, RecipeItemStack addition, Item result) {
        return new UpgradeRecipeBuilder(RecipeSerializer.SMITHING, base, addition, result);
    }

    public UpgradeRecipeBuilder unlocks(String criterionName, CriterionInstance conditions) {
        this.advancement.addCriterion(criterionName, conditions);
        return this;
    }

    public void save(Consumer<FinishedRecipe> exporter, String recipeId) {
        this.save(exporter, new MinecraftKey(recipeId));
    }

    public void save(Consumer<FinishedRecipe> exporter, MinecraftKey recipeId) {
        this.ensureValid(recipeId);
        this.advancement.parent(new MinecraftKey("recipes/root")).addCriterion("has_the_recipe", CriterionTriggerRecipeUnlocked.unlocked(recipeId)).rewards(AdvancementRewards.Builder.recipe(recipeId)).requirements(AdvancementRequirements.OR);
        exporter.accept(new UpgradeRecipeBuilder.Result(recipeId, this.type, this.base, this.addition, this.result, this.advancement, new MinecraftKey(recipeId.getNamespace(), "recipes/" + this.result.getItemCategory().getRecipeFolderName() + "/" + recipeId.getKey())));
    }

    private void ensureValid(MinecraftKey recipeId) {
        if (this.advancement.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + recipeId);
        }
    }

    public static class Result implements FinishedRecipe {
        private final MinecraftKey id;
        private final RecipeItemStack base;
        private final RecipeItemStack addition;
        private final Item result;
        private final Advancement.SerializedAdvancement advancement;
        private final MinecraftKey advancementId;
        private final RecipeSerializer<?> type;

        public Result(MinecraftKey recipeId, RecipeSerializer<?> serializer, RecipeItemStack base, RecipeItemStack addition, Item result, Advancement.SerializedAdvancement builder, MinecraftKey advancementId) {
            this.id = recipeId;
            this.type = serializer;
            this.base = base;
            this.addition = addition;
            this.result = result;
            this.advancement = builder;
            this.advancementId = advancementId;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            json.add("base", this.base.toJson());
            json.add("addition", this.addition.toJson());
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("item", IRegistry.ITEM.getKey(this.result).toString());
            json.add("result", jsonObject);
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
