package net.minecraft.network.protocol.login;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketLoginOutSetCompression implements Packet<PacketLoginOutListener> {
    private final int compressionThreshold;

    public PacketLoginOutSetCompression(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    public PacketLoginOutSetCompression(PacketDataSerializer buf) {
        this.compressionThreshold = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.compressionThreshold);
    }

    @Override
    public void handle(PacketLoginOutListener listener) {
        listener.handleCompression(this);
    }

    public int getCompressionThreshold() {
        return this.compressionThreshold;
    }
}
