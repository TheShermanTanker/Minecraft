package net.minecraft.network.protocol.handshake;

import net.minecraft.SharedConstants;
import net.minecraft.network.EnumProtocol;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketHandshakingInSetProtocol implements Packet<PacketHandshakingInListener> {
    private static final int MAX_HOST_LENGTH = 255;
    private final int protocolVersion;
    public String hostName;
    public final int port;
    private final EnumProtocol intention;

    public PacketHandshakingInSetProtocol(String address, int port, EnumProtocol intendedState) {
        this.protocolVersion = SharedConstants.getGameVersion().getProtocolVersion();
        this.hostName = address;
        this.port = port;
        this.intention = intendedState;
    }

    public PacketHandshakingInSetProtocol(PacketDataSerializer buf) {
        this.protocolVersion = buf.readVarInt();
        this.hostName = buf.readUtf(255);
        this.port = buf.readUnsignedShort();
        this.intention = EnumProtocol.getById(buf.readVarInt());
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.protocolVersion);
        buf.writeUtf(this.hostName);
        buf.writeShort(this.port);
        buf.writeVarInt(this.intention.getId());
    }

    @Override
    public void handle(PacketHandshakingInListener listener) {
        listener.handleIntention(this);
    }

    public EnumProtocol getIntention() {
        return this.intention;
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }

    public String getHostName() {
        return this.hostName;
    }

    public int getPort() {
        return this.port;
    }
}
