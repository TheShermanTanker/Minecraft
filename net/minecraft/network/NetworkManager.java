package net.minecraft.network;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Queue;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutKickDisconnect;
import net.minecraft.network.protocol.login.PacketLoginOutDisconnect;
import net.minecraft.server.CancelledPacketHandleException;
import net.minecraft.server.network.LoginListener;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.util.LazyInitVar;
import net.minecraft.util.MathHelper;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class NetworkManager extends SimpleChannelInboundHandler<Packet<?>> {
    private static final float AVERAGE_PACKETS_SMOOTHING = 0.75F;
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Marker ROOT_MARKER = MarkerManager.getMarker("NETWORK");
    public static final Marker PACKET_MARKER = MarkerManager.getMarker("NETWORK_PACKETS", ROOT_MARKER);
    public static final AttributeKey<EnumProtocol> ATTRIBUTE_PROTOCOL = AttributeKey.valueOf("protocol");
    public static final LazyInitVar<NioEventLoopGroup> NETWORK_WORKER_GROUP = new LazyInitVar<>(() -> {
        return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build());
    });
    public static final LazyInitVar<EpollEventLoopGroup> NETWORK_EPOLL_WORKER_GROUP = new LazyInitVar<>(() -> {
        return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).build());
    });
    public static final LazyInitVar<DefaultEventLoopGroup> LOCAL_WORKER_GROUP = new LazyInitVar<>(() -> {
        return new DefaultEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Local Client IO #%d").setDaemon(true).build());
    });
    private final EnumProtocolDirection receiving;
    private final Queue<NetworkManager.QueuedPacket> queue = Queues.newConcurrentLinkedQueue();
    public Channel channel;
    public SocketAddress address;
    private PacketListener packetListener;
    private IChatBaseComponent disconnectedReason;
    private boolean encrypted;
    private boolean disconnectionHandled;
    private int receivedPackets;
    private int sentPackets;
    private float averageReceivedPackets;
    private float averageSentPackets;
    private int tickCount;
    private boolean handlingFault;

    public NetworkManager(EnumProtocolDirection side) {
        this.receiving = side;
    }

    public void channelActive(ChannelHandlerContext channelHandlerContext) throws Exception {
        super.channelActive(channelHandlerContext);
        this.channel = channelHandlerContext.channel();
        this.address = this.channel.remoteAddress();

        try {
            this.setProtocol(EnumProtocol.HANDSHAKING);
        } catch (Throwable var3) {
            LOGGER.fatal(var3);
        }

    }

    public void setProtocol(EnumProtocol state) {
        this.channel.attr(ATTRIBUTE_PROTOCOL).set(state);
        this.channel.config().setAutoRead(true);
        LOGGER.debug("Enabled auto read");
    }

    public void channelInactive(ChannelHandlerContext channelHandlerContext) {
        this.close(new ChatMessage("disconnect.endOfStream"));
    }

    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) {
        if (throwable instanceof SkipEncodeException) {
            LOGGER.debug("Skipping packet due to errors", throwable.getCause());
        } else {
            boolean bl = !this.handlingFault;
            this.handlingFault = true;
            if (this.channel.isOpen()) {
                if (throwable instanceof TimeoutException) {
                    LOGGER.debug("Timeout", throwable);
                    this.close(new ChatMessage("disconnect.timeout"));
                } else {
                    IChatBaseComponent component = new ChatMessage("disconnect.genericReason", "Internal Exception: " + throwable);
                    if (bl) {
                        LOGGER.debug("Failed to sent packet", throwable);
                        EnumProtocol connectionProtocol = this.getCurrentProtocol();
                        Packet<?> packet = (Packet<?>)(connectionProtocol == EnumProtocol.LOGIN ? new PacketLoginOutDisconnect(component) : new PacketPlayOutKickDisconnect(component));
                        this.send(packet, (future) -> {
                            this.close(component);
                        });
                        this.stopReading();
                    } else {
                        LOGGER.debug("Double fault", throwable);
                        this.close(component);
                    }
                }

            }
        }
    }

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet) {
        if (this.channel.isOpen()) {
            try {
                genericsFtw(packet, this.packetListener);
            } catch (CancelledPacketHandleException var4) {
            } catch (ClassCastException var5) {
                LOGGER.error("Received {} that couldn't be processed", packet.getClass(), var5);
                this.close(new ChatMessage("multiplayer.disconnect.invalid_packet"));
            }

            ++this.receivedPackets;
        }

    }

    private static <T extends PacketListener> void genericsFtw(Packet<T> packet, PacketListener listener) {
        packet.handle((T)listener);
    }

    public void setPacketListener(PacketListener listener) {
        Validate.notNull(listener, "packetListener");
        this.packetListener = listener;
    }

    public void sendPacket(Packet<?> packet) {
        this.send(packet, (GenericFutureListener<? extends Future<? super Void>>)null);
    }

    public void send(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
        if (this.isConnected()) {
            this.flushQueue();
            this.sendPacket(packet, callback);
        } else {
            this.queue.add(new NetworkManager.QueuedPacket(packet, callback));
        }

    }

    private void sendPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
        EnumProtocol connectionProtocol = EnumProtocol.getProtocolForPacket(packet);
        EnumProtocol connectionProtocol2 = this.getCurrentProtocol();
        ++this.sentPackets;
        if (connectionProtocol2 != connectionProtocol) {
            LOGGER.debug("Disabled auto read");
            this.channel.config().setAutoRead(false);
        }

        if (this.channel.eventLoop().inEventLoop()) {
            this.doSendPacket(packet, callback, connectionProtocol, connectionProtocol2);
        } else {
            this.channel.eventLoop().execute(() -> {
                this.doSendPacket(packet, callback, connectionProtocol, connectionProtocol2);
            });
        }

    }

    private void doSendPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, EnumProtocol packetState, EnumProtocol currentState) {
        if (packetState != currentState) {
            this.setProtocol(packetState);
        }

        ChannelFuture channelFuture = this.channel.writeAndFlush(packet);
        if (callback != null) {
            channelFuture.addListener(callback);
        }

        channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    private EnumProtocol getCurrentProtocol() {
        return this.channel.attr(ATTRIBUTE_PROTOCOL).get();
    }

    private void flushQueue() {
        if (this.channel != null && this.channel.isOpen()) {
            synchronized(this.queue) {
                NetworkManager.QueuedPacket packetHolder;
                while((packetHolder = this.queue.poll()) != null) {
                    this.sendPacket(packetHolder.packet, packetHolder.listener);
                }

            }
        }
    }

    public void tick() {
        this.flushQueue();
        if (this.packetListener instanceof LoginListener) {
            ((LoginListener)this.packetListener).tick();
        }

        if (this.packetListener instanceof PlayerConnection) {
            ((PlayerConnection)this.packetListener).tick();
        }

        if (!this.isConnected() && !this.disconnectionHandled) {
            this.handleDisconnection();
        }

        if (this.channel != null) {
            this.channel.flush();
        }

        if (this.tickCount++ % 20 == 0) {
            this.tickSecond();
        }

    }

    protected void tickSecond() {
        this.averageSentPackets = MathHelper.lerp(0.75F, (float)this.sentPackets, this.averageSentPackets);
        this.averageReceivedPackets = MathHelper.lerp(0.75F, (float)this.receivedPackets, this.averageReceivedPackets);
        this.sentPackets = 0;
        this.receivedPackets = 0;
    }

    public SocketAddress getSocketAddress() {
        return this.address;
    }

    public void close(IChatBaseComponent disconnectReason) {
        if (this.channel.isOpen()) {
            this.channel.close().awaitUninterruptibly();
            this.disconnectedReason = disconnectReason;
        }

    }

    public boolean isLocal() {
        return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
    }

    public EnumProtocolDirection getReceiving() {
        return this.receiving;
    }

    public EnumProtocolDirection getSending() {
        return this.receiving.getOpposite();
    }

    public static NetworkManager connectToServer(InetSocketAddress address, boolean useEpoll) {
        final NetworkManager connection = new NetworkManager(EnumProtocolDirection.CLIENTBOUND);
        Class<? extends SocketChannel> class_;
        LazyInitVar<? extends EventLoopGroup> lazyLoadedValue;
        if (Epoll.isAvailable() && useEpoll) {
            class_ = EpollSocketChannel.class;
            lazyLoadedValue = NETWORK_EPOLL_WORKER_GROUP;
        } else {
            class_ = NioSocketChannel.class;
            lazyLoadedValue = NETWORK_WORKER_GROUP;
        }

        (new Bootstrap()).group(lazyLoadedValue.get()).handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) {
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException var3) {
                }

                channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30)).addLast("splitter", new PacketSplitter()).addLast("decoder", new PacketDecoder(EnumProtocolDirection.CLIENTBOUND)).addLast("prepender", new PacketPrepender()).addLast("encoder", new PacketEncoder(EnumProtocolDirection.SERVERBOUND)).addLast("packet_handler", connection);
            }
        }).channel(class_).connect(address.getAddress(), address.getPort()).syncUninterruptibly();
        return connection;
    }

    public static NetworkManager connectToLocalServer(SocketAddress address) {
        final NetworkManager connection = new NetworkManager(EnumProtocolDirection.CLIENTBOUND);
        (new Bootstrap()).group(LOCAL_WORKER_GROUP.get()).handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) {
                channel.pipeline().addLast("packet_handler", connection);
            }
        }).channel(LocalChannel.class).connect(address).syncUninterruptibly();
        return connection;
    }

    public void setEncryptionKey(Cipher decryptionCipher, Cipher encryptionCipher) {
        this.encrypted = true;
        this.channel.pipeline().addBefore("splitter", "decrypt", new PacketDecrypter(decryptionCipher));
        this.channel.pipeline().addBefore("prepender", "encrypt", new PacketEncrypter(encryptionCipher));
    }

    public boolean isEncrypted() {
        return this.encrypted;
    }

    public boolean isConnected() {
        return this.channel != null && this.channel.isOpen();
    }

    public boolean isConnecting() {
        return this.channel == null;
    }

    public PacketListener getPacketListener() {
        return this.packetListener;
    }

    @Nullable
    public IChatBaseComponent getDisconnectedReason() {
        return this.disconnectedReason;
    }

    public void stopReading() {
        this.channel.config().setAutoRead(false);
    }

    public void setCompressionLevel(int compressionThreshold, boolean rejectsBadPackets) {
        if (compressionThreshold >= 0) {
            if (this.channel.pipeline().get("decompress") instanceof PacketDecompressor) {
                ((PacketDecompressor)this.channel.pipeline().get("decompress")).setThreshold(compressionThreshold, rejectsBadPackets);
            } else {
                this.channel.pipeline().addBefore("decoder", "decompress", new PacketDecompressor(compressionThreshold, rejectsBadPackets));
            }

            if (this.channel.pipeline().get("compress") instanceof PacketCompressor) {
                ((PacketCompressor)this.channel.pipeline().get("compress")).setThreshold(compressionThreshold);
            } else {
                this.channel.pipeline().addBefore("encoder", "compress", new PacketCompressor(compressionThreshold));
            }
        } else {
            if (this.channel.pipeline().get("decompress") instanceof PacketDecompressor) {
                this.channel.pipeline().remove("decompress");
            }

            if (this.channel.pipeline().get("compress") instanceof PacketCompressor) {
                this.channel.pipeline().remove("compress");
            }
        }

    }

    public void handleDisconnection() {
        if (this.channel != null && !this.channel.isOpen()) {
            if (this.disconnectionHandled) {
                LOGGER.warn("handleDisconnection() called twice");
            } else {
                this.disconnectionHandled = true;
                if (this.getDisconnectedReason() != null) {
                    this.getPacketListener().onDisconnect(this.getDisconnectedReason());
                } else if (this.getPacketListener() != null) {
                    this.getPacketListener().onDisconnect(new ChatMessage("multiplayer.disconnect.generic"));
                }
            }

        }
    }

    public float getAverageReceivedPackets() {
        return this.averageReceivedPackets;
    }

    public float getAverageSentPackets() {
        return this.averageSentPackets;
    }

    static class QueuedPacket {
        final Packet<?> packet;
        @Nullable
        final GenericFutureListener<? extends Future<? super Void>> listener;

        public QueuedPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
            this.packet = packet;
            this.listener = callback;
        }
    }
}
