package net.minecraft.world.phys.shapes;

import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumAxisCycle;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public abstract class VoxelShape {
    protected final VoxelShapeDiscrete shape;
    @Nullable
    private VoxelShape[] faces;

    VoxelShape(VoxelShapeDiscrete voxels) {
        this.shape = voxels;
    }

    public double min(EnumDirection.EnumAxis axis) {
        int i = this.shape.firstFull(axis);
        return i >= this.shape.getSize(axis) ? Double.POSITIVE_INFINITY : this.get(axis, i);
    }

    public double max(EnumDirection.EnumAxis axis) {
        int i = this.shape.lastFull(axis);
        return i <= 0 ? Double.NEGATIVE_INFINITY : this.get(axis, i);
    }

    public AxisAlignedBB getBoundingBox() {
        if (this.isEmpty()) {
            throw (UnsupportedOperationException)SystemUtils.pauseInIde(new UnsupportedOperationException("No bounds for empty shape."));
        } else {
            return new AxisAlignedBB(this.min(EnumDirection.EnumAxis.X), this.min(EnumDirection.EnumAxis.Y), this.min(EnumDirection.EnumAxis.Z), this.max(EnumDirection.EnumAxis.X), this.max(EnumDirection.EnumAxis.Y), this.max(EnumDirection.EnumAxis.Z));
        }
    }

    protected double get(EnumDirection.EnumAxis axis, int index) {
        return this.getCoords(axis).getDouble(index);
    }

    protected abstract DoubleList getCoords(EnumDirection.EnumAxis axis);

    public boolean isEmpty() {
        return this.shape.isEmpty();
    }

    public VoxelShape move(double x, double y, double z) {
        return (VoxelShape)(this.isEmpty() ? VoxelShapes.empty() : new VoxelShapeArray(this.shape, (DoubleList)(new DoubleListOffset(this.getCoords(EnumDirection.EnumAxis.X), x)), (DoubleList)(new DoubleListOffset(this.getCoords(EnumDirection.EnumAxis.Y), y)), (DoubleList)(new DoubleListOffset(this.getCoords(EnumDirection.EnumAxis.Z), z))));
    }

    public VoxelShape optimize() {
        VoxelShape[] voxelShapes = new VoxelShape[]{VoxelShapes.empty()};
        this.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            voxelShapes[0] = VoxelShapes.joinUnoptimized(voxelShapes[0], VoxelShapes.box(minX, minY, minZ, maxX, maxY, maxZ), OperatorBoolean.OR);
        });
        return voxelShapes[0];
    }

    public void forAllEdges(VoxelShapes.DoubleLineConsumer consumer) {
        this.shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            consumer.consume(this.get(EnumDirection.EnumAxis.X, minX), this.get(EnumDirection.EnumAxis.Y, minY), this.get(EnumDirection.EnumAxis.Z, minZ), this.get(EnumDirection.EnumAxis.X, maxX), this.get(EnumDirection.EnumAxis.Y, maxY), this.get(EnumDirection.EnumAxis.Z, maxZ));
        }, true);
    }

    public void forAllBoxes(VoxelShapes.DoubleLineConsumer consumer) {
        DoubleList doubleList = this.getCoords(EnumDirection.EnumAxis.X);
        DoubleList doubleList2 = this.getCoords(EnumDirection.EnumAxis.Y);
        DoubleList doubleList3 = this.getCoords(EnumDirection.EnumAxis.Z);
        this.shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            consumer.consume(doubleList.getDouble(minX), doubleList2.getDouble(minY), doubleList3.getDouble(minZ), doubleList.getDouble(maxX), doubleList2.getDouble(maxY), doubleList3.getDouble(maxZ));
        }, true);
    }

    public List<AxisAlignedBB> toList() {
        List<AxisAlignedBB> list = Lists.newArrayList();
        this.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            list.add(new AxisAlignedBB(x1, y1, z1, x2, y2, z2));
        });
        return list;
    }

    public double min(EnumDirection.EnumAxis axis, double from, double to) {
        EnumDirection.EnumAxis axis2 = EnumAxisCycle.FORWARD.cycle(axis);
        EnumDirection.EnumAxis axis3 = EnumAxisCycle.BACKWARD.cycle(axis);
        int i = this.findIndex(axis2, from);
        int j = this.findIndex(axis3, to);
        int k = this.shape.firstFull(axis, i, j);
        return k >= this.shape.getSize(axis) ? Double.POSITIVE_INFINITY : this.get(axis, k);
    }

    public double max(EnumDirection.EnumAxis axis, double from, double to) {
        EnumDirection.EnumAxis axis2 = EnumAxisCycle.FORWARD.cycle(axis);
        EnumDirection.EnumAxis axis3 = EnumAxisCycle.BACKWARD.cycle(axis);
        int i = this.findIndex(axis2, from);
        int j = this.findIndex(axis3, to);
        int k = this.shape.lastFull(axis, i, j);
        return k <= 0 ? Double.NEGATIVE_INFINITY : this.get(axis, k);
    }

    protected int findIndex(EnumDirection.EnumAxis axis, double coord) {
        return MathHelper.binarySearch(0, this.shape.getSize(axis) + 1, (i) -> {
            return coord < this.get(axis, i);
        }) - 1;
    }

    @Nullable
    public MovingObjectPositionBlock rayTrace(Vec3D start, Vec3D end, BlockPosition pos) {
        if (this.isEmpty()) {
            return null;
        } else {
            Vec3D vec3 = end.subtract(start);
            if (vec3.lengthSqr() < 1.0E-7D) {
                return null;
            } else {
                Vec3D vec32 = start.add(vec3.scale(0.001D));
                return this.shape.isFullWide(this.findIndex(EnumDirection.EnumAxis.X, vec32.x - (double)pos.getX()), this.findIndex(EnumDirection.EnumAxis.Y, vec32.y - (double)pos.getY()), this.findIndex(EnumDirection.EnumAxis.Z, vec32.z - (double)pos.getZ())) ? new MovingObjectPositionBlock(vec32, EnumDirection.getNearest(vec3.x, vec3.y, vec3.z).opposite(), pos, true) : AxisAlignedBB.clip(this.toList(), start, end, pos);
            }
        }
    }

    public Optional<Vec3D> closestPointTo(Vec3D target) {
        if (this.isEmpty()) {
            return Optional.empty();
        } else {
            Vec3D[] vec3s = new Vec3D[1];
            this.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
                double d = MathHelper.clamp(target.getX(), minX, maxX);
                double e = MathHelper.clamp(target.getY(), minY, maxY);
                double f = MathHelper.clamp(target.getZ(), minZ, maxZ);
                if (vec3s[0] == null || target.distanceToSqr(d, e, f) < target.distanceSquared(vec3s[0])) {
                    vec3s[0] = new Vec3D(d, e, f);
                }

            });
            return Optional.of(vec3s[0]);
        }
    }

    public VoxelShape getFaceShape(EnumDirection facing) {
        if (!this.isEmpty() && this != VoxelShapes.block()) {
            if (this.faces != null) {
                VoxelShape voxelShape = this.faces[facing.ordinal()];
                if (voxelShape != null) {
                    return voxelShape;
                }
            } else {
                this.faces = new VoxelShape[6];
            }

            VoxelShape voxelShape2 = this.calculateFace(facing);
            this.faces[facing.ordinal()] = voxelShape2;
            return voxelShape2;
        } else {
            return this;
        }
    }

    private VoxelShape calculateFace(EnumDirection direction) {
        EnumDirection.EnumAxis axis = direction.getAxis();
        DoubleList doubleList = this.getCoords(axis);
        if (doubleList.size() == 2 && DoubleMath.fuzzyEquals(doubleList.getDouble(0), 0.0D, 1.0E-7D) && DoubleMath.fuzzyEquals(doubleList.getDouble(1), 1.0D, 1.0E-7D)) {
            return this;
        } else {
            EnumDirection.EnumAxisDirection axisDirection = direction.getAxisDirection();
            int i = this.findIndex(axis, axisDirection == EnumDirection.EnumAxisDirection.POSITIVE ? 0.9999999D : 1.0E-7D);
            return new VoxelShapeSlice(this, axis, i);
        }
    }

    public double collide(EnumDirection.EnumAxis axis, AxisAlignedBB box, double maxDist) {
        return this.collideX(EnumAxisCycle.between(axis, EnumDirection.EnumAxis.X), box, maxDist);
    }

    protected double collideX(EnumAxisCycle axisCycle, AxisAlignedBB box, double maxDist) {
        if (this.isEmpty()) {
            return maxDist;
        } else if (Math.abs(maxDist) < 1.0E-7D) {
            return 0.0D;
        } else {
            EnumAxisCycle axisCycle2 = axisCycle.inverse();
            EnumDirection.EnumAxis axis = axisCycle2.cycle(EnumDirection.EnumAxis.X);
            EnumDirection.EnumAxis axis2 = axisCycle2.cycle(EnumDirection.EnumAxis.Y);
            EnumDirection.EnumAxis axis3 = axisCycle2.cycle(EnumDirection.EnumAxis.Z);
            double d = box.max(axis);
            double e = box.min(axis);
            int i = this.findIndex(axis, e + 1.0E-7D);
            int j = this.findIndex(axis, d - 1.0E-7D);
            int k = Math.max(0, this.findIndex(axis2, box.min(axis2) + 1.0E-7D));
            int l = Math.min(this.shape.getSize(axis2), this.findIndex(axis2, box.max(axis2) - 1.0E-7D) + 1);
            int m = Math.max(0, this.findIndex(axis3, box.min(axis3) + 1.0E-7D));
            int n = Math.min(this.shape.getSize(axis3), this.findIndex(axis3, box.max(axis3) - 1.0E-7D) + 1);
            int o = this.shape.getSize(axis);
            if (maxDist > 0.0D) {
                for(int p = j + 1; p < o; ++p) {
                    for(int q = k; q < l; ++q) {
                        for(int r = m; r < n; ++r) {
                            if (this.shape.isFullWide(axisCycle2, p, q, r)) {
                                double f = this.get(axis, p) - d;
                                if (f >= -1.0E-7D) {
                                    maxDist = Math.min(maxDist, f);
                                }

                                return maxDist;
                            }
                        }
                    }
                }
            } else if (maxDist < 0.0D) {
                for(int s = i - 1; s >= 0; --s) {
                    for(int t = k; t < l; ++t) {
                        for(int u = m; u < n; ++u) {
                            if (this.shape.isFullWide(axisCycle2, s, t, u)) {
                                double g = this.get(axis, s + 1) - e;
                                if (g <= 1.0E-7D) {
                                    maxDist = Math.max(maxDist, g);
                                }

                                return maxDist;
                            }
                        }
                    }
                }
            }

            return maxDist;
        }
    }

    @Override
    public String toString() {
        return this.isEmpty() ? "EMPTY" : "VoxelShape[" + this.getBoundingBox() + "]";
    }
}
