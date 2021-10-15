package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInItemName implements Packet<PacketListenerPlayIn> {
    private final String name;

    public PacketPlayInItemName(String name) {
        this.name = name;
    }

    public PacketPlayInItemName(PacketDataSerializer buf) {
        this.name = buf.readUtf();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeUtf(this.name);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleRenameItem(this);
    }

    public String getName() {
        return this.name;
    }
}
