package net.minecraft.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.IChatBaseComponent;

public class CommandException extends RuntimeException {
    private final IChatBaseComponent message;

    public CommandException(IChatBaseComponent message) {
        super(message.getString(), (Throwable)null, CommandSyntaxException.ENABLE_COMMAND_STACK_TRACES, CommandSyntaxException.ENABLE_COMMAND_STACK_TRACES);
        this.message = message;
    }

    public IChatBaseComponent getComponent() {
        return this.message;
    }
}
