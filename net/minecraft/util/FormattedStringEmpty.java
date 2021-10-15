package net.minecraft.util;

import net.minecraft.network.chat.ChatModifier;

@FunctionalInterface
public interface FormattedStringEmpty {
    boolean accept(int index, ChatModifier style, int codePoint);
}
