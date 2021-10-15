package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.IRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class ShapedRecipes implements RecipeCrafting {
    final int width;
    final int height;
    final NonNullList<RecipeItemStack> recipeItems;
    final ItemStack result;
    private final MinecraftKey id;
    final String group;

    public ShapedRecipes(MinecraftKey id, String group, int width, int height, NonNullList<RecipeItemStack> input, ItemStack output) {
        this.id = id;
        this.group = group;
        this.width = width;
        this.height = height;
        this.recipeItems = input;
        this.result = output;
    }

    @Override
    public MinecraftKey getKey() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.SHAPED_RECIPE;
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
        return this.recipeItems;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.width && height >= this.height;
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        for(int i = 0; i <= inventory.getWidth() - this.width; ++i) {
            for(int j = 0; j <= inventory.getHeight() - this.height; ++j) {
                if (this.matches(inventory, i, j, true)) {
                    return true;
                }

                if (this.matches(inventory, i, j, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean matches(InventoryCrafting inv, int offsetX, int offsetY, boolean flipped) {
        for(int i = 0; i < inv.getWidth(); ++i) {
            for(int j = 0; j < inv.getHeight(); ++j) {
                int k = i - offsetX;
                int l = j - offsetY;
                RecipeItemStack ingredient = RecipeItemStack.EMPTY;
                if (k >= 0 && l >= 0 && k < this.width && l < this.height) {
                    if (flipped) {
                        ingredient = this.recipeItems.get(this.width - k - 1 + l * this.width);
                    } else {
                        ingredient = this.recipeItems.get(k + l * this.width);
                    }
                }

                if (!ingredient.test(inv.getItem(i + j * inv.getWidth()))) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        return this.getResult().cloneItemStack();
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    static NonNullList<RecipeItemStack> dissolvePattern(String[] pattern, Map<String, RecipeItemStack> symbols, int width, int height) {
        NonNullList<RecipeItemStack> nonNullList = NonNullList.withSize(width * height, RecipeItemStack.EMPTY);
        Set<String> set = Sets.newHashSet(symbols.keySet());
        set.remove(" ");

        for(int i = 0; i < pattern.length; ++i) {
            for(int j = 0; j < pattern[i].length(); ++j) {
                String string = pattern[i].substring(j, j + 1);
                RecipeItemStack ingredient = symbols.get(string);
                if (ingredient == null) {
                    throw new JsonSyntaxException("Pattern references symbol '" + string + "' but it's not defined in the key");
                }

                set.remove(string);
                nonNullList.set(j + width * i, ingredient);
            }
        }

        if (!set.isEmpty()) {
            throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + set);
        } else {
            return nonNullList;
        }
    }

    @VisibleForTesting
    static String[] shrink(String... pattern) {
        int i = Integer.MAX_VALUE;
        int j = 0;
        int k = 0;
        int l = 0;

        for(int m = 0; m < pattern.length; ++m) {
            String string = pattern[m];
            i = Math.min(i, firstNonSpace(string));
            int n = lastNonSpace(string);
            j = Math.max(j, n);
            if (n < 0) {
                if (k == m) {
                    ++k;
                }

                ++l;
            } else {
                l = 0;
            }
        }

        if (pattern.length == l) {
            return new String[0];
        } else {
            String[] strings = new String[pattern.length - l - k];

            for(int o = 0; o < strings.length; ++o) {
                strings[o] = pattern[o + k].substring(i, j + 1);
            }

            return strings;
        }
    }

    @Override
    public boolean isIncomplete() {
        NonNullList<RecipeItemStack> nonNullList = this.getIngredients();
        return nonNullList.isEmpty() || nonNullList.stream().filter((ingredient) -> {
            return !ingredient.isEmpty();
        }).anyMatch((ingredient) -> {
            return ingredient.getItems().length == 0;
        });
    }

    private static int firstNonSpace(String line) {
        int i;
        for(i = 0; i < line.length() && line.charAt(i) == ' '; ++i) {
        }

        return i;
    }

    private static int lastNonSpace(String pattern) {
        int i;
        for(i = pattern.length() - 1; i >= 0 && pattern.charAt(i) == ' '; --i) {
        }

        return i;
    }

    static String[] patternFromJson(JsonArray json) {
        String[] strings = new String[json.size()];
        if (strings.length > 3) {
            throw new JsonSyntaxException("Invalid pattern: too many rows, 3 is maximum");
        } else if (strings.length == 0) {
            throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
        } else {
            for(int i = 0; i < strings.length; ++i) {
                String string = ChatDeserializer.convertToString(json.get(i), "pattern[" + i + "]");
                if (string.length() > 3) {
                    throw new JsonSyntaxException("Invalid pattern: too many columns, 3 is maximum");
                }

                if (i > 0 && strings[0].length() != string.length()) {
                    throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
                }

                strings[i] = string;
            }

            return strings;
        }
    }

    static Map<String, RecipeItemStack> keyFromJson(JsonObject json) {
        Map<String, RecipeItemStack> map = Maps.newHashMap();

        for(Entry<String, JsonElement> entry : json.entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonSyntaxException("Invalid key entry: '" + (String)entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            }

            if (" ".equals(entry.getKey())) {
                throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            }

            map.put(entry.getKey(), RecipeItemStack.fromJson(entry.getValue()));
        }

        map.put(" ", RecipeItemStack.EMPTY);
        return map;
    }

    public static ItemStack itemStackFromJson(JsonObject json) {
        Item item = itemFromJson(json);
        if (json.has("data")) {
            throw new JsonParseException("Disallowed data tag found");
        } else {
            int i = ChatDeserializer.getAsInt(json, "count", 1);
            if (i < 1) {
                throw new JsonSyntaxException("Invalid output count: " + i);
            } else {
                return new ItemStack(item, i);
            }
        }
    }

    public static Item itemFromJson(JsonObject json) {
        String string = ChatDeserializer.getAsString(json, "item");
        Item item = IRegistry.ITEM.getOptional(new MinecraftKey(string)).orElseThrow(() -> {
            return new JsonSyntaxException("Unknown item '" + string + "'");
        });
        if (item == Items.AIR) {
            throw new JsonSyntaxException("Invalid item: " + string);
        } else {
            return item;
        }
    }

    public static class Serializer implements RecipeSerializer<ShapedRecipes> {
        @Override
        public ShapedRecipes fromJson(MinecraftKey resourceLocation, JsonObject jsonObject) {
            String string = ChatDeserializer.getAsString(jsonObject, "group", "");
            Map<String, RecipeItemStack> map = ShapedRecipes.keyFromJson(ChatDeserializer.getAsJsonObject(jsonObject, "key"));
            String[] strings = ShapedRecipes.shrink(ShapedRecipes.patternFromJson(ChatDeserializer.getAsJsonArray(jsonObject, "pattern")));
            int i = strings[0].length();
            int j = strings.length;
            NonNullList<RecipeItemStack> nonNullList = ShapedRecipes.dissolvePattern(strings, map, i, j);
            ItemStack itemStack = ShapedRecipes.itemStackFromJson(ChatDeserializer.getAsJsonObject(jsonObject, "result"));
            return new ShapedRecipes(resourceLocation, string, i, j, nonNullList, itemStack);
        }

        @Override
        public ShapedRecipes fromNetwork(MinecraftKey resourceLocation, PacketDataSerializer friendlyByteBuf) {
            int i = friendlyByteBuf.readVarInt();
            int j = friendlyByteBuf.readVarInt();
            String string = friendlyByteBuf.readUtf();
            NonNullList<RecipeItemStack> nonNullList = NonNullList.withSize(i * j, RecipeItemStack.EMPTY);

            for(int k = 0; k < nonNullList.size(); ++k) {
                nonNullList.set(k, RecipeItemStack.fromNetwork(friendlyByteBuf));
            }

            ItemStack itemStack = friendlyByteBuf.readItem();
            return new ShapedRecipes(resourceLocation, string, i, j, nonNullList, itemStack);
        }

        @Override
        public void toNetwork(PacketDataSerializer buf, ShapedRecipes recipe) {
            buf.writeVarInt(recipe.width);
            buf.writeVarInt(recipe.height);
            buf.writeUtf(recipe.group);

            for(RecipeItemStack ingredient : recipe.recipeItems) {
                ingredient.toNetwork(buf);
            }

            buf.writeItem(recipe.result);
        }
    }
}
