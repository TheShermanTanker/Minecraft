package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.animal.EntityCat;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.BlockBed;
import net.minecraft.world.level.block.BlockFurnaceFurace;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyBedPart;

public class PathfinderGoalJumpOnBlock extends PathfinderGoalGotoTarget {
    private final EntityCat cat;

    public PathfinderGoalJumpOnBlock(EntityCat cat, double speed) {
        super(cat, speed, 8);
        this.cat = cat;
    }

    @Override
    public boolean canUse() {
        return this.cat.isTamed() && !this.cat.isWillSit() && super.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.cat.setSitting(false);
    }

    @Override
    public void stop() {
        super.stop();
        this.cat.setSitting(false);
    }

    @Override
    public void tick() {
        super.tick();
        this.cat.setSitting(this.isReachedTarget());
    }

    @Override
    protected boolean isValidTarget(IWorldReader world, BlockPosition pos) {
        if (!world.isEmpty(pos.above())) {
            return false;
        } else {
            IBlockData blockState = world.getType(pos);
            if (blockState.is(Blocks.CHEST)) {
                return TileEntityChest.getOpenCount(world, pos) < 1;
            } else {
                return blockState.is(Blocks.FURNACE) && blockState.get(BlockFurnaceFurace.LIT) ? true : blockState.is(TagsBlock.BEDS, (state) -> {
                    return state.<BlockPropertyBedPart>getOptionalValue(BlockBed.PART).map((part) -> {
                        return part != BlockPropertyBedPart.HEAD;
                    }).orElse(true);
                });
            }
        }
    }
}
