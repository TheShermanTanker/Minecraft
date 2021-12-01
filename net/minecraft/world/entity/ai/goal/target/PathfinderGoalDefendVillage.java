package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.phys.AxisAlignedBB;

public class PathfinderGoalDefendVillage extends PathfinderGoalTarget {
    private final EntityIronGolem golem;
    @Nullable
    private EntityLiving potentialTarget;
    private final PathfinderTargetCondition attackTargeting = PathfinderTargetCondition.forCombat().range(64.0D);

    public PathfinderGoalDefendVillage(EntityIronGolem golem) {
        super(golem, false, true);
        this.golem = golem;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.TARGET));
    }

    @Override
    public boolean canUse() {
        AxisAlignedBB aABB = this.golem.getBoundingBox().grow(10.0D, 8.0D, 10.0D);
        List<? extends EntityLiving> list = this.golem.level.getNearbyEntities(EntityVillager.class, this.attackTargeting, this.golem, aABB);
        List<EntityHuman> list2 = this.golem.level.getNearbyPlayers(this.attackTargeting, this.golem, aABB);

        for(EntityLiving livingEntity : list) {
            EntityVillager villager = (EntityVillager)livingEntity;

            for(EntityHuman player : list2) {
                int i = villager.getPlayerReputation(player);
                if (i <= -100) {
                    this.potentialTarget = player;
                }
            }
        }

        if (this.potentialTarget == null) {
            return false;
        } else {
            return !(this.potentialTarget instanceof EntityHuman) || !this.potentialTarget.isSpectator() && !((EntityHuman)this.potentialTarget).isCreative();
        }
    }

    @Override
    public void start() {
        this.golem.setGoalTarget(this.potentialTarget);
        super.start();
    }
}
