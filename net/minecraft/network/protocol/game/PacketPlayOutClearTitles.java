package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutClearTitles implements Packet<PacketListenerPlayOut> {
    private final boolean resetTimes;

    public PacketPlayOutClearTitles(boolean reset) {
        this.resetTimes = reset;
    }

    public PacketPlayOutClearTitles(PacketDataSerializer buf) {
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
