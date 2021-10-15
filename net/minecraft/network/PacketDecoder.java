package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.IOException;
import java.util.List;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.Packet;
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

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (byteBuf.readableBytes() != 0) {
            PacketDataSerializer friendlyByteBuf = new PacketDataSerializer(byteBuf);
            int i = friendlyByteBuf.readVarInt();
            Packet<?> packet = channelHandlerContext.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get().createPacket(this.flow, i, friendlyByteBuf);
            if (packet == null) {
                throw new IOException("Bad packet id " + i);
            } else if (friendlyByteBuf.readableBytes() > 0) {
                throw new IOException("Packet " + channelHandlerContext.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get().getId() + "/" + i + " (" + packet.getClass().getSimpleName() + ") was larger than I expected, found " + friendlyByteBuf.readableBytes() + " bytes extra whilst reading packet " + i);
            } else {
                list.add(packet);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(MARKER, " IN: [{}:{}] {}", channelHandlerContext.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get(), i, packet.getClass().getName());
                }

            }
        }
    }
}
