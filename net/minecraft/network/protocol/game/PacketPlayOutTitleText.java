package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutTitleText implements Packet<PacketListenerPlayOut> {
    private final IChatBaseComponent text;

    public PacketPlayOutTitleText(IChatBaseComponent title) {
        this.text = title;
    }

    public PacketPlayOutTitleText(PacketDataSerializer buf) {
        this.text = buf.readComponent();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeComponent(this.text);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.setTitleText(this);
    }

    public IChatBaseComponent getText() {
        return this.text;
    }
}
