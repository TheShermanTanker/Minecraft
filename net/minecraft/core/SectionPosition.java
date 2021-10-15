package net.minecraft.core;

import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.chunk.IChunkAccess;

public class SectionPosition extends BaseBlockPosition {
    public static final int SECTION_BITS = 4;
    public static final int SECTION_SIZE = 16;
    private static final int SECTION_MASK = 15;
    public static final int SECTION_HALF_SIZE = 8;
    public static final int SECTION_MAX_INDEX = 15;
    private static final int PACKED_X_LENGTH = 22;
    private static final int PACKED_Y_LENGTH = 20;
    private static final int PACKED_Z_LENGTH = 22;
    private static final long PACKED_X_MASK = 4194303L;
    private static final long PACKED_Y_MASK = 1048575L;
    private static final long PACKED_Z_MASK = 4194303L;
    private static final int Y_OFFSET = 0;
    private static final int Z_OFFSET = 20;
    private static final int X_OFFSET = 42;
    private static final int RELATIVE_X_SHIFT = 8;
    private static final int RELATIVE_Y_SHIFT = 0;
    private static final int RELATIVE_Z_SHIFT = 4;

    SectionPosition(int x, int y, int z) {
        super(x, y, z);
    }

    public static SectionPosition of(int x, int y, int z) {
        return new SectionPosition(x, y, z);
    }

    public static SectionPosition of(BlockPosition pos) {
        return new SectionPosition(blockToSectionCoord(pos.getX()), blockToSectionCoord(pos.getY()), blockToSectionCoord(pos.getZ()));
    }

    public static SectionPosition of(ChunkCoordIntPair chunkPos, int y) {
        return new SectionPosition(chunkPos.x, y, chunkPos.z);
    }

    public static SectionPosition of(Entity entity) {
        return new SectionPosition(blockToSectionCoord(entity.getBlockX()), blockToSectionCoord(entity.getBlockY()), blockToSectionCoord(entity.getBlockZ()));
    }

    public static SectionPosition of(long packed) {
        return new SectionPosition(x(packed), y(packed), z(packed));
    }

    public static SectionPosition bottomOf(IChunkAccess chunk) {
        return of(chunk.getPos(), chunk.getMinSection());
    }

    public static long offset(long packed, EnumDirection direction) {
        return offset(packed, direction.getAdjacentX(), direction.getAdjacentY(), direction.getAdjacentZ());
    }

    public static long offset(long packed, int x, int y, int z) {
        return asLong(x(packed) + x, y(packed) + y, z(packed) + z);
    }

    public static int posToSectionCoord(double coord) {
        return blockToSectionCoord(MathHelper.floor(coord));
    }

    public static int blockToSectionCoord(int coord) {
        return coord >> 4;
    }

    public static int sectionRelative(int coord) {
        return coord & 15;
    }

    public static short sectionRelativePos(BlockPosition pos) {
        int i = sectionRelative(pos.getX());
        int j = sectionRelative(pos.getY());
        int k = sectionRelative(pos.getZ());
        return (short)(i << 8 | k << 4 | j << 0);
    }

    public static int sectionRelativeX(short packedLocalPos) {
        return packedLocalPos >>> 8 & 15;
    }

    public static int sectionRelativeY(short packedLocalPos) {
        return packedLocalPos >>> 0 & 15;
    }

    public static int sectionRelativeZ(short packedLocalPos) {
        return packedLocalPos >>> 4 & 15;
    }

    public int relativeToBlockX(short packedLocalPos) {
        return this.minBlockX() + sectionRelativeX(packedLocalPos);
    }

    public int relativeToBlockY(short packedLocalPos) {
        return this.minBlockY() + sectionRelativeY(packedLocalPos);
    }

    public int relativeToBlockZ(short packedLocalPos) {
        return this.minBlockZ() + sectionRelativeZ(packedLocalPos);
    }

    public BlockPosition relativeToBlockPos(short packedLocalPos) {
        return new BlockPosition(this.relativeToBlockX(packedLocalPos), this.relativeToBlockY(packedLocalPos), this.relativeToBlockZ(packedLocalPos));
    }

