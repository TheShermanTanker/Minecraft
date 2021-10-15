package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;

public class PacketPlayInCustomPayload implements Packet<PacketListenerPlayIn> {
    private static final int MAX_PAYLOAD_SIZE = 32767;
    public static final MinecraftKey BRAND = new MinecraftKey("brand");
    public final MinecraftKey identifier;
    public final PacketDataSerializer data;

    public PacketPlayInCustomPayload(MinecraftKey channel, PacketDataSerializer data) {
        this.identifier = channel;
        this.data = data;
    }

    public PacketPlayInCustomPayload(PacketDataSerializer buf) {
        this.identifier = buf.readResourceLocation();
        int i = buf.readableBytes();
        if (i >= 0 && i <= 32767) {
            this.data = new PacketDataSerializer(buf.readBytes(i));
        } else {
            throw new IllegalArgumentException("Payload may not be larger than 32767 bytes");
        }
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeResourceLocation(this.identifier);
        buf.writeBytes((ByteBuf)this.data);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleCustomPayload(this);
        this.data.release();
    }

    public MinecraftKey getIdentifier() {
        return this.identifier;
    }

    public PacketDataSerializer getData() {
        return this.data;
    }
}
