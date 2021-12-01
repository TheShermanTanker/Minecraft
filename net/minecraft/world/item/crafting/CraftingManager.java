package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.server.packs.resources.ResourceDataJson;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CraftingManager extends ResourceDataJson {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogManager.getLogger();
    public Map<Recipes<?>, Map<MinecraftKey, IRecipe<?>>> recipes = ImmutableMap.of();
    public Map<MinecraftKey, IRecipe<?>> byName = ImmutableMap.of();
    private boolean hasErrors;

    public CraftingManager() {
        super(GSON, "recipes");
    }

    @Override
    protected void apply(Map<MinecraftKey, JsonElement> prepared, IResourceManager manager, GameProfilerFiller profiler) {
        this.hasErrors = false;
        Map<Recipes<?>, Builder<MinecraftKey, IRecipe<?>>> map = Maps.newHashMap();
        Builder<MinecraftKey, IRecipe<?>> builder = ImmutableMap.builder();

        for(Entry<MinecraftKey, JsonElement> entry : prepared.entrySet()) {
            MinecraftKey resourceLocation = entry.getKey();

            try {
                IRecipe<?> recipe = fromJson(resourceLocation, ChatDeserializer.convertToJsonObject(entry.getValue(), "top element"));
                map.computeIfAbsent(recipe.getType(), (recipeType) -> {
                    return ImmutableMap.builder();
                }).put(resourceLocation, recipe);
                builder.put(resourceLocation, recipe);
            } catch (IllegalArgumentException | JsonParseException var10) {
                LOGGER.error("Parsing error loading recipe {}", resourceLocation, var10);
            }
        }

        this.recipes = map.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (entryx) -> {
            return entryx.getValue().build();
        }));
        this.byName = builder.build();
        LOGGER.info("Loaded {} recipes", (int)map.size());
    }

    public boolean hadErrorsLoading() {
        return this.hasErrors;
    }

    public <C extends IInventory, T extends IRecipe<C>> Optional<T> craft(Recipes<T> type, C inventory, World world) {
        return this.byType(type).values().stream().flatMap((recipe) -> {
            return SystemUtils.toStream(type.tryMatch(recipe, world, inventory));
        }).findFirst();
    }

    public <C extends IInventory, T extends IRecipe<C>> List<T> getAllRecipesFor(Recipes<T> type) {
        return this.byType(type).values().stream().map((recipe) -> {
            return recipe;
        }).collect(Collectors.toList());
    }

    public <C extends IInventory, T extends IRecipe<C>> List<T> getRecipesFor(Recipes<T> type, C inventory, World world) {
        return this.byType(type).values().stream().flatMap((recipe) -> {
            return SystemUtils.toStream(type.tryMatch(recipe, world, inventory));
        }).sorted(Comparator.comparing((recipe) -> {
            return recipe.getResult().getDescriptionId();
        })).collect(Collectors.toList());
    }

    private <C extends IInventory, T extends IRecipe<C>> Map<MinecraftKey, IRecipe<C>> byType(Recipes<T> type) {
        return this.recipes.getOrDefault(type, Collections.emptyMap());
    }

    public <C extends IInventory, T extends IRecipe<C>> NonNullList<ItemStack> getRemainingItemsFor(Recipes<T> type, C inventory, World world) {
        Optional<T> optional = this.craft(type, inventory, world);
        if (optional.isPresent()) {
            return optional.get().getRemainingItems(inventory);
        } else {
            NonNullList<ItemStack> nonNullList = NonNullList.withSize(inventory.getSize(), ItemStack.EMPTY);

            for(int i = 0; i < nonNullList.size(); ++i) {
                nonNullList.set(i, inventory.getItem(i));
            }

            return nonNullList;
        }
    }

    public Optional<? extends IRecipe<?>> getRecipe(MinecraftKey id) {
        return Optional.ofNullable(this.byName.get(id));
    }

    public Collection<IRecipe<?>> getRecipes() {
        return this.recipes.values().stream().flatMap((map) -> {
            return map.values().stream();
        }).collect(Collectors.toSet());
    }

    public Stream<MinecraftKey> getRecipeIds() {
        return this.recipes.values().stream().flatMap((map) -> {
            return map.keySet().stream();
        });
    }

    public static IRecipe<?> fromJson(MinecraftKey id, JsonObject json) {
        String string = ChatDeserializer.getAsString(json, "type");
        return IRegistry.RECIPE_SERIALIZER.getOptional(new MinecraftKey(string)).orElseThrow(() -> {
            return new JsonSyntaxException("Invalid or unsupported recipe type '" + string + "'");
        }).fromJson(id, json);
    }

    public void replaceRecipes(Iterable<IRecipe<?>> recipes) {
        this.hasErrors = false;
        Map<Recipes<?>, Map<MinecraftKey, IRecipe<?>>> map = Maps.newHashMap();
        Builder<MinecraftKey, IRecipe<?>> builder = ImmutableMap.builder();
        recipes.forEach((recipe) -> {
            Map<MinecraftKey, IRecipe<?>> map2 = map.computeIfAbsent(recipe.getType(), (t) -> {
                return Maps.newHashMap();
            });
            MinecraftKey resourceLocation = recipe.getKey();
            IRecipe<?> recipe2 = map2.put(resourceLocation, recipe);
            builder.put(resourceLocation, recipe);
            if (recipe2 != null) {
                throw new IllegalStateException("Duplicate recipe ignored with ID " + resourceLocation);
            }
        });
        this.recipes = ImmutableMap.copyOf(map);
        this.byName = builder.build();
    }
}
