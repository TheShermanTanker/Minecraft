package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.EnumPistonReaction;

public class BlockEndRod extends BlockRod {
    protected BlockEndRod(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.UP));
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        EnumDirection direction = ctx.getClickedFace();
        IBlockData blockState = ctx.getWorld().getType(ctx.getClickPosition().relative(direction.opposite()));
        return blockState.is(this) && blockState.get(FACING) == direction ? this.getBlockData().set(FACING, direction.opposite()) : this.getBlockData().set(FACING, direction);
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        EnumDirection direction = state.get(FACING);
        double d = (double)pos.getX() + 0.55D - (double)(random.nextFloat() * 0.1F);
        double e = (double)pos.getY() + 0.55D - (double)(random.nextFloat() * 0.1F);
        double f = (double)pos.getZ() + 0.55D - (double)(random.nextFloat() * 0.1F);
        double g = (double)(0.4F - (random.nextFloat() + random.nextFloat()) * 0.4F);
        if (random.nextInt(5) == 0) {
            world.addParticle(Particles.END_ROD, d + (double)direction.getAdjacentX() * g, e + (double)direction.getAdjacentY() * g, f + (double)direction.getAdjacentZ() * g, random.nextGaussian() * 0.005D, random.nextGaussian() * 0.005D, random.nextGaussian() * 0.005D);
        }

    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING);
    }

    @Override
    public EnumPistonReaction getPushReaction(IBlockData state) {
        return EnumPistonReaction.NORMAL;
    }
}
