package net.minecraft.world.phys.shapes;

import net.minecraft.core.EnumAxisCycle;
import net.minecraft.core.EnumDirection;

public abstract class VoxelShapeDiscrete {
    private static final EnumDirection.EnumAxis[] AXIS_VALUES = EnumDirection.EnumAxis.values();
    protected final int xSize;
    protected final int ySize;
    protected final int zSize;

    protected VoxelShapeDiscrete(int sizeX, int sizeY, int sizeZ) {
        if (sizeX >= 0 && sizeY >= 0 && sizeZ >= 0) {
            this.xSize = sizeX;
            this.ySize = sizeY;
            this.zSize = sizeZ;
        } else {
            throw new IllegalArgumentException("Need all positive sizes: x: " + sizeX + ", y: " + sizeY + ", z: " + sizeZ);
        }
    }

    public boolean isFullWide(EnumAxisCycle cycle, int x, int y, int z) {
        return this.isFullWide(cycle.cycle(x, y, z, EnumDirection.EnumAxis.X), cycle.cycle(x, y, z, EnumDirection.EnumAxis.Y), cycle.cycle(x, y, z, EnumDirection.EnumAxis.Z));
    }

    public boolean isFullWide(int x, int y, int z) {
        if (x >= 0 && y >= 0 && z >= 0) {
            return x < this.xSize && y < this.ySize && z < this.zSize ? this.isFull(x, y, z) : false;
        } else {
            return false;
        }
    }

    public boolean isFull(EnumAxisCycle cycle, int x, int y, int z) {
        return this.isFull(cycle.cycle(x, y, z, EnumDirection.EnumAxis.X), cycle.cycle(x, y, z, EnumDirection.EnumAxis.Y), cycle.cycle(x, y, z, EnumDirection.EnumAxis.Z));
    }

    public abstract boolean isFull(int x, int y, int z);

    public abstract void fill(int x, int y, int z);

    public boolean isEmpty() {
        for(EnumDirection.EnumAxis axis : AXIS_VALUES) {
            if (this.firstFull(axis) >= this.lastFull(axis)) {
                return true;
            }
        }

        return false;
    }

    public abstract int firstFull(EnumDirection.EnumAxis axis);

    public abstract int lastFull(EnumDirection.EnumAxis axis);

    public int firstFull(EnumDirection.EnumAxis axis, int i, int j) {
        int k = this.getSize(axis);
        if (i >= 0 && j >= 0) {
            EnumDirection.EnumAxis axis2 = EnumAxisCycle.FORWARD.cycle(axis);
            EnumDirection.EnumAxis axis3 = EnumAxisCycle.BACKWARD.cycle(axis);
            if (i < this.getSize(axis2) && j < this.getSize(axis3)) {
                EnumAxisCycle axisCycle = EnumAxisCycle.between(EnumDirection.EnumAxis.X, axis);

                for(int l = 0; l < k; ++l) {
                    if (this.isFull(axisCycle, l, i, j)) {
                        return l;
                    }
                }

                return k;
            } else {
                return k;
            }
        } else {
            return k;
        }
    }

