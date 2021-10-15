package net.minecraft.world.level.block;

import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockStem extends BlockPlant implements IBlockFragilePlantElement {
    public static final int MAX_AGE = 7;
    public static final BlockStateInteger AGE = BlockProperties.AGE_7;
    protected static final float AABB_OFFSET = 1.0F;
    protected static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{Block.box(7.0D, 0.0D, 7.0D, 9.0D, 2.0D, 9.0D), Block.box(7.0D, 0.0D, 7.0D, 9.0D, 4.0D, 9.0D), Block.box(7.0D, 0.0D, 7.0D, 9.0D, 6.0D, 9.0D), Block.box(7.0D, 0.0D, 7.0D, 9.0D, 8.0D, 9.0D), Block.box(7.0D, 0.0D, 7.0D, 9.0D, 10.0D, 9.0D), Block.box(7.0D, 0.0D, 7.0D, 9.0D, 12.0D, 9.0D), Block.box(7.0D, 0.0D, 7.0D, 9.0D, 14.0D, 9.0D), Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D)};
    private final BlockStemmed fruit;
    private final Supplier<Item> seedSupplier;

    protected BlockStem(BlockStemmed gourdBlock, Supplier<Item> pickBlockItem, BlockBase.Info settings) {
        super(settings);
        this.fruit = gourdBlock;
        this.seedSupplier = pickBlockItem;
        this.registerDefaultState(this.stateDefinition.getBlockData().set(AGE, Integer.valueOf(0)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE_BY_AGE[state.get(AGE)];
    }

    @Override
    protected boolean mayPlaceOn(IBlockData floor, IBlockAccess world, BlockPosition pos) {
        return floor.is(Blocks.FARMLAND);
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (world.getLightLevel(pos, 0) >= 9) {
            float f = BlockCrops.getGrowthSpeed(this, world, pos);
            if (random.nextInt((int)(25.0F / f) + 1) == 0) {
                int i = state.get(AGE);
                if (i < 7) {
                    state = state.set(AGE, Integer.valueOf(i + 1));
                    world.setTypeAndData(pos, state, 2);
                } else {
                    EnumDirection direction = EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(random);
                    BlockPosition blockPos = pos.relative(direction);
                    IBlockData blockState = world.getType(blockPos.below());
                    if (world.getType(blockPos).isAir() && (blockState.is(Blocks.FARMLAND) || blockState.is(TagsBlock.DIRT))) {
                        world.setTypeUpdate(blockPos, this.fruit.getBlockData());
                        world.setTypeUpdate(pos, this.fruit.getAttachedStem().getBlockData().set(BlockFacingHorizontal.FACING, direction));
                    }
                }
            }

        }
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return new ItemStack(this.seedSupplier.get());
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return state.get(AGE) != 7;
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        int i = Math.min(7, state.get(AGE) + MathHelper.nextInt(world.random, 2, 5));
        IBlockData blockState = state.set(AGE, Integer.valueOf(i));
        world.setTypeAndData(pos, blockState, 2);
        if (i == 7) {
            blockState.randomTick(world, pos, world.random);
        }

    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(AGE);
    }

    public BlockStemmed getFruit() {
        return this.fruit;
    }
}
