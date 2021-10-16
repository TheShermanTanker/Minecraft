package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;

public class BlockTallPlant extends BlockPlant {
    public static final BlockStateEnum<BlockPropertyDoubleBlockHalf> HALF = BlockProperties.DOUBLE_BLOCK_HALF;

    public BlockTallPlant(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(HALF, BlockPropertyDoubleBlockHalf.LOWER));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        BlockPropertyDoubleBlockHalf doubleBlockHalf = state.get(HALF);
        if (direction.getAxis() != EnumDirection.EnumAxis.Y || doubleBlockHalf == BlockPropertyDoubleBlockHalf.LOWER != (direction == EnumDirection.UP) || neighborState.is(this) && neighborState.get(HALF) != doubleBlockHalf) {
            return doubleBlockHalf == BlockPropertyDoubleBlockHalf.LOWER && direction == EnumDirection.DOWN && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
        } else {
            return Blocks.AIR.getBlockData();
        }
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        BlockPosition blockPos = ctx.getClickPosition();
        World level = ctx.getWorld();
        return blockPos.getY() < level.getMaxBuildHeight() - 1 && level.getType(blockPos.above()).canBeReplaced(ctx) ? super.getPlacedState(ctx) : null;
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        BlockPosition blockPos = pos.above();
        world.setTypeAndData(blockPos, copyWaterloggedFrom(world, blockPos, this.getBlockData().set(HALF, BlockPropertyDoubleBlockHalf.UPPER)), 3);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        if (state.get(HALF) != BlockPropertyDoubleBlockHalf.UPPER) {
            return super.canPlace(state, world, pos);
        } else {
            IBlockData blockState = world.getType(pos.below());
            return blockState.is(this) && blockState.get(HALF) == BlockPropertyDoubleBlockHalf.LOWER;
        }
    }

    public static void placeAt(GeneratorAccess world, IBlockData state, BlockPosition pos, int flags) {
        BlockPosition blockPos = pos.above();
        world.setTypeAndData(pos, copyWaterloggedFrom(world, pos, state.set(HALF, BlockPropertyDoubleBlockHalf.LOWER)), flags);
        world.setTypeAndData(blockPos, copyWaterloggedFrom(world, blockPos, state.set(HALF, BlockPropertyDoubleBlockHalf.UPPER)), flags);
    }

    public static IBlockData copyWaterloggedFrom(IWorldReader world, BlockPosition pos, IBlockData state) {
        return state.hasProperty(BlockProperties.WATERLOGGED) ? state.set(BlockProperties.WATERLOGGED, Boolean.valueOf(world.isWaterAt(pos))) : state;
    }

    @Override
    public void playerWillDestroy(World world, BlockPosition pos, IBlockData state, EntityHuman player) {
        if (!world.isClientSide) {
            if (player.isCreative()) {
                preventCreativeDropFromBottomPart(world, pos, state, player);
            } else {
                dropItems(state, world, pos, (TileEntity)null, player, player.getItemInMainHand());
            }
        }

        super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void playerDestroy(World world, EntityHuman player, BlockPosition pos, IBlockData state, @Nullable TileEntity blockEntity, ItemStack stack) {
        super.playerDestroy(world, player, pos, Blocks.AIR.getBlockData(), blockEntity, stack);
    }

    protected static void preventCreativeDropFromBottomPart(World world, BlockPosition pos, IBlockData state, EntityHuman player) {
        BlockPropertyDoubleBlockHalf doubleBlockHalf = state.get(HALF);
        if (doubleBlockHalf == BlockPropertyDoubleBlockHalf.UPPER) {
            BlockPosition blockPos = pos.below();
            IBlockData blockState = world.getType(blockPos);
            if (blockState.is(state.getBlock()) && blockState.get(HALF) == BlockPropertyDoubleBlockHalf.LOWER) {
                IBlockData blockState2 = blockState.hasProperty(BlockProperties.WATERLOGGED) && blockState.get(BlockProperties.WATERLOGGED) ? Blocks.WATER.getBlockData() : Blocks.AIR.getBlockData();
                world.setTypeAndData(blockPos, blockState2, 35);
                world.triggerEffect(player, 2001, blockPos, Block.getCombinedId(blockState));
            }
        }

    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(HALF);
    }

    @Override
    public BlockBase.EnumRandomOffset getOffsetType() {
        return BlockBase.EnumRandomOffset.XZ;
    }

    @Override
    public long getSeed(IBlockData state, BlockPosition pos) {
        return MathHelper.getSeed(pos.getX(), pos.below(state.get(HALF) == BlockPropertyDoubleBlockHalf.LOWER ? 0 : 1).getY(), pos.getZ());
    }
}
