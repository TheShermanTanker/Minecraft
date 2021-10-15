package net.minecraft.world.phys.shapes;

import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;

public final class VoxelShapeDiscreteSlice extends VoxelShapeDiscrete {
    private final VoxelShapeDiscrete parent;
    private final int startX;
    private final int startY;
    private final int startZ;
    private final int endX;
    private final int endY;
    private final int endZ;

    protected VoxelShapeDiscreteSlice(VoxelShapeDiscrete parent, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        super(maxX - minX, maxY - minY, maxZ - minZ);
        this.parent = parent;
        this.startX = minX;
        this.startY = minY;
        this.startZ = minZ;
        this.endX = maxX;
        this.endY = maxY;
        this.endZ = maxZ;
    }

    @Override
    public boolean isFull(int x, int y, int z) {
        return this.parent.isFull(this.startX + x, this.startY + y, this.startZ + z);
    }

    @Override
    public void fill(int x, int y, int z) {
        this.parent.fill(this.startX + x, this.startY + y, this.startZ + z);
    }

    @Override
    public int firstFull(EnumDirection.EnumAxis axis) {
        return this.clampToShape(axis, this.parent.firstFull(axis));
    }

    @Override
    public int lastFull(EnumDirection.EnumAxis axis) {
        return this.clampToShape(axis, this.parent.lastFull(axis));
    }

    private int clampToShape(EnumDirection.EnumAxis axis, int value) {
        int i = axis.choose(this.startX, this.startY, this.startZ);
        int j = axis.choose(this.endX, this.endY, this.endZ);
        return MathHelper.clamp(value, i, j) - i;
    }
}
