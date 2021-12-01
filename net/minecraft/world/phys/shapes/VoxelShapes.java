package net.minecraft.world.phys.shapes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.SystemUtils;
import net.minecraft.core.EnumAxisCycle;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.phys.AxisAlignedBB;

public final class VoxelShapes {
    public static final double EPSILON = 1.0E-7D;
    public static final double BIG_EPSILON = 1.0E-6D;
    private static final VoxelShape BLOCK = SystemUtils.make(() -> {
        VoxelShapeDiscrete discreteVoxelShape = new VoxelShapeBitSet(1, 1, 1);
        discreteVoxelShape.fill(0, 0, 0);
        return new VoxelShapeCube(discreteVoxelShape);
    });
    public static final VoxelShape INFINITY = box(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    private static final VoxelShape EMPTY = new VoxelShapeArray(new VoxelShapeBitSet(0, 0, 0), (DoubleList)(new DoubleArrayList(new double[]{0.0D})), (DoubleList)(new DoubleArrayList(new double[]{0.0D})), (DoubleList)(new DoubleArrayList(new double[]{0.0D})));

    public static VoxelShape empty() {
        return EMPTY;
    }

    public static VoxelShape block() {
        return BLOCK;
    }

    public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!(minX > maxX) && !(minY > maxY) && !(minZ > maxZ)) {
            return create(minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
        }
    }

