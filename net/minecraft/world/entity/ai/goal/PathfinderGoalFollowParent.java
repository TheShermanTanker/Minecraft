package net.minecraft.world.entity.ai.goal;

import java.util.List;
import net.minecraft.world.entity.animal.EntityAnimal;

public class PathfinderGoalFollowParent extends PathfinderGoal {
    public static final int HORIZONTAL_SCAN_RANGE = 8;
    public static final int VERTICAL_SCAN_RANGE = 4;
    public static final int DONT_FOLLOW_IF_CLOSER_THAN = 3;
    private final EntityAnimal animal;
    private EntityAnimal parent;
    private final double speedModifier;
    private int timeToRecalcPath;

    public PathfinderGoalFollowParent(EntityAnimal animal, double speed) {
        this.animal = animal;
        this.speedModifier = speed;
    }

    @Override
    public boolean canUse() {
        if (this.animal.getAge() >= 0) {
            return false;
        } else {
            List<? extends EntityAnimal> list = this.animal.level.getEntitiesOfClass(this.animal.getClass(), this.animal.getBoundingBox().grow(8.0D, 4.0D, 8.0D));
            EntityAnimal animal = null;
            double d = Double.MAX_VALUE;

            for(EntityAnimal animal2 : list) {
                if (animal2.getAge() >= 0) {
                    double e = this.animal.distanceToSqr(animal2);
                    if (!(e > d)) {
                        d = e;
                        animal = animal2;
                    }
                }
            }

            if (animal == null) {
                return false;
            } else if (d < 9.0D) {
                return false;
            } else {
                this.parent = animal;
                return true;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.animal.getAge() >= 0) {
            return false;
        } else if (!this.parent.isAlive()) {
            return false;
        } else {
            double d = this.animal.distanceToSqr(this.parent);
            return !(d < 9.0D) && !(d > 256.0D);
        }
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.parent = null;
    }

    @Override
    public void tick() {
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            this.animal.getNavigation().moveTo(this.parent, this.speedModifier);
        }
    }
}
