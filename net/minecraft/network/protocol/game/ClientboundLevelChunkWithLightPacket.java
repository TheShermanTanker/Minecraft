package net.minecraft.network.protocol.game;

import java.util.BitSet;
import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.lighting.LightEngine;

public class ClientboundLevelChunkWithLightPacket implements Packet<PacketListenerPlayOut> {
    private final int x;
    private final int z;
    private final ClientboundLevelChunkPacketData chunkData;
    private final ClientboundLightUpdatePacketData lightData;

    public ClientboundLevelChunkWithLightPacket(Chunk chunk, LightEngine lightProvider, @Nullable BitSet skyBits, @Nullable BitSet blockBits, boolean nonEdge) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        this.x = chunkPos.x;
        this.z = chunkPos.z;
        this.chunkData = new ClientboundLevelChunkPacketData(chunk);
        this.lightData = new ClientboundLightUpdatePacketData(chunkPos, lightProvider, skyBits, blockBits, nonEdge);
    }

    public ClientboundLevelChunkWithLightPacket(PacketDataSerializer buf) {
        this.x = buf.readInt();
        this.z = buf.readInt();
        this.chunkData = new ClientboundLevelChunkPacketData(buf, this.x, this.z);
        this.lightData = new ClientboundLightUpdatePacketData(buf, this.x, this.z);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeInt(this.x);
        buf.writeInt(this.z);
        this.chunkData.write(buf);
        this.lightData.write(buf);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleLevelChunkWithLight(this);
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public ClientboundLevelChunkPacketData getChunkData() {
        return this.chunkData;
    }

    public ClientboundLightUpdatePacketData getLightData() {
        return this.lightData;
    }
}
