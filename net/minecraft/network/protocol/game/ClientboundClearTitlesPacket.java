package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class ClientboundClearTitlesPacket implements Packet<PacketListenerPlayOut> {
    private final boolean resetTimes;

    public ClientboundClearTitlesPacket(boolean reset) {
        this.resetTimes = reset;
    }

    public ClientboundClearTitlesPacket(PacketDataSerializer buf) {
        this.resetTimes = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBoolean(this.resetTimes);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleTitlesClear(this);
    }

    public boolean shouldResetTimes() {
        return this.resetTimes;
    }
}
