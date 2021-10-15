package net.minecraft.server.network;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.status.PacketStatusInListener;
import net.minecraft.network.protocol.status.PacketStatusInPing;
import net.minecraft.network.protocol.status.PacketStatusInStart;
import net.minecraft.network.protocol.status.PacketStatusOutPong;
import net.minecraft.network.protocol.status.PacketStatusOutServerInfo;
import net.minecraft.server.MinecraftServer;

public class PacketStatusListener implements PacketStatusInListener {
    private static final IChatBaseComponent DISCONNECT_REASON = new ChatMessage("multiplayer.status.request_handled");
    private final MinecraftServer server;
    private final NetworkManager connection;
    private boolean hasRequestedStatus;

    public PacketStatusListener(MinecraftServer server, NetworkManager connection) {
        this.server = server;
        this.connection = connection;
    }

    @Override
    public void onDisconnect(IChatBaseComponent reason) {
    }

    @Override
    public NetworkManager getConnection() {
        return this.connection;
    }

    @Override
    public void handleStatusRequest(PacketStatusInStart packet) {
        if (this.hasRequestedStatus) {
            this.connection.close(DISCONNECT_REASON);
        } else {
            this.hasRequestedStatus = true;
            this.connection.sendPacket(new PacketStatusOutServerInfo(this.server.getServerPing()));
        }
    }

    @Override
    public void handlePingRequest(PacketStatusInPing packet) {
        this.connection.sendPacket(new PacketStatusOutPong(packet.getTime()));
        this.connection.close(DISCONNECT_REASON);
    }
}
