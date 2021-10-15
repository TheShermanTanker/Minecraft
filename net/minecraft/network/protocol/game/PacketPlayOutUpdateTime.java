package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutUpdateTime implements Packet<PacketListenerPlayOut> {
    private final long gameTime;
    private final long dayTime;

    public PacketPlayOutUpdateTime(long time, long timeOfDay, boolean doDaylightCycle) {
        this.gameTime = time;
        long l = timeOfDay;
        if (!doDaylightCycle) {
            l = -timeOfDay;
            if (l == 0L) {
                l = -1L;
            }
        }

        this.dayTime = l;
    }

    public PacketPlayOutUpdateTime(PacketDataSerializer buf) {
        this.gameTime = buf.readLong();
        this.dayTime = buf.readLong();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeLong(this.gameTime);
        buf.writeLong(this.dayTime);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetTime(this);
    }

    public long getGameTime() {
        return this.gameTime;
    }

    public long getDayTime() {
        return this.dayTime;
    }
}
