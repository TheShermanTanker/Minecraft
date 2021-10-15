package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tags;
import net.minecraft.world.item.Item;

public class ArgumentParserItemStack {
    public static final SimpleCommandExceptionType ERROR_NO_TAGS_ALLOWED = new SimpleCommandExceptionType(new ChatMessage("argument.item.tag.disallowed"));
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("argument.item.id.invalid", object);
    });
    private static final char SYNTAX_START_NBT = '{';
    private static final char SYNTAX_TAG = '#';
    private static final BiFunction<SuggestionsBuilder, Tags<Item>, CompletableFuture<Suggestions>> SUGGEST_NOTHING = (suggestionsBuilder, tagCollection) -> {
        return suggestionsBuilder.buildFuture();
    };
    private final StringReader reader;
    private final boolean forTesting;
    private Item item;
    @Nullable
    private NBTTagCompound nbt;
    private MinecraftKey tag = new MinecraftKey("");
    private int tagCursor;
    private BiFunction<SuggestionsBuilder, Tags<Item>, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;

    public ArgumentParserItemStack(StringReader reader, boolean allowTag) {
        this.reader = reader;
        this.forTesting = allowTag;
    }

    public Item getItem() {
        return this.item;
    }

    @Nullable
    public NBTTagCompound getNbt() {
        return this.nbt;
    }

    public MinecraftKey getTag() {
        return this.tag;
    }

    public void readItem() throws CommandSyntaxException {
        int i = this.reader.getCursor();
        MinecraftKey resourceLocation = MinecraftKey.read(this.reader);
        this.item = IRegistry.ITEM.getOptional(resourceLocation).orElseThrow(() -> {
            this.reader.setCursor(i);
            return ERROR_UNKNOWN_ITEM.createWithContext(this.reader, resourceLocation.toString());
        });
    }

    public void readTag() throws CommandSyntaxException {
        if (!this.forTesting) {
            throw ERROR_NO_TAGS_ALLOWED.create();
        } else {
            this.suggestions = this::suggestTag;
            this.reader.expect('#');
            this.tagCursor = this.reader.getCursor();
            this.tag = MinecraftKey.read(this.reader);
        }
    }

    public void readNbt() throws CommandSyntaxException {
        this.nbt = (new MojangsonParser(this.reader)).readStruct();
    }

    public ArgumentParserItemStack parse() throws CommandSyntaxException {
        this.suggestions = this::suggestItemIdOrTag;
        if (this.reader.canRead() && this.reader.peek() == '#') {
            this.readTag();
        } else {
            this.readItem();
            this.suggestions = this::suggestOpenNbt;
        }

        if (this.reader.canRead() && this.reader.peek() == '{') {
            this.suggestions = SUGGEST_NOTHING;
            this.readNbt();
        }

        return this;
    }

    private CompletableFuture<Suggestions> suggestOpenNbt(SuggestionsBuilder suggestionsBuilder, Tags<Item> tagCollection) {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
            suggestionsBuilder.suggest(String.valueOf('{'));
        }

        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestTag(SuggestionsBuilder suggestionsBuilder, Tags<Item> tagCollection) {
        return ICompletionProvider.suggestResource(tagCollection.getAvailableTags(), suggestionsBuilder.createOffset(this.tagCursor));
    }

    private CompletableFuture<Suggestions> suggestItemIdOrTag(SuggestionsBuilder suggestionsBuilder, Tags<Item> tagCollection) {
        if (this.forTesting) {
            ICompletionProvider.suggestResource(tagCollection.getAvailableTags(), suggestionsBuilder, String.valueOf('#'));
        }

        return ICompletionProvider.suggestResource(IRegistry.ITEM.keySet(), suggestionsBuilder);
    }

    public CompletableFuture<Suggestions> fillSuggestions(SuggestionsBuilder builder, Tags<Item> tagCollection) {
        return this.suggestions.apply(builder.createOffset(this.reader.getCursor()), tagCollection);
    }
}
