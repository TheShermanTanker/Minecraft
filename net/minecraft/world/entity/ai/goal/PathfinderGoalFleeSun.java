package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalFleeSun extends PathfinderGoal {
    protected final EntityCreature mob;
    private double wantedX;
    private double wantedY;
    private double wantedZ;
    private final double speedModifier;
    private final World level;

    public PathfinderGoalFleeSun(EntityCreature mob, double speed) {
        this.mob = mob;
        this.speedModifier = speed;
        this.level = mob.level;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getGoalTarget() != null) {
            return false;
        } else if (!this.level.isDay()) {
            return false;
        } else if (!this.mob.isBurning()) {
            return false;
        } else if (!this.level.canSeeSky(this.mob.getChunkCoordinates())) {
            return false;
        } else {
            return !this.mob.getEquipment(EnumItemSlot.HEAD).isEmpty() ? false : this.setWantedPos();
        }
    }

    protected boolean setWantedPos() {
        Vec3D vec3 = this.getHidePos();
        if (vec3 == null) {
            return false;
        } else {
            this.wantedX = vec3.x;
            this.wantedY = vec3.y;
            this.wantedZ = vec3.z;
            return true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
    }

    @Nullable
    protected Vec3D getHidePos() {
        Random random = this.mob.getRandom();
        BlockPosition blockPos = this.mob.getChunkCoordinates();

        for(int i = 0; i < 10; ++i) {
            BlockPosition blockPos2 = blockPos.offset(random.nextInt(20) - 10, random.nextInt(6) - 3, random.nextInt(20) - 10);
            if (!this.level.canSeeSky(blockPos2) && this.mob.getWalkTargetValue(blockPos2) < 0.0F) {
                return Vec3D.atBottomCenterOf(blockPos2);
            }
        }

        return null;
    }
}
