package net.minecraft.world.item.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;

public class RecipeSerializerCooking<T extends RecipeCooking> implements RecipeSerializer<T> {
    private final int defaultCookingTime;
    private final RecipeSerializerCooking.CookieBaker<T> factory;

    public RecipeSerializerCooking(RecipeSerializerCooking.CookieBaker<T> recipeFactory, int cookingTime) {
        this.defaultCookingTime = cookingTime;
        this.factory = recipeFactory;
    }

    @Override
    public T fromJson(MinecraftKey resourceLocation, JsonObject jsonObject) {
        String string = ChatDeserializer.getAsString(jsonObject, "group", "");
        JsonElement jsonElement = (JsonElement)(ChatDeserializer.isArrayNode(jsonObject, "ingredient") ? ChatDeserializer.getAsJsonArray(jsonObject, "ingredient") : ChatDeserializer.getAsJsonObject(jsonObject, "ingredient"));
        RecipeItemStack ingredient = RecipeItemStack.fromJson(jsonElement);
        String string2 = ChatDeserializer.getAsString(jsonObject, "result");
        MinecraftKey resourceLocation2 = new MinecraftKey(string2);
        ItemStack itemStack = new ItemStack(IRegistry.ITEM.getOptional(resourceLocation2).orElseThrow(() -> {
            return new IllegalStateException("Item: " + string2 + " does not exist");
        }));
        float f = ChatDeserializer.getAsFloat(jsonObject, "experience", 0.0F);
        int i = ChatDeserializer.getAsInt(jsonObject, "cookingtime", this.defaultCookingTime);
        return this.factory.create(resourceLocation, string, ingredient, itemStack, f, i);
    }

    @Override
    public T fromNetwork(MinecraftKey resourceLocation, PacketDataSerializer friendlyByteBuf) {
        String string = friendlyByteBuf.readUtf();
        RecipeItemStack ingredient = RecipeItemStack.fromNetwork(friendlyByteBuf);
        ItemStack itemStack = friendlyByteBuf.readItem();
        float f = friendlyByteBuf.readFloat();
        int i = friendlyByteBuf.readVarInt();
        return this.factory.create(resourceLocation, string, ingredient, itemStack, f, i);
    }

    @Override
    public void toNetwork(PacketDataSerializer buf, T recipe) {
        buf.writeUtf(recipe.group);
        recipe.ingredient.toNetwork(buf);
        buf.writeItem(recipe.result);
        buf.writeFloat(recipe.experience);
        buf.writeVarInt(recipe.cookingTime);
    }

    interface CookieBaker<T extends RecipeCooking> {
        T create(MinecraftKey id, String group, RecipeItemStack input, ItemStack output, float experience, int cookTime);
    }
}
