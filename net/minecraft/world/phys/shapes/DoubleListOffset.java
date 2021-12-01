package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public class DoubleListOffset extends AbstractDoubleList {
    private final DoubleList delegate;
    private final double offset;

    public DoubleListOffset(DoubleList oldList, double offset) {
        this.delegate = oldList;
        this.offset = offset;
    }

    public double getDouble(int i) {
        return this.delegate.getDouble(i) + this.offset;
    }

    public int size() {
        return this.delegate.size();
    }
}
