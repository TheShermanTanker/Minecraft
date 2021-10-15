package net.minecraft.world.item.crafting;

import com.google.gson.JsonObject;
import java.util.stream.Stream;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;

public class RecipeSmithing implements IRecipe<IInventory> {
    final RecipeItemStack base;
    final RecipeItemStack addition;
    final ItemStack result;
    private final MinecraftKey id;

    public RecipeSmithing(MinecraftKey id, RecipeItemStack base, RecipeItemStack addition, ItemStack result) {
        this.id = id;
        this.base = base;
        this.addition = addition;
        this.result = result;
    }

    @Override
    public boolean matches(IInventory inventory, World world) {
        return this.base.test(inventory.getItem(0)) && this.addition.test(inventory.getItem(1));
    }

    @Override
    public ItemStack assemble(IInventory inventory) {
        ItemStack itemStack = this.result.cloneItemStack();
        NBTTagCompound compoundTag = inventory.getItem(0).getTag();
        if (compoundTag != null) {
            itemStack.setTag(compoundTag.c());
        }

        return itemStack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResult() {
        return this.result;
    }

    public boolean isAdditionIngredient(ItemStack stack) {
        return this.addition.test(stack);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.SMITHING_TABLE);
    }

    @Override
    public MinecraftKey getKey() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.SMITHING;
    }

    @Override
    public Recipes<?> getType() {
        return Recipes.SMITHING;
    }

    @Override
    public boolean isIncomplete() {
        return Stream.of(this.base, this.addition).anyMatch((ingredient) -> {
            return ingredient.getItems().length == 0;
        });
    }

    public static class Serializer implements RecipeSerializer<RecipeSmithing> {
        @Override
        public RecipeSmithing fromJson(MinecraftKey resourceLocation, JsonObject jsonObject) {
            RecipeItemStack ingredient = RecipeItemStack.fromJson(ChatDeserializer.getAsJsonObject(jsonObject, "base"));
            RecipeItemStack ingredient2 = RecipeItemStack.fromJson(ChatDeserializer.getAsJsonObject(jsonObject, "addition"));
            ItemStack itemStack = ShapedRecipes.itemStackFromJson(ChatDeserializer.getAsJsonObject(jsonObject, "result"));
            return new RecipeSmithing(resourceLocation, ingredient, ingredient2, itemStack);
        }

        @Override
        public RecipeSmithing fromNetwork(MinecraftKey resourceLocation, PacketDataSerializer friendlyByteBuf) {
            RecipeItemStack ingredient = RecipeItemStack.fromNetwork(friendlyByteBuf);
            RecipeItemStack ingredient2 = RecipeItemStack.fromNetwork(friendlyByteBuf);
            ItemStack itemStack = friendlyByteBuf.readItem();
            return new RecipeSmithing(resourceLocation, ingredient, ingredient2, itemStack);
        }

        @Override
        public void toNetwork(PacketDataSerializer buf, RecipeSmithing recipe) {
            recipe.base.toNetwork(buf);
            recipe.addition.toNetwork(buf);
            buf.writeItem(recipe.result);
        }
    }
}
