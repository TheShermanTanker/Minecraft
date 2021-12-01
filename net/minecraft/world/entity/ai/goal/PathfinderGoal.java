package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.util.MathHelper;

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

    public boolean requiresUpdateEveryTick() {
        return false;
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

    protected int adjustedTickDelay(int ticks) {
        return this.requiresUpdateEveryTick() ? ticks : reducedTickDelay(ticks);
    }

    protected static int reducedTickDelay(int serverTicks) {
        return MathHelper.positiveCeilDiv(serverTicks, 2);
    }

    public static enum Type {
        MOVE,
        LOOK,
        JUMP,
        TARGET;
    }
}
