package net.minecraft.server.network;

import net.minecraft.SharedConstants;
import net.minecraft.network.EnumProtocol;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.handshake.PacketHandshakingInListener;
import net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol;
import net.minecraft.network.protocol.login.PacketLoginOutDisconnect;
import net.minecraft.server.MinecraftServer;

public class HandshakeListener implements PacketHandshakingInListener {
    private static final IChatBaseComponent IGNORE_STATUS_REASON = new ChatComponentText("Ignoring status request");
    private final MinecraftServer server;
    private final NetworkManager connection;

    public HandshakeListener(MinecraftServer server, NetworkManager connection) {
        this.server = server;
        this.connection = connection;
    }

    @Override
    public void handleIntention(PacketHandshakingInSetProtocol packet) {
        switch(packet.getIntention()) {
        case LOGIN:
            this.connection.setProtocol(EnumProtocol.LOGIN);
            if (packet.getProtocolVersion() != SharedConstants.getGameVersion().getProtocolVersion()) {
                IChatBaseComponent component;
                if (packet.getProtocolVersion() < 754) {
                    component = new ChatMessage("multiplayer.disconnect.outdated_client", SharedConstants.getGameVersion().getName());
                } else {
                    component = new ChatMessage("multiplayer.disconnect.incompatible", SharedConstants.getGameVersion().getName());
                }

                this.connection.sendPacket(new PacketLoginOutDisconnect(component));
                this.connection.close(component);
            } else {
                this.connection.setPacketListener(new LoginListener(this.server, this.connection));
            }
            break;
        case STATUS:
            if (this.server.repliesToStatus()) {
                this.connection.setProtocol(EnumProtocol.STATUS);
                this.connection.setPacketListener(new PacketStatusListener(this.server, this.connection));
            } else {
                this.connection.close(IGNORE_STATUS_REASON);
            }
            break;
        default:
            throw new UnsupportedOperationException("Invalid intention " + packet.getIntention());
        }

    }

    @Override
    public void onDisconnect(IChatBaseComponent reason) {
    }

    @Override
    public NetworkManager getConnection() {
        return this.connection;
    }
}
