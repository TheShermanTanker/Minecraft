package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;

public class PathfinderGoalWrapped extends PathfinderGoal {
    private final PathfinderGoal goal;
    private final int priority;
    private boolean isRunning;

    public PathfinderGoalWrapped(int priority, PathfinderGoal goal) {
        this.priority = priority;
        this.goal = goal;
    }

    public boolean canBeReplacedBy(PathfinderGoalWrapped goal) {
        return this.isInterruptable() && goal.getPriority() < this.getPriority();
    }

    @Override
    public boolean canUse() {
        return this.goal.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.goal.canContinueToUse();
    }

    @Override
    public boolean isInterruptable() {
        return this.goal.isInterruptable();
    }

    @Override
    public void start() {
        if (!this.isRunning) {
            this.isRunning = true;
            this.goal.start();
        }
    }

    @Override
    public void stop() {
        if (this.isRunning) {
            this.isRunning = false;
            this.goal.stop();
        }
    }

    @Override
    public void tick() {
        this.goal.tick();
    }

    @Override
    public void setFlags(EnumSet<PathfinderGoal.Type> controls) {
        this.goal.setFlags(controls);
    }

    @Override
    public EnumSet<PathfinderGoal.Type> getFlags() {
        return this.goal.getFlags();
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public int getPriority() {
        return this.priority;
    }

    public PathfinderGoal getGoal() {
        return this.goal;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) {
            return true;
        } else {
            return object != null && this.getClass() == object.getClass() ? this.goal.equals(((PathfinderGoalWrapped)object).goal) : false;
        }
    }

    @Override
    public int hashCode() {
        return this.goal.hashCode();
    }
}
