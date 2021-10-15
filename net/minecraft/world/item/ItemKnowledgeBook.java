package net.minecraft.world.item;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.level.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ItemKnowledgeBook extends Item {
    private static final String RECIPE_TAG = "Recipes";
    private static final Logger LOGGER = LogManager.getLogger();

    public ItemKnowledgeBook(Item.Info settings) {
        super(settings);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        NBTTagCompound compoundTag = itemStack.getTag();
        if (!user.getAbilities().instabuild) {
            user.setItemInHand(hand, ItemStack.EMPTY);
        }

        if (compoundTag != null && compoundTag.hasKeyOfType("Recipes", 9)) {
            if (!world.isClientSide) {
                NBTTagList listTag = compoundTag.getList("Recipes", 8);
                List<IRecipe<?>> list = Lists.newArrayList();
                CraftingManager recipeManager = world.getMinecraftServer().getCraftingManager();

                for(int i = 0; i < listTag.size(); ++i) {
                    String string = listTag.getString(i);
                    Optional<? extends IRecipe<?>> optional = recipeManager.getRecipe(new MinecraftKey(string));
                    if (!optional.isPresent()) {
                        LOGGER.error("Invalid recipe: {}", (Object)string);
                        return InteractionResultWrapper.fail(itemStack);
                    }

                    list.add(optional.get());
                }

                user.discoverRecipes(list);
                user.awardStat(StatisticList.ITEM_USED.get(this));
            }

            return InteractionResultWrapper.sidedSuccess(itemStack, world.isClientSide());
        } else {
            LOGGER.error("Tag not valid: {}", (Object)compoundTag);
            return InteractionResultWrapper.fail(itemStack);
        }
    }
}
