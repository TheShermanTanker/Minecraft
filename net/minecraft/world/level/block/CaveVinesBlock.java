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

public class CaveVinesBlock extends BlockGrowingTop implements IBlockFragilePlantElement, CaveVines {
    private static final float CHANCE_OF_BERRIES_ON_GROWTH = 0.11F;

    public CaveVinesBlock(BlockBase.Info settings) {
        super(settings, EnumDirection.DOWN, SHAPE, false, 0.1D);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(AGE, Integer.valueOf(0)).set(BERRIES, Boolean.valueOf(false)));
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(Random random) {
        return 1;
    }

    @Override
    protected boolean canGrowInto(IBlockData state) {
        return state.isAir();
    }

    @Override
    protected Block getBodyBlock() {
        return Blocks.CAVE_VINES_PLANT;
    }

    @Override
    protected IBlockData updateBodyAfterConvertedFromHead(IBlockData from, IBlockData to) {
        return to.set(BERRIES, from.get(BERRIES));
    }

    @Override
    protected IBlockData getGrowIntoState(IBlockData state, Random random) {
        return super.getGrowIntoState(state, random).set(BERRIES, Boolean.valueOf(random.nextFloat() < 0.11F));
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return new ItemStack(Items.GLOW_BERRIES);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        return CaveVines.harvest(state, world, pos);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        super.createBlockStateDefinition(builder);
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
