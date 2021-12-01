package net.minecraft.world.level.storage.loot.functions;

import java.util.function.BiFunction;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.JsonRegistry;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class LootItemFunctions {
    public static final BiFunction<ItemStack, LootTableInfo, ItemStack> IDENTITY = (stack, context) -> {
        return stack;
    };
    public static final LootItemFunctionType SET_COUNT = register("set_count", new LootItemFunctionSetCount.Serializer());
    public static final LootItemFunctionType ENCHANT_WITH_LEVELS = register("enchant_with_levels", new LootEnchantLevel.Serializer());
    public static final LootItemFunctionType ENCHANT_RANDOMLY = register("enchant_randomly", new LootItemFunctionEnchant.Serializer());
    public static final LootItemFunctionType SET_ENCHANTMENTS = register("set_enchantments", new SetEnchantmentsFunction.Serializer());
    public static final LootItemFunctionType SET_NBT = register("set_nbt", new LootItemFunctionSetTag.Serializer());
    public static final LootItemFunctionType FURNACE_SMELT = register("furnace_smelt", new LootItemFunctionSmelt.Serializer());
    public static final LootItemFunctionType LOOTING_ENCHANT = register("looting_enchant", new LootEnchantFunction.Serializer());
    public static final LootItemFunctionType SET_DAMAGE = register("set_damage", new LootItemFunctionSetDamage.Serializer());
    public static final LootItemFunctionType SET_ATTRIBUTES = register("set_attributes", new LootItemFunctionSetAttribute.Serializer());
    public static final LootItemFunctionType SET_NAME = register("set_name", new LootItemFunctionSetName.Serializer());
    public static final LootItemFunctionType EXPLORATION_MAP = register("exploration_map", new LootItemFunctionExplorationMap.Serializer());
    public static final LootItemFunctionType SET_STEW_EFFECT = register("set_stew_effect", new LootItemFunctionSetStewEffect.Serializer());
    public static final LootItemFunctionType COPY_NAME = register("copy_name", new LootItemFunctionCopyName.Serializer());
    public static final LootItemFunctionType SET_CONTENTS = register("set_contents", new LootItemFunctionSetContents.Serializer());
    public static final LootItemFunctionType LIMIT_COUNT = register("limit_count", new LootItemFunctionLimitCount.Serializer());
    public static final LootItemFunctionType APPLY_BONUS = register("apply_bonus", new LootItemFunctionApplyBonus.Serializer());
    public static final LootItemFunctionType SET_LOOT_TABLE = register("set_loot_table", new LootItemFunctionSetTable.Serializer());
    public static final LootItemFunctionType EXPLOSION_DECAY = register("explosion_decay", new LootItemFunctionExplosionDecay.Serializer());
    public static final LootItemFunctionType SET_LORE = register("set_lore", new LootItemFunctionSetLore.Serializer());
    public static final LootItemFunctionType FILL_PLAYER_HEAD = register("fill_player_head", new LootItemFunctionFillPlayerHead.Serializer());
    public static final LootItemFunctionType COPY_NBT = register("copy_nbt", new LootItemFunctionCopyNBT.Serializer());
    public static final LootItemFunctionType COPY_STATE = register("copy_state", new LootItemFunctionCopyState.Serializer());
    public static final LootItemFunctionType SET_BANNER_PATTERN = register("set_banner_pattern", new SetBannerPatternFunction.Serializer());
    public static final LootItemFunctionType SET_POTION = register("set_potion", new SetPotionFunction.Serializer());

    private static LootItemFunctionType register(String id, LootSerializer<? extends LootItemFunction> jsonSerializer) {
        return IRegistry.register(IRegistry.LOOT_FUNCTION_TYPE, new MinecraftKey(id), new LootItemFunctionType(jsonSerializer));
    }

    public static Object createGsonAdapter() {
        return JsonRegistry.builder(IRegistry.LOOT_FUNCTION_TYPE, "function", "function", LootItemFunction::getType).build();
    }

    public static BiFunction<ItemStack, LootTableInfo, ItemStack> compose(BiFunction<ItemStack, LootTableInfo, ItemStack>[] lootFunctions) {
        switch(lootFunctions.length) {
        case 0:
            return IDENTITY;
        case 1:
            return lootFunctions[0];
        case 2:
            BiFunction<ItemStack, LootTableInfo, ItemStack> biFunction = lootFunctions[0];
            BiFunction<ItemStack, LootTableInfo, ItemStack> biFunction2 = lootFunctions[1];
            return (stack, context) -> {
                return biFunction2.apply(biFunction.apply(stack, context), context);
            };
        default:
            return (stack, context) -> {
                for(BiFunction<ItemStack, LootTableInfo, ItemStack> biFunction : lootFunctions) {
                    stack = biFunction.apply(stack, context);
                }

                return stack;
            };
        }
    }
}
