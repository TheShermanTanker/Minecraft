package net.minecraft.server.commands.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentMinecraftKeyRegistered;
import net.minecraft.commands.arguments.ArgumentNBTKey;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.storage.PersistentCommandStorage;

public class CommandDataStorage implements CommandDataAccessor {
    static final SuggestionProvider<CommandListenerWrapper> SUGGEST_STORAGE = (commandContext, suggestionsBuilder) -> {
        return ICompletionProvider.suggestResource(getGlobalTags(commandContext).keys(), suggestionsBuilder);
    };
    public static final Function<String, CommandData.DataProvider> PROVIDER = (string) -> {
        return new CommandData.DataProvider() {
            @Override
            public CommandDataAccessor access(CommandContext<CommandListenerWrapper> context) {
                return new CommandDataStorage(CommandDataStorage.getGlobalTags(context), ArgumentMinecraftKeyRegistered.getId(context, string));
            }

            @Override
            public ArgumentBuilder<CommandListenerWrapper, ?> wrap(ArgumentBuilder<CommandListenerWrapper, ?> argument, Function<ArgumentBuilder<CommandListenerWrapper, ?>, ArgumentBuilder<CommandListenerWrapper, ?>> argumentAdder) {
                return argument.then(CommandDispatcher.literal("storage").then(argumentAdder.apply(CommandDispatcher.argument(string, ArgumentMinecraftKeyRegistered.id()).suggests(CommandDataStorage.SUGGEST_STORAGE))));
            }
        };
    };
    private final PersistentCommandStorage storage;
    private final MinecraftKey id;

    static PersistentCommandStorage getGlobalTags(CommandContext<CommandListenerWrapper> commandContext) {
        return commandContext.getSource().getServer().getCommandStorage();
    }

    CommandDataStorage(PersistentCommandStorage commandStorage, MinecraftKey resourceLocation) {
        this.storage = commandStorage;
        this.id = resourceLocation;
    }

    @Override
    public void setData(NBTTagCompound nbt) {
        this.storage.set(this.id, nbt);
    }

    @Override
    public NBTTagCompound getData() {
        return this.storage.get(this.id);
    }

    @Override
    public IChatBaseComponent getModifiedSuccess() {
        return new ChatMessage("commands.data.storage.modified", this.id);
    }

    @Override
    public IChatBaseComponent getPrintSuccess(NBTBase element) {
        return new ChatMessage("commands.data.storage.query", this.id, GameProfileSerializer.toPrettyComponent(element));
    }

    @Override
    public IChatBaseComponent getPrintSuccess(ArgumentNBTKey.NbtPath path, double scale, int result) {
        return new ChatMessage("commands.data.storage.get", path, this.id, String.format(Locale.ROOT, "%.2f", scale), result);
    }
}
