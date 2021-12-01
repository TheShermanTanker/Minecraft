package net.minecraft.world.level.levelgen.structure;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StructureBoundingBox {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Codec<StructureBoundingBox> CODEC = Codec.INT_STREAM.comapFlatMap((values) -> {
        return SystemUtils.fixedSize(values, 6).map((array) -> {
            return new StructureBoundingBox(array[0], array[1], array[2], array[3], array[4], array[5]);
        });
    }, (box) -> {
        return IntStream.of(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }).stable();
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    public StructureBoundingBox(BlockPosition pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public StructureBoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        if (maxX < minX || maxY < minY || maxZ < minZ) {
            String string = "Invalid bounding box data, inverted bounds for: " + this;
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                throw new IllegalStateException(string);
            }

            LOGGER.error(string);
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
        }

    }

    public static StructureBoundingBox fromCorners(BaseBlockPosition first, BaseBlockPosition second) {
        return new StructureBoundingBox(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()), Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
    }

    public static StructureBoundingBox infinite() {
        return new StructureBoundingBox(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public static StructureBoundingBox orientBox(int x, int y, int z, int offsetX, int offsetY, int offsetZ, int sizeX, int sizeY, int sizeZ, EnumDirection facing) {
        switch(facing) {
        case SOUTH:
        default:
            return new StructureBoundingBox(x + offsetX, y + offsetY, z + offsetZ, x + sizeX - 1 + offsetX, y + sizeY - 1 + offsetY, z + sizeZ - 1 + offsetZ);
        case NORTH:
            return new StructureBoundingBox(x + offsetX, y + offsetY, z - sizeZ + 1 + offsetZ, x + sizeX - 1 + offsetX, y + sizeY - 1 + offsetY, z + offsetZ);
        case WEST:
            return new StructureBoundingBox(x - sizeZ + 1 + offsetZ, y + offsetY, z + offsetX, x + offsetZ, y + sizeY - 1 + offsetY, z + sizeX - 1 + offsetX);
        case EAST:
            return new StructureBoundingBox(x + offsetZ, y + offsetY, z + offsetX, x + sizeZ - 1 + offsetZ, y + sizeY - 1 + offsetY, z + sizeX - 1 + offsetX);
        }
    }

    public boolean intersects(StructureBoundingBox other) {
        return this.maxX >= other.minX && this.minX <= other.maxX && this.maxZ >= other.minZ && this.minZ <= other.maxZ && this.maxY >= other.minY && this.minY <= other.maxY;
    }

    public boolean intersects(int minX, int minZ, int maxX, int maxZ) {
        return this.maxX >= minX && this.minX <= maxX && this.maxZ >= minZ && this.minZ <= maxZ;
    }

    public static Optional<StructureBoundingBox> encapsulatingPositions(Iterable<BlockPosition> positions) {
        Iterator<BlockPosition> iterator = positions.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        } else {
            StructureBoundingBox boundingBox = new StructureBoundingBox(iterator.next());
            iterator.forEachRemaining(boundingBox::encapsulate);
            return Optional.of(boundingBox);
        }
    }

    public static Optional<StructureBoundingBox> encapsulatingBoxes(Iterable<StructureBoundingBox> boxes) {
        Iterator<StructureBoundingBox> iterator = boxes.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        } else {
            StructureBoundingBox boundingBox = iterator.next();
            StructureBoundingBox boundingBox2 = new StructureBoundingBox(boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
            iterator.forEachRemaining(boundingBox2::encapsulate);
            return Optional.of(boundingBox2);
        }
    }

    /** @deprecated */
    @Deprecated
    public StructureBoundingBox encapsulate(StructureBoundingBox box) {
        this.minX = Math.min(this.minX, box.minX);
        this.minY = Math.min(this.minY, box.minY);
        this.minZ = Math.min(this.minZ, box.minZ);
        this.maxX = Math.max(this.maxX, box.maxX);
        this.maxY = Math.max(this.maxY, box.maxY);
        this.maxZ = Math.max(this.maxZ, box.maxZ);
        return this;
    }

    /** @deprecated */
    @Deprecated
    public StructureBoundingBox encapsulate(BlockPosition pos) {
        this.minX = Math.min(this.minX, pos.getX());
        this.minY = Math.min(this.minY, pos.getY());
        this.minZ = Math.min(this.minZ, pos.getZ());
        this.maxX = Math.max(this.maxX, pos.getX());
        this.maxY = Math.max(this.maxY, pos.getY());
        this.maxZ = Math.max(this.maxZ, pos.getZ());
        return this;
    }

    /** @deprecated */
    @Deprecated
    public StructureBoundingBox move(int dx, int dy, int dz) {
        this.minX += dx;
        this.minY += dy;
        this.minZ += dz;
        this.maxX += dx;
        this.maxY += dy;
        this.maxZ += dz;
        return this;
    }

    /** @deprecated */
    @Deprecated
    public StructureBoundingBox move(BaseBlockPosition vec) {
        return this.move(vec.getX(), vec.getY(), vec.getZ());
    }

    public StructureBoundingBox moved(int x, int y, int z) {
        return new StructureBoundingBox(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    public StructureBoundingBox inflatedBy(int offset) {
        return new StructureBoundingBox(this.minX() - offset, this.minY() - offset, this.minZ() - offset, this.maxX() + offset, this.maxY() + offset, this.maxZ() + offset);
    }

    public boolean isInside(BaseBlockPosition vec) {
        return vec.getX() >= this.minX && vec.getX() <= this.maxX && vec.getZ() >= this.minZ && vec.getZ() <= this.maxZ && vec.getY() >= this.minY && vec.getY() <= this.maxY;
    }

    public BaseBlockPosition getLength() {
        return new BaseBlockPosition(this.maxX - this.minX, this.maxY - this.minY, this.maxZ - this.minZ);
    }

    public int getXSpan() {
        return this.maxX - this.minX + 1;
    }

    public int getYSpan() {
        return this.maxY - this.minY + 1;
    }

    public int getZSpan() {
        return this.maxZ - this.minZ + 1;
    }

    public BlockPosition getCenter() {
        return new BlockPosition(this.minX + (this.maxX - this.minX + 1) / 2, this.minY + (this.maxY - this.minY + 1) / 2, this.minZ + (this.maxZ - this.minZ + 1) / 2);
    }

    public void forAllCorners(Consumer<BlockPosition> consumer) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        consumer.accept(mutableBlockPos.set(this.maxX, this.maxY, this.maxZ));
        consumer.accept(mutableBlockPos.set(this.minX, this.maxY, this.maxZ));
        consumer.accept(mutableBlockPos.set(this.maxX, this.minY, this.maxZ));
        consumer.accept(mutableBlockPos.set(this.minX, this.minY, this.maxZ));
        consumer.accept(mutableBlockPos.set(this.maxX, this.maxY, this.minZ));
        consumer.accept(mutableBlockPos.set(this.minX, this.maxY, this.minZ));
        consumer.accept(mutableBlockPos.set(this.maxX, this.minY, this.minZ));
        consumer.accept(mutableBlockPos.set(this.minX, this.minY, this.minZ));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("minX", this.minX).add("minY", this.minY).add("minZ", this.minZ).add("maxX", this.maxX).add("maxY", this.maxY).add("maxZ", this.maxZ).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof StructureBoundingBox)) {
            return false;
        } else {
            StructureBoundingBox boundingBox = (StructureBoundingBox)object;
            return this.minX == boundingBox.minX && this.minY == boundingBox.minY && this.minZ == boundingBox.minZ && this.maxX == boundingBox.maxX && this.maxY == boundingBox.maxY && this.maxZ == boundingBox.maxZ;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public int minX() {
        return this.minX;
    }

    public int minY() {
        return this.minY;
    }

    public int minZ() {
        return this.minZ;
    }

    public int maxX() {
        return this.maxX;
    }

    public int maxY() {
        return this.maxY;
    }

    public int maxZ() {
        return this.maxZ;
    }
}
