package net.minecraft.world.item.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.NonNullList;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.player.AutoRecipeStackManager;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;

public class ShapelessRecipes implements RecipeCrafting {
    private final MinecraftKey id;
    final String group;
    final ItemStack result;
    final NonNullList<RecipeItemStack> ingredients;

    public ShapelessRecipes(MinecraftKey id, String group, ItemStack output, NonNullList<RecipeItemStack> input) {
        this.id = id;
        this.group = group;
        this.result = output;
        this.ingredients = input;
    }

    @Override
    public MinecraftKey getKey() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.SHAPELESS_RECIPE;
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
        return this.ingredients;
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        AutoRecipeStackManager stackedContents = new AutoRecipeStackManager();
        int i = 0;

        for(int j = 0; j < inventory.getSize(); ++j) {
            ItemStack itemStack = inventory.getItem(j);
            if (!itemStack.isEmpty()) {
                ++i;
                stackedContents.accountStack(itemStack, 1);
            }
        }

        return i == this.ingredients.size() && stackedContents.canCraft(this, (IntList)null);
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        return this.result.cloneItemStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.ingredients.size();
    }

    public static class Serializer implements RecipeSerializer<ShapelessRecipes> {
        @Override
        public ShapelessRecipes fromJson(MinecraftKey resourceLocation, JsonObject jsonObject) {
            String string = ChatDeserializer.getAsString(jsonObject, "group", "");
            NonNullList<RecipeItemStack> nonNullList = itemsFromJson(ChatDeserializer.getAsJsonArray(jsonObject, "ingredients"));
            if (nonNullList.isEmpty()) {
                throw new JsonParseException("No ingredients for shapeless recipe");
            } else if (nonNullList.size() > 9) {
                throw new JsonParseException("Too many ingredients for shapeless recipe");
            } else {
                ItemStack itemStack = ShapedRecipes.itemStackFromJson(ChatDeserializer.getAsJsonObject(jsonObject, "result"));
                return new ShapelessRecipes(resourceLocation, string, itemStack, nonNullList);
            }
        }

        private static NonNullList<RecipeItemStack> itemsFromJson(JsonArray json) {
            NonNullList<RecipeItemStack> nonNullList = NonNullList.create();

            for(int i = 0; i < json.size(); ++i) {
                RecipeItemStack ingredient = RecipeItemStack.fromJson(json.get(i));
                if (!ingredient.isEmpty()) {
                    nonNullList.add(ingredient);
                }
            }

            return nonNullList;
        }

        @Override
        public ShapelessRecipes fromNetwork(MinecraftKey resourceLocation, PacketDataSerializer friendlyByteBuf) {
            String string = friendlyByteBuf.readUtf();
            int i = friendlyByteBuf.readVarInt();
            NonNullList<RecipeItemStack> nonNullList = NonNullList.withSize(i, RecipeItemStack.EMPTY);

            for(int j = 0; j < nonNullList.size(); ++j) {
                nonNullList.set(j, RecipeItemStack.fromNetwork(friendlyByteBuf));
            }

            ItemStack itemStack = friendlyByteBuf.readItem();
            return new ShapelessRecipes(resourceLocation, string, itemStack, nonNullList);
        }

        @Override
        public void toNetwork(PacketDataSerializer buf, ShapelessRecipes recipe) {
            buf.writeUtf(recipe.group);
            buf.writeVarInt(recipe.ingredients.size());

            for(RecipeItemStack ingredient : recipe.ingredients) {
                ingredient.toNetwork(buf);
            }

            buf.writeItem(recipe.result);
        }
    }
}
