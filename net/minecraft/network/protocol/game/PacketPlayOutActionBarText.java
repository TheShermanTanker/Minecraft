package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutActionBarText implements Packet<PacketListenerPlayOut> {
    private final IChatBaseComponent text;

    public PacketPlayOutActionBarText(IChatBaseComponent message) {
        this.text = message;
    }

    public PacketPlayOutActionBarText(PacketDataSerializer buf) {
        this.text = buf.readComponent();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeComponent(this.text);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.setActionBarText(this);
    }

    public IChatBaseComponent getText() {
        return this.text;
    }
}
