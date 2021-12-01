package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.IOException;
import java.util.List;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class PacketDecoder extends ByteToMessageDecoder {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker MARKER = MarkerManager.getMarker("PACKET_RECEIVED", NetworkManager.PACKET_MARKER);
    private final EnumProtocolDirection flow;

    public PacketDecoder(EnumProtocolDirection side) {
        this.flow = side;
    }

    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        int i = byteBuf.readableBytes();
        if (i != 0) {
            PacketDataSerializer friendlyByteBuf = new PacketDataSerializer(byteBuf);
            int j = friendlyByteBuf.readVarInt();
            Packet<?> packet = channelHandlerContext.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get().createPacket(this.flow, j, friendlyByteBuf);
            if (packet == null) {
                throw new IOException("Bad packet id " + j);
            } else {
                int k = channelHandlerContext.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get().getId();
                JvmProfiler.INSTANCE.onPacketReceived(k, j, channelHandlerContext.channel().remoteAddress(), i);
                if (friendlyByteBuf.readableBytes() > 0) {
                    throw new IOException("Packet " + channelHandlerContext.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get().getId() + "/" + j + " (" + packet.getClass().getSimpleName() + ") was larger than I expected, found " + friendlyByteBuf.readableBytes() + " bytes extra whilst reading packet " + j);
                } else {
                    list.add(packet);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(MARKER, " IN: [{}:{}] {}", channelHandlerContext.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get(), j, packet.getClass().getName());
                    }

                }
            }
        }
    }
}
