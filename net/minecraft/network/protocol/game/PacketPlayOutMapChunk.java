package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.chunk.BiomeStorage;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.levelgen.HeightMap;

public class PacketPlayOutMapChunk implements Packet<PacketListenerPlayOut> {
    public static final int TWO_MEGABYTES = 2097152;
    private final int x;
    private final int z;
    private final BitSet availableSections;
    private final NBTTagCompound heightmaps;
    private final int[] biomes;
    private final byte[] buffer;
    private final List<NBTTagCompound> blockEntitiesTags;

    public PacketPlayOutMapChunk(Chunk chunk) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        this.x = chunkPos.x;
        this.z = chunkPos.z;
        this.heightmaps = new NBTTagCompound();

        for(Entry<HeightMap.Type, HeightMap> entry : chunk.getHeightmaps()) {
            if (entry.getKey().sendToClient()) {
                this.heightmaps.set(entry.getKey().getSerializationKey(), new NBTTagLongArray(entry.getValue().getRawData()));
            }
        }

        this.biomes = chunk.getBiomeIndex().writeBiomes();
        this.buffer = new byte[this.calculateChunkSize(chunk)];
        this.availableSections = this.extractChunkData(new PacketDataSerializer(this.getWriteBuffer()), chunk);
        this.blockEntitiesTags = Lists.newArrayList();

        for(Entry<BlockPosition, TileEntity> entry2 : chunk.getTileEntities().entrySet()) {
            TileEntity blockEntity = entry2.getValue();
            NBTTagCompound compoundTag = blockEntity.getUpdateTag();
            this.blockEntitiesTags.add(compoundTag);
        }

    }

    public PacketPlayOutMapChunk(PacketDataSerializer buf) {
        this.x = buf.readInt();
        this.z = buf.readInt();
        this.availableSections = buf.readBitSet();
        this.heightmaps = buf.readNbt();
        if (this.heightmaps == null) {
            throw new RuntimeException("Can't read heightmap in packet for [" + this.x + ", " + this.z + "]");
        } else {
            this.biomes = buf.readVarIntArray(BiomeStorage.MAX_SIZE);
            int i = buf.readVarInt();
            if (i > 2097152) {
                throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
            } else {
                this.buffer = new byte[i];
                buf.readBytes(this.buffer);
                this.blockEntitiesTags = buf.readList(PacketDataSerializer::readNbt);
            }
        }
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeInt(this.x);
        buf.writeInt(this.z);
        buf.writeBitSet(this.availableSections);
        buf.writeNbt(this.heightmaps);
        buf.writeVarIntArray(this.biomes);
        buf.writeVarInt(this.buffer.length);
        buf.writeBytes(this.buffer);
        buf.writeCollection(this.blockEntitiesTags, PacketDataSerializer::writeNbt);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleLevelChunk(this);
    }

    public PacketDataSerializer getReadBuffer() {
        return new PacketDataSerializer(Unpooled.wrappedBuffer(this.buffer));
    }

    private ByteBuf getWriteBuffer() {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(this.buffer);
        byteBuf.writerIndex(0);
        return byteBuf;
    }

    public BitSet extractChunkData(PacketDataSerializer buf, Chunk chunk) {
        BitSet bitSet = new BitSet();
        ChunkSection[] levelChunkSections = chunk.getSections();
        int i = 0;

        for(int j = levelChunkSections.length; i < j; ++i) {
            ChunkSection levelChunkSection = levelChunkSections[i];
            if (levelChunkSection != Chunk.EMPTY_SECTION && !levelChunkSection.isEmpty()) {
                bitSet.set(i);
                levelChunkSection.write(buf);
            }
        }

        return bitSet;
    }

    protected int calculateChunkSize(Chunk chunk) {
        int i = 0;
        ChunkSection[] levelChunkSections = chunk.getSections();
        int j = 0;

        for(int k = levelChunkSections.length; j < k; ++j) {
            ChunkSection levelChunkSection = levelChunkSections[j];
            if (levelChunkSection != Chunk.EMPTY_SECTION && !levelChunkSection.isEmpty()) {
                i += levelChunkSection.getSerializedSize();
            }
        }

        return i;
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public BitSet getAvailableSections() {
        return this.availableSections;
    }

    public NBTTagCompound getHeightmaps() {
        return this.heightmaps;
    }

    public List<NBTTagCompound> getBlockEntitiesTags() {
        return this.blockEntitiesTags;
    }

    public int[] getBiomes() {
        return this.biomes;
    }
}
