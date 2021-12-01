package net.minecraft.network.protocol.game;

import java.util.BitSet;
import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.lighting.LightEngine;

public class PacketPlayOutLightUpdate implements Packet<PacketListenerPlayOut> {
    private final int x;
    private final int z;
    private final ClientboundLightUpdatePacketData lightData;

    public PacketPlayOutLightUpdate(ChunkCoordIntPair chunkPos, LightEngine lightProvider, @Nullable BitSet skyBits, @Nullable BitSet blockBits, boolean nonEdge) {
        this.x = chunkPos.x;
        this.z = chunkPos.z;
        this.lightData = new ClientboundLightUpdatePacketData(chunkPos, lightProvider, skyBits, blockBits, nonEdge);
    }

    public PacketPlayOutLightUpdate(PacketDataSerializer buf) {
        this.x = buf.readVarInt();
        this.z = buf.readVarInt();
        this.lightData = new ClientboundLightUpdatePacketData(buf, this.x, this.z);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.x);
        buf.writeVarInt(this.z);
        this.lightData.write(buf);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleLightUpdatePacket(this);
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public ClientboundLightUpdatePacketData getLightData() {
        return this.lightData;
    }
}
