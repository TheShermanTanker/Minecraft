package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInEnchantItem implements Packet<PacketListenerPlayIn> {
    private final int containerId;
    private final int buttonId;

    public PacketPlayInEnchantItem(int syncId, int buttonId) {
        this.containerId = syncId;
        this.buttonId = buttonId;
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleContainerButtonClick(this);
    }

    public PacketPlayInEnchantItem(PacketDataSerializer buf) {
        this.containerId = buf.readByte();
        this.buttonId = buf.readByte();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.containerId);
        buf.writeByte(this.buttonId);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getButtonId() {
        return this.buttonId;
    }
}
