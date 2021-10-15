package net.minecraft.core;

import com.google.common.collect.AbstractIterator;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.concurrent.Immutable;
import net.minecraft.SystemUtils;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Immutable
public class BlockPosition extends BaseBlockPosition {
    public static final Codec<BlockPosition> CODEC = Codec.INT_STREAM.comapFlatMap((stream) -> {
        return SystemUtils.fixedSize(stream, 3).map((values) -> {
            return new BlockPosition(values[0], values[1], values[2]);
        });
    }, (pos) -> {
        return IntStream.of(pos.getX(), pos.getY(), pos.getZ());
    }).stable();
    private static final Logger LOGGER = LogManager.getLogger();
    public static final BlockPosition ZERO = new BlockPosition(0, 0, 0);
    private static final int PACKED_X_LENGTH = 1 + MathHelper.log2(MathHelper.smallestEncompassingPowerOfTwo(30000000));
    private static final int PACKED_Z_LENGTH = PACKED_X_LENGTH;
    public static final int PACKED_Y_LENGTH = 64 - PACKED_X_LENGTH - PACKED_Z_LENGTH;
    private static final long PACKED_X_MASK = (1L << PACKED_X_LENGTH) - 1L;
    private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
    private static final long PACKED_Z_MASK = (1L << PACKED_Z_LENGTH) - 1L;
    private static final int Y_OFFSET = 0;
    private static final int Z_OFFSET = PACKED_Y_LENGTH;
    private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;

    public BlockPosition(int x, int y, int z) {
        super(x, y, z);
    }

    public BlockPosition(double x, double y, double z) {
        super(x, y, z);
    }

    public BlockPosition(Vec3D pos) {
        this(pos.x, pos.y, pos.z);
    }