    public static VoxelShape create(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!(maxX - minX < 1.0E-7D) && !(maxY - minY < 1.0E-7D) && !(maxZ - minZ < 1.0E-7D)) {
            int i = findBits(minX, maxX);
            int j = findBits(minY, maxY);
            int k = findBits(minZ, maxZ);
            if (i >= 0 && j >= 0 && k >= 0) {
                if (i == 0 && j == 0 && k == 0) {
                    return block();
                } else {
                    int l = 1 << i;
                    int m = 1 << j;
                    int n = 1 << k;
                    VoxelShapeBitSet bitSetDiscreteVoxelShape = VoxelShapeBitSet.withFilledBounds(l, m, n, (int)Math.round(minX * (double)l), (int)Math.round(minY * (double)m), (int)Math.round(minZ * (double)n), (int)Math.round(maxX * (double)l), (int)Math.round(maxY * (double)m), (int)Math.round(maxZ * (double)n));
                    return new VoxelShapeCube(bitSetDiscreteVoxelShape);
                }
            } else {
                return new VoxelShapeArray(BLOCK.shape, (DoubleList)DoubleArrayList.wrap(new double[]{minX, maxX}), (DoubleList)DoubleArrayList.wrap(new double[]{minY, maxY}), (DoubleList)DoubleArrayList.wrap(new double[]{minZ, maxZ}));
            }
        } else {
            return empty();
        }
    }

    public static VoxelShape create(AxisAlignedBB box) {
        return create(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    @VisibleForTesting
    protected static int findBits(double min, double max) {
        if (!(min < -1.0E-7D) && !(max > 1.0000001D)) {
            for(int i = 0; i <= 3; ++i) {
                int j = 1 << i;
                double d = min * (double)j;
                double e = max * (double)j;
                boolean bl = Math.abs(d - (double)Math.round(d)) < 1.0E-7D * (double)j;
                boolean bl2 = Math.abs(e - (double)Math.round(e)) < 1.0E-7D * (double)j;
                if (bl && bl2) {
                    return i;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }

    protected static long lcm(int a, int b) {
        return (long)a * (long)(b / IntMath.gcd(a, b));
    }

    public static VoxelShape or(VoxelShape first, VoxelShape second) {
        return join(first, second, OperatorBoolean.OR);
    }

    public static VoxelShape or(VoxelShape first, VoxelShape... others) {
        return Arrays.stream(others).reduce(first, VoxelShapes::or);
    }

    public static VoxelShape join(VoxelShape first, VoxelShape second, OperatorBoolean function) {
        return joinUnoptimized(first, second, function).optimize();
    }

    public static VoxelShape joinUnoptimized(VoxelShape one, VoxelShape two, OperatorBoolean function) {
        if (function.apply(false, false)) {
            throw (IllegalArgumentException)SystemUtils.pauseInIde(new IllegalArgumentException());
        } else if (one == two) {
            return function.apply(true, true) ? one : empty();
        } else {
            boolean bl = function.apply(true, false);
            boolean bl2 = function.apply(false, true);
            if (one.isEmpty()) {
                return bl2 ? two : empty();
            } else if (two.isEmpty()) {
                return bl ? one : empty();
            } else {
                VoxelShapeMerger indexMerger = createIndexMerger(1, one.getCoords(EnumDirection.EnumAxis.X), two.getCoords(EnumDirection.EnumAxis.X), bl, bl2);
                VoxelShapeMerger indexMerger2 = createIndexMerger(indexMerger.size() - 1, one.getCoords(EnumDirection.EnumAxis.Y), two.getCoords(EnumDirection.EnumAxis.Y), bl, bl2);
                VoxelShapeMerger indexMerger3 = createIndexMerger((indexMerger.size() - 1) * (indexMerger2.size() - 1), one.getCoords(EnumDirection.EnumAxis.Z), two.getCoords(EnumDirection.EnumAxis.Z), bl, bl2);
                VoxelShapeBitSet bitSetDiscreteVoxelShape = VoxelShapeBitSet.join(one.shape, two.shape, indexMerger, indexMerger2, indexMerger3, function);
                return (VoxelShape)(indexMerger instanceof VoxelShapeCubeMerger && indexMerger2 instanceof VoxelShapeCubeMerger && indexMerger3 instanceof VoxelShapeCubeMerger ? new VoxelShapeCube(bitSetDiscreteVoxelShape) : new VoxelShapeArray(bitSetDiscreteVoxelShape, indexMerger.getList(), indexMerger2.getList(), indexMerger3.getList()));
            }
        }
    }

    public static boolean joinIsNotEmpty(VoxelShape shape1, VoxelShape shape2, OperatorBoolean predicate) {
        if (predicate.apply(false, false)) {
            throw (IllegalArgumentException)SystemUtils.pauseInIde(new IllegalArgumentException());
        } else {
            boolean bl = shape1.isEmpty();
            boolean bl2 = shape2.isEmpty();
            if (!bl && !bl2) {
                if (shape1 == shape2) {
                    return predicate.apply(true, true);
                } else {
                    boolean bl3 = predicate.apply(true, false);
                    boolean bl4 = predicate.apply(false, true);

                    for(EnumDirection.EnumAxis axis : EnumAxisCycle.AXIS_VALUES) {
                        if (shape1.max(axis) < shape2.min(axis) - 1.0E-7D) {
                            return bl3 || bl4;
                        }

                        if (shape2.max(axis) < shape1.min(axis) - 1.0E-7D) {
                            return bl3 || bl4;
                        }
                    }

                    VoxelShapeMerger indexMerger = createIndexMerger(1, shape1.getCoords(EnumDirection.EnumAxis.X), shape2.getCoords(EnumDirection.EnumAxis.X), bl3, bl4);
                    VoxelShapeMerger indexMerger2 = createIndexMerger(indexMerger.size() - 1, shape1.getCoords(EnumDirection.EnumAxis.Y), shape2.getCoords(EnumDirection.EnumAxis.Y), bl3, bl4);
                    VoxelShapeMerger indexMerger3 = createIndexMerger((indexMerger.size() - 1) * (indexMerger2.size() - 1), shape1.getCoords(EnumDirection.EnumAxis.Z), shape2.getCoords(EnumDirection.EnumAxis.Z), bl3, bl4);
                    return joinIsNotEmpty(indexMerger, indexMerger2, indexMerger3, shape1.shape, shape2.shape, predicate);
                }
            } else {
                return predicate.apply(!bl, !bl2);
            }
        }
    }

    private static boolean joinIsNotEmpty(VoxelShapeMerger mergedX, VoxelShapeMerger mergedY, VoxelShapeMerger mergedZ, VoxelShapeDiscrete shape1, VoxelShapeDiscrete shape2, OperatorBoolean predicate) {
        return !mergedX.forMergedIndexes((x1, x2, index1) -> {
            return mergedY.forMergedIndexes((y1, y2, index2) -> {
                return mergedZ.forMergedIndexes((z1, z2, index3) -> {
                    return !predicate.apply(shape1.isFullWide(x1, y1, z1), shape2.isFullWide(x2, y2, z2));
                });
            });
        });
    }

    public static double collide(EnumDirection.EnumAxis axis, AxisAlignedBB box, Iterable<VoxelShape> shapes, double maxDist) {
        for(VoxelShape voxelShape : shapes) {
            if (Math.abs(maxDist) < 1.0E-7D) {
                return 0.0D;
            }

            maxDist = voxelShape.collide(axis, box, maxDist);
        }

        return maxDist;
    }

    public static boolean blockOccudes(VoxelShape shape, VoxelShape neighbor, EnumDirection direction) {
        if (shape == block() && neighbor == block()) {
            return true;
        } else if (neighbor.isEmpty()) {
            return false;
        } else {
            EnumDirection.EnumAxis axis = direction.getAxis();
            EnumDirection.EnumAxisDirection axisDirection = direction.getAxisDirection();
            VoxelShape voxelShape = axisDirection == EnumDirection.EnumAxisDirection.POSITIVE ? shape : neighbor;
            VoxelShape voxelShape2 = axisDirection == EnumDirection.EnumAxisDirection.POSITIVE ? neighbor : shape;
            OperatorBoolean booleanOp = axisDirection == EnumDirection.EnumAxisDirection.POSITIVE ? OperatorBoolean.ONLY_FIRST : OperatorBoolean.ONLY_SECOND;
            return DoubleMath.fuzzyEquals(voxelShape.max(axis), 1.0D, 1.0E-7D) && DoubleMath.fuzzyEquals(voxelShape2.min(axis), 0.0D, 1.0E-7D) && !joinIsNotEmpty(new VoxelShapeSlice(voxelShape, axis, voxelShape.shape.getSize(axis) - 1), new VoxelShapeSlice(voxelShape2, axis, 0), booleanOp);
        }
    }

    public static VoxelShape getFaceShape(VoxelShape shape, EnumDirection direction) {
        if (shape == block()) {
            return block();
        } else {
            EnumDirection.EnumAxis axis = direction.getAxis();
            boolean bl;
            int i;
            if (direction.getAxisDirection() == EnumDirection.EnumAxisDirection.POSITIVE) {
                bl = DoubleMath.fuzzyEquals(shape.max(axis), 1.0D, 1.0E-7D);
                i = shape.shape.getSize(axis) - 1;
            } else {
                bl = DoubleMath.fuzzyEquals(shape.min(axis), 0.0D, 1.0E-7D);
                i = 0;
            }

            return (VoxelShape)(!bl ? empty() : new VoxelShapeSlice(shape, axis, i));
        }
    }

    public static boolean mergedFaceOccludes(VoxelShape one, VoxelShape two, EnumDirection direction) {
        if (one != block() && two != block()) {
            EnumDirection.EnumAxis axis = direction.getAxis();
            EnumDirection.EnumAxisDirection axisDirection = direction.getAxisDirection();
            VoxelShape voxelShape = axisDirection == EnumDirection.EnumAxisDirection.POSITIVE ? one : two;
            VoxelShape voxelShape2 = axisDirection == EnumDirection.EnumAxisDirection.POSITIVE ? two : one;
            if (!DoubleMath.fuzzyEquals(voxelShape.max(axis), 1.0D, 1.0E-7D)) {
                voxelShape = empty();
            }

            if (!DoubleMath.fuzzyEquals(voxelShape2.min(axis), 0.0D, 1.0E-7D)) {
                voxelShape2 = empty();
            }

            return !joinIsNotEmpty(block(), joinUnoptimized(new VoxelShapeSlice(voxelShape, axis, voxelShape.shape.getSize(axis) - 1), new VoxelShapeSlice(voxelShape2, axis, 0), OperatorBoolean.OR), OperatorBoolean.ONLY_FIRST);
        } else {
            return true;
        }
    }

    public static boolean faceShapeOccludes(VoxelShape one, VoxelShape two) {
        if (one != block() && two != block()) {
            if (one.isEmpty() && two.isEmpty()) {
                return false;
            } else {
                return !joinIsNotEmpty(block(), joinUnoptimized(one, two, OperatorBoolean.OR), OperatorBoolean.ONLY_FIRST);
            }
        } else {
            return true;
        }
    }

    @VisibleForTesting
    protected static VoxelShapeMerger createIndexMerger(int size, DoubleList first, DoubleList second, boolean includeFirst, boolean includeSecond) {
        int i = first.size() - 1;
        int j = second.size() - 1;
        if (first instanceof VoxelShapeCubePoint && second instanceof VoxelShapeCubePoint) {
            long l = lcm(i, j);
            if ((long)size * l <= 256L) {
                return new VoxelShapeCubeMerger(i, j);
            }
        }

        if (first.getDouble(i) < second.getDouble(0) - 1.0E-7D) {
            return new VoxelShapeMergerDisjoint(first, second, false);
        } else if (second.getDouble(j) < first.getDouble(0) - 1.0E-7D) {
            return new VoxelShapeMergerDisjoint(second, first, true);
        } else {
            return (VoxelShapeMerger)(i == j && Objects.equals(first, second) ? new VoxelShapeMergerIdentical(first) : new VoxelShapeMergerList(first, second, includeFirst, includeSecond));
        }
    }

    public interface DoubleLineConsumer {
        void consume(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    }
}
