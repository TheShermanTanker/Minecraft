package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;

public class BlockCoral extends Block {
    private final Block deadBlock;

    public BlockCoral(Block deadCoralBlock, BlockBase.Info settings) {
        super(settings);
        this.deadBlock = deadCoralBlock;
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!this.scanForWater(world, pos)) {
            world.setTypeAndData(pos, this.deadBlock.getBlockData(), 2);
        }

    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (!this.scanForWater(world, pos)) {
            world.getBlockTickList().scheduleTick(pos, this, 60 + world.getRandom().nextInt(40));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    protected boolean scanForWater(IBlockAccess world, BlockPosition pos) {
        for(EnumDirection direction : EnumDirection.values()) {
            Fluid fluidState = world.getFluid(pos.relative(direction));
            if (fluidState.is(TagsFluid.WATER)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        if (!this.scanForWater(ctx.getWorld(), ctx.getClickPosition())) {
            ctx.getWorld().getBlockTickList().scheduleTick(ctx.getClickPosition(), this, 60 + ctx.getWorld().getRandom().nextInt(40));
        }

        return this.getBlockData();
    }
}
