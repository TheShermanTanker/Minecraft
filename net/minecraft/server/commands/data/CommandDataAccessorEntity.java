package net.minecraft.server.commands.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.advancements.critereon.CriterionConditionNBT;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentNBTKey;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;

public class CommandDataAccessorEntity implements CommandDataAccessor {
    private static final SimpleCommandExceptionType ERROR_NO_PLAYERS = new SimpleCommandExceptionType(new ChatMessage("commands.data.entity.invalid"));
    public static final Function<String, CommandData.DataProvider> PROVIDER = (string) -> {
        return new CommandData.DataProvider() {
            @Override
            public CommandDataAccessor access(CommandContext<CommandListenerWrapper> context) throws CommandSyntaxException {
                return new CommandDataAccessorEntity(ArgumentEntity.getEntity(context, string));
            }

            @Override
            public ArgumentBuilder<CommandListenerWrapper, ?> wrap(ArgumentBuilder<CommandListenerWrapper, ?> argument, Function<ArgumentBuilder<CommandListenerWrapper, ?>, ArgumentBuilder<CommandListenerWrapper, ?>> argumentAdder) {
                return argument.then(CommandDispatcher.literal("entity").then(argumentAdder.apply(CommandDispatcher.argument(string, ArgumentEntity.entity()))));
            }
        };
    };
    private final Entity entity;

    public CommandDataAccessorEntity(Entity entity) {
        this.entity = entity;
    }

    @Override
    public void setData(NBTTagCompound nbt) throws CommandSyntaxException {
        if (this.entity instanceof EntityHuman) {
            throw ERROR_NO_PLAYERS.create();
        } else {
            UUID uUID = this.entity.getUniqueID();
            this.entity.load(nbt);
            this.entity.setUUID(uUID);
        }
    }

    @Override
    public NBTTagCompound getData() {
        return CriterionConditionNBT.getEntityTagToCompare(this.entity);
    }

    @Override
    public IChatBaseComponent getModifiedSuccess() {
        return new ChatMessage("commands.data.entity.modified", this.entity.getScoreboardDisplayName());
    }

    @Override
    public IChatBaseComponent getPrintSuccess(NBTBase element) {
        return new ChatMessage("commands.data.entity.query", this.entity.getScoreboardDisplayName(), GameProfileSerializer.toPrettyComponent(element));
    }

    @Override
    public IChatBaseComponent getPrintSuccess(ArgumentNBTKey.NbtPath path, double scale, int result) {
        return new ChatMessage("commands.data.entity.get", path, this.entity.getScoreboardDisplayName(), String.format(Locale.ROOT, "%.2f", scale), result);
    }
}
