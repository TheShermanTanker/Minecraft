package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalPanic extends PathfinderGoal {
    protected final EntityCreature mob;
    protected final double speedModifier;
    protected double posX;
    protected double posY;
    protected double posZ;
    protected boolean isRunning;

    public PathfinderGoalPanic(EntityCreature mob, double speed) {
        this.mob = mob;
        this.speedModifier = speed;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getLastDamager() == null && !this.mob.isBurning()) {
            return false;
        } else {
            if (this.mob.isBurning()) {
                BlockPosition blockPos = this.lookForWater(this.mob.level, this.mob, 5, 4);
                if (blockPos != null) {
                    this.posX = (double)blockPos.getX();
                    this.posY = (double)blockPos.getY();
                    this.posZ = (double)blockPos.getZ();
                    return true;
                }
            }

            return this.findRandomPosition();
        }
    }

    protected boolean findRandomPosition() {
        Vec3D vec3 = DefaultRandomPos.getPos(this.mob, 5, 4);
        if (vec3 == null) {
            return false;
        } else {
            this.posX = vec3.x;
            this.posY = vec3.y;
            this.posZ = vec3.z;
            return true;
        }
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.posX, this.posY, this.posZ, this.speedModifier);
        this.isRunning = true;
    }

    @Override
    public void stop() {
        this.isRunning = false;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone();
    }

    @Nullable
    protected BlockPosition lookForWater(IBlockAccess blockView, Entity entity, int rangeX, int rangeY) {
        BlockPosition blockPos = entity.getChunkCoordinates();
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        float f = (float)(rangeX * rangeX * rangeY * 2);
        BlockPosition blockPos2 = null;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int l = i - rangeX; l <= i + rangeX; ++l) {
            for(int m = j - rangeY; m <= j + rangeY; ++m) {
                for(int n = k - rangeX; n <= k + rangeX; ++n) {
                    mutableBlockPos.set(l, m, n);
                    if (blockView.getFluid(mutableBlockPos).is(TagsFluid.WATER)) {
                        float g = (float)((l - i) * (l - i) + (m - j) * (m - j) + (n - k) * (n - k));
                        if (g < f) {
                            f = g;
                            blockPos2 = new BlockPosition(mutableBlockPos);
                        }
                    }
                }
            }
        }

        return blockPos2;
    }
}
