package net.minecraft.network;

import net.minecraft.network.chat.IChatBaseComponent;

public interface PacketListener {
    void onDisconnect(IChatBaseComponent reason);

    NetworkManager getConnection();
}
