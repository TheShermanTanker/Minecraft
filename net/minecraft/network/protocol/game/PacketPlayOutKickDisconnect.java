package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutKickDisconnect implements Packet<PacketListenerPlayOut> {
    private final IChatBaseComponent reason;

    public PacketPlayOutKickDisconnect(IChatBaseComponent reason) {
        this.reason = reason;
    }

    public PacketPlayOutKickDisconnect(PacketDataSerializer buf) {
        this.reason = buf.readComponent();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeComponent(this.reason);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleDisconnect(this);
    }

    public IChatBaseComponent getReason() {
        return this.reason;
    }
}
