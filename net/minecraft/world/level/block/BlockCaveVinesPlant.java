package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockCaveVinesPlant extends BlockGrowingStem implements IBlockFragilePlantElement, ICaveVine {
    public BlockCaveVinesPlant(BlockBase.Info settings) {
        super(settings, EnumDirection.DOWN, SHAPE, false);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(BERRIES, Boolean.valueOf(false)));
    }

    @Override
    protected BlockGrowingTop getHeadBlock() {
        return (BlockGrowingTop)Blocks.CAVE_VINES;
    }

    @Override
    protected IBlockData updateHeadAfterConvertedFromBody(IBlockData from, IBlockData to) {
        return to.set(BERRIES, from.get(BERRIES));
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return new ItemStack(Items.GLOW_BERRIES);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        return ICaveVine.harvest(state, world, pos);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(BERRIES);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return !state.get(BERRIES);
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        world.setTypeAndData(pos, state.set(BERRIES, Boolean.valueOf(true)), 2);
    }
}
