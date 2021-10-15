package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.level.IWorldReader;

public abstract class PathfinderGoalGotoTarget extends PathfinderGoal {
    private static final int GIVE_UP_TICKS = 1200;
    private static final int STAY_TICKS = 1200;
    private static final int INTERVAL_TICKS = 200;
    protected final EntityCreature mob;
    public final double speedModifier;
    protected int nextStartTick;
    protected int tryTicks;
    private int maxStayTicks;
    protected BlockPosition blockPos = BlockPosition.ZERO;
    private boolean reachedTarget;
    private final int searchRange;
    private final int verticalSearchRange;
    protected int verticalSearchStart;

    public PathfinderGoalGotoTarget(EntityCreature mob, double speed, int range) {
        this(mob, speed, range, 1);
    }

    public PathfinderGoalGotoTarget(EntityCreature mob, double speed, int range, int maxYDifference) {
        this.mob = mob;
        this.speedModifier = speed;
        this.searchRange = range;
        this.verticalSearchStart = 0;
        this.verticalSearchRange = maxYDifference;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        } else {
            this.nextStartTick = this.nextStartTick(this.mob);
            return this.findNearestBlock();
        }
    }

    protected int nextStartTick(EntityCreature mob) {
        return 200 + mob.getRandom().nextInt(200);
    }

    @Override
    public boolean canContinueToUse() {
        return this.tryTicks >= -this.maxStayTicks && this.tryTicks <= 1200 && this.isValidTarget(this.mob.level, this.blockPos);
    }

    @Override
    public void start() {
        this.moveMobToBlock();
        this.tryTicks = 0;
        this.maxStayTicks = this.mob.getRandom().nextInt(this.mob.getRandom().nextInt(1200) + 1200) + 1200;
    }

    protected void moveMobToBlock() {
        this.mob.getNavigation().moveTo((double)((float)this.blockPos.getX()) + 0.5D, (double)(this.blockPos.getY() + 1), (double)((float)this.blockPos.getZ()) + 0.5D, this.speedModifier);
    }

    public double acceptedDistance() {
        return 1.0D;
    }

    protected BlockPosition getMoveToTarget() {
        return this.blockPos.above();
    }

    @Override
    public void tick() {
        BlockPosition blockPos = this.getMoveToTarget();
        if (!blockPos.closerThan(this.mob.getPositionVector(), this.acceptedDistance())) {
            this.reachedTarget = false;
            ++this.tryTicks;
            if (this.shouldRecalculatePath()) {
                this.mob.getNavigation().moveTo((double)((float)blockPos.getX()) + 0.5D, (double)blockPos.getY(), (double)((float)blockPos.getZ()) + 0.5D, this.speedModifier);
            }
        } else {
            this.reachedTarget = true;
            --this.tryTicks;
        }

    }

    public boolean shouldRecalculatePath() {
        return this.tryTicks % 40 == 0;
    }

    protected boolean isReachedTarget() {
        return this.reachedTarget;
    }

    protected boolean findNearestBlock() {
        int i = this.searchRange;
        int j = this.verticalSearchRange;
        BlockPosition blockPos = this.mob.getChunkCoordinates();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int k = this.verticalSearchStart; k <= j; k = k > 0 ? -k : 1 - k) {
            for(int l = 0; l < i; ++l) {
                for(int m = 0; m <= l; m = m > 0 ? -m : 1 - m) {
                    for(int n = m < l && m > -l ? l : 0; n <= l; n = n > 0 ? -n : 1 - n) {
                        mutableBlockPos.setWithOffset(blockPos, m, k - 1, n);
                        if (this.mob.isWithinRestriction(mutableBlockPos) && this.isValidTarget(this.mob.level, mutableBlockPos)) {
                            this.blockPos = mutableBlockPos;
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    protected abstract boolean isValidTarget(IWorldReader world, BlockPosition pos);
}
