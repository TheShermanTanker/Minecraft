package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInChat implements Packet<PacketListenerPlayIn> {
    private static final int MAX_MESSAGE_LENGTH = 256;
    private final String message;

    public PacketPlayInChat(String chatMessage) {
        if (chatMessage.length() > 256) {
            chatMessage = chatMessage.substring(0, 256);
        }

        this.message = chatMessage;
    }

    public PacketPlayInChat(PacketDataSerializer buf) {
        this.message = buf.readUtf(256);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeUtf(this.message);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleChat(this);
    }

    public String getMessage() {
        return this.message;
    }
}
