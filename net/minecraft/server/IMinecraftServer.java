package net.minecraft.server;

import net.minecraft.server.dedicated.DedicatedServerProperties;

public interface IMinecraftServer {
    DedicatedServerProperties getDedicatedServerProperties();

    String getServerIp();

    int getServerPort();

    String getServerName();

    String getVersion();

    int getPlayerCount();

    int getMaxPlayers();

    String[] getPlayers();

    String getWorld();

    String getPlugins();

    String executeRemoteCommand(String command);
}
