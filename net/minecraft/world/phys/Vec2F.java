package net.minecraft.world.phys;

import net.minecraft.util.MathHelper;

public class Vec2F {
    public static final Vec2F ZERO = new Vec2F(0.0F, 0.0F);
    public static final Vec2F ONE = new Vec2F(1.0F, 1.0F);
    public static final Vec2F UNIT_X = new Vec2F(1.0F, 0.0F);
    public static final Vec2F NEG_UNIT_X = new Vec2F(-1.0F, 0.0F);
    public static final Vec2F UNIT_Y = new Vec2F(0.0F, 1.0F);
    public static final Vec2F NEG_UNIT_Y = new Vec2F(0.0F, -1.0F);
    public static final Vec2F MAX = new Vec2F(Float.MAX_VALUE, Float.MAX_VALUE);
    public static final Vec2F MIN = new Vec2F(Float.MIN_VALUE, Float.MIN_VALUE);
    public final float x;
    public final float y;

    public Vec2F(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vec2F scale(float value) {
        return new Vec2F(this.x * value, this.y * value);
    }

    public float dot(Vec2F vec) {
        return this.x * vec.x + this.y * vec.y;
    }

    public Vec2F add(Vec2F vec) {
        return new Vec2F(this.x + vec.x, this.y + vec.y);
    }

    public Vec2F add(float value) {
        return new Vec2F(this.x + value, this.y + value);
    }

    public boolean equals(Vec2F other) {
        return this.x == other.x && this.y == other.y;
    }

    public Vec2F normalized() {
        float f = MathHelper.sqrt(this.x * this.x + this.y * this.y);
        return f < 1.0E-4F ? ZERO : new Vec2F(this.x / f, this.y / f);
    }

    public float length() {
        return MathHelper.sqrt(this.x * this.x + this.y * this.y);
    }

    public float lengthSquared() {
        return this.x * this.x + this.y * this.y;
    }

    public float distanceToSqr(Vec2F vec) {
        float f = vec.x - this.x;
        float g = vec.y - this.y;
        return f * f + g * g;
    }

    public Vec2F negated() {
        return new Vec2F(-this.x, -this.y);
    }
}
