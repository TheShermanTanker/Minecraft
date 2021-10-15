package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.world.level.GameRules;

public class CommandGamerule {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        final LiteralArgumentBuilder<CommandListenerWrapper> literalArgumentBuilder = net.minecraft.commands.CommandDispatcher.literal("gamerule").requires((source) -> {
            return source.hasPermission(2);
        });
        GameRules.visitGameRuleTypes(new GameRules.GameRuleVisitor() {
            @Override
            public <T extends GameRules.GameRuleValue<T>> void visit(GameRules.GameRuleKey<T> key, GameRules.GameRuleDefinition<T> type) {
                literalArgumentBuilder.then(net.minecraft.commands.CommandDispatcher.literal(key.getId()).executes((context) -> {
                    return CommandGamerule.queryRule(context.getSource(), key);
                }).then(type.createArgument("value").executes((context) -> {
                    return CommandGamerule.setRule(context, key);
                })));
            }
        });
        dispatcher.register(literalArgumentBuilder);
    }

    static <T extends GameRules.GameRuleValue<T>> int setRule(CommandContext<CommandListenerWrapper> context, GameRules.GameRuleKey<T> key) {
        CommandListenerWrapper commandSourceStack = context.getSource();
        T value = commandSourceStack.getServer().getGameRules().get(key);
        value.setFromArgument(context, "value");
        commandSourceStack.sendMessage(new ChatMessage("commands.gamerule.set", key.getId(), value.toString()), true);
        return value.getIntValue();
    }

    static <T extends GameRules.GameRuleValue<T>> int queryRule(CommandListenerWrapper source, GameRules.GameRuleKey<T> key) {
        T value = source.getServer().getGameRules().get(key);
        source.sendMessage(new ChatMessage("commands.gamerule.query", key.getId(), value.toString()), false);
        return value.getIntValue();
    }
}
