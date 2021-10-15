package net.minecraft.server.rcon.thread;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.IMinecraftServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoteControlListener extends RemoteConnectionThread {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ServerSocket socket;
    private final String rconPassword;
    private final List<RemoteControlSession> clients = Lists.newArrayList();
    private final IMinecraftServer serverInterface;

    private RemoteControlListener(IMinecraftServer server, ServerSocket listener, String password) {
        super("RCON Listener");
        this.serverInterface = server;
        this.socket = listener;
        this.rconPassword = password;
    }

    private void clearClients() {
        this.clients.removeIf((client) -> {
            return !client.isRunning();
        });
    }

    @Override
    public void run() {
        try {
            while(this.running) {
                try {
                    Socket socket = this.socket.accept();
                    RemoteControlSession rconClient = new RemoteControlSession(this.serverInterface, this.rconPassword, socket);
                    rconClient.start();
                    this.clients.add(rconClient);
                    this.clearClients();
                } catch (SocketTimeoutException var7) {
                    this.clearClients();
                } catch (IOException var8) {
                    if (this.running) {
                        LOGGER.info("IO exception: ", (Throwable)var8);
                    }
                }
            }
        } finally {
            this.closeSocket(this.socket);
        }

    }

    @Nullable
    public static RemoteControlListener create(IMinecraftServer server) {
        DedicatedServerProperties dedicatedServerProperties = server.getDedicatedServerProperties();
        String string = server.getServerIp();
        if (string.isEmpty()) {
            string = "0.0.0.0";
        }

        int i = dedicatedServerProperties.rconPort;
        if (0 < i && 65535 >= i) {
            String string2 = dedicatedServerProperties.rconPassword;
            if (string2.isEmpty()) {
                LOGGER.warn("No rcon password set in server.properties, rcon disabled!");
                return null;
            } else {
                try {
                    ServerSocket serverSocket = new ServerSocket(i, 0, InetAddress.getByName(string));
                    serverSocket.setSoTimeout(500);
                    RemoteControlListener rconThread = new RemoteControlListener(server, serverSocket, string2);
                    if (!rconThread.start()) {
                        return null;
                    } else {
                        LOGGER.info("RCON running on {}:{}", string, i);
                        return rconThread;
                    }
                } catch (IOException var7) {
                    LOGGER.warn("Unable to initialise RCON on {}:{}", string, i, var7);
                    return null;
                }
            }
        } else {
            LOGGER.warn("Invalid rcon port {} found in server.properties, rcon disabled!", (int)i);
            return null;
        }
    }

    @Override
    public void stop() {
        this.running = false;
        this.closeSocket(this.socket);
        super.stop();

        for(RemoteControlSession rconClient : this.clients) {
            if (rconClient.isRunning()) {
                rconClient.stop();
            }
        }

        this.clients.clear();
    }

    private void closeSocket(ServerSocket socket) {
        LOGGER.debug("closeSocket: {}", (Object)socket);

        try {
            socket.close();
        } catch (IOException var3) {
            LOGGER.warn("Failed to close socket", (Throwable)var3);
        }

    }
}
