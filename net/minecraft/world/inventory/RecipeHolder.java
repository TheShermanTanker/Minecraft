package net.minecraft.world.inventory;

import java.util.Collections;
import javax.annotation.Nullable;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;

public interface RecipeHolder {
    void setRecipeUsed(@Nullable IRecipe<?> recipe);

    @Nullable
    IRecipe<?> getRecipeUsed();

    default void awardUsedRecipes(EntityHuman player) {
        IRecipe<?> recipe = this.getRecipeUsed();
        if (recipe != null && !recipe.isComplex()) {
            player.discoverRecipes(Collections.singleton(recipe));
            this.setRecipeUsed((IRecipe<?>)null);
        }

    }

    default boolean setRecipeUsed(World world, EntityPlayer player, IRecipe<?> recipe) {
        if (!recipe.isComplex() && world.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING) && !player.getRecipeBook().contains(recipe)) {
            return false;
        } else {
            this.setRecipeUsed(recipe);
            return true;
        }
    }
}
