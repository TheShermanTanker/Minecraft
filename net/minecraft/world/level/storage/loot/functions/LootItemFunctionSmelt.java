package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.FurnaceRecipe;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootItemFunctionSmelt extends LootItemFunctionConditional {
    private static final Logger LOGGER = LogManager.getLogger();

    LootItemFunctionSmelt(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.FURNACE_SMELT;
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        if (stack.isEmpty()) {
            return stack;
        } else {
            Optional<FurnaceRecipe> optional = context.getWorld().getCraftingManager().craft(Recipes.SMELTING, new InventorySubcontainer(stack), context.getWorld());
            if (optional.isPresent()) {
                ItemStack itemStack = optional.get().getResult();
                if (!itemStack.isEmpty()) {
                    ItemStack itemStack2 = itemStack.cloneItemStack();
                    itemStack2.setCount(stack.getCount());
                    return itemStack2;
                }
            }

            LOGGER.warn("Couldn't smelt {} because there is no smelting recipe", (Object)stack);
            return stack;
        }
    }

    public static LootItemFunctionConditional.Builder<?> smelted() {
        return simpleBuilder(LootItemFunctionSmelt::new);
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionSmelt> {
        @Override
        public LootItemFunctionSmelt deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            return new LootItemFunctionSmelt(lootItemConditions);
        }
    }
}
