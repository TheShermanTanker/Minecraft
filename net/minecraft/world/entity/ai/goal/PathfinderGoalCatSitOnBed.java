package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.animal.EntityCat;
import net.minecraft.world.level.IWorldReader;

public class PathfinderGoalCatSitOnBed extends PathfinderGoalGotoTarget {
    private final EntityCat cat;

    public PathfinderGoalCatSitOnBed(EntityCat cat, double speed, int range) {
        super(cat, speed, range, 6);
        this.cat = cat;
        this.verticalSearchStart = -2;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.JUMP, PathfinderGoal.Type.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.cat.isTamed() && !this.cat.isWillSit() && !this.cat.isLying() && super.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.cat.setSitting(false);
    }

    @Override
    protected int nextStartTick(EntityCreature mob) {
        return 40;
    }

    @Override
    public void stop() {
        super.stop();
        this.cat.setLying(false);
    }

    @Override
    public void tick() {
        super.tick();
        this.cat.setSitting(false);
        if (!this.isReachedTarget()) {
            this.cat.setLying(false);
        } else if (!this.cat.isLying()) {
            this.cat.setLying(true);
        }

    }

    @Override
    protected boolean isValidTarget(IWorldReader world, BlockPosition pos) {
        return world.isEmpty(pos.above()) && world.getType(pos).is(TagsBlock.BEDS);
    }
}
