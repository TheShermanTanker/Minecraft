package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.world.entity.Entity;

public class ChatComponentUtils {
    public static final String DEFAULT_SEPARATOR_TEXT = ", ";
    public static final IChatBaseComponent DEFAULT_SEPARATOR = (new ChatComponentText(", ")).withStyle(EnumChatFormat.GRAY);
    public static final IChatBaseComponent DEFAULT_NO_STYLE_SEPARATOR = new ChatComponentText(", ");

    public static IChatMutableComponent mergeStyles(IChatMutableComponent text, ChatModifier style) {
        if (style.isEmpty()) {
            return text;
        } else {
            ChatModifier style2 = text.getChatModifier();
            if (style2.isEmpty()) {
                return text.setChatModifier(style);
            } else {
                return style2.equals(style) ? text : text.setChatModifier(style2.setChatModifier(style));
            }
        }
    }

    public static Optional<IChatMutableComponent> updateForEntity(@Nullable CommandListenerWrapper source, Optional<IChatBaseComponent> text, @Nullable Entity sender, int depth) throws CommandSyntaxException {
        return text.isPresent() ? Optional.of(filterForDisplay(source, text.get(), sender, depth)) : Optional.empty();
    }

    public static IChatMutableComponent filterForDisplay(@Nullable CommandListenerWrapper source, IChatBaseComponent text, @Nullable Entity sender, int depth) throws CommandSyntaxException {
        if (depth > 100) {
            return text.mutableCopy();
        } else {
            IChatMutableComponent mutableComponent = text instanceof ChatComponentContextual ? ((ChatComponentContextual)text).resolve(source, sender, depth + 1) : text.plainCopy();

            for(IChatBaseComponent component : text.getSiblings()) {
                mutableComponent.addSibling(filterForDisplay(source, component, sender, depth + 1));
            }

            return mutableComponent.withStyle(resolveStyle(source, text.getChatModifier(), sender, depth));
        }
    }

    private static ChatModifier resolveStyle(@Nullable CommandListenerWrapper source, ChatModifier style, @Nullable Entity sender, int depth) throws CommandSyntaxException {
        ChatHoverable hoverEvent = style.getHoverEvent();
        if (hoverEvent != null) {
            IChatBaseComponent component = hoverEvent.getValue(ChatHoverable.EnumHoverAction.SHOW_TEXT);
            if (component != null) {
                ChatHoverable hoverEvent2 = new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, filterForDisplay(source, component, sender, depth + 1));
                return style.setChatHoverable(hoverEvent2);
            }
        }

        return style;
    }

    public static IChatBaseComponent getDisplayName(GameProfile profile) {
        if (profile.getName() != null) {
            return new ChatComponentText(profile.getName());
        } else {
            return profile.getId() != null ? new ChatComponentText(profile.getId().toString()) : new ChatComponentText("(unknown)");
        }
    }

    public static IChatBaseComponent formatList(Collection<String> strings) {
        return formatAndSortList(strings, (string) -> {
            return (new ChatComponentText(string)).withStyle(EnumChatFormat.GREEN);
        });
    }

    public static <T extends Comparable<T>> IChatBaseComponent formatAndSortList(Collection<T> elements, Function<T, IChatBaseComponent> transformer) {
        if (elements.isEmpty()) {
            return ChatComponentText.EMPTY;
        } else if (elements.size() == 1) {
            return transformer.apply(elements.iterator().next());
        } else {
            List<T> list = Lists.newArrayList(elements);
            list.sort(Comparable::compareTo);
            return formatList(list, transformer);
        }
    }

    public static <T> IChatBaseComponent formatList(Collection<? extends T> elements, Function<T, IChatBaseComponent> transformer) {
        return formatList(elements, DEFAULT_SEPARATOR, transformer);
    }

    public static <T> IChatMutableComponent formatList(Collection<? extends T> elements, Optional<? extends IChatBaseComponent> separator, Function<T, IChatBaseComponent> transformer) {
        return formatList(elements, DataFixUtils.orElse(separator, DEFAULT_SEPARATOR), transformer);
    }

    public static IChatBaseComponent formatList(Collection<? extends IChatBaseComponent> texts, IChatBaseComponent separator) {
        return formatList(texts, separator, Function.identity());
    }

    public static <T> IChatMutableComponent formatList(Collection<? extends T> elements, IChatBaseComponent separator, Function<T, IChatBaseComponent> transformer) {
        if (elements.isEmpty()) {
            return new ChatComponentText("");
        } else if (elements.size() == 1) {
            return transformer.apply(elements.iterator().next()).mutableCopy();
        } else {
            IChatMutableComponent mutableComponent = new ChatComponentText("");
            boolean bl = true;

            for(T object : elements) {
                if (!bl) {
                    mutableComponent.addSibling(separator);
                }

                mutableComponent.addSibling(transformer.apply(object));
                bl = false;
            }

            return mutableComponent;
        }
    }

    public static IChatMutableComponent wrapInSquareBrackets(IChatBaseComponent text) {
        return new ChatMessage("chat.square_brackets", text);
    }

    public static IChatBaseComponent fromMessage(Message message) {
        return (IChatBaseComponent)(message instanceof IChatBaseComponent ? (IChatBaseComponent)message : new ChatComponentText(message.getString()));
    }
}
