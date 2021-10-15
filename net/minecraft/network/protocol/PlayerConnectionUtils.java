package net.minecraft.network.protocol;

import net.minecraft.network.PacketListener;
import net.minecraft.server.CancelledPacketHandleException;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.thread.IAsyncTaskHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlayerConnectionUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    public static <T extends PacketListener> void ensureMainThread(Packet<T> packet, T listener, WorldServer world) throws CancelledPacketHandleException {
        ensureMainThread(packet, listener, world.getMinecraftServer());
    }

    public static <T extends PacketListener> void ensureMainThread(Packet<T> packet, T listener, IAsyncTaskHandler<?> engine) throws CancelledPacketHandleException {
        if (!engine.isMainThread()) {
            engine.execute(() -> {
                if (listener.getConnection().isConnected()) {
                    packet.handle(listener);
                } else {
                    LOGGER.debug("Ignoring packet due to disconnection: {}", (Object)packet);
                }

            });
            throw CancelledPacketHandleException.RUNNING_ON_DIFFERENT_THREAD;
        }
    }
}
