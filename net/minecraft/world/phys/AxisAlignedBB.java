package net.minecraft.world.phys;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;

public class AxisAlignedBB {
    private static final double EPSILON = 1.0E-7D;
    public final double minX;
    public final double minY;
    public final double minZ;
    public final double maxX;
    public final double maxY;
    public final double maxZ;

    public AxisAlignedBB(double x1, double y1, double z1, double x2, double y2, double z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public AxisAlignedBB(BlockPosition pos) {
        this((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1));
    }

    public AxisAlignedBB(BlockPosition pos1, BlockPosition pos2) {
        this((double)pos1.getX(), (double)pos1.getY(), (double)pos1.getZ(), (double)pos2.getX(), (double)pos2.getY(), (double)pos2.getZ());
    }

    public AxisAlignedBB(Vec3D pos1, Vec3D pos2) {
        this(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z);
    }

    public static AxisAlignedBB of(StructureBoundingBox mutable) {
        return new AxisAlignedBB((double)mutable.minX(), (double)mutable.minY(), (double)mutable.minZ(), (double)(mutable.maxX() + 1), (double)(mutable.maxY() + 1), (double)(mutable.maxZ() + 1));
    }

    public static AxisAlignedBB unitCubeFromLowerCorner(Vec3D pos) {
        return new AxisAlignedBB(pos.x, pos.y, pos.z, pos.x + 1.0D, pos.y + 1.0D, pos.z + 1.0D);
    }

    public AxisAlignedBB setMinX(double minX) {
        return new AxisAlignedBB(minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public AxisAlignedBB setMinY(double minY) {
        return new AxisAlignedBB(this.minX, minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public AxisAlignedBB setMinZ(double minZ) {
        return new AxisAlignedBB(this.minX, this.minY, minZ, this.maxX, this.maxY, this.maxZ);
    }

    public AxisAlignedBB setMaxX(double maxX) {
        return new AxisAlignedBB(this.minX, this.minY, this.minZ, maxX, this.maxY, this.maxZ);
    }

    public AxisAlignedBB setMaxY(double maxY) {
        return new AxisAlignedBB(this.minX, this.minY, this.minZ, this.maxX, maxY, this.maxZ);
    }

    public AxisAlignedBB setMaxZ(double maxZ) {
        return new AxisAlignedBB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, maxZ);
    }

    public double min(EnumDirection.EnumAxis axis) {
        return axis.choose(this.minX, this.minY, this.minZ);
    }

    public double max(EnumDirection.EnumAxis axis) {
        return axis.choose(this.maxX, this.maxY, this.maxZ);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof AxisAlignedBB)) {
            return false;
        } else {
            AxisAlignedBB aABB = (AxisAlignedBB)object;
            if (Double.compare(aABB.minX, this.minX) != 0) {
                return false;
            } else if (Double.compare(aABB.minY, this.minY) != 0) {
                return false;
            } else if (Double.compare(aABB.minZ, this.minZ) != 0) {
                return false;
            } else if (Double.compare(aABB.maxX, this.maxX) != 0) {
                return false;
            } else if (Double.compare(aABB.maxY, this.maxY) != 0) {
                return false;
            } else {
                return Double.compare(aABB.maxZ, this.maxZ) == 0;
            }
        }
    }

    @Override
    public int hashCode() {
        long l = Double.doubleToLongBits(this.minX);
        int i = (int)(l ^ l >>> 32);
        l = Double.doubleToLongBits(this.minY);
        i = 31 * i + (int)(l ^ l >>> 32);
        l = Double.doubleToLongBits(this.minZ);
        i = 31 * i + (int)(l ^ l >>> 32);
        l = Double.doubleToLongBits(this.maxX);
        i = 31 * i + (int)(l ^ l >>> 32);
        l = Double.doubleToLongBits(this.maxY);
        i = 31 * i + (int)(l ^ l >>> 32);
        l = Double.doubleToLongBits(this.maxZ);
        return 31 * i + (int)(l ^ l >>> 32);
    }

    public AxisAlignedBB contract(double x, double y, double z) {
        double d = this.minX;
        double e = this.minY;
        double f = this.minZ;
        double g = this.maxX;
        double h = this.maxY;
        double i = this.maxZ;
        if (x < 0.0D) {
            d -= x;
        } else if (x > 0.0D) {
            g -= x;
        }

        if (y < 0.0D) {
            e -= y;
        } else if (y > 0.0D) {
            h -= y;
        }

        if (z < 0.0D) {
            f -= z;
        } else if (z > 0.0D) {
            i -= z;
        }

        return new AxisAlignedBB(d, e, f, g, h, i);
    }

    public AxisAlignedBB expandTowards(Vec3D scale) {
        return this.expandTowards(scale.x, scale.y, scale.z);
    }

    public AxisAlignedBB expandTowards(double x, double y, double z) {
        double d = this.minX;
        double e = this.minY;
        double f = this.minZ;
        double g = this.maxX;
        double h = this.maxY;
        double i = this.maxZ;
        if (x < 0.0D) {
            d += x;
        } else if (x > 0.0D) {
            g += x;
        }

        if (y < 0.0D) {
            e += y;
        } else if (y > 0.0D) {
            h += y;
        }

        if (z < 0.0D) {
            f += z;
        } else if (z > 0.0D) {
            i += z;
        }

        return new AxisAlignedBB(d, e, f, g, h, i);
    }

    public AxisAlignedBB grow(double x, double y, double z) {
        double d = this.minX - x;
        double e = this.minY - y;
        double f = this.minZ - z;
        double g = this.maxX + x;
        double h = this.maxY + y;
        double i = this.maxZ + z;
        return new AxisAlignedBB(d, e, f, g, h, i);
    }

    public AxisAlignedBB inflate(double value) {
        return this.grow(value, value, value);
    }

    public AxisAlignedBB intersect(AxisAlignedBB box) {
        double d = Math.max(this.minX, box.minX);
        double e = Math.max(this.minY, box.minY);
        double f = Math.max(this.minZ, box.minZ);
        double g = Math.min(this.maxX, box.maxX);
        double h = Math.min(this.maxY, box.maxY);
        double i = Math.min(this.maxZ, box.maxZ);
        return new AxisAlignedBB(d, e, f, g, h, i);
    }

    public AxisAlignedBB minmax(AxisAlignedBB box) {
        double d = Math.min(this.minX, box.minX);
        double e = Math.min(this.minY, box.minY);
        double f = Math.min(this.minZ, box.minZ);
        double g = Math.max(this.maxX, box.maxX);
        double h = Math.max(this.maxY, box.maxY);
        double i = Math.max(this.maxZ, box.maxZ);
        return new AxisAlignedBB(d, e, f, g, h, i);
    }

    public AxisAlignedBB move(double x, double y, double z) {
        return new AxisAlignedBB(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    public AxisAlignedBB move(BlockPosition blockPos) {
        return new AxisAlignedBB(this.minX + (double)blockPos.getX(), this.minY + (double)blockPos.getY(), this.minZ + (double)blockPos.getZ(), this.maxX + (double)blockPos.getX(), this.maxY + (double)blockPos.getY(), this.maxZ + (double)blockPos.getZ());
    }

    public AxisAlignedBB move(Vec3D vec) {
        return this.move(vec.x, vec.y, vec.z);
    }

    public boolean intersects(AxisAlignedBB box) {
        return this.intersects(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.minX < maxX && this.maxX > minX && this.minY < maxY && this.maxY > minY && this.minZ < maxZ && this.maxZ > minZ;
    }

    public boolean intersects(Vec3D pos1, Vec3D pos2) {
        return this.intersects(Math.min(pos1.x, pos2.x), Math.min(pos1.y, pos2.y), Math.min(pos1.z, pos2.z), Math.max(pos1.x, pos2.x), Math.max(pos1.y, pos2.y), Math.max(pos1.z, pos2.z));
    }

    public boolean contains(Vec3D pos) {
        return this.contains(pos.x, pos.y, pos.z);
    }

    public boolean contains(double x, double y, double z) {
        return x >= this.minX && x < this.maxX && y >= this.minY && y < this.maxY && z >= this.minZ && z < this.maxZ;
    }

    public double getSize() {
        double d = this.getXsize();
        double e = this.getYsize();
        double f = this.getZsize();
        return (d + e + f) / 3.0D;
    }

    public double getXsize() {
        return this.maxX - this.minX;
    }

    public double getYsize() {
        return this.maxY - this.minY;
    }

    public double getZsize() {
        return this.maxZ - this.minZ;
    }

    public AxisAlignedBB deflate(double x, double y, double z) {
        return this.grow(-x, -y, -z);
    }

    public AxisAlignedBB shrink(double value) {
        return this.inflate(-value);
    }

    public Optional<Vec3D> clip(Vec3D min, Vec3D max) {
        double[] ds = new double[]{1.0D};
        double d = max.x - min.x;
        double e = max.y - min.y;
        double f = max.z - min.z;
        EnumDirection direction = getDirection(this, min, ds, (EnumDirection)null, d, e, f);
        if (direction == null) {
            return Optional.empty();
        } else {
            double g = ds[0];
            return Optional.of(min.add(g * d, g * e, g * f));
        }
    }

    @Nullable
    public static MovingObjectPositionBlock clip(Iterable<AxisAlignedBB> boxes, Vec3D from, Vec3D to, BlockPosition pos) {
        double[] ds = new double[]{1.0D};
        EnumDirection direction = null;
        double d = to.x - from.x;
        double e = to.y - from.y;
        double f = to.z - from.z;

        for(AxisAlignedBB aABB : boxes) {
            direction = getDirection(aABB.move(pos), from, ds, direction, d, e, f);
        }

        if (direction == null) {
            return null;
        } else {
            double g = ds[0];
            return new MovingObjectPositionBlock(from.add(g * d, g * e, g * f), direction, pos, false);
        }
    }

    @Nullable
    private static EnumDirection getDirection(AxisAlignedBB box, Vec3D intersectingVector, double[] traceDistanceResult, @Nullable EnumDirection approachDirection, double deltaX, double deltaY, double deltaZ) {
        if (deltaX > 1.0E-7D) {
            approachDirection = clipPoint(traceDistanceResult, approachDirection, deltaX, deltaY, deltaZ, box.minX, box.minY, box.maxY, box.minZ, box.maxZ, EnumDirection.WEST, intersectingVector.x, intersectingVector.y, intersectingVector.z);
        } else if (deltaX < -1.0E-7D) {
            approachDirection = clipPoint(traceDistanceResult, approachDirection, deltaX, deltaY, deltaZ, box.maxX, box.minY, box.maxY, box.minZ, box.maxZ, EnumDirection.EAST, intersectingVector.x, intersectingVector.y, intersectingVector.z);
        }

        if (deltaY > 1.0E-7D) {
            approachDirection = clipPoint(traceDistanceResult, approachDirection, deltaY, deltaZ, deltaX, box.minY, box.minZ, box.maxZ, box.minX, box.maxX, EnumDirection.DOWN, intersectingVector.y, intersectingVector.z, intersectingVector.x);
        } else if (deltaY < -1.0E-7D) {
            approachDirection = clipPoint(traceDistanceResult, approachDirection, deltaY, deltaZ, deltaX, box.maxY, box.minZ, box.maxZ, box.minX, box.maxX, EnumDirection.UP, intersectingVector.y, intersectingVector.z, intersectingVector.x);
        }

        if (deltaZ > 1.0E-7D) {
            approachDirection = clipPoint(traceDistanceResult, approachDirection, deltaZ, deltaX, deltaY, box.minZ, box.minX, box.maxX, box.minY, box.maxY, EnumDirection.NORTH, intersectingVector.z, intersectingVector.x, intersectingVector.y);
        } else if (deltaZ < -1.0E-7D) {
            approachDirection = clipPoint(traceDistanceResult, approachDirection, deltaZ, deltaX, deltaY, box.maxZ, box.minX, box.maxX, box.minY, box.maxY, EnumDirection.SOUTH, intersectingVector.z, intersectingVector.x, intersectingVector.y);
        }

        return approachDirection;
    }

    @Nullable
    private static EnumDirection clipPoint(double[] traceDistanceResult, @Nullable EnumDirection approachDirection, double deltaX, double deltaY, double deltaZ, double begin, double minX, double maxX, double minZ, double maxZ, EnumDirection resultDirection, double startX, double startY, double startZ) {
        double d = (begin - startX) / deltaX;
        double e = startY + d * deltaY;
        double f = startZ + d * deltaZ;
        if (0.0D < d && d < traceDistanceResult[0] && minX - 1.0E-7D < e && e < maxX + 1.0E-7D && minZ - 1.0E-7D < f && f < maxZ + 1.0E-7D) {
            traceDistanceResult[0] = d;
            return resultDirection;
        } else {
            return approachDirection;
        }
    }

    @Override
    public String toString() {
        return "AABB[" + this.minX + ", " + this.minY + ", " + this.minZ + "] -> [" + this.maxX + ", " + this.maxY + ", " + this.maxZ + "]";
    }

    public boolean hasNaN() {
        return Double.isNaN(this.minX) || Double.isNaN(this.minY) || Double.isNaN(this.minZ) || Double.isNaN(this.maxX) || Double.isNaN(this.maxY) || Double.isNaN(this.maxZ);
    }

    public Vec3D getCenter() {
        return new Vec3D(MathHelper.lerp(0.5D, this.minX, this.maxX), MathHelper.lerp(0.5D, this.minY, this.maxY), MathHelper.lerp(0.5D, this.minZ, this.maxZ));
    }

    public static AxisAlignedBB ofSize(Vec3D center, double dx, double dy, double dz) {
        return new AxisAlignedBB(center.x - dx / 2.0D, center.y - dy / 2.0D, center.z - dz / 2.0D, center.x + dx / 2.0D, center.y + dy / 2.0D, center.z + dz / 2.0D);
    }
}
