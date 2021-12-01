package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.advancements.Advancement;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.level.storage.loot.ItemModifierManager;
import net.minecraft.world.level.storage.loot.LootPredicateManager;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ArgumentMinecraftKeyRegistered implements ArgumentType<MinecraftKey> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ADVANCEMENT = new DynamicCommandExceptionType((id) -> {
        return new ChatMessage("advancement.advancementNotFound", id);
    });
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_RECIPE = new DynamicCommandExceptionType((id) -> {
        return new ChatMessage("recipe.notFound", id);
    });
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_PREDICATE = new DynamicCommandExceptionType((id) -> {
        return new ChatMessage("predicate.unknown", id);
    });
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ATTRIBUTE = new DynamicCommandExceptionType((id) -> {
        return new ChatMessage("attribute.unknown", id);
    });
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM_MODIFIER = new DynamicCommandExceptionType((id) -> {
        return new ChatMessage("item_modifier.unknown", id);
    });

    public static ArgumentMinecraftKeyRegistered id() {
        return new ArgumentMinecraftKeyRegistered();
    }

    public static Advancement getAdvancement(CommandContext<CommandListenerWrapper> context, String argumentName) throws CommandSyntaxException {
        MinecraftKey resourceLocation = context.getArgument(argumentName, MinecraftKey.class);
        Advancement advancement = context.getSource().getServer().getAdvancementData().getAdvancement(resourceLocation);
        if (advancement == null) {
            throw ERROR_UNKNOWN_ADVANCEMENT.create(resourceLocation);
        } else {
            return advancement;
        }
    }

    public static IRecipe<?> getRecipe(CommandContext<CommandListenerWrapper> context, String argumentName) throws CommandSyntaxException {
        CraftingManager recipeManager = context.getSource().getServer().getCraftingManager();
        MinecraftKey resourceLocation = context.getArgument(argumentName, MinecraftKey.class);
        return recipeManager.getRecipe(resourceLocation).orElseThrow(() -> {
            return ERROR_UNKNOWN_RECIPE.create(resourceLocation);
        });
    }

    public static LootItemCondition getPredicate(CommandContext<CommandListenerWrapper> context, String argumentName) throws CommandSyntaxException {
        MinecraftKey resourceLocation = context.getArgument(argumentName, MinecraftKey.class);
        LootPredicateManager predicateManager = context.getSource().getServer().getLootPredicateManager();
        LootItemCondition lootItemCondition = predicateManager.get(resourceLocation);
        if (lootItemCondition == null) {
            throw ERROR_UNKNOWN_PREDICATE.create(resourceLocation);
        } else {
            return lootItemCondition;
        }
    }

    public static LootItemFunction getItemModifier(CommandContext<CommandListenerWrapper> context, String argumentName) throws CommandSyntaxException {
        MinecraftKey resourceLocation = context.getArgument(argumentName, MinecraftKey.class);
        ItemModifierManager itemModifierManager = context.getSource().getServer().getItemModifierManager();
        LootItemFunction lootItemFunction = itemModifierManager.get(resourceLocation);
        if (lootItemFunction == null) {
            throw ERROR_UNKNOWN_ITEM_MODIFIER.create(resourceLocation);
        } else {
            return lootItemFunction;
        }
    }

    public static AttributeBase getAttribute(CommandContext<CommandListenerWrapper> context, String argumentName) throws CommandSyntaxException {
        MinecraftKey resourceLocation = context.getArgument(argumentName, MinecraftKey.class);
        return IRegistry.ATTRIBUTE.getOptional(resourceLocation).orElseThrow(() -> {
            return ERROR_UNKNOWN_ATTRIBUTE.create(resourceLocation);
        });
    }

    public static MinecraftKey getId(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, MinecraftKey.class);
    }

    public MinecraftKey parse(StringReader stringReader) throws CommandSyntaxException {
        return MinecraftKey.read(stringReader);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
