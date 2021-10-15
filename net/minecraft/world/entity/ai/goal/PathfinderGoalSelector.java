package net.minecraft.world.entity.ai.goal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.util.profiling.GameProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PathfinderGoalSelector {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final PathfinderGoalWrapped NO_GOAL = new PathfinderGoalWrapped(Integer.MAX_VALUE, new PathfinderGoal() {
        @Override
        public boolean canUse() {
            return false;
        }
    }) {
        @Override
        public boolean isRunning() {
            return false;
        }
    };
    private final Map<PathfinderGoal.Type, PathfinderGoalWrapped> lockedFlags = new EnumMap<>(PathfinderGoal.Type.class);
    public final Set<PathfinderGoalWrapped> availableGoals = Sets.newLinkedHashSet();
    private final Supplier<GameProfilerFiller> profiler;
    private final EnumSet<PathfinderGoal.Type> disabledFlags = EnumSet.noneOf(PathfinderGoal.Type.class);
    private int tickCount;
    private int newGoalRate = 3;

    public PathfinderGoalSelector(Supplier<GameProfilerFiller> profiler) {
        this.profiler = profiler;
    }

    public void addGoal(int priority, PathfinderGoal goal) {
        this.availableGoals.add(new PathfinderGoalWrapped(priority, goal));
    }

    @VisibleForTesting
    public void removeAllGoals() {
        this.availableGoals.clear();
    }

    public void removeGoal(PathfinderGoal goal) {
        this.availableGoals.stream().filter((wrappedGoal) -> {
            return wrappedGoal.getGoal() == goal;
        }).filter(PathfinderGoalWrapped::isRunning).forEach(PathfinderGoalWrapped::stop);
        this.availableGoals.removeIf((wrappedGoal) -> {
            return wrappedGoal.getGoal() == goal;
        });
    }

    public void doTick() {
        GameProfilerFiller profilerFiller = this.profiler.get();
        profilerFiller.enter("goalCleanup");
        this.getRunningGoals().filter((wrappedGoal) -> {
            return !wrappedGoal.isRunning() || wrappedGoal.getFlags().stream().anyMatch(this.disabledFlags::contains) || !wrappedGoal.canContinueToUse();
        }).forEach(PathfinderGoal::stop);
        this.lockedFlags.forEach((flag, wrappedGoal) -> {
            if (!wrappedGoal.isRunning()) {
                this.lockedFlags.remove(flag);
            }

        });
        profilerFiller.exit();
        profilerFiller.enter("goalUpdate");
        this.availableGoals.stream().filter((wrappedGoal) -> {
            return !wrappedGoal.isRunning();
        }).filter((wrappedGoal) -> {
            return wrappedGoal.getFlags().stream().noneMatch(this.disabledFlags::contains);
        }).filter((wrappedGoal) -> {
            return wrappedGoal.getFlags().stream().allMatch((flag) -> {
                return this.lockedFlags.getOrDefault(flag, NO_GOAL).canBeReplacedBy(wrappedGoal);
            });
        }).filter(PathfinderGoalWrapped::canUse).forEach((wrappedGoal) -> {
            wrappedGoal.getFlags().forEach((flag) -> {
                PathfinderGoalWrapped wrappedGoal2 = this.lockedFlags.getOrDefault(flag, NO_GOAL);
                wrappedGoal2.stop();
                this.lockedFlags.put(flag, wrappedGoal);
            });
            wrappedGoal.start();
        });
        profilerFiller.exit();
        profilerFiller.enter("goalTick");
        this.getRunningGoals().forEach(PathfinderGoalWrapped::tick);
        profilerFiller.exit();
    }

    public Set<PathfinderGoalWrapped> getAvailableGoals() {
        return this.availableGoals;
    }

    public Stream<PathfinderGoalWrapped> getRunningGoals() {
        return this.availableGoals.stream().filter(PathfinderGoalWrapped::isRunning);
    }

    public void setNewGoalRate(int timeInterval) {
        this.newGoalRate = timeInterval;
    }

    public void disableControlFlag(PathfinderGoal.Type control) {
        this.disabledFlags.add(control);
    }

    public void enableControlFlag(PathfinderGoal.Type control) {
        this.disabledFlags.remove(control);
    }

    public void setControlFlag(PathfinderGoal.Type control, boolean enabled) {
        if (enabled) {
            this.enableControlFlag(control);
        } else {
            this.disableControlFlag(control);
        }

    }
}
