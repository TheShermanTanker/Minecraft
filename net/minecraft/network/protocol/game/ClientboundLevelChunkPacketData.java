package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.levelgen.HeightMap;

public class ClientboundLevelChunkPacketData {
    private static final int TWO_MEGABYTES = 2097152;
    private final NBTTagCompound heightmaps;
    private final byte[] buffer;
    private final List<ClientboundLevelChunkPacketData.BlockEntityInfo> blockEntitiesData;

    public ClientboundLevelChunkPacketData(Chunk chunk) {
        this.heightmaps = new NBTTagCompound();

        for(Entry<HeightMap.Type, HeightMap> entry : chunk.getHeightmaps()) {
            if (entry.getKey().sendToClient()) {
                this.heightmaps.set(entry.getKey().getSerializationKey(), new NBTTagLongArray(entry.getValue().getRawData()));
            }
        }

        this.buffer = new byte[calculateChunkSize(chunk)];
        extractChunkData(new PacketDataSerializer(this.getWriteBuffer()), chunk);
        this.blockEntitiesData = Lists.newArrayList();

        for(Entry<BlockPosition, TileEntity> entry2 : chunk.getTileEntities().entrySet()) {
            this.blockEntitiesData.add(ClientboundLevelChunkPacketData.BlockEntityInfo.create(entry2.getValue()));
        }

    }

    public ClientboundLevelChunkPacketData(PacketDataSerializer buf, int x, int z) {
        this.heightmaps = buf.readNbt();
        if (this.heightmaps == null) {
            throw new RuntimeException("Can't read heightmap in packet for [" + x + ", " + z + "]");
        } else {
            int i = buf.readVarInt();
            if (i > 2097152) {
                throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
            } else {
                this.buffer = new byte[i];
                buf.readBytes(this.buffer);
                this.blockEntitiesData = buf.readList(ClientboundLevelChunkPacketData.BlockEntityInfo::new);
            }
        }
    }

    public void write(PacketDataSerializer buf) {
        buf.writeNbt(this.heightmaps);
        buf.writeVarInt(this.buffer.length);
        buf.writeBytes(this.buffer);
        buf.writeCollection(this.blockEntitiesData, (bufx, entry) -> {
            entry.write(bufx);
        });
    }

    private static int calculateChunkSize(Chunk chunk) {
        int i = 0;

        for(ChunkSection levelChunkSection : chunk.getSections()) {
            i += levelChunkSection.getSerializedSize();
        }

        return i;
    }

    private ByteBuf getWriteBuffer() {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(this.buffer);
        byteBuf.writerIndex(0);
        return byteBuf;
    }

    public static void extractChunkData(PacketDataSerializer buf, Chunk chunk) {
        for(ChunkSection levelChunkSection : chunk.getSections()) {
            levelChunkSection.write(buf);
        }

    }

    public Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> getBlockEntitiesTagsConsumer(int x, int z) {
        return (visitor) -> {
            this.getBlockEntitiesTags(visitor, x, z);
        };
    }

    private void getBlockEntitiesTags(ClientboundLevelChunkPacketData.BlockEntityTagOutput consumer, int x, int z) {
        int i = 16 * x;
        int j = 16 * z;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(ClientboundLevelChunkPacketData.BlockEntityInfo blockEntityInfo : this.blockEntitiesData) {
            int k = i + SectionPosition.sectionRelative(blockEntityInfo.packedXZ >> 4);
            int l = j + SectionPosition.sectionRelative(blockEntityInfo.packedXZ);
            mutableBlockPos.set(k, blockEntityInfo.y, l);
            consumer.accept(mutableBlockPos, blockEntityInfo.type, blockEntityInfo.tag);
        }

    }

    public PacketDataSerializer getReadBuffer() {
        return new PacketDataSerializer(Unpooled.wrappedBuffer(this.buffer));
    }

    public NBTTagCompound getHeightmaps() {
        return this.heightmaps;
    }

    static class BlockEntityInfo {
        final int packedXZ;
        final int y;
        final TileEntityTypes<?> type;
        @Nullable
        final NBTTagCompound tag;

        private BlockEntityInfo(int localXz, int y, TileEntityTypes<?> type, @Nullable NBTTagCompound nbt) {
            this.packedXZ = localXz;
            this.y = y;
            this.type = type;
            this.tag = nbt;
        }

        private BlockEntityInfo(PacketDataSerializer buf) {
            this.packedXZ = buf.readByte();
            this.y = buf.readShort();
            int i = buf.readVarInt();
            this.type = IRegistry.BLOCK_ENTITY_TYPE.fromId(i);
            this.tag = buf.readNbt();
        }

        void write(PacketDataSerializer buf) {
            buf.writeByte(this.packedXZ);
            buf.writeShort(this.y);
            buf.writeVarInt(IRegistry.BLOCK_ENTITY_TYPE.getId(this.type));
            buf.writeNbt(this.tag);
        }

        static ClientboundLevelChunkPacketData.BlockEntityInfo create(TileEntity blockEntity) {
            NBTTagCompound compoundTag = blockEntity.getUpdateTag();
            BlockPosition blockPos = blockEntity.getPosition();
            int i = SectionPosition.sectionRelative(blockPos.getX()) << 4 | SectionPosition.sectionRelative(blockPos.getZ());
            return new ClientboundLevelChunkPacketData.BlockEntityInfo(i, blockPos.getY(), blockEntity.getTileType(), compoundTag.isEmpty() ? null : compoundTag);
        }
    }

    @FunctionalInterface
    public interface BlockEntityTagOutput {
        void accept(BlockPosition pos, TileEntityTypes<?> type, @Nullable NBTTagCompound nbt);
    }
}
