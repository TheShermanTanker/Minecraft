package net.minecraft.world.phys;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;

public class MovingObjectPositionBlock extends MovingObjectPosition {
    private final EnumDirection direction;
    private final BlockPosition blockPos;
    private final boolean miss;
    private final boolean inside;

    public static MovingObjectPositionBlock miss(Vec3D pos, EnumDirection side, BlockPosition blockPos) {
        return new MovingObjectPositionBlock(true, pos, side, blockPos, false);
    }

    public MovingObjectPositionBlock(Vec3D pos, EnumDirection side, BlockPosition blockPos, boolean insideBlock) {
        this(false, pos, side, blockPos, insideBlock);
    }

    private MovingObjectPositionBlock(boolean missed, Vec3D pos, EnumDirection side, BlockPosition blockPos, boolean insideBlock) {
        super(pos);
        this.miss = missed;
        this.direction = side;
        this.blockPos = blockPos;
        this.inside = insideBlock;
    }

    public MovingObjectPositionBlock withDirection(EnumDirection side) {
        return new MovingObjectPositionBlock(this.miss, this.location, side, this.blockPos, this.inside);
    }

    public MovingObjectPositionBlock withPosition(BlockPosition blockPos) {
        return new MovingObjectPositionBlock(this.miss, this.location, this.direction, blockPos, this.inside);
    }

    public BlockPosition getBlockPosition() {
        return this.blockPos;
    }

    public EnumDirection getDirection() {
        return this.direction;
    }

    @Override
    public MovingObjectPosition.EnumMovingObjectType getType() {
        return this.miss ? MovingObjectPosition.EnumMovingObjectType.MISS : MovingObjectPosition.EnumMovingObjectType.BLOCK;
    }

    public boolean isInside() {
        return this.inside;
    }
}
