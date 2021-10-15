package net.minecraft.core;

import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.MathHelper;

public class Vector3f {
    protected final float x;
    protected final float y;
    protected final float z;

    public Vector3f(float pitch, float yaw, float roll) {
        this.x = !Float.isInfinite(pitch) && !Float.isNaN(pitch) ? pitch % 360.0F : 0.0F;
        this.y = !Float.isInfinite(yaw) && !Float.isNaN(yaw) ? yaw % 360.0F : 0.0F;
        this.z = !Float.isInfinite(roll) && !Float.isNaN(roll) ? roll % 360.0F : 0.0F;
    }

    public Vector3f(NBTTagList serialized) {
        this(serialized.getFloat(0), serialized.getFloat(1), serialized.getFloat(2));
    }

    public NBTTagList save() {
        NBTTagList listTag = new NBTTagList();
        listTag.add(NBTTagFloat.valueOf(this.x));
        listTag.add(NBTTagFloat.valueOf(this.y));
        listTag.add(NBTTagFloat.valueOf(this.z));
        return listTag;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Vector3f)) {
            return false;
        } else {
            Vector3f rotations = (Vector3f)object;
            return this.x == rotations.x && this.y == rotations.y && this.z == rotations.z;
        }
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getZ() {
        return this.z;
    }

    public float getWrappedX() {
        return MathHelper.wrapDegrees(this.x);
    }

    public float getWrappedY() {
        return MathHelper.wrapDegrees(this.y);
    }

    public float getWrappedZ() {
        return MathHelper.wrapDegrees(this.z);
    }
}
