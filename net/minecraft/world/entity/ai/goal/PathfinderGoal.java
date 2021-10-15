package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;

public abstract class PathfinderGoal {
    private final EnumSet<PathfinderGoal.Type> flags = EnumSet.noneOf(PathfinderGoal.Type.class);

    public abstract boolean canUse();

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public boolean isInterruptable() {
        return true;
    }

    public void start() {
    }

    public void stop() {
    }

    public void tick() {
    }

    public void setFlags(EnumSet<PathfinderGoal.Type> controls) {
        this.flags.clear();
        this.flags.addAll(controls);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public EnumSet<PathfinderGoal.Type> getFlags() {
        return this.flags;
    }

    public static enum Type {
        MOVE,
        LOOK,
        JUMP,
        TARGET;
    }
}
