package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetworkManagerServer;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.PacketPrepender;
import net.minecraft.network.PacketSplitter;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.game.PacketPlayOutKickDisconnect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.LazyInitVar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerConnection {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final LazyInitVar<NioEventLoopGroup> SERVER_EVENT_GROUP = new LazyInitVar<>(() -> {
        return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Server IO #%d").setDaemon(true).build());
    });
    public static final LazyInitVar<EpollEventLoopGroup> SERVER_EPOLL_EVENT_GROUP = new LazyInitVar<>(() -> {
        return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Server IO #%d").setDaemon(true).build());
    });
    final MinecraftServer server;
    public volatile boolean running;
    private final List<ChannelFuture> channels = Collections.synchronizedList(Lists.newArrayList());
    final List<NetworkManager> connections = Collections.synchronizedList(Lists.newArrayList());

    public ServerConnection(MinecraftServer server) {
        this.server = server;
        this.running = true;
    }

    public void startTcpServerListener(@Nullable InetAddress address, int port) throws IOException {
        synchronized(this.channels) {
            Class<? extends ServerSocketChannel> class_;
            LazyInitVar<? extends EventLoopGroup> lazyLoadedValue;
            if (Epoll.isAvailable() && this.server.isEpollEnabled()) {
                class_ = EpollServerSocketChannel.class;
                lazyLoadedValue = SERVER_EPOLL_EVENT_GROUP;
                LOGGER.info("Using epoll channel type");
            } else {
                class_ = NioServerSocketChannel.class;
                lazyLoadedValue = SERVER_EVENT_GROUP;
                LOGGER.info("Using default channel type");
            }

            this.channels.add((new ServerBootstrap()).channel(class_).childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    try {
                        channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                    } catch (ChannelException var4) {
                    }

                    channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30)).addLast("legacy_query", new LegacyPingHandler(ServerConnection.this)).addLast("splitter", new PacketSplitter()).addLast("decoder", new PacketDecoder(EnumProtocolDirection.SERVERBOUND)).addLast("prepender", new PacketPrepender()).addLast("encoder", new PacketEncoder(EnumProtocolDirection.CLIENTBOUND));
                    int i = ServerConnection.this.server.getRateLimitPacketsPerSecond();
                    NetworkManager connection = (NetworkManager)(i > 0 ? new NetworkManagerServer(i) : new NetworkManager(EnumProtocolDirection.SERVERBOUND));
                    ServerConnection.this.connections.add(connection);
                    channel.pipeline().addLast("packet_handler", connection);
                    connection.setPacketListener(new HandshakeListener(ServerConnection.this.server, connection));
                }
            }).group(lazyLoadedValue.get()).localAddress(address, port).bind().syncUninterruptibly());
        }
    }

    public SocketAddress startMemoryChannel() {
        ChannelFuture channelFuture;
        synchronized(this.channels) {
            channelFuture = (new ServerBootstrap()).channel(LocalServerChannel.class).childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    NetworkManager connection = new NetworkManager(EnumProtocolDirection.SERVERBOUND);
                    connection.setPacketListener(new HandshakeMemoryListener(ServerConnection.this.server, connection));
                    ServerConnection.this.connections.add(connection);
                    channel.pipeline().addLast("packet_handler", connection);
                }
            }).group(SERVER_EVENT_GROUP.get()).localAddress(LocalAddress.ANY).bind().syncUninterruptibly();
            this.channels.add(channelFuture);
        }

        return channelFuture.channel().localAddress();
    }

    public void stop() {
        this.running = false;

        for(ChannelFuture channelFuture : this.channels) {
            try {
                channelFuture.channel().close().sync();
            } catch (InterruptedException var4) {
                LOGGER.error("Interrupted whilst closing channel");
            }
        }

    }

    public void tick() {
        synchronized(this.connections) {
            Iterator<NetworkManager> iterator = this.connections.iterator();

            while(iterator.hasNext()) {
                NetworkManager connection = iterator.next();
                if (!connection.isConnecting()) {
                    if (connection.isConnected()) {
                        try {
                            connection.tick();
                        } catch (Exception var7) {
                            if (connection.isLocal()) {
                                throw new ReportedException(CrashReport.forThrowable(var7, "Ticking memory connection"));
                            }

                            LOGGER.warn("Failed to handle packet for {}", connection.getSocketAddress(), var7);
                            IChatBaseComponent component = new ChatComponentText("Internal server error");
                            connection.send(new PacketPlayOutKickDisconnect(component), (future) -> {
                                connection.close(component);
                            });
                            connection.stopReading();
                        }
                    } else {
                        iterator.remove();
                        connection.handleDisconnection();
                    }
                }
            }

        }
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    static class LatencySimulator extends ChannelInboundHandlerAdapter {
        private static final Timer TIMER = new HashedWheelTimer();
        private final int delay;
        private final int jitter;
        private final List<ServerConnection.LatencySimulator.DelayedMessage> queuedMessages = Lists.newArrayList();

        public LatencySimulator(int baseDelay, int extraDelay) {
            this.delay = baseDelay;
            this.jitter = extraDelay;
        }

        @Override
        public void channelRead(ChannelHandlerContext channelHandlerContext, Object object) {
            this.delayDownstream(channelHandlerContext, object);
        }

        private void delayDownstream(ChannelHandlerContext ctx, Object msg) {
            int i = this.delay + (int)(Math.random() * (double)this.jitter);
            this.queuedMessages.add(new ServerConnection.LatencySimulator.DelayedMessage(ctx, msg));
            TIMER.newTimeout(this::onTimeout, (long)i, TimeUnit.MILLISECONDS);
        }

        private void onTimeout(Timeout timeout) {
            ServerConnection.LatencySimulator.DelayedMessage delayedMessage = this.queuedMessages.remove(0);
            delayedMessage.ctx.fireChannelRead(delayedMessage.msg);
        }

        static class DelayedMessage {
            public final ChannelHandlerContext ctx;
            public final Object msg;

            public DelayedMessage(ChannelHandlerContext context, Object message) {
                this.ctx = context;
                this.msg = message;
            }
        }
    }
}
