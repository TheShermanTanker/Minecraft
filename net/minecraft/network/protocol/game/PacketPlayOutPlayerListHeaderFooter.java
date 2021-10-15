package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutPlayerListHeaderFooter implements Packet<PacketListenerPlayOut> {
    public final IChatBaseComponent header;
    public final IChatBaseComponent footer;

    public PacketPlayOutPlayerListHeaderFooter(IChatBaseComponent header, IChatBaseComponent footer) {
        this.header = header;
        this.footer = footer;
    }

    public PacketPlayOutPlayerListHeaderFooter(PacketDataSerializer buf) {
        this.header = buf.readComponent();
        this.footer = buf.readComponent();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeComponent(this.header);
        buf.writeComponent(this.footer);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleTabListCustomisation(this);
    }

    public IChatBaseComponent getHeader() {
        return this.header;
    }

    public IChatBaseComponent getFooter() {
        return this.footer;
    }
}
