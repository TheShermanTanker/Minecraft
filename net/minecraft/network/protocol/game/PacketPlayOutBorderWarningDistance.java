package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.border.WorldBorder;

public class PacketPlayOutBorderWarningDistance implements Packet<PacketListenerPlayOut> {
    private final int warningBlocks;

    public PacketPlayOutBorderWarningDistance(WorldBorder worldBorder) {
        this.warningBlocks = worldBorder.getWarningDistance();
    }

    public PacketPlayOutBorderWarningDistance(PacketDataSerializer buf) {
        this.warningBlocks = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.warningBlocks);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetBorderWarningDistance(this);
    }

    public int getWarningBlocks() {
        return this.warningBlocks;
    }
}
