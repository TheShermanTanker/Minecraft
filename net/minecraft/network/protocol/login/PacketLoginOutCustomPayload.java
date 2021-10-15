package net.minecraft.network.protocol.login;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;

public class PacketLoginOutCustomPayload implements Packet<PacketLoginOutListener> {
    private static final int MAX_PAYLOAD_SIZE = 1048576;
    private final int transactionId;
    private final MinecraftKey identifier;
    private final PacketDataSerializer data;

    public PacketLoginOutCustomPayload(int queryId, MinecraftKey channel, PacketDataSerializer payload) {
        this.transactionId = queryId;
        this.identifier = channel;
        this.data = payload;
    }

    public PacketLoginOutCustomPayload(PacketDataSerializer buf) {
        this.transactionId = buf.readVarInt();
        this.identifier = buf.readResourceLocation();
        int i = buf.readableBytes();
        if (i >= 0 && i <= 1048576) {
            this.data = new PacketDataSerializer(buf.readBytes(i));
        } else {
            throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
        }
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.transactionId);
        buf.writeResourceLocation(this.identifier);
        buf.writeBytes(this.data.copy());
    }

    @Override
    public void handle(PacketLoginOutListener listener) {
        listener.handleCustomQuery(this);
    }

    public int getTransactionId() {
        return this.transactionId;
    }

    public MinecraftKey getIdentifier() {
        return this.identifier;
    }

    public PacketDataSerializer getData() {
        return this.data;
    }
}
