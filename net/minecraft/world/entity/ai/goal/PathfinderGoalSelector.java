package net.minecraft.world.entity.ai.goal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
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

    private static boolean goalContainsAnyFlags(PathfinderGoalWrapped goal, EnumSet<PathfinderGoal.Type> controls) {
        for(PathfinderGoal.Type flag : goal.getFlags()) {
            if (controls.contains(flag)) {
                return true;
            }
        }

        return false;
    }

    private static boolean goalCanBeReplacedForAllFlags(PathfinderGoalWrapped goal, Map<PathfinderGoal.Type, PathfinderGoalWrapped> goalsByControl) {
        for(PathfinderGoal.Type flag : goal.getFlags()) {
            if (!goalsByControl.getOrDefault(flag, NO_GOAL).canBeReplacedBy(goal)) {
                return false;
            }
        }

        return true;
    }

    public void doTick() {
        GameProfilerFiller profilerFiller = this.profiler.get();
        profilerFiller.enter("goalCleanup");

        for(PathfinderGoalWrapped wrappedGoal : this.availableGoals) {
            if (wrappedGoal.isRunning() && (goalContainsAnyFlags(wrappedGoal, this.disabledFlags) || !wrappedGoal.canContinueToUse())) {
                wrappedGoal.stop();
            }
        }

        Iterator<Entry<PathfinderGoal.Type, PathfinderGoalWrapped>> iterator = this.lockedFlags.entrySet().iterator();

        while(iterator.hasNext()) {
            Entry<PathfinderGoal.Type, PathfinderGoalWrapped> entry = iterator.next();
            if (!entry.getValue().isRunning()) {
                iterator.remove();
            }
        }

        profilerFiller.exit();
        profilerFiller.enter("goalUpdate");

        for(PathfinderGoalWrapped wrappedGoal2 : this.availableGoals) {
            if (!wrappedGoal2.isRunning() && !goalContainsAnyFlags(wrappedGoal2, this.disabledFlags) && goalCanBeReplacedForAllFlags(wrappedGoal2, this.lockedFlags) && wrappedGoal2.canUse()) {
                for(PathfinderGoal.Type flag : wrappedGoal2.getFlags()) {
                    PathfinderGoalWrapped wrappedGoal3 = this.lockedFlags.getOrDefault(flag, NO_GOAL);
                    wrappedGoal3.stop();
                    this.lockedFlags.put(flag, wrappedGoal2);
                }

                wrappedGoal2.start();
            }
        }

        profilerFiller.exit();
        this.tickRunningGoals(true);
    }

    public void tickRunningGoals(boolean tickAll) {
        GameProfilerFiller profilerFiller = this.profiler.get();
        profilerFiller.enter("goalTick");

        for(PathfinderGoalWrapped wrappedGoal : this.availableGoals) {
            if (wrappedGoal.isRunning() && (tickAll || wrappedGoal.requiresUpdateEveryTick())) {
                wrappedGoal.tick();
            }
        }

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
