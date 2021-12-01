package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ArgumentItemPredicate implements ArgumentType<ArgumentItemPredicate.Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo=bar}");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType((id) -> {
        return new ChatMessage("arguments.item.tag.unknown", id);
    });

    public static ArgumentItemPredicate itemPredicate() {
        return new ArgumentItemPredicate();
    }

    public ArgumentItemPredicate.Result parse(StringReader stringReader) throws CommandSyntaxException {
        ArgumentParserItemStack itemParser = (new ArgumentParserItemStack(stringReader, true)).parse();
        if (itemParser.getItem() != null) {
            ArgumentItemPredicate.ItemPredicate itemPredicate = new ArgumentItemPredicate.ItemPredicate(itemParser.getItem(), itemParser.getNbt());
            return (context) -> {
                return itemPredicate;
            };
        } else {
            MinecraftKey resourceLocation = itemParser.getTag();
            return (context) -> {
                Tag<Item> tag = context.getSource().getServer().getTagRegistry().getTagOrThrow(IRegistry.ITEM_REGISTRY, resourceLocation, (id) -> {
                    return ERROR_UNKNOWN_TAG.create(id.toString());
                });
                return new ArgumentItemPredicate.TagPredicate(tag, itemParser.getNbt());
            };
        }
    }

    public static Predicate<ItemStack> getItemPredicate(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, ArgumentItemPredicate.Result.class).create(context);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        StringReader stringReader = new StringReader(suggestionsBuilder.getInput());
        stringReader.setCursor(suggestionsBuilder.getStart());
        ArgumentParserItemStack itemParser = new ArgumentParserItemStack(stringReader, true);

        try {
            itemParser.parse();
        } catch (CommandSyntaxException var6) {
        }

        return itemParser.fillSuggestions(suggestionsBuilder, TagsItem.getAllTags());
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    static class ItemPredicate implements Predicate<ItemStack> {
        private final Item item;
        @Nullable
        private final NBTTagCompound nbt;

        public ItemPredicate(Item item, @Nullable NBTTagCompound nbt) {
            this.item = item;
            this.nbt = nbt;
        }

        @Override
        public boolean test(ItemStack itemStack) {
            return itemStack.is(this.item) && GameProfileSerializer.compareNbt(this.nbt, itemStack.getTag(), true);
        }
    }

    public interface Result {
        Predicate<ItemStack> create(CommandContext<CommandListenerWrapper> context) throws CommandSyntaxException;
    }

    static class TagPredicate implements Predicate<ItemStack> {
        private final Tag<Item> tag;
        @Nullable
        private final NBTTagCompound nbt;

        public TagPredicate(Tag<Item> tag, @Nullable NBTTagCompound nbt) {
            this.tag = tag;
            this.nbt = nbt;
        }

        @Override
        public boolean test(ItemStack itemStack) {
            return itemStack.is(this.tag) && GameProfileSerializer.compareNbt(this.nbt, itemStack.getTag(), true);
        }
    }
}
