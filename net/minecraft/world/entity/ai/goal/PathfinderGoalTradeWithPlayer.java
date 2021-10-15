package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.npc.EntityVillagerAbstract;
import net.minecraft.world.entity.player.EntityHuman;

public class PathfinderGoalTradeWithPlayer extends PathfinderGoal {
    private final EntityVillagerAbstract mob;

    public PathfinderGoalTradeWithPlayer(EntityVillagerAbstract merchant) {
        this.mob = merchant;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.JUMP, PathfinderGoal.Type.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.mob.isAlive()) {
            return false;
        } else if (this.mob.isInWater()) {
            return false;
        } else if (!this.mob.isOnGround()) {
            return false;
        } else if (this.mob.hurtMarked) {
            return false;
        } else {
            EntityHuman player = this.mob.getTrader();
            if (player == null) {
                return false;
            } else if (this.mob.distanceToSqr(player) > 16.0D) {
                return false;
            } else {
                return player.containerMenu != null;
            }
        }
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.mob.setTradingPlayer((EntityHuman)null);
    }
}
