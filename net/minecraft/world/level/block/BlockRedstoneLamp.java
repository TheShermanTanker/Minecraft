package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;

public class BlockRedstoneLamp extends Block {
    public static final BlockStateBoolean LIT = BlockRedstoneTorch.LIT;

    public BlockRedstoneLamp(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.getBlockData().set(LIT, Boolean.valueOf(false)));
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(LIT, Boolean.valueOf(ctx.getWorld().isBlockIndirectlyPowered(ctx.getClickPosition())));
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (!world.isClientSide) {
            boolean bl = state.get(LIT);
            if (bl != world.isBlockIndirectlyPowered(pos)) {
                if (bl) {
                    world.scheduleTick(pos, this, 4);
                } else {
                    world.setTypeAndData(pos, state.cycle(LIT), 2);
                }
            }

        }
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (state.get(LIT) && !world.isBlockIndirectlyPowered(pos)) {
            world.setTypeAndData(pos, state.cycle(LIT), 2);
        }

    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(LIT);
    }
}
