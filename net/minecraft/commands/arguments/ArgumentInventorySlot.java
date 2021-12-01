package net.minecraft.commands.arguments;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.world.entity.EnumItemSlot;

public class ArgumentInventorySlot implements ArgumentType<Integer> {
    private static final Collection<String> EXAMPLES = Arrays.asList("container.5", "12", "weapon");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_SLOT = new DynamicCommandExceptionType((name) -> {
        return new ChatMessage("slot.unknown", name);
    });
    private static final Map<String, Integer> SLOTS = SystemUtils.make(Maps.newHashMap(), (map) -> {
        for(int i = 0; i < 54; ++i) {
            map.put("container." + i, i);
        }

        for(int j = 0; j < 9; ++j) {
            map.put("hotbar." + j, j);
        }

        for(int k = 0; k < 27; ++k) {
            map.put("inventory." + k, 9 + k);
        }

        for(int l = 0; l < 27; ++l) {
            map.put("enderchest." + l, 200 + l);
        }

        for(int m = 0; m < 8; ++m) {
            map.put("villager." + m, 300 + m);
        }

        for(int n = 0; n < 15; ++n) {
            map.put("horse." + n, 500 + n);
        }

        map.put("weapon", EnumItemSlot.MAINHAND.getIndex(98));
        map.put("weapon.mainhand", EnumItemSlot.MAINHAND.getIndex(98));
        map.put("weapon.offhand", EnumItemSlot.OFFHAND.getIndex(98));
        map.put("armor.head", EnumItemSlot.HEAD.getIndex(100));
        map.put("armor.chest", EnumItemSlot.CHEST.getIndex(100));
        map.put("armor.legs", EnumItemSlot.LEGS.getIndex(100));
        map.put("armor.feet", EnumItemSlot.FEET.getIndex(100));
        map.put("horse.saddle", 400);
        map.put("horse.armor", 401);
        map.put("horse.chest", 499);
    });

    public static ArgumentInventorySlot slot() {
        return new ArgumentInventorySlot();
    }

    public static int getSlot(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, Integer.class);
    }

    public Integer parse(StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.readUnquotedString();
        if (!SLOTS.containsKey(string)) {
            throw ERROR_UNKNOWN_SLOT.create(string);
        } else {
            return SLOTS.get(string);
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return ICompletionProvider.suggest(SLOTS.keySet(), suggestionsBuilder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
