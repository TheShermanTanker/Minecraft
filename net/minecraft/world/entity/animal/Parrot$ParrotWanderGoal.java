package net.minecraft.world.entity.animal;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomFly;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.block.BlockLeaves;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

class Parrot$ParrotWanderGoal extends PathfinderGoalRandomFly {
    public Parrot$ParrotWanderGoal(EntityCreature mob, double speed) {
        super(mob, speed);
    }

    @Nullable
    @Override
    protected Vec3D getPosition() {
        Vec3D vec3 = null;
        if (this.mob.isInWater()) {
            vec3 = LandRandomPos.getPos(this.mob, 15, 15);
        }

        if (this.mob.getRandom().nextFloat() >= this.probability) {
            vec3 = this.getTreePos();
        }

        return vec3 == null ? super.getPosition() : vec3;
    }

    @Nullable
    private Vec3D getTreePos() {
        BlockPosition blockPos = this.mob.getChunkCoordinates();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        BlockPosition.MutableBlockPosition mutableBlockPos2 = new BlockPosition.MutableBlockPosition();

        for(BlockPosition blockPos2 : BlockPosition.betweenClosed(MathHelper.floor(this.mob.locX() - 3.0D), MathHelper.floor(this.mob.locY() - 6.0D), MathHelper.floor(this.mob.locZ() - 3.0D), MathHelper.floor(this.mob.locX() + 3.0D), MathHelper.floor(this.mob.locY() + 6.0D), MathHelper.floor(this.mob.locZ() + 3.0D))) {
            if (!blockPos.equals(blockPos2)) {
                IBlockData blockState = this.mob.level.getType(mutableBlockPos2.setWithOffset(blockPos2, EnumDirection.DOWN));
                boolean bl = blockState.getBlock() instanceof BlockLeaves || blockState.is(TagsBlock.LOGS);
                if (bl && this.mob.level.isEmpty(blockPos2) && this.mob.level.isEmpty(mutableBlockPos.setWithOffset(blockPos2, EnumDirection.UP))) {
                    return Vec3D.atBottomCenterOf(blockPos2);
                }
            }
        }

        return null;
    }
}
