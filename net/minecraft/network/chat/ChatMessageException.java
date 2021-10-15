package net.minecraft.network.chat;

public class ChatMessageException extends IllegalArgumentException {
    public ChatMessageException(ChatMessage text, String message) {
        super(String.format("Error parsing: %s: %s", text, message));
    }

    public ChatMessageException(ChatMessage text, int index) {
        super(String.format("Invalid index %d requested for %s", index, text));
    }

    public ChatMessageException(ChatMessage text, Throwable cause) {
        super(String.format("Error while parsing: %s", text), cause);
    }
}
