package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public record ClientboundSetSimulationDistancePacket(int simulationDistance) implements Packet<PacketListenerPlayOut> {
    public ClientboundSetSimulationDistancePacket(PacketDataSerializer buf) {
        this(buf.readVarInt());
    }

    public ClientboundSetSimulationDistancePacket(int i) {
        this.simulationDistance = i;
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.simulationDistance);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetSimulationDistance(this);
    }

    public int simulationDistance() {
        return this.simulationDistance;
    }
}