    public int lastFull(EnumDirection.EnumAxis axis, int from, int to) {
        if (from >= 0 && to >= 0) {
            EnumDirection.EnumAxis axis2 = EnumAxisCycle.FORWARD.cycle(axis);
            EnumDirection.EnumAxis axis3 = EnumAxisCycle.BACKWARD.cycle(axis);
            if (from < this.getSize(axis2) && to < this.getSize(axis3)) {
                int i = this.getSize(axis);
                EnumAxisCycle axisCycle = EnumAxisCycle.between(EnumDirection.EnumAxis.X, axis);

                for(int j = i - 1; j >= 0; --j) {
                    if (this.isFull(axisCycle, j, from, to)) {
                        return j + 1;
                    }
                }

                return 0;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public int getSize(EnumDirection.EnumAxis axis) {
        return axis.choose(this.xSize, this.ySize, this.zSize);
    }

    public int getXSize() {
        return this.getSize(EnumDirection.EnumAxis.X);
    }

    public int getYSize() {
        return this.getSize(EnumDirection.EnumAxis.Y);
    }

    public int getZSize() {
        return this.getSize(EnumDirection.EnumAxis.Z);
    }

    public void forAllEdges(VoxelShapeDiscrete.IntLineConsumer intLineConsumer, boolean bl) {
        this.forAllAxisEdges(intLineConsumer, EnumAxisCycle.NONE, bl);
        this.forAllAxisEdges(intLineConsumer, EnumAxisCycle.FORWARD, bl);
        this.forAllAxisEdges(intLineConsumer, EnumAxisCycle.BACKWARD, bl);
    }

    private void forAllAxisEdges(VoxelShapeDiscrete.IntLineConsumer intLineConsumer, EnumAxisCycle direction, boolean bl) {
        EnumAxisCycle axisCycle = direction.inverse();
        int i = this.getSize(axisCycle.cycle(EnumDirection.EnumAxis.X));
        int j = this.getSize(axisCycle.cycle(EnumDirection.EnumAxis.Y));
        int k = this.getSize(axisCycle.cycle(EnumDirection.EnumAxis.Z));

        for(int l = 0; l <= i; ++l) {
            for(int m = 0; m <= j; ++m) {
                int n = -1;

                for(int o = 0; o <= k; ++o) {
                    int p = 0;
                    int q = 0;

                    for(int r = 0; r <= 1; ++r) {
                        for(int s = 0; s <= 1; ++s) {
                            if (this.isFullWide(axisCycle, l + r - 1, m + s - 1, o)) {
                                ++p;
                                q ^= r ^ s;
                            }
                        }
                    }

                    if (p == 1 || p == 3 || p == 2 && (q & 1) == 0) {
                        if (bl) {
                            if (n == -1) {
                                n = o;
                            }
                        } else {
                            intLineConsumer.consume(axisCycle.cycle(l, m, o, EnumDirection.EnumAxis.X), axisCycle.cycle(l, m, o, EnumDirection.EnumAxis.Y), axisCycle.cycle(l, m, o, EnumDirection.EnumAxis.Z), axisCycle.cycle(l, m, o + 1, EnumDirection.EnumAxis.X), axisCycle.cycle(l, m, o + 1, EnumDirection.EnumAxis.Y), axisCycle.cycle(l, m, o + 1, EnumDirection.EnumAxis.Z));
                        }
                    } else if (n != -1) {
                        intLineConsumer.consume(axisCycle.cycle(l, m, n, EnumDirection.EnumAxis.X), axisCycle.cycle(l, m, n, EnumDirection.EnumAxis.Y), axisCycle.cycle(l, m, n, EnumDirection.EnumAxis.Z), axisCycle.cycle(l, m, o, EnumDirection.EnumAxis.X), axisCycle.cycle(l, m, o, EnumDirection.EnumAxis.Y), axisCycle.cycle(l, m, o, EnumDirection.EnumAxis.Z));
                        n = -1;
                    }
                }
            }
        }

    }

    public void forAllBoxes(VoxelShapeDiscrete.IntLineConsumer consumer, boolean largest) {
        VoxelShapeBitSet.forAllBoxes(this, consumer, largest);
    }

    public void forAllFaces(VoxelShapeDiscrete.IntFaceConsumer intFaceConsumer) {
        this.forAllAxisFaces(intFaceConsumer, EnumAxisCycle.NONE);
        this.forAllAxisFaces(intFaceConsumer, EnumAxisCycle.FORWARD);
        this.forAllAxisFaces(intFaceConsumer, EnumAxisCycle.BACKWARD);
    }

    private void forAllAxisFaces(VoxelShapeDiscrete.IntFaceConsumer intFaceConsumer, EnumAxisCycle direction) {
        EnumAxisCycle axisCycle = direction.inverse();
        EnumDirection.EnumAxis axis = axisCycle.cycle(EnumDirection.EnumAxis.Z);
        int i = this.getSize(axisCycle.cycle(EnumDirection.EnumAxis.X));
        int j = this.getSize(axisCycle.cycle(EnumDirection.EnumAxis.Y));
        int k = this.getSize(axis);
        EnumDirection direction2 = EnumDirection.fromAxisAndDirection(axis, EnumDirection.EnumAxisDirection.NEGATIVE);
        EnumDirection direction3 = EnumDirection.fromAxisAndDirection(axis, EnumDirection.EnumAxisDirection.POSITIVE);

        for(int l = 0; l < i; ++l) {
            for(int m = 0; m < j; ++m) {
                boolean bl = false;

                for(int n = 0; n <= k; ++n) {
                    boolean bl2 = n != k && this.isFull(axisCycle, l, m, n);
                    if (!bl && bl2) {
                        intFaceConsumer.consume(direction2, axisCycle.cycle(l, m, n, EnumDirection.EnumAxis.X), axisCycle.cycle(l, m, n, EnumDirection.EnumAxis.Y), axisCycle.cycle(l, m, n, EnumDirection.EnumAxis.Z));
                    }

                    if (bl && !bl2) {
                        intFaceConsumer.consume(direction3, axisCycle.cycle(l, m, n - 1, EnumDirection.EnumAxis.X), axisCycle.cycle(l, m, n - 1, EnumDirection.EnumAxis.Y), axisCycle.cycle(l, m, n - 1, EnumDirection.EnumAxis.Z));
                    }

                    bl = bl2;
                }
            }
        }

    }

    public interface IntFaceConsumer {
        void consume(EnumDirection direction, int x, int y, int z);
    }

    public interface IntLineConsumer {
        void consume(int x1, int y1, int z1, int x2, int y2, int z2);
    }
}
