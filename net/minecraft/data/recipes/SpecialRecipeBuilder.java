package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializerComplex;

public class SpecialRecipeBuilder {
    final RecipeSerializerComplex<?> serializer;

    public SpecialRecipeBuilder(RecipeSerializerComplex<?> serializer) {
        this.serializer = serializer;
    }

    public static SpecialRecipeBuilder special(RecipeSerializerComplex<?> serializer) {
        return new SpecialRecipeBuilder(serializer);
    }

    public void save(Consumer<FinishedRecipe> exporter, String recipeId) {
        exporter.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject json) {
            }

            @Override
            public RecipeSerializer<?> getType() {
                return SpecialRecipeBuilder.this.serializer;
            }

            @Override
            public MinecraftKey getId() {
                return new MinecraftKey(recipeId);
            }

            @Nullable
            @Override
            public JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public MinecraftKey getAdvancementId() {
                return new MinecraftKey("");
            }
        });
    }
}
