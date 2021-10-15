package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutChat implements Packet<PacketListenerPlayOut> {
    private final IChatBaseComponent message;
    private final ChatMessageType type;
    private final UUID sender;

    public PacketPlayOutChat(IChatBaseComponent message, ChatMessageType location, UUID sender) {
        this.message = message;
        this.type = location;
        this.sender = sender;
    }

    public PacketPlayOutChat(PacketDataSerializer buf) {
        this.message = buf.readComponent();
        this.type = ChatMessageType.getForIndex(buf.readByte());
        this.sender = buf.readUUID();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeComponent(this.message);
        buf.writeByte(this.type.getIndex());
        buf.writeUUID(this.sender);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleChat(this);
    }

    public IChatBaseComponent getMessage() {
        return this.message;
    }

    public ChatMessageType getType() {
        return this.type;
    }

    public UUID getSender() {
        return this.sender;
    }

    @Override
    public boolean isSkippable() {
        return true;
    }
}
