package net.minecraft.world.item.crafting;

import com.google.gson.JsonObject;
import net.minecraft.core.IRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;

public abstract class RecipeSingleItem implements IRecipe<IInventory> {
    protected final RecipeItemStack ingredient;
    protected final ItemStack result;
    private final Recipes<?> type;
    private final RecipeSerializer<?> serializer;
    protected final MinecraftKey id;
    protected final String group;

    public RecipeSingleItem(Recipes<?> type, RecipeSerializer<?> serializer, MinecraftKey id, String group, RecipeItemStack input, ItemStack output) {
        this.type = type;
        this.serializer = serializer;
        this.id = id;
        this.group = group;
        this.ingredient = input;
        this.result = output;
    }

    @Override
    public Recipes<?> getType() {
        return this.type;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return this.serializer;
    }

    @Override
    public MinecraftKey getKey() {
        return this.id;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public ItemStack getResult() {
        return this.result;
    }

    @Override
    public NonNullList<RecipeItemStack> getIngredients() {
        NonNullList<RecipeItemStack> nonNullList = NonNullList.create();
        nonNullList.add(this.ingredient);
        return nonNullList;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack assemble(IInventory inventory) {
        return this.result.cloneItemStack();
    }

    public static class Serializer<T extends RecipeSingleItem> implements RecipeSerializer<T> {
        final RecipeSingleItem.Serializer.SingleItemMaker<T> factory;

        protected Serializer(RecipeSingleItem.Serializer.SingleItemMaker<T> recipeFactory) {
            this.factory = recipeFactory;
        }

        @Override
        public T fromJson(MinecraftKey resourceLocation, JsonObject jsonObject) {
            String string = ChatDeserializer.getAsString(jsonObject, "group", "");
            RecipeItemStack ingredient;
            if (ChatDeserializer.isArrayNode(jsonObject, "ingredient")) {
                ingredient = RecipeItemStack.fromJson(ChatDeserializer.getAsJsonArray(jsonObject, "ingredient"));
            } else {
                ingredient = RecipeItemStack.fromJson(ChatDeserializer.getAsJsonObject(jsonObject, "ingredient"));
            }

            String string2 = ChatDeserializer.getAsString(jsonObject, "result");
            int i = ChatDeserializer.getAsInt(jsonObject, "count");
            ItemStack itemStack = new ItemStack(IRegistry.ITEM.get(new MinecraftKey(string2)), i);
            return this.factory.create(resourceLocation, string, ingredient, itemStack);
        }

        @Override
        public T fromNetwork(MinecraftKey resourceLocation, PacketDataSerializer friendlyByteBuf) {
            String string = friendlyByteBuf.readUtf();
            RecipeItemStack ingredient = RecipeItemStack.fromNetwork(friendlyByteBuf);
            ItemStack itemStack = friendlyByteBuf.readItem();
            return this.factory.create(resourceLocation, string, ingredient, itemStack);
        }

        @Override
        public void toNetwork(PacketDataSerializer buf, T recipe) {
            buf.writeUtf(recipe.group);
            recipe.ingredient.toNetwork(buf);
            buf.writeItem(recipe.result);
        }

        interface SingleItemMaker<T extends RecipeSingleItem> {
            T create(MinecraftKey id, String group, RecipeItemStack input, ItemStack output);
        }
    }
}
