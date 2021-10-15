package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class CandleCakeBlock extends BlockCandleAbstract {
    public static final BlockStateBoolean LIT = BlockCandleAbstract.LIT;
    protected static final float AABB_OFFSET = 1.0F;
    protected static final VoxelShape CAKE_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D);
    protected static final VoxelShape CANDLE_SHAPE = Block.box(7.0D, 8.0D, 7.0D, 9.0D, 14.0D, 9.0D);
    protected static final VoxelShape SHAPE = VoxelShapes.or(CAKE_SHAPE, CANDLE_SHAPE);
    private static final Map<Block, CandleCakeBlock> BY_CANDLE = Maps.newHashMap();
    private static final Iterable<Vec3D> PARTICLE_OFFSETS = ImmutableList.of(new Vec3D(0.5D, 1.0D, 0.5D));

    protected CandleCakeBlock(Block candle, BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(LIT, Boolean.valueOf(false)));
        BY_CANDLE.put(candle, this);
    }

    @Override
    protected Iterable<Vec3D> getParticleOffsets(IBlockData state) {
        return PARTICLE_OFFSETS;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.is(Items.FLINT_AND_STEEL) && !itemStack.is(Items.FIRE_CHARGE)) {
            if (candleHit(hit) && player.getItemInHand(hand).isEmpty() && state.get(LIT)) {
                extinguish(player, state, world, pos);
                return EnumInteractionResult.sidedSuccess(world.isClientSide);
            } else {
                EnumInteractionResult interactionResult = BlockCake.eat(world, pos, Blocks.CAKE.getBlockData(), player);
                if (interactionResult.consumesAction()) {
                    dropResources(state, world, pos);
                }

                return interactionResult;
            }
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    private static boolean candleHit(MovingObjectPositionBlock hitResult) {
        return hitResult.getPos().y - (double)hitResult.getBlockPosition().getY() > 0.5D;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(LIT);
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return new ItemStack(Blocks.CAKE);
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
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return BlockCake.FULL_CAKE_SIGNAL;
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    public static IBlockData byCandle(Block candle) {
        return BY_CANDLE.get(candle).getBlockData();
    }

    public static boolean canLight(IBlockData state) {
        return state.is(TagsBlock.CANDLE_CAKES, (statex) -> {
            return statex.hasProperty(LIT) && !state.get(LIT);
        });
    }
}
