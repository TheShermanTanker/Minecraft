package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.crafting.IRecipe;

public class CriterionTriggerRecipeUnlocked extends CriterionTriggerAbstract<CriterionTriggerRecipeUnlocked.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("recipe_unlocked");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerRecipeUnlocked.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "recipe"));
        return new CriterionTriggerRecipeUnlocked.TriggerInstance(composite, resourceLocation);
    }

    public void trigger(EntityPlayer player, IRecipe<?> recipe) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(recipe);
        });
    }

    public static CriterionTriggerRecipeUnlocked.TriggerInstance unlocked(MinecraftKey id) {
        return new CriterionTriggerRecipeUnlocked.TriggerInstance(CriterionConditionEntity.Composite.ANY, id);
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final MinecraftKey recipe;

        public TriggerInstance(CriterionConditionEntity.Composite player, MinecraftKey recipe) {
            super(CriterionTriggerRecipeUnlocked.ID, player);
            this.recipe = recipe;
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.addProperty("recipe", this.recipe.toString());
            return jsonObject;
        }

        public boolean matches(IRecipe<?> recipe) {
            return this.recipe.equals(recipe.getKey());
        }
    }
}
