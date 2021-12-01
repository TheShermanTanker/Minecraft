package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToByteEncoder;

@Sharable
public class PacketPrepender extends MessageToByteEncoder<ByteBuf> {
    private static final int MAX_BYTES = 3;

    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, ByteBuf byteBuf2) {
        int i = byteBuf.readableBytes();
        int j = PacketDataSerializer.getVarIntSize(i);
        if (j > 3) {
            throw new IllegalArgumentException("unable to fit " + i + " into 3");
        } else {
            PacketDataSerializer friendlyByteBuf = new PacketDataSerializer(byteBuf2);
            friendlyByteBuf.ensureWritable(j + i);
            friendlyByteBuf.writeVarInt(i);
            friendlyByteBuf.writeBytes(byteBuf, byteBuf.readerIndex(), i);
        }
    }
}
