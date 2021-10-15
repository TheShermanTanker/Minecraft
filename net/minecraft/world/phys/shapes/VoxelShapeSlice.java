package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.EnumDirection;

public class VoxelShapeSlice extends VoxelShape {
    private final VoxelShape delegate;
    private final EnumDirection.EnumAxis axis;
    private static final DoubleList SLICE_COORDS = new VoxelShapeCubePoint(1);

    public VoxelShapeSlice(VoxelShape shape, EnumDirection.EnumAxis axis, int sliceWidth) {
        super(makeSlice(shape.shape, axis, sliceWidth));
        this.delegate = shape;
        this.axis = axis;
    }

    private static VoxelShapeDiscrete makeSlice(VoxelShapeDiscrete voxelSet, EnumDirection.EnumAxis axis, int sliceWidth) {
        return new VoxelShapeDiscreteSlice(voxelSet, axis.choose(sliceWidth, 0, 0), axis.choose(0, sliceWidth, 0), axis.choose(0, 0, sliceWidth), axis.choose(sliceWidth + 1, voxelSet.xSize, voxelSet.xSize), axis.choose(voxelSet.ySize, sliceWidth + 1, voxelSet.ySize), axis.choose(voxelSet.zSize, voxelSet.zSize, sliceWidth + 1));
    }

    @Override
    protected DoubleList getCoords(EnumDirection.EnumAxis axis) {
        return axis == this.axis ? SLICE_COORDS : this.delegate.getCoords(axis);
    }
}
