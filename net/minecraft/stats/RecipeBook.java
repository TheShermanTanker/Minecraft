package net.minecraft.stats;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.ContainerRecipeBook;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.IRecipe;

public class RecipeBook {
    public final Set<MinecraftKey> known = Sets.newHashSet();
    protected final Set<MinecraftKey> highlight = Sets.newHashSet();
    private final RecipeBookSettings bookSettings = new RecipeBookSettings();

    public void copyOverData(RecipeBook book) {
        this.known.clear();
        this.highlight.clear();
        this.bookSettings.replaceFrom(book.bookSettings);
        this.known.addAll(book.known);
        this.highlight.addAll(book.highlight);
    }

    public void add(IRecipe<?> recipe) {
        if (!recipe.isComplex()) {
            this.add(recipe.getKey());
        }

    }

    protected void add(MinecraftKey id) {
        this.known.add(id);
    }

    public boolean contains(@Nullable IRecipe<?> recipe) {
        return recipe == null ? false : this.known.contains(recipe.getKey());
    }

    public boolean hasDiscoveredRecipe(MinecraftKey id) {
        return this.known.contains(id);
    }

    public void remove(IRecipe<?> recipe) {
        this.remove(recipe.getKey());
    }

    protected void remove(MinecraftKey id) {
        this.known.remove(id);
        this.highlight.remove(id);
    }

    public boolean willHighlight(IRecipe<?> recipe) {
        return this.highlight.contains(recipe.getKey());
    }

    public void removeHighlight(IRecipe<?> recipe) {
        this.highlight.remove(recipe.getKey());
    }

    public void addHighlight(IRecipe<?> recipe) {
        this.addHighlight(recipe.getKey());
    }

    protected void addHighlight(MinecraftKey id) {
        this.highlight.add(id);
    }

    public boolean isOpen(RecipeBookType category) {
        return this.bookSettings.isOpen(category);
    }

    public void setOpen(RecipeBookType category, boolean open) {
        this.bookSettings.setOpen(category, open);
    }

    public boolean isFiltering(ContainerRecipeBook<?> handler) {
        return this.isFiltering(handler.getRecipeBookType());
    }

    public boolean isFiltering(RecipeBookType category) {
        return this.bookSettings.isFiltering(category);
    }

    public void setFiltering(RecipeBookType category, boolean filteringCraftable) {
        this.bookSettings.setFiltering(category, filteringCraftable);
    }

    public void setBookSettings(RecipeBookSettings options) {
        this.bookSettings.replaceFrom(options);
    }

    public RecipeBookSettings getBookSettings() {
        return this.bookSettings.copy();
    }

    public void setBookSetting(RecipeBookType category, boolean guiOpen, boolean filteringCraftable) {
        this.bookSettings.setOpen(category, guiOpen);
        this.bookSettings.setFiltering(category, filteringCraftable);
    }
}
