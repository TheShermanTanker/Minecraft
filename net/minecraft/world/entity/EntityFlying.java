package net.minecraft.world.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

public abstract class EntityFlying extends EntityInsentient {
    protected EntityFlying(EntityTypes<? extends EntityFlying> type, World world) {
        super(type, world);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, IBlockData landedState, BlockPosition landedPosition) {
    }

    @Override
    public void travel(Vec3D movementInput) {
        if (this.isInWater()) {
            this.moveRelative(0.02F, movementInput);
            this.move(EnumMoveType.SELF, this.getMot());
            this.setMot(this.getMot().scale((double)0.8F));
        } else if (this.isInLava()) {
            this.moveRelative(0.02F, movementInput);
            this.move(EnumMoveType.SELF, this.getMot());
            this.setMot(this.getMot().scale(0.5D));
        } else {
            float f = 0.91F;
            if (this.onGround) {
                f = this.level.getType(new BlockPosition(this.locX(), this.locY() - 1.0D, this.locZ())).getBlock().getFrictionFactor() * 0.91F;
            }

            float g = 0.16277137F / (f * f * f);
            f = 0.91F;
            if (this.onGround) {
                f = this.level.getType(new BlockPosition(this.locX(), this.locY() - 1.0D, this.locZ())).getBlock().getFrictionFactor() * 0.91F;
            }

            this.moveRelative(this.onGround ? 0.1F * g : 0.02F, movementInput);
            this.move(EnumMoveType.SELF, this.getMot());
            this.setMot(this.getMot().scale((double)f));
        }

        this.calculateEntityAnimation(this, false);
    }

    @Override
    public boolean isCurrentlyClimbing() {
        return false;
    }
}
