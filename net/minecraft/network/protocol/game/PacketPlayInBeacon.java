package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInBeacon implements Packet<PacketListenerPlayIn> {
    private final int primary;
    private final int secondary;

    public PacketPlayInBeacon(int primaryEffectId, int secondaryEffectId) {
        this.primary = primaryEffectId;
        this.secondary = secondaryEffectId;
    }

    public PacketPlayInBeacon(PacketDataSerializer buf) {
        this.primary = buf.readVarInt();
        this.secondary = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.primary);
        buf.writeVarInt(this.secondary);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleSetBeaconPacket(this);
    }

    public int getPrimary() {
        return this.primary;
    }

    public int getSecondary() {
        return this.secondary;
    }
}
