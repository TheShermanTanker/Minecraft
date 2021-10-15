package net.minecraft.network.protocol.login;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;

public class PacketLoginOutDisconnect implements Packet<PacketLoginOutListener> {
    private final IChatBaseComponent reason;

    public PacketLoginOutDisconnect(IChatBaseComponent reason) {
        this.reason = reason;
    }

    public PacketLoginOutDisconnect(PacketDataSerializer buf) {
        this.reason = IChatBaseComponent.ChatSerializer.fromJsonLenient(buf.readUtf(262144));
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeComponent(this.reason);
    }

    @Override
    public void handle(PacketLoginOutListener listener) {
        listener.handleDisconnect(this);
    }

    public IChatBaseComponent getReason() {
        return this.reason;
    }
}
