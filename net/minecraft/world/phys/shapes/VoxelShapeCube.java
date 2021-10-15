package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;

public final class VoxelShapeCube extends VoxelShape {
    protected VoxelShapeCube(VoxelShapeDiscrete voxels) {
        super(voxels);
    }

    @Override
    protected DoubleList getCoords(EnumDirection.EnumAxis axis) {
        return new VoxelShapeCubePoint(this.shape.getSize(axis));
    }

    @Override
    protected int findIndex(EnumDirection.EnumAxis axis, double coord) {
        int i = this.shape.getSize(axis);
        return MathHelper.floor(MathHelper.clamp(coord * (double)i, -1.0D, (double)i));
    }
}