    public BlockPosition(IPosition pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPosition(BaseBlockPosition pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public static long offset(long value, EnumDirection direction) {
        return offset(value, direction.getAdjacentX(), direction.getAdjacentY(), direction.getAdjacentZ());
    }

    public static long offset(long value, int x, int y, int z) {
        return asLong(getX(value) + x, getY(value) + y, getZ(value) + z);
    }

    public static int getX(long packedPos) {
        return (int)(packedPos << 64 - X_OFFSET - PACKED_X_LENGTH >> 64 - PACKED_X_LENGTH);
    }

    public static int getY(long packedPos) {
        return (int)(packedPos << 64 - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH);
    }

    public static int getZ(long packedPos) {
        return (int)(packedPos << 64 - Z_OFFSET - PACKED_Z_LENGTH >> 64 - PACKED_Z_LENGTH);
    }

    public static BlockPosition fromLong(long packedPos) {
        return new BlockPosition(getX(packedPos), getY(packedPos), getZ(packedPos));
    }

    public long asLong() {
        return asLong(this.getX(), this.getY(), this.getZ());
    }

    public static long asLong(int x, int y, int z) {
        long l = 0L;
        l = l | ((long)x & PACKED_X_MASK) << X_OFFSET;
        l = l | ((long)y & PACKED_Y_MASK) << 0;
        return l | ((long)z & PACKED_Z_MASK) << Z_OFFSET;
    }

    public static long getFlatIndex(long y) {
        return y & -16L;
    }

    @Override
    public BlockPosition offset(double d, double e, double f) {
        return d == 0.0D && e == 0.0D && f == 0.0D ? this : new BlockPosition((double)this.getX() + d, (double)this.getY() + e, (double)this.getZ() + f);
    }

    @Override
    public BlockPosition offset(int i, int j, int k) {
        return i == 0 && j == 0 && k == 0 ? this : new BlockPosition(this.getX() + i, this.getY() + j, this.getZ() + k);
    }

    @Override
    public BlockPosition offset(BaseBlockPosition vec3i) {
        return this.offset(vec3i.getX(), vec3i.getY(), vec3i.getZ());
    }

    @Override
    public BlockPosition subtract(BaseBlockPosition vec3i) {
        return this.offset(-vec3i.getX(), -vec3i.getY(), -vec3i.getZ());
    }

    @Override
    public BlockPosition multiply(int i) {
        if (i == 1) {
            return this;
        } else {
            return i == 0 ? ZERO : new BlockPosition(this.getX() * i, this.getY() * i, this.getZ() * i);
        }
    }

    @Override
    public BlockPosition above() {
        return this.relative(EnumDirection.UP);
    }

    @Override
    public BlockPosition above(int distance) {
        return this.relative(EnumDirection.UP, distance);
    }

    @Override
    public BlockPosition below() {
        return this.relative(EnumDirection.DOWN);
    }

    @Override
    public BlockPosition below(int i) {
        return this.relative(EnumDirection.DOWN, i);
    }

    @Override
    public BlockPosition north() {
        return this.relative(EnumDirection.NORTH);
    }

    @Override
    public BlockPosition north(int distance) {
        return this.relative(EnumDirection.NORTH, distance);
    }

    @Override
    public BlockPosition south() {
        return this.relative(EnumDirection.SOUTH);
    }

    @Override
    public BlockPosition south(int distance) {
        return this.relative(EnumDirection.SOUTH, distance);
    }

    @Override
    public BlockPosition west() {
        return this.relative(EnumDirection.WEST);
    }

    @Override
    public BlockPosition west(int distance) {
        return this.relative(EnumDirection.WEST, distance);
    }

    @Override
    public BlockPosition east() {
        return this.relative(EnumDirection.EAST);
    }

    @Override
    public BlockPosition east(int distance) {
        return this.relative(EnumDirection.EAST, distance);
    }

    @Override
    public BlockPosition relative(EnumDirection direction) {
        return new BlockPosition(this.getX() + direction.getAdjacentX(), this.getY() + direction.getAdjacentY(), this.getZ() + direction.getAdjacentZ());
    }

    @Override
    public BlockPosition relative(EnumDirection direction, int i) {
        return i == 0 ? this : new BlockPosition(this.getX() + direction.getAdjacentX() * i, this.getY() + direction.getAdjacentY() * i, this.getZ() + direction.getAdjacentZ() * i);
    }

    @Override
    public BlockPosition relative(EnumDirection.EnumAxis axis, int i) {
        if (i == 0) {
            return this;
        } else {
            int j = axis == EnumDirection.EnumAxis.X ? i : 0;
            int k = axis == EnumDirection.EnumAxis.Y ? i : 0;
            int l = axis == EnumDirection.EnumAxis.Z ? i : 0;
            return new BlockPosition(this.getX() + j, this.getY() + k, this.getZ() + l);
        }
    }

    public BlockPosition rotate(EnumBlockRotation rotation) {
        switch(rotation) {
        case NONE:
        default:
            return this;
        case CLOCKWISE_90:
            return new BlockPosition(-this.getZ(), this.getY(), this.getX());
        case CLOCKWISE_180:
            return new BlockPosition(-this.getX(), this.getY(), -this.getZ());
        case COUNTERCLOCKWISE_90:
            return new BlockPosition(this.getZ(), this.getY(), -this.getX());
        }
    }

    @Override
    public BlockPosition cross(BaseBlockPosition pos) {
        return new BlockPosition(this.getY() * pos.getZ() - this.getZ() * pos.getY(), this.getZ() * pos.getX() - this.getX() * pos.getZ(), this.getX() * pos.getY() - this.getY() * pos.getX());
    }

    public BlockPosition atY(int y) {
        return new BlockPosition(this.getX(), y, this.getZ());
    }

    public BlockPosition immutableCopy() {
        return this;
    }

    public BlockPosition.MutableBlockPosition mutable() {
        return new BlockPosition.MutableBlockPosition(this.getX(), this.getY(), this.getZ());
    }

    public static Iterable<BlockPosition> randomInCube(Random random, int count, BlockPosition around, int range) {
        return randomBetweenClosed(random, count, around.getX() - range, around.getY() - range, around.getZ() - range, around.getX() + range, around.getY() + range, around.getZ() + range);
    }

    public static Iterable<BlockPosition> randomBetweenClosed(Random random, int count, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int i = maxX - minX + 1;
        int j = maxY - minY + 1;
        int k = maxZ - minZ + 1;
        return () -> {
            return new AbstractIterator<BlockPosition>() {
                final BlockPosition.MutableBlockPosition nextPos = new BlockPosition.MutableBlockPosition();
                int counter = count;

                @Override
                protected BlockPosition computeNext() {
                    if (this.counter <= 0) {
                        return this.endOfData();
                    } else {
                        BlockPosition blockPos = this.nextPos.set(minX + random.nextInt(i), minY + random.nextInt(j), minZ + random.nextInt(k));
                        --this.counter;
                        return blockPos;
                    }
                }
            };
        };
    }

    public static Iterable<BlockPosition> withinManhattan(BlockPosition center, int rangeX, int rangeY, int rangeZ) {
        int i = rangeX + rangeY + rangeZ;
        int j = center.getX();
        int k = center.getY();
        int l = center.getZ();
        return () -> {
            return new AbstractIterator<BlockPosition>() {
                private final BlockPosition.MutableBlockPosition cursor = new BlockPosition.MutableBlockPosition();
                private int currentDepth;
                private int maxX;
                private int maxY;
                private int x;
                private int y;
                private boolean zMirror;

                @Override
                protected BlockPosition computeNext() {
                    if (this.zMirror) {
                        this.zMirror = false;
                        this.cursor.setZ(l - (this.cursor.getZ() - l));
                        return this.cursor;
                    } else {
                        BlockPosition blockPos;
                        for(blockPos = null; blockPos == null; ++this.y) {
                            if (this.y > this.maxY) {
                                ++this.x;
                                if (this.x > this.maxX) {
                                    ++this.currentDepth;
                                    if (this.currentDepth > i) {
                                        return this.endOfData();
                                    }

                                    this.maxX = Math.min(rangeX, this.currentDepth);
                                    this.x = -this.maxX;
                                }

                                this.maxY = Math.min(rangeY, this.currentDepth - Math.abs(this.x));
                                this.y = -this.maxY;
                            }

                            int i = this.x;
                            int j = this.y;
                            int k = this.currentDepth - Math.abs(i) - Math.abs(j);
                            if (k <= rangeZ) {
                                this.zMirror = k != 0;
                                blockPos = this.cursor.set(j + i, k + j, l + k);
                            }
                        }

                        return blockPos;
                    }
                }
            };
        };
    }

    public static Optional<BlockPosition> findClosestMatch(BlockPosition pos, int horizontalRange, int verticalRange, Predicate<BlockPosition> condition) {
        return withinManhattanStream(pos, horizontalRange, verticalRange, horizontalRange).filter(condition).findFirst();
    }

    public static Stream<BlockPosition> withinManhattanStream(BlockPosition center, int maxX, int maxY, int maxZ) {
        return StreamSupport.stream(withinManhattan(center, maxX, maxY, maxZ).spliterator(), false);
    }

    public static Iterable<BlockPosition> betweenClosed(BlockPosition start, BlockPosition end) {
        return betweenClosed(Math.min(start.getX(), end.getX()), Math.min(start.getY(), end.getY()), Math.min(start.getZ(), end.getZ()), Math.max(start.getX(), end.getX()), Math.max(start.getY(), end.getY()), Math.max(start.getZ(), end.getZ()));
    }

    public static Stream<BlockPosition> betweenClosedStream(BlockPosition start, BlockPosition end) {
        return StreamSupport.stream(betweenClosed(start, end).spliterator(), false);
    }

    public static Stream<BlockPosition> betweenClosedStream(StructureBoundingBox box) {
        return betweenClosedStream(Math.min(box.minX(), box.maxX()), Math.min(box.minY(), box.maxY()), Math.min(box.minZ(), box.maxZ()), Math.max(box.minX(), box.maxX()), Math.max(box.minY(), box.maxY()), Math.max(box.minZ(), box.maxZ()));
    }

    public static Stream<BlockPosition> betweenClosedStream(AxisAlignedBB box) {
        return betweenClosedStream(MathHelper.floor(box.minX), MathHelper.floor(box.minY), MathHelper.floor(box.minZ), MathHelper.floor(box.maxX), MathHelper.floor(box.maxY), MathHelper.floor(box.maxZ));
    }

    public static Stream<BlockPosition> betweenClosedStream(int startX, int startY, int startZ, int endX, int endY, int endZ) {
        return StreamSupport.stream(betweenClosed(startX, startY, startZ, endX, endY, endZ).spliterator(), false);
    }

    public static Iterable<BlockPosition> betweenClosed(int startX, int startY, int startZ, int endX, int endY, int endZ) {
        int i = endX - startX + 1;
        int j = endY - startY + 1;
        int k = endZ - startZ + 1;
        int l = i * j * k;
        return () -> {
            return new AbstractIterator<BlockPosition>() {
                private final BlockPosition.MutableBlockPosition cursor = new BlockPosition.MutableBlockPosition();
                private int index;

                @Override
                protected BlockPosition computeNext() {
                    if (this.index == l) {
                        return this.endOfData();
                    } else {
                        int i = this.index % i;
                        int j = this.index / i;
                        int k = j % j;
                        int l = j / j;
                        ++this.index;
                        return this.cursor.set(startX + i, startY + k, startZ + l);
                    }
                }
            };
        };
    }

    public static Iterable<BlockPosition.MutableBlockPosition> spiralAround(BlockPosition center, int radius, EnumDirection firstDirection, EnumDirection secondDirection) {
        Validate.validState(firstDirection.getAxis() != secondDirection.getAxis(), "The two directions cannot be on the same axis");
        return () -> {
            return new AbstractIterator<BlockPosition.MutableBlockPosition>() {
                private final EnumDirection[] directions = new EnumDirection[]{firstDirection, secondDirection, firstDirection.opposite(), secondDirection.opposite()};
                private final BlockPosition.MutableBlockPosition cursor = center.mutable().move(secondDirection);
                private final int legs = 4 * radius;
                private int leg = -1;
                private int legSize;
                private int legIndex;
                private int lastX = this.cursor.getX();
                private int lastY = this.cursor.getY();
                private int lastZ = this.cursor.getZ();

                @Override
                protected BlockPosition.MutableBlockPosition computeNext() {
                    this.cursor.set(this.lastX, this.lastY, this.lastZ).move(this.directions[(this.leg + 4) % 4]);
                    this.lastX = this.cursor.getX();
                    this.lastY = this.cursor.getY();
                    this.lastZ = this.cursor.getZ();
                    if (this.legIndex >= this.legSize) {
                        if (this.leg >= this.legs) {
                            return this.endOfData();
                        }

                        ++this.leg;
                        this.legIndex = 0;
                        this.legSize = this.leg / 2 + 1;
                    }

                    ++this.legIndex;
                    return this.cursor;
                }
            };
        };
    }

    public static class MutableBlockPosition extends BlockPosition {
        public MutableBlockPosition() {
            this(0, 0, 0);
        }

        public MutableBlockPosition(int x, int y, int z) {
            super(x, y, z);
        }

        public MutableBlockPosition(double x, double y, double z) {
            this(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        }

        @Override
        public BlockPosition offset(double d, double e, double f) {
            return super.offset(d, e, f).immutableCopy();
        }

        @Override
        public BlockPosition offset(int i, int j, int k) {
            return super.offset(i, j, k).immutableCopy();
        }

        @Override
        public BlockPosition multiply(int i) {
            return super.multiply(i).immutableCopy();
        }

        @Override
        public BlockPosition relative(EnumDirection direction, int i) {
            return super.relative(direction, i).immutableCopy();
        }

        @Override
        public BlockPosition relative(EnumDirection.EnumAxis axis, int i) {
            return super.relative(axis, i).immutableCopy();
        }

        @Override
        public BlockPosition rotate(EnumBlockRotation rotation) {
            return super.rotate(rotation).immutableCopy();
        }

        public BlockPosition.MutableBlockPosition set(int x, int y, int z) {
            this.setX(x);
            this.setY(y);
            this.setZ(z);
            return this;
        }

        public BlockPosition.MutableBlockPosition set(double x, double y, double z) {
            return this.set(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        }

        public BlockPosition.MutableBlockPosition set(BaseBlockPosition pos) {
            return this.set(pos.getX(), pos.getY(), pos.getZ());
        }

        public BlockPosition.MutableBlockPosition set(long pos) {
            return this.set(getX(pos), getY(pos), getZ(pos));
        }

        public BlockPosition.MutableBlockPosition set(EnumAxisCycle axis, int x, int y, int z) {
            return this.set(axis.cycle(x, y, z, EnumDirection.EnumAxis.X), axis.cycle(x, y, z, EnumDirection.EnumAxis.Y), axis.cycle(x, y, z, EnumDirection.EnumAxis.Z));
        }

        public BlockPosition.MutableBlockPosition setWithOffset(BaseBlockPosition pos, EnumDirection direction) {
            return this.set(pos.getX() + direction.getAdjacentX(), pos.getY() + direction.getAdjacentY(), pos.getZ() + direction.getAdjacentZ());
        }

        public BlockPosition.MutableBlockPosition setWithOffset(BaseBlockPosition pos, int x, int y, int z) {
            return this.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
        }

        public BlockPosition.MutableBlockPosition setWithOffset(BaseBlockPosition vec1, BaseBlockPosition vec2) {
            return this.set(vec1.getX() + vec2.getX(), vec1.getY() + vec2.getY(), vec1.getZ() + vec2.getZ());
        }

        public BlockPosition.MutableBlockPosition move(EnumDirection direction) {
            return this.move(direction, 1);
        }

        public BlockPosition.MutableBlockPosition move(EnumDirection direction, int distance) {
            return this.set(this.getX() + direction.getAdjacentX() * distance, this.getY() + direction.getAdjacentY() * distance, this.getZ() + direction.getAdjacentZ() * distance);
        }

        public BlockPosition.MutableBlockPosition move(int dx, int dy, int dz) {
            return this.set(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
        }

        public BlockPosition.MutableBlockPosition move(BaseBlockPosition vec) {
            return this.set(this.getX() + vec.getX(), this.getY() + vec.getY(), this.getZ() + vec.getZ());
        }

        public BlockPosition.MutableBlockPosition clamp(EnumDirection.EnumAxis axis, int min, int max) {
            switch(axis) {
            case X:
                return this.set(MathHelper.clamp(this.getX(), min, max), this.getY(), this.getZ());
            case Y:
                return this.set(this.getX(), MathHelper.clamp(this.getY(), min, max), this.getZ());
            case Z:
                return this.set(this.getX(), this.getY(), MathHelper.clamp(this.getZ(), min, max));
            default:
                throw new IllegalStateException("Unable to clamp axis " + axis);
            }
        }

        @Override
        public BlockPosition.MutableBlockPosition setX(int i) {
            super.setX(i);
            return this;
        }

        @Override
        public BlockPosition.MutableBlockPosition setY(int i) {
            super.setY(i);
            return this;
        }

        @Override
        public BlockPosition.MutableBlockPosition setZ(int i) {
            super.setZ(i);
            return this;
        }

        @Override
        public BlockPosition immutableCopy() {
            return new BlockPosition(this);
        }
    }
}