    public static int sectionToBlockCoord(int sectionCoord) {
        return sectionCoord << 4;
    }

    public static int sectionToBlockCoord(int chunkCoord, int offset) {
        return sectionToBlockCoord(chunkCoord) + offset;
    }

    public static int x(long packed) {
        return (int)(packed << 0 >> 42);
    }

    public static int y(long packed) {
        return (int)(packed << 44 >> 44);
    }

    public static int z(long packed) {
        return (int)(packed << 22 >> 42);
    }

    public int x() {
        return this.getX();
    }

    public int y() {
        return this.getY();
    }

    public int z() {
        return this.getZ();
    }

    public int minBlockX() {
        return sectionToBlockCoord(this.x());
    }

    public int minBlockY() {
        return sectionToBlockCoord(this.y());
    }

    public int minBlockZ() {
        return sectionToBlockCoord(this.z());
    }

    public int maxBlockX() {
        return sectionToBlockCoord(this.x(), 15);
    }

    public int maxBlockY() {
        return sectionToBlockCoord(this.y(), 15);
    }

    public int maxBlockZ() {
        return sectionToBlockCoord(this.z(), 15);
    }

    public static long blockToSection(long blockPos) {
        return asLong(blockToSectionCoord(BlockPosition.getX(blockPos)), blockToSectionCoord(BlockPosition.getY(blockPos)), blockToSectionCoord(BlockPosition.getZ(blockPos)));
    }

    public static long getZeroNode(long pos) {
        return pos & -1048576L;
    }

    public BlockPosition origin() {
        return new BlockPosition(sectionToBlockCoord(this.x()), sectionToBlockCoord(this.y()), sectionToBlockCoord(this.z()));
    }

    public BlockPosition center() {
        int i = 8;
        return this.origin().offset(8, 8, 8);
    }

    public ChunkCoordIntPair chunk() {
        return new ChunkCoordIntPair(this.x(), this.z());
    }

    public static long asLong(BlockPosition pos) {
        return asLong(blockToSectionCoord(pos.getX()), blockToSectionCoord(pos.getY()), blockToSectionCoord(pos.getZ()));
    }

    public static long asLong(int x, int y, int z) {
        long l = 0L;
        l = l | ((long)x & 4194303L) << 42;
        l = l | ((long)y & 1048575L) << 0;
        return l | ((long)z & 4194303L) << 20;
    }

    public long asLong() {
        return asLong(this.x(), this.y(), this.z());
    }

    @Override
    public SectionPosition offset(int i, int j, int k) {
        return i == 0 && j == 0 && k == 0 ? this : new SectionPosition(this.x() + i, this.y() + j, this.z() + k);
    }

    public Stream<BlockPosition> blocksInside() {
        return BlockPosition.betweenClosedStream(this.minBlockX(), this.minBlockY(), this.minBlockZ(), this.maxBlockX(), this.maxBlockY(), this.maxBlockZ());
    }

    public static Stream<SectionPosition> cube(SectionPosition center, int radius) {
        int i = center.x();
        int j = center.y();
        int k = center.z();
        return betweenClosedStream(i - radius, j - radius, k - radius, i + radius, j + radius, k + radius);
    }

    public static Stream<SectionPosition> aroundChunk(ChunkCoordIntPair center, int radius, int minY, int maxY) {
        int i = center.x;
        int j = center.z;
        return betweenClosedStream(i - radius, minY, j - radius, i + radius, maxY - 1, j + radius);
    }

    public static Stream<SectionPosition> betweenClosedStream(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return StreamSupport.stream(new AbstractSpliterator<SectionPosition>((long)((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1)), 64) {
            final CursorPosition cursor = new CursorPosition(minX, minY, minZ, maxX, maxY, maxZ);

            @Override
            public boolean tryAdvance(Consumer<? super SectionPosition> consumer) {
                if (this.cursor.advance()) {
                    consumer.accept(new SectionPosition(this.cursor.nextX(), this.cursor.nextY(), this.cursor.nextZ()));
                    return true;
                } else {
                    return false;
                }
            }
        }, false);
    }
}
