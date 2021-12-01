package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;

public class VoxelShapeCubePoint extends AbstractDoubleList {
    private final int parts;

    VoxelShapeCubePoint(int sectionCount) {
        if (sectionCount <= 0) {
            throw new IllegalArgumentException("Need at least 1 part");
        } else {
            this.parts = sectionCount;
        }
    }

    public double getDouble(int i) {
        return (double)i / (double)this.parts;
    }

    public int size() {
        return this.parts + 1;
    }
}
