package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderWarningDelayPacket implements Packet<PacketListenerPlayOut> {
    private final int warningDelay;

    public ClientboundSetBorderWarningDelayPacket(WorldBorder worldBorder) {
        this.warningDelay = worldBorder.getWarningTime();
    }

    public ClientboundSetBorderWarningDelayPacket(PacketDataSerializer buf) {
        this.warningDelay = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.warningDelay);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetBorderWarningDelay(this);
    }

    public int getWarningDelay() {
        return this.warningDelay;
    }
}
