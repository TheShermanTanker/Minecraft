package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.gameevent.GameEvent;

public class PathfinderGoalEatTile extends PathfinderGoal {
    private static final int EAT_ANIMATION_TICKS = 40;
    private static final Predicate<IBlockData> IS_TALL_GRASS = BlockStatePredicate.forBlock(Blocks.GRASS);
    private final EntityInsentient mob;
    private final World level;
    private int eatAnimationTick;

    public PathfinderGoalEatTile(EntityInsentient mob) {
        this.mob = mob;
        this.level = mob.level;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK, PathfinderGoal.Type.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getRandom().nextInt(this.mob.isBaby() ? 50 : 1000) != 0) {
            return false;
        } else {
            BlockPosition blockPos = this.mob.getChunkCoordinates();
            if (IS_TALL_GRASS.test(this.level.getType(blockPos))) {
                return true;
            } else {
                return this.level.getType(blockPos.below()).is(Blocks.GRASS_BLOCK);
            }
        }
    }

    @Override
    public void start() {
        this.eatAnimationTick = this.adjustedTickDelay(40);
        this.level.broadcastEntityEffect(this.mob, (byte)10);
        this.mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.eatAnimationTick = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.eatAnimationTick > 0;
    }

    public int getEatAnimationTick() {
        return this.eatAnimationTick;
    }

    @Override
    public void tick() {
        this.eatAnimationTick = Math.max(0, this.eatAnimationTick - 1);
        if (this.eatAnimationTick == this.adjustedTickDelay(4)) {
            BlockPosition blockPos = this.mob.getChunkCoordinates();
            if (IS_TALL_GRASS.test(this.level.getType(blockPos))) {
                if (this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                    this.level.destroyBlock(blockPos, false);
                }

                this.mob.blockEaten();
                this.mob.gameEvent(GameEvent.EAT, this.mob.eyeBlockPosition());
            } else {
                BlockPosition blockPos2 = blockPos.below();
                if (this.level.getType(blockPos2).is(Blocks.GRASS_BLOCK)) {
                    if (this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                        this.level.triggerEffect(2001, blockPos2, Block.getCombinedId(Blocks.GRASS_BLOCK.getBlockData()));
                        this.level.setTypeAndData(blockPos2, Blocks.DIRT.getBlockData(), 2);
                    }

                    this.mob.blockEaten();
                    this.mob.gameEvent(GameEvent.EAT, this.mob.eyeBlockPosition());
                }
            }

        }
    }
}
