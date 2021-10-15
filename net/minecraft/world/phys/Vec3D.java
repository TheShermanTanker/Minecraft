package net.minecraft.world.phys;

import com.mojang.math.Vector3fa;
import java.util.EnumSet;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IPosition;
import net.minecraft.util.MathHelper;

public class Vec3D implements IPosition {
    public static final Vec3D ZERO = new Vec3D(0.0D, 0.0D, 0.0D);
    public final double x;
    public final double y;
    public final double z;

    public static Vec3D fromRGB24(int rgb) {
        double d = (double)(rgb >> 16 & 255) / 255.0D;
        double e = (double)(rgb >> 8 & 255) / 255.0D;
        double f = (double)(rgb & 255) / 255.0D;
        return new Vec3D(d, e, f);
    }

    public static Vec3D atCenterOf(BaseBlockPosition vec) {
        return new Vec3D((double)vec.getX() + 0.5D, (double)vec.getY() + 0.5D, (double)vec.getZ() + 0.5D);
    }

    public static Vec3D atLowerCornerOf(BaseBlockPosition vec) {
        return new Vec3D((double)vec.getX(), (double)vec.getY(), (double)vec.getZ());
    }

    public static Vec3D atBottomCenterOf(BaseBlockPosition vec) {
        return new Vec3D((double)vec.getX() + 0.5D, (double)vec.getY(), (double)vec.getZ() + 0.5D);
    }

    public static Vec3D upFromBottomCenterOf(BaseBlockPosition vec, double deltaY) {
        return new Vec3D((double)vec.getX() + 0.5D, (double)vec.getY() + deltaY, (double)vec.getZ() + 0.5D);
    }

    public Vec3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3D(Vector3fa vec) {
        this((double)vec.x(), (double)vec.y(), (double)vec.z());
    }

    public Vec3D vectorTo(Vec3D vec) {
        return new Vec3D(vec.x - this.x, vec.y - this.y, vec.z - this.z);
    }

    public Vec3D normalize() {
        double d = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        return d < 1.0E-4D ? ZERO : new Vec3D(this.x / d, this.y / d, this.z / d);
    }

    public double dot(Vec3D vec) {
        return this.x * vec.x + this.y * vec.y + this.z * vec.z;
    }

    public Vec3D cross(Vec3D vec) {
        return new Vec3D(this.y * vec.z - this.z * vec.y, this.z * vec.x - this.x * vec.z, this.x * vec.y - this.y * vec.x);
    }

    public Vec3D subtract(Vec3D vec) {
        return this.subtract(vec.x, vec.y, vec.z);
    }

    public Vec3D subtract(double x, double y, double z) {
        return this.add(-x, -y, -z);
    }

    public Vec3D add(Vec3D vec) {
        return this.add(vec.x, vec.y, vec.z);
    }

    public Vec3D add(double x, double y, double z) {
        return new Vec3D(this.x + x, this.y + y, this.z + z);
    }

    public boolean closerThan(IPosition pos, double radius) {
        return this.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < radius * radius;
    }

    public double distanceTo(Vec3D vec) {
        double d = vec.x - this.x;
        double e = vec.y - this.y;
        double f = vec.z - this.z;
        return Math.sqrt(d * d + e * e + f * f);
    }

    public double distanceSquared(Vec3D vec) {
        double d = vec.x - this.x;
        double e = vec.y - this.y;
        double f = vec.z - this.z;
        return d * d + e * e + f * f;
    }

    public double distanceToSqr(double x, double y, double z) {
        double d = x - this.x;
        double e = y - this.y;
        double f = z - this.z;
        return d * d + e * e + f * f;
    }

    public Vec3D scale(double value) {
        return this.multiply(value, value, value);
    }

    public Vec3D reverse() {
        return this.scale(-1.0D);
    }

    public Vec3D multiply(Vec3D vec) {
        return this.multiply(vec.x, vec.y, vec.z);
    }

    public Vec3D multiply(double x, double y, double z) {
        return new Vec3D(this.x * x, this.y * y, this.z * z);
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public double lengthSqr() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public double horizontalDistance() {
        return Math.sqrt(this.x * this.x + this.z * this.z);
    }

    public double horizontalDistanceSqr() {
        return this.x * this.x + this.z * this.z;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Vec3D)) {
            return false;
        } else {
            Vec3D vec3 = (Vec3D)object;
            if (Double.compare(vec3.x, this.x) != 0) {
                return false;
            } else if (Double.compare(vec3.y, this.y) != 0) {
                return false;
            } else {
                return Double.compare(vec3.z, this.z) == 0;
            }
        }
    }

    @Override
    public int hashCode() {
        long l = Double.doubleToLongBits(this.x);
        int i = (int)(l ^ l >>> 32);
        l = Double.doubleToLongBits(this.y);
        i = 31 * i + (int)(l ^ l >>> 32);
        l = Double.doubleToLongBits(this.z);
        return 31 * i + (int)(l ^ l >>> 32);
    }

    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ")";
    }

    public Vec3D lerp(Vec3D to, double delta) {
        return new Vec3D(MathHelper.lerp(delta, this.x, to.x), MathHelper.lerp(delta, this.y, to.y), MathHelper.lerp(delta, this.z, to.z));
    }

    public Vec3D xRot(float angle) {
        float f = MathHelper.cos(angle);
        float g = MathHelper.sin(angle);
        double d = this.x;
        double e = this.y * (double)f + this.z * (double)g;
        double h = this.z * (double)f - this.y * (double)g;
        return new Vec3D(d, e, h);
    }

    public Vec3D yRot(float angle) {
        float f = MathHelper.cos(angle);
        float g = MathHelper.sin(angle);
        double d = this.x * (double)f + this.z * (double)g;
        double e = this.y;
        double h = this.z * (double)f - this.x * (double)g;
        return new Vec3D(d, e, h);
    }

    public Vec3D zRot(float angle) {
        float f = MathHelper.cos(angle);
        float g = MathHelper.sin(angle);
        double d = this.x * (double)f + this.y * (double)g;
        double e = this.y * (double)f - this.x * (double)g;
        double h = this.z;
        return new Vec3D(d, e, h);
    }

    public static Vec3D directionFromRotation(Vec2F polar) {
        return directionFromRotation(polar.x, polar.y);
    }

    public static Vec3D directionFromRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * ((float)Math.PI / 180F) - (float)Math.PI);
        float g = MathHelper.sin(-yaw * ((float)Math.PI / 180F) - (float)Math.PI);
        float h = -MathHelper.cos(-pitch * ((float)Math.PI / 180F));
        float i = MathHelper.sin(-pitch * ((float)Math.PI / 180F));
        return new Vec3D((double)(g * h), (double)i, (double)(f * h));
    }

    public Vec3D align(EnumSet<EnumDirection.EnumAxis> axes) {
        double d = axes.contains(EnumDirection.EnumAxis.X) ? (double)MathHelper.floor(this.x) : this.x;
        double e = axes.contains(EnumDirection.EnumAxis.Y) ? (double)MathHelper.floor(this.y) : this.y;
        double f = axes.contains(EnumDirection.EnumAxis.Z) ? (double)MathHelper.floor(this.z) : this.z;
        return new Vec3D(d, e, f);
    }

    public double get(EnumDirection.EnumAxis axis) {
        return axis.choose(this.x, this.y, this.z);
    }

    @Override
    public final double getX() {
        return this.x;
    }

    @Override
    public final double getY() {
        return this.y;
    }

    @Override
    public final double getZ() {
        return this.z;
    }
}
