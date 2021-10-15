package net.minecraft.server.level;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.ChunkCoordIntPair;

public class BlockPosition2D {
    private static final long COORD_BITS = 32L;
    private static final long COORD_MASK = 4294967295L;
    private static final int HASH_A = 1664525;
    private static final int HASH_C = 1013904223;
    private static final int HASH_Z_XOR = -559038737;
    public final int x;
    public final int z;

    public BlockPosition2D(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public BlockPosition2D(BlockPosition pos) {
        this.x = pos.getX();
        this.z = pos.getZ();
    }

    public ChunkCoordIntPair toChunkPos() {
        return new ChunkCoordIntPair(SectionPosition.blockToSectionCoord(this.x), SectionPosition.blockToSectionCoord(this.z));
    }

    public long toLong() {
        return asLong(this.x, this.z);
    }

    public static long asLong(int x, int z) {
        return (long)x & 4294967295L | ((long)z & 4294967295L) << 32;
    }

    @Override
    public String toString() {
        return "[" + this.x + ", " + this.z + "]";
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
        } else if (!(object instanceof BlockPosition2D)) {
            return false;
        } else {
            BlockPosition2D columnPos = (BlockPosition2D)object;
            return this.x == columnPos.x && this.z == columnPos.z;
        }
    }
}
