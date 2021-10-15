package net.minecraft.world.level;

import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;

public class ChunkCoordIntPair {
    public static final long INVALID_CHUNK_POS = pair(1875016, 1875016);
    private static final long COORD_BITS = 32L;
    private static final long COORD_MASK = 4294967295L;
    private static final int REGION_BITS = 5;
    private static final int REGION_MASK = 31;
    public final int x;
    public final int z;
    private static final int HASH_A = 1664525;
    private static final int HASH_C = 1013904223;
    private static final int HASH_Z_XOR = -559038737;

    public ChunkCoordIntPair(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public ChunkCoordIntPair(BlockPosition pos) {
        this.x = SectionPosition.blockToSectionCoord(pos.getX());
        this.z = SectionPosition.blockToSectionCoord(pos.getZ());
    }

    public ChunkCoordIntPair(long pos) {
        this.x = (int)pos;
        this.z = (int)(pos >> 32);
    }

    public long pair() {
        return pair(this.x, this.z);
    }

    public static long pair(int chunkX, int chunkZ) {
        return (long)chunkX & 4294967295L | ((long)chunkZ & 4294967295L) << 32;
    }

    public static long asLong(BlockPosition blockPos) {
        return pair(SectionPosition.blockToSectionCoord(blockPos.getX()), SectionPosition.blockToSectionCoord(blockPos.getZ()));
    }

    public static int getX(long pos) {
        return (int)(pos & 4294967295L);
    }

    public static int getZ(long pos) {
        return (int)(pos >>> 32 & 4294967295L);
    }

    @Override
    public int hashCode() {
        int i = 1664525 * this.x + 1013904223;
        int j = 1664525 * (this.z ^ -559038737) + 1013904223;
        return i ^ j;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ChunkCoordIntPair)) {
            return false;
        } else {
            ChunkCoordIntPair chunkPos = (ChunkCoordIntPair)object;
            return this.x == chunkPos.x && this.z == chunkPos.z;
        }
    }

    public int getMiddleBlockX() {
        return this.getBlockX(8);
    }

    public int getMiddleBlockZ() {
        return this.getBlockZ(8);
    }

    public int getMinBlockX() {
        return SectionPosition.sectionToBlockCoord(this.x);
    }

    public int getMinBlockZ() {
        return SectionPosition.sectionToBlockCoord(this.z);
    }

    public int getMaxBlockX() {
        return this.getBlockX(15);
    }

    public int getMaxBlockZ() {
        return this.getBlockZ(15);
    }

    public int getRegionX() {
        return this.x >> 5;
    }

    public int getRegionZ() {
        return this.z >> 5;
    }

    public int getRegionLocalX() {
        return this.x & 31;
    }

    public int getRegionLocalZ() {
        return this.z & 31;
    }

    public BlockPosition getBlockAt(int offsetX, int y, int offsetZ) {
        return new BlockPosition(this.getBlockX(offsetX), y, this.getBlockZ(offsetZ));
    }

    public int getBlockX(int offset) {
        return SectionPosition.sectionToBlockCoord(this.x, offset);
    }

    public int getBlockZ(int offset) {
        return SectionPosition.sectionToBlockCoord(this.z, offset);
    }

    public BlockPosition getMiddleBlockPosition(int y) {
        return new BlockPosition(this.getMiddleBlockX(), y, this.getMiddleBlockZ());
    }

    @Override
    public String toString() {
        return "[" + this.x + ", " + this.z + "]";
    }

    public BlockPosition getWorldPosition() {
        return new BlockPosition(this.getMinBlockX(), 0, this.getMinBlockZ());
    }

    public int getChessboardDistance(ChunkCoordIntPair pos) {
        return Math.max(Math.abs(this.x - pos.x), Math.abs(this.z - pos.z));
    }

    public static Stream<ChunkCoordIntPair> rangeClosed(ChunkCoordIntPair center, int radius) {
        return rangeClosed(new ChunkCoordIntPair(center.x - radius, center.z - radius), new ChunkCoordIntPair(center.x + radius, center.z + radius));
    }

    public static Stream<ChunkCoordIntPair> rangeClosed(ChunkCoordIntPair pos1, ChunkCoordIntPair pos2) {
        int i = Math.abs(pos1.x - pos2.x) + 1;
        int j = Math.abs(pos1.z - pos2.z) + 1;
        final int k = pos1.x < pos2.x ? 1 : -1;
        final int l = pos1.z < pos2.z ? 1 : -1;
        return StreamSupport.stream(new AbstractSpliterator<ChunkCoordIntPair>((long)(i * j), 64) {
            @Nullable
            private ChunkCoordIntPair pos;

            @Override
            public boolean tryAdvance(Consumer<? super ChunkCoordIntPair> consumer) {
                if (this.pos == null) {
                    this.pos = pos1;
                } else {
                    int i = this.pos.x;
                    int j = this.pos.z;
                    if (i == pos2.x) {
                        if (j == pos2.z) {
                            return false;
                        }

                        this.pos = new ChunkCoordIntPair(pos1.x, j + l);
                    } else {
                        this.pos = new ChunkCoordIntPair(i + k, j);
                    }
                }

                consumer.accept(this.pos);
                return true;
            }
        }, false);
    }
}
