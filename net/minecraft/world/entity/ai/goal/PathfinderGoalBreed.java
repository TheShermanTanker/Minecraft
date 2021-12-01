package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.level.World;

public class PathfinderGoalBreed extends PathfinderGoal {
    private static final PathfinderTargetCondition PARTNER_TARGETING = PathfinderTargetCondition.forNonCombat().range(8.0D).ignoreLineOfSight();
    protected final EntityAnimal animal;
    private final Class<? extends EntityAnimal> partnerClass;
    protected final World level;
    @Nullable
    protected EntityAnimal partner;
    private int loveTime;
    private final double speedModifier;

    public PathfinderGoalBreed(EntityAnimal animal, double chance) {
        this(animal, chance, animal.getClass());
    }

    public PathfinderGoalBreed(EntityAnimal animal, double chance, Class<? extends EntityAnimal> entityClass) {
        this.animal = animal;
        this.level = animal.level;
        this.partnerClass = entityClass;
        this.speedModifier = chance;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.animal.isInLove()) {
            return false;
        } else {
            this.partner = this.getFreePartner();
            return this.partner != null;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.partner.isAlive() && this.partner.isInLove() && this.loveTime < 60;
    }

    @Override
    public void stop() {
        this.partner = null;
        this.loveTime = 0;
    }

    @Override
    public void tick() {
        this.animal.getControllerLook().setLookAt(this.partner, 10.0F, (float)this.animal.getMaxHeadXRot());
        this.animal.getNavigation().moveTo(this.partner, this.speedModifier);
        ++this.loveTime;
        if (this.loveTime >= this.adjustedTickDelay(60) && this.animal.distanceToSqr(this.partner) < 9.0D) {
            this.breed();
        }

    }

    @Nullable
    private EntityAnimal getFreePartner() {
        List<? extends EntityAnimal> list = this.level.getNearbyEntities(this.partnerClass, PARTNER_TARGETING, this.animal, this.animal.getBoundingBox().inflate(8.0D));
        double d = Double.MAX_VALUE;
        EntityAnimal animal = null;

        for(EntityAnimal animal2 : list) {
            if (this.animal.mate(animal2) && this.animal.distanceToSqr(animal2) < d) {
                animal = animal2;
                d = this.animal.distanceToSqr(animal2);
            }
        }

        return animal;
    }

    protected void breed() {
        this.animal.spawnChildFromBreeding((WorldServer)this.level, this.partner);
    }
}
