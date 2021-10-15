package net.minecraft.world.entity.ai.control;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfinderAbstract;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ControllerMove implements Control {
    public static final float MIN_SPEED = 5.0E-4F;
    public static final float MIN_SPEED_SQR = 2.5000003E-7F;
    protected static final int MAX_TURN = 90;
    protected final EntityInsentient mob;
    protected double wantedX;
    protected double wantedY;
    protected double wantedZ;
    protected double speedModifier;
    protected float strafeForwards;
    protected float strafeRight;
    protected ControllerMove.Operation operation = ControllerMove.Operation.WAIT;

    public ControllerMove(EntityInsentient entity) {
        this.mob = entity;
    }

    public boolean hasWanted() {
        return this.operation == ControllerMove.Operation.MOVE_TO;
    }

    public double getSpeedModifier() {
        return this.speedModifier;
    }

    public void setWantedPosition(double x, double y, double z, double speed) {
        this.wantedX = x;
        this.wantedY = y;
        this.wantedZ = z;
        this.speedModifier = speed;
        if (this.operation != ControllerMove.Operation.JUMPING) {
            this.operation = ControllerMove.Operation.MOVE_TO;
        }

    }

    public void strafe(float forward, float sideways) {
        this.operation = ControllerMove.Operation.STRAFE;
        this.strafeForwards = forward;
        this.strafeRight = sideways;
        this.speedModifier = 0.25D;
    }

    public void tick() {
        if (this.operation == ControllerMove.Operation.STRAFE) {
            float f = (float)this.mob.getAttributeValue(GenericAttributes.MOVEMENT_SPEED);
            float g = (float)this.speedModifier * f;
            float h = this.strafeForwards;
            float i = this.strafeRight;
            float j = MathHelper.sqrt(h * h + i * i);
            if (j < 1.0F) {
                j = 1.0F;
            }

            j = g / j;
            h = h * j;
            i = i * j;
            float k = MathHelper.sin(this.mob.getYRot() * ((float)Math.PI / 180F));
            float l = MathHelper.cos(this.mob.getYRot() * ((float)Math.PI / 180F));
            float m = h * l - i * k;
            float n = i * l + h * k;
            if (!this.isWalkable(m, n)) {
                this.strafeForwards = 1.0F;
                this.strafeRight = 0.0F;
            }

            this.mob.setSpeed(g);
            this.mob.setZza(this.strafeForwards);
            this.mob.setXxa(this.strafeRight);
            this.operation = ControllerMove.Operation.WAIT;
        } else if (this.operation == ControllerMove.Operation.MOVE_TO) {
            this.operation = ControllerMove.Operation.WAIT;
            double d = this.wantedX - this.mob.locX();
            double e = this.wantedZ - this.mob.locZ();
            double o = this.wantedY - this.mob.locY();
            double p = d * d + o * o + e * e;
            if (p < (double)2.5000003E-7F) {
                this.mob.setZza(0.0F);
                return;
            }

            float q = (float)(MathHelper.atan2(e, d) * (double)(180F / (float)Math.PI)) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), q, 90.0F));
            this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(GenericAttributes.MOVEMENT_SPEED)));
            BlockPosition blockPos = this.mob.getChunkCoordinates();
            IBlockData blockState = this.mob.level.getType(blockPos);
            VoxelShape voxelShape = blockState.getCollisionShape(this.mob.level, blockPos);
            if (o > (double)this.mob.maxUpStep && d * d + e * e < (double)Math.max(1.0F, this.mob.getWidth()) || !voxelShape.isEmpty() && this.mob.locY() < voxelShape.max(EnumDirection.EnumAxis.Y) + (double)blockPos.getY() && !blockState.is(TagsBlock.DOORS) && !blockState.is(TagsBlock.FENCES)) {
                this.mob.getControllerJump().jump();
                this.operation = ControllerMove.Operation.JUMPING;
            }
        } else if (this.operation == ControllerMove.Operation.JUMPING) {
            this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(GenericAttributes.MOVEMENT_SPEED)));
            if (this.mob.isOnGround()) {
                this.operation = ControllerMove.Operation.WAIT;
            }
        } else {
            this.mob.setZza(0.0F);
        }

    }

    private boolean isWalkable(float f, float g) {
        NavigationAbstract pathNavigation = this.mob.getNavigation();
        if (pathNavigation != null) {
            PathfinderAbstract nodeEvaluator = pathNavigation.getPathFinder();
            if (nodeEvaluator != null && nodeEvaluator.getBlockPathType(this.mob.level, MathHelper.floor(this.mob.locX() + (double)f), this.mob.getBlockY(), MathHelper.floor(this.mob.locZ() + (double)g)) != PathType.WALKABLE) {
                return false;
            }
        }

        return true;
    }

    protected float rotlerp(float from, float to, float max) {
        float f = MathHelper.wrapDegrees(to - from);
        if (f > max) {
            f = max;
        }

        if (f < -max) {
            f = -max;
        }

        float g = from + f;
        if (g < 0.0F) {
            g += 360.0F;
        } else if (g > 360.0F) {
            g -= 360.0F;
        }

        return g;
    }

    public double getWantedX() {
        return this.wantedX;
    }

    public double getWantedY() {
        return this.wantedY;
    }

    public double getWantedZ() {
        return this.wantedZ;
    }

    public static enum Operation {
        WAIT,
        MOVE_TO,
        STRAFE,
        JUMPING;
    }
}
