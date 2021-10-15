package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;

interface VoxelShapeMerger {
    DoubleList getList();

    boolean forMergedIndexes(VoxelShapeMerger.IndexConsumer predicate);

    int size();

    public interface IndexConsumer {
        boolean merge(int x, int y, int index);
    }
}
