package net.minecraft.network.chat;

import java.util.Arrays;
import java.util.Collection;

public class CommonComponents {
    public static final IChatBaseComponent OPTION_ON = new ChatMessage("options.on");
    public static final IChatBaseComponent OPTION_OFF = new ChatMessage("options.off");
    public static final IChatBaseComponent GUI_DONE = new ChatMessage("gui.done");
    public static final IChatBaseComponent GUI_CANCEL = new ChatMessage("gui.cancel");
    public static final IChatBaseComponent GUI_YES = new ChatMessage("gui.yes");
    public static final IChatBaseComponent GUI_NO = new ChatMessage("gui.no");
    public static final IChatBaseComponent GUI_PROCEED = new ChatMessage("gui.proceed");
    public static final IChatBaseComponent GUI_BACK = new ChatMessage("gui.back");
    public static final IChatBaseComponent CONNECT_FAILED = new ChatMessage("connect.failed");
    public static final IChatBaseComponent NEW_LINE = new ChatComponentText("\n");
    public static final IChatBaseComponent NARRATION_SEPARATOR = new ChatComponentText(". ");

    public static IChatBaseComponent optionStatus(boolean on) {
        return on ? OPTION_ON : OPTION_OFF;
    }

    public static IChatMutableComponent optionStatus(IChatBaseComponent text, boolean value) {
        return new ChatMessage(value ? "options.on.composed" : "options.off.composed", text);
    }

    public static IChatMutableComponent optionNameValue(IChatBaseComponent text, IChatBaseComponent value) {
        return new ChatMessage("options.generic_value", text, value);
    }

    public static IChatMutableComponent joinForNarration(IChatBaseComponent first, IChatBaseComponent second) {
        return (new ChatComponentText("")).addSibling(first).addSibling(NARRATION_SEPARATOR).addSibling(second);
    }

    public static IChatBaseComponent joinLines(IChatBaseComponent... texts) {
        return joinLines(Arrays.asList(texts));
    }

    public static IChatBaseComponent joinLines(Collection<? extends IChatBaseComponent> texts) {
        return ChatComponentUtils.formatList(texts, NEW_LINE);
    }
}
