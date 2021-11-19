package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializerComplex;

public class RecipeBuilderSpecial {
    final RecipeSerializerComplex<?> serializer;

    public RecipeBuilderSpecial(RecipeSerializerComplex<?> serializer) {
        this.serializer = serializer;
    }

    public static RecipeBuilderSpecial special(RecipeSerializerComplex<?> serializer) {
        return new RecipeBuilderSpecial(serializer);
    }

    public void save(Consumer<IFinishedRecipe> exporter, String recipeId) {
        exporter.accept(new IFinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject json) {
            }

            @Override
            public RecipeSerializer<?> getType() {
                return RecipeBuilderSpecial.this.serializer;
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
