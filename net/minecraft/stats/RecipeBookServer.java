package net.minecraft.stats;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ResourceKeyInvalidException;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.protocol.game.PacketPlayOutRecipes;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.item.crafting.IRecipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RecipeBookServer extends RecipeBook {
    public static final String RECIPE_BOOK_TAG = "recipeBook";
    private static final Logger LOGGER = LogManager.getLogger();

    public int addRecipes(Collection<IRecipe<?>> recipes, EntityPlayer player) {
        List<MinecraftKey> list = Lists.newArrayList();
        int i = 0;

        for(IRecipe<?> recipe : recipes) {
            MinecraftKey resourceLocation = recipe.getKey();
            if (!this.known.contains(resourceLocation) && !recipe.isComplex()) {
                this.add(resourceLocation);
                this.addHighlight(resourceLocation);
                list.add(resourceLocation);
                CriterionTriggers.RECIPE_UNLOCKED.trigger(player, recipe);
                ++i;
            }
        }

        this.sendRecipes(PacketPlayOutRecipes.Action.ADD, player, list);
        return i;
    }

    public int removeRecipes(Collection<IRecipe<?>> recipes, EntityPlayer player) {
        List<MinecraftKey> list = Lists.newArrayList();
        int i = 0;

        for(IRecipe<?> recipe : recipes) {
            MinecraftKey resourceLocation = recipe.getKey();
            if (this.known.contains(resourceLocation)) {
                this.remove(resourceLocation);
                list.add(resourceLocation);
                ++i;
            }
        }

        this.sendRecipes(PacketPlayOutRecipes.Action.REMOVE, player, list);
        return i;
    }

    private void sendRecipes(PacketPlayOutRecipes.Action action, EntityPlayer player, List<MinecraftKey> recipeIds) {
        player.connection.sendPacket(new PacketPlayOutRecipes(action, recipeIds, Collections.emptyList(), this.getBookSettings()));
    }

    public NBTTagCompound save() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        this.getBookSettings().write(compoundTag);
        NBTTagList listTag = new NBTTagList();

        for(MinecraftKey resourceLocation : this.known) {
            listTag.add(NBTTagString.valueOf(resourceLocation.toString()));
        }

        compoundTag.set("recipes", listTag);
        NBTTagList listTag2 = new NBTTagList();

        for(MinecraftKey resourceLocation2 : this.highlight) {
            listTag2.add(NBTTagString.valueOf(resourceLocation2.toString()));
        }

        compoundTag.set("toBeDisplayed", listTag2);
        return compoundTag;
    }

    public void fromNbt(NBTTagCompound nbt, CraftingManager recipeManager) {
        this.setBookSettings(RecipeBookSettings.read(nbt));
        NBTTagList listTag = nbt.getList("recipes", 8);
        this.loadRecipes(listTag, this::add, recipeManager);
        NBTTagList listTag2 = nbt.getList("toBeDisplayed", 8);
        this.loadRecipes(listTag2, this::addHighlight, recipeManager);
    }

    private void loadRecipes(NBTTagList list, Consumer<IRecipe<?>> handler, CraftingManager recipeManager) {
        for(int i = 0; i < list.size(); ++i) {
            String string = list.getString(i);

            try {
                MinecraftKey resourceLocation = new MinecraftKey(string);
                Optional<? extends IRecipe<?>> optional = recipeManager.getRecipe(resourceLocation);
                if (!optional.isPresent()) {
                    LOGGER.error("Tried to load unrecognized recipe: {} removed now.", (Object)resourceLocation);
                } else {
                    handler.accept(optional.get());
                }
            } catch (ResourceKeyInvalidException var8) {
                LOGGER.error("Tried to load improperly formatted recipe: {} removed now.", (Object)string);
            }
        }

    }

    public void sendInitialRecipeBook(EntityPlayer player) {
        player.connection.sendPacket(new PacketPlayOutRecipes(PacketPlayOutRecipes.Action.INIT, this.known, this.highlight, this.getBookSettings()));
    }
}
