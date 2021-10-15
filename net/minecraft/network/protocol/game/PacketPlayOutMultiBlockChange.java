package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkSection;

public class PacketPlayOutMultiBlockChange implements Packet<PacketListenerPlayOut> {
    private static final int POS_IN_SECTION_BITS = 12;
    private final SectionPosition sectionPos;
    private final short[] positions;
    private final IBlockData[] states;
    private final boolean suppressLightUpdates;

    public PacketPlayOutMultiBlockChange(SectionPosition sectionPos, ShortSet positions, ChunkSection section, boolean noLightingUpdates) {
        this.sectionPos = sectionPos;
        this.suppressLightUpdates = noLightingUpdates;
        int i = positions.size();
        this.positions = new short[i];
        this.states = new IBlockData[i];
        int j = 0;

        for(short s : positions) {
            this.positions[j] = s;
            this.states[j] = section.getType(SectionPosition.sectionRelativeX(s), SectionPosition.sectionRelativeY(s), SectionPosition.sectionRelativeZ(s));
            ++j;
        }

    }

    public PacketPlayOutMultiBlockChange(PacketDataSerializer buf) {
        this.sectionPos = SectionPosition.of(buf.readLong());
        this.suppressLightUpdates = buf.readBoolean();
        int i = buf.readVarInt();
        this.positions = new short[i];
        this.states = new IBlockData[i];

        for(int j = 0; j < i; ++j) {
            long l = buf.readVarLong();
            this.positions[j] = (short)((int)(l & 4095L));
            this.states[j] = Block.BLOCK_STATE_REGISTRY.fromId((int)(l >>> 12));
        }

    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeLong(this.sectionPos.asLong());
        buf.writeBoolean(this.suppressLightUpdates);
        buf.writeVarInt(this.positions.length);

        for(int i = 0; i < this.positions.length; ++i) {
            buf.writeVarLong((long)(Block.getCombinedId(this.states[i]) << 12 | this.positions[i]));
        }

    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleChunkBlocksUpdate(this);
    }

    public void runUpdates(BiConsumer<BlockPosition, IBlockData> biConsumer) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int i = 0; i < this.positions.length; ++i) {
            short s = this.positions[i];
            mutableBlockPos.set(this.sectionPos.relativeToBlockX(s), this.sectionPos.relativeToBlockY(s), this.sectionPos.relativeToBlockZ(s));
            biConsumer.accept(mutableBlockPos, this.states[i]);
        }

    }

    public boolean shouldSuppressLightUpdates() {
        return this.suppressLightUpdates;
    }
}
