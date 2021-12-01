package net.minecraft.world.level.pathfinder;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.phys.Vec3D;

public class PathPoint {
    public final int x;
    public final int y;
    public final int z;
    private final int hash;
    public int heapIdx = -1;
    public float g;
    public float h;
    public float f;
    @Nullable
    public PathPoint cameFrom;
    public boolean closed;
    public float walkedDistance;
    public float costMalus;
    public PathType type = PathType.BLOCKED;

    public PathPoint(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.hash = createHash(x, y, z);
    }

    public PathPoint cloneAndMove(int x, int y, int z) {
        PathPoint node = new PathPoint(x, y, z);
        node.heapIdx = this.heapIdx;
        node.g = this.g;
        node.h = this.h;
        node.f = this.f;
        node.cameFrom = this.cameFrom;
        node.closed = this.closed;
        node.walkedDistance = this.walkedDistance;
        node.costMalus = this.costMalus;
        node.type = this.type;
        return node;
    }

    public static int createHash(int x, int y, int z) {
        return y & 255 | (x & 32767) << 8 | (z & 32767) << 24 | (x < 0 ? Integer.MIN_VALUE : 0) | (z < 0 ? '\u8000' : 0);
    }

    public float distanceTo(PathPoint node) {
        float f = (float)(node.x - this.x);
        float g = (float)(node.y - this.y);
        float h = (float)(node.z - this.z);
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    public float distanceTo(BlockPosition pos) {
        float f = (float)(pos.getX() - this.x);
        float g = (float)(pos.getY() - this.y);
        float h = (float)(pos.getZ() - this.z);
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    public float distanceToSqr(PathPoint node) {
        float f = (float)(node.x - this.x);
        float g = (float)(node.y - this.y);
        float h = (float)(node.z - this.z);
        return f * f + g * g + h * h;
    }

    public float distanceToSqr(BlockPosition pos) {
        float f = (float)(pos.getX() - this.x);
        float g = (float)(pos.getY() - this.y);
        float h = (float)(pos.getZ() - this.z);
        return f * f + g * g + h * h;
    }

    public float distanceManhattan(PathPoint node) {
        float f = (float)Math.abs(node.x - this.x);
        float g = (float)Math.abs(node.y - this.y);
        float h = (float)Math.abs(node.z - this.z);
        return f + g + h;
    }

    public float distanceManhattan(BlockPosition pos) {
        float f = (float)Math.abs(pos.getX() - this.x);
        float g = (float)Math.abs(pos.getY() - this.y);
        float h = (float)Math.abs(pos.getZ() - this.z);
        return f + g + h;
    }

    public BlockPosition asBlockPos() {
        return new BlockPosition(this.x, this.y, this.z);
    }

    public Vec3D asVec3() {
        return new Vec3D((double)this.x, (double)this.y, (double)this.z);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PathPoint)) {
            return false;
        } else {
            PathPoint node = (PathPoint)object;
            return this.hash == node.hash && this.x == node.x && this.y == node.y && this.z == node.z;
        }
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    public boolean inOpenSet() {
        return this.heapIdx >= 0;
    }

    @Override
    public String toString() {
        return "Node{x=" + this.x + ", y=" + this.y + ", z=" + this.z + "}";
    }

    public void writeToStream(PacketDataSerializer buffer) {
        buffer.writeInt(this.x);
        buffer.writeInt(this.y);
        buffer.writeInt(this.z);
        buffer.writeFloat(this.walkedDistance);
        buffer.writeFloat(this.costMalus);
        buffer.writeBoolean(this.closed);
        buffer.writeInt(this.type.ordinal());
        buffer.writeFloat(this.f);
    }

    public static PathPoint createFromStream(PacketDataSerializer buf) {
        PathPoint node = new PathPoint(buf.readInt(), buf.readInt(), buf.readInt());
        node.walkedDistance = buf.readFloat();
        node.costMalus = buf.readFloat();
        node.closed = buf.readBoolean();
        node.type = PathType.values()[buf.readInt()];
        node.f = buf.readFloat();
        return node;
    }
}
