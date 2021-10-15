package net.minecraft.server.network;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.handshake.PacketHandshakingInListener;
import net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol;
import net.minecraft.server.MinecraftServer;

public class MemoryServerHandshakePacketListenerImpl implements PacketHandshakingInListener {
    private final MinecraftServer server;
    private final NetworkManager connection;

    public MemoryServerHandshakePacketListenerImpl(MinecraftServer server, NetworkManager connection) {
        this.server = server;
        this.connection = connection;
    }

    @Override
    public void handleIntention(PacketHandshakingInSetProtocol packet) {
        this.connection.setProtocol(packet.getIntention());
        this.connection.setPacketListener(new LoginListener(this.server, this.connection));
    }

    @Override
    public void onDisconnect(IChatBaseComponent reason) {
    }

    @Override
    public NetworkManager getConnection() {
        return this.connection;
    }
}
