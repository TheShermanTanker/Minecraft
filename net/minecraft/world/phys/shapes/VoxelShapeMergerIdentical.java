package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;

public class VoxelShapeMergerIdentical implements VoxelShapeMerger {
    private final DoubleList coords;

    public VoxelShapeMergerIdentical(DoubleList values) {
        this.coords = values;
    }

    @Override
    public boolean forMergedIndexes(VoxelShapeMerger.IndexConsumer predicate) {
        int i = this.coords.size() - 1;

        for(int j = 0; j < i; ++j) {
            if (!predicate.merge(j, j, j)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int size() {
        return this.coords.size();
    }

    @Override
    public DoubleList getList() {
        return this.coords;
    }
}
