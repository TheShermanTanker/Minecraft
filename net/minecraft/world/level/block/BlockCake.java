package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockCake extends Block {
    public static final int MAX_BITES = 6;
    public static final BlockStateInteger BITES = BlockProperties.BITES;
    public static final int FULL_CAKE_SIGNAL = getOutputSignal(0);
    protected static final float AABB_OFFSET = 1.0F;
    protected static final float AABB_SIZE_PER_BITE = 2.0F;
    protected static final VoxelShape[] SHAPE_BY_BITE = new VoxelShape[]{Block.box(1.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D), Block.box(3.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D), Block.box(5.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D), Block.box(7.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D), Block.box(9.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D), Block.box(11.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D), Block.box(13.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D)};

    protected BlockCake(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(BITES, Integer.valueOf(0)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE_BY_BITE[state.get(BITES)];
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        Item item = itemStack.getItem();
        if (itemStack.is(TagsItem.CANDLES) && state.get(BITES) == 0) {
            Block block = Block.asBlock(item);
            if (block instanceof BlockCandle) {
                if (!player.isCreative()) {
                    itemStack.subtract(1);
                }

                world.playSound((EntityHuman)null, pos, SoundEffects.CAKE_ADD_CANDLE, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                world.setTypeUpdate(pos, BlockCandleCake.byCandle(block));
                world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                player.awardStat(StatisticList.ITEM_USED.get(item));
                return EnumInteractionResult.SUCCESS;
            }
        }

        if (world.isClientSide) {
            if (eat(world, pos, state, player).consumesAction()) {
                return EnumInteractionResult.SUCCESS;
            }

            if (itemStack.isEmpty()) {
                return EnumInteractionResult.CONSUME;
            }
        }

        return eat(world, pos, state, player);
    }

    protected static EnumInteractionResult eat(GeneratorAccess world, BlockPosition pos, IBlockData state, EntityHuman player) {
        if (!player.canEat(false)) {
            return EnumInteractionResult.PASS;
        } else {
            player.awardStat(StatisticList.EAT_CAKE_SLICE);
            player.getFoodData().eat(2, 0.1F);
            int i = state.get(BITES);
            world.gameEvent(player, GameEvent.EAT, pos);
            if (i < 6) {
                world.setTypeAndData(pos, state.set(BITES, Integer.valueOf(i + 1)), 3);
            } else {
                world.removeBlock(pos, false);
                world.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            }

            return EnumInteractionResult.SUCCESS;
        }
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction == EnumDirection.DOWN && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return world.getType(pos.below()).getMaterial().isBuildable();
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(BITES);
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return getOutputSignal(state.get(BITES));
    }

    public static int getOutputSignal(int bites) {
        return (7 - bites) * 2;
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
