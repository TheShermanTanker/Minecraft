package net.minecraft.core;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import java.util.stream.IntStream;
import javax.annotation.concurrent.Immutable;
import net.minecraft.SystemUtils;
import net.minecraft.util.MathHelper;

@Immutable
public class BaseBlockPosition implements Comparable<BaseBlockPosition> {
    public static final Codec<BaseBlockPosition> CODEC = Codec.INT_STREAM.comapFlatMap((intStream) -> {
        return SystemUtils.fixedSize(intStream, 3).map((is) -> {
            return new BaseBlockPosition(is[0], is[1], is[2]);
        });
    }, (vec3i) -> {
        return IntStream.of(vec3i.getX(), vec3i.getY(), vec3i.getZ());
    });
    public static final BaseBlockPosition ZERO = new BaseBlockPosition(0, 0, 0);
    private int x;
    private int y;
    private int z;

    public BaseBlockPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BaseBlockPosition(double x, double y, double z) {
        this(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof BaseBlockPosition)) {
            return false;
        } else {
            BaseBlockPosition vec3i = (BaseBlockPosition)object;
            if (this.getX() != vec3i.getX()) {
                return false;
            } else if (this.getY() != vec3i.getY()) {
                return false;
            } else {
                return this.getZ() == vec3i.getZ();
            }
        }
    }

    @Override
    public int hashCode() {
        return (this.getY() + this.getZ() * 31) * 31 + this.getX();
    }

    @Override
    public int compareTo(BaseBlockPosition vec3i) {
        if (this.getY() == vec3i.getY()) {
            return this.getZ() == vec3i.getZ() ? this.getX() - vec3i.getX() : this.getZ() - vec3i.getZ();
        } else {
            return this.getY() - vec3i.getY();
        }
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    protected BaseBlockPosition setX(int x) {
        this.x = x;
        return this;
    }

    protected BaseBlockPosition setY(int y) {
        this.y = y;
        return this;
    }

    protected BaseBlockPosition setZ(int z) {
        this.z = z;
        return this;
    }

    public BaseBlockPosition offset(double x, double y, double z) {
        return x == 0.0D && y == 0.0D && z == 0.0D ? this : new BaseBlockPosition((double)this.getX() + x, (double)this.getY() + y, (double)this.getZ() + z);
    }

    public BaseBlockPosition offset(int x, int y, int z) {
        return x == 0 && y == 0 && z == 0 ? this : new BaseBlockPosition(this.getX() + x, this.getY() + y, this.getZ() + z);
    }

    public BaseBlockPosition offset(BaseBlockPosition vec) {
        return this.offset(vec.getX(), vec.getY(), vec.getZ());
    }

    public BaseBlockPosition subtract(BaseBlockPosition vec) {
        return this.offset(-vec.getX(), -vec.getY(), -vec.getZ());
    }

    public BaseBlockPosition multiply(int scale) {
        if (scale == 1) {
            return this;
        } else {
            return scale == 0 ? ZERO : new BaseBlockPosition(this.getX() * scale, this.getY() * scale, this.getZ() * scale);
        }
    }

    public BaseBlockPosition up() {
        return this.up(1);
    }

    public BaseBlockPosition up(int distance) {
        return this.shift(EnumDirection.UP, distance);
    }

    public BaseBlockPosition down() {
        return this.down(1);
    }

    public BaseBlockPosition down(int distance) {
        return this.shift(EnumDirection.DOWN, distance);
    }

    public BaseBlockPosition north() {
        return this.north(1);
    }

    public BaseBlockPosition north(int distance) {
        return this.shift(EnumDirection.NORTH, distance);
    }

    public BaseBlockPosition south() {
        return this.south(1);
    }

    public BaseBlockPosition south(int distance) {
        return this.shift(EnumDirection.SOUTH, distance);
    }

    public BaseBlockPosition west() {
        return this.west(1);
    }

    public BaseBlockPosition west(int distance) {
        return this.shift(EnumDirection.WEST, distance);
    }

    public BaseBlockPosition east() {
        return this.east(1);
    }

    public BaseBlockPosition east(int distance) {
        return this.shift(EnumDirection.EAST, distance);
    }

    public BaseBlockPosition shift(EnumDirection direction) {
        return this.shift(direction, 1);
    }

    public BaseBlockPosition shift(EnumDirection direction, int distance) {
        return distance == 0 ? this : new BaseBlockPosition(this.getX() + direction.getAdjacentX() * distance, this.getY() + direction.getAdjacentY() * distance, this.getZ() + direction.getAdjacentZ() * distance);
    }

    public BaseBlockPosition relative(EnumDirection.EnumAxis axis, int distance) {
        if (distance == 0) {
            return this;
        } else {
            int i = axis == EnumDirection.EnumAxis.X ? distance : 0;
            int j = axis == EnumDirection.EnumAxis.Y ? distance : 0;
            int k = axis == EnumDirection.EnumAxis.Z ? distance : 0;
            return new BaseBlockPosition(this.getX() + i, this.getY() + j, this.getZ() + k);
        }
    }

    public BaseBlockPosition cross(BaseBlockPosition vec) {
        return new BaseBlockPosition(this.getY() * vec.getZ() - this.getZ() * vec.getY(), this.getZ() * vec.getX() - this.getX() * vec.getZ(), this.getX() * vec.getY() - this.getY() * vec.getX());
    }

    public boolean closerThan(BaseBlockPosition vec, double distance) {
        return this.distanceSquared((double)vec.getX(), (double)vec.getY(), (double)vec.getZ(), false) < distance * distance;
    }

    public boolean closerThan(IPosition pos, double distance) {
        return this.distanceSquared(pos.getX(), pos.getY(), pos.getZ(), true) < distance * distance;
    }

    public double distSqr(BaseBlockPosition vec) {
        return this.distanceSquared((double)vec.getX(), (double)vec.getY(), (double)vec.getZ(), true);
    }

    public double distSqr(IPosition pos, boolean treatAsBlockPos) {
        return this.distanceSquared(pos.getX(), pos.getY(), pos.getZ(), treatAsBlockPos);
    }

    public double distSqr(BaseBlockPosition vec, boolean treatAsBlockPos) {
        return this.distanceSquared((double)vec.x, (double)vec.y, (double)vec.z, treatAsBlockPos);
    }

    public double distanceSquared(double x, double y, double z, boolean treatAsBlockPos) {
        double d = treatAsBlockPos ? 0.5D : 0.0D;
        double e = (double)this.getX() + d - x;
        double f = (double)this.getY() + d - y;
        double g = (double)this.getZ() + d - z;
        return e * e + f * f + g * g;
    }

    public int distManhattan(BaseBlockPosition vec) {
        float f = (float)Math.abs(vec.getX() - this.getX());
        float g = (float)Math.abs(vec.getY() - this.getY());
        float h = (float)Math.abs(vec.getZ() - this.getZ());
        return (int)(f + g + h);
    }

    public int get(EnumDirection.EnumAxis axis) {
        return axis.choose(this.x, this.y, this.z);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("x", this.getX()).add("y", this.getY()).add("z", this.getZ()).toString();
    }

    public String toShortString() {
        return this.getX() + ", " + this.getY() + ", " + this.getZ();
    }
}
