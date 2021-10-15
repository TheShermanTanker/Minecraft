package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.crafting.RecipeSerializer;

public interface FinishedRecipe {
    void serializeRecipeData(JsonObject json);

    default JsonObject serializeRecipe() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", IRegistry.RECIPE_SERIALIZER.getKey(this.getType()).toString());
        this.serializeRecipeData(jsonObject);
        return jsonObject;
    }

    MinecraftKey getId();

    RecipeSerializer<?> getType();

    @Nullable
    JsonObject serializeAdvancement();

    @Nullable
    MinecraftKey getAdvancementId();
}
