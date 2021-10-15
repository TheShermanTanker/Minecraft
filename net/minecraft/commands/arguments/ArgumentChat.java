package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.selector.ArgumentParserSelector;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;

public class ArgumentChat implements ArgumentType<ArgumentChat.Message> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");

    public static ArgumentChat message() {
        return new ArgumentChat();
    }

    public static IChatBaseComponent getMessage(CommandContext<CommandListenerWrapper> command, String name) throws CommandSyntaxException {
        return command.getArgument(name, ArgumentChat.Message.class).toComponent(command.getSource(), command.getSource().hasPermission(2));
    }

    @Override
    public ArgumentChat.Message parse(StringReader stringReader) throws CommandSyntaxException {
        return ArgumentChat.Message.parseText(stringReader, true);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Message {
        private final String text;
        private final ArgumentChat.Part[] parts;

        public Message(String contents, ArgumentChat.Part[] selectors) {
            this.text = contents;
            this.parts = selectors;
        }

        public String getText() {
            return this.text;
        }

        public ArgumentChat.Part[] getParts() {
            return this.parts;
        }

        public IChatBaseComponent toComponent(CommandListenerWrapper source, boolean bl) throws CommandSyntaxException {
            if (this.parts.length != 0 && bl) {
                IChatMutableComponent mutableComponent = new ChatComponentText(this.text.substring(0, this.parts[0].getStart()));
                int i = this.parts[0].getStart();

                for(ArgumentChat.Part part : this.parts) {
                    IChatBaseComponent component = part.toComponent(source);
                    if (i < part.getStart()) {
                        mutableComponent.append(this.text.substring(i, part.getStart()));
                    }

                    if (component != null) {
                        mutableComponent.addSibling(component);
                    }

                    i = part.getEnd();
                }

                if (i < this.text.length()) {
                    mutableComponent.append(this.text.substring(i, this.text.length()));
                }

                return mutableComponent;
            } else {
                return new ChatComponentText(this.text);
            }
        }

        public static ArgumentChat.Message parseText(StringReader reader, boolean bl) throws CommandSyntaxException {
            String string = reader.getString().substring(reader.getCursor(), reader.getTotalLength());
            if (!bl) {
                reader.setCursor(reader.getTotalLength());
                return new ArgumentChat.Message(string, new ArgumentChat.Part[0]);
            } else {
                List<ArgumentChat.Part> list = Lists.newArrayList();
                int i = reader.getCursor();

                while(true) {
                    int j;
                    EntitySelector entitySelector;
                    while(true) {
                        if (!reader.canRead()) {
                            return new ArgumentChat.Message(string, list.toArray(new ArgumentChat.Part[list.size()]));
                        }

                        if (reader.peek() == '@') {
                            j = reader.getCursor();

                            try {
                                ArgumentParserSelector entitySelectorParser = new ArgumentParserSelector(reader);
                                entitySelector = entitySelectorParser.parse();
                                break;
                            } catch (CommandSyntaxException var8) {
                                if (var8.getType() != ArgumentParserSelector.ERROR_MISSING_SELECTOR_TYPE && var8.getType() != ArgumentParserSelector.ERROR_UNKNOWN_SELECTOR_TYPE) {
                                    throw var8;
                                }

                                reader.setCursor(j + 1);
                            }
                        } else {
                            reader.skip();
                        }
                    }

                    list.add(new ArgumentChat.Part(j - i, reader.getCursor() - i, entitySelector));
                }
            }
        }
    }

    public static class Part {
        private final int start;
        private final int end;
        private final EntitySelector selector;

        public Part(int start, int end, EntitySelector selector) {
            this.start = start;
            this.end = end;
            this.selector = selector;
        }

        public int getStart() {
            return this.start;
        }

        public int getEnd() {
            return this.end;
        }

        public EntitySelector getSelector() {
            return this.selector;
        }

        @Nullable
        public IChatBaseComponent toComponent(CommandListenerWrapper source) throws CommandSyntaxException {
            return EntitySelector.joinNames(this.selector.getEntities(source));
        }
    }
}
