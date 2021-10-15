package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.chunk.NibbleArray;
import net.minecraft.world.level.lighting.LightEngine;

public class PacketPlayOutLightUpdate implements Packet<PacketListenerPlayOut> {
    private final int x;
    private final int z;
    private final BitSet skyYMask;
    private final BitSet blockYMask;
    private final BitSet emptySkyYMask;
    private final BitSet emptyBlockYMask;
    private final List<byte[]> skyUpdates;
    private final List<byte[]> blockUpdates;
    private final boolean trustEdges;

    public PacketPlayOutLightUpdate(ChunkCoordIntPair chunkPos, LightEngine lightProvider, @Nullable BitSet bitSet, @Nullable BitSet bitSet2, boolean nonEdge) {
        this.x = chunkPos.x;
        this.z = chunkPos.z;
        this.trustEdges = nonEdge;
        this.skyYMask = new BitSet();
        this.blockYMask = new BitSet();
        this.emptySkyYMask = new BitSet();
        this.emptyBlockYMask = new BitSet();
        this.skyUpdates = Lists.newArrayList();
        this.blockUpdates = Lists.newArrayList();

        for(int i = 0; i < lightProvider.getLightSectionCount(); ++i) {
            if (bitSet == null || bitSet.get(i)) {
                prepareSectionData(chunkPos, lightProvider, EnumSkyBlock.SKY, i, this.skyYMask, this.emptySkyYMask, this.skyUpdates);
            }

            if (bitSet2 == null || bitSet2.get(i)) {
                prepareSectionData(chunkPos, lightProvider, EnumSkyBlock.BLOCK, i, this.blockYMask, this.emptyBlockYMask, this.blockUpdates);
            }
        }

    }

    private static void prepareSectionData(ChunkCoordIntPair chunkPos, LightEngine lightProvider, EnumSkyBlock lightType, int i, BitSet bitSet, BitSet bitSet2, List<byte[]> list) {
        NibbleArray dataLayer = lightProvider.getLayerListener(lightType).getDataLayerData(SectionPosition.of(chunkPos, lightProvider.getMinLightSection() + i));
        if (dataLayer != null) {
            if (dataLayer.isEmpty()) {
                bitSet2.set(i);
            } else {
                bitSet.set(i);
                list.add((byte[])dataLayer.asBytes().clone());
            }
        }

    }

    public PacketPlayOutLightUpdate(PacketDataSerializer buf) {
        this.x = buf.readVarInt();
        this.z = buf.readVarInt();
        this.trustEdges = buf.readBoolean();
        this.skyYMask = buf.readBitSet();
        this.blockYMask = buf.readBitSet();
        this.emptySkyYMask = buf.readBitSet();
        this.emptyBlockYMask = buf.readBitSet();
        this.skyUpdates = buf.readList((friendlyByteBuf) -> {
            return friendlyByteBuf.readByteArray(2048);
        });
        this.blockUpdates = buf.readList((friendlyByteBuf) -> {
            return friendlyByteBuf.readByteArray(2048);
        });
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.x);
        buf.writeVarInt(this.z);
        buf.writeBoolean(this.trustEdges);
        buf.writeBitSet(this.skyYMask);
        buf.writeBitSet(this.blockYMask);
        buf.writeBitSet(this.emptySkyYMask);
        buf.writeBitSet(this.emptyBlockYMask);
        buf.writeCollection(this.skyUpdates, PacketDataSerializer::writeByteArray);
        buf.writeCollection(this.blockUpdates, PacketDataSerializer::writeByteArray);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleLightUpdatePacked(this);
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public BitSet getSkyYMask() {
        return this.skyYMask;
    }

    public BitSet getEmptySkyYMask() {
        return this.emptySkyYMask;
    }

    public List<byte[]> getSkyUpdates() {
        return this.skyUpdates;
    }

    public BitSet getBlockYMask() {
        return this.blockYMask;
    }

    public BitSet getEmptyBlockYMask() {
        return this.emptyBlockYMask;
    }

    public List<byte[]> getBlockUpdates() {
        return this.blockUpdates;
    }

    public boolean getTrustEdges() {
        return this.trustEdges;
    }
}
