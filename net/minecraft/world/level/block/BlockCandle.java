package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockCandle extends BlockCandleAbstract implements IBlockWaterlogged {
    public static final int MIN_CANDLES = 1;
    public static final int MAX_CANDLES = 4;
    public static final BlockStateInteger CANDLES = BlockProperties.CANDLES;
    public static final BlockStateBoolean LIT = BlockCandleAbstract.LIT;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    public static final ToIntFunction<IBlockData> LIGHT_EMISSION = (state) -> {
        return state.get(LIT) ? 3 * state.get(CANDLES) : 0;
    };
    private static final Int2ObjectMap<List<Vec3D>> PARTICLE_OFFSETS = SystemUtils.make(() -> {
        Int2ObjectMap<List<Vec3D>> int2ObjectMap = new Int2ObjectOpenHashMap<>();
        int2ObjectMap.defaultReturnValue(ImmutableList.of());
        int2ObjectMap.put(1, ImmutableList.of(new Vec3D(0.5D, 0.5D, 0.5D)));
        int2ObjectMap.put(2, ImmutableList.of(new Vec3D(0.375D, 0.44D, 0.5D), new Vec3D(0.625D, 0.5D, 0.44D)));
        int2ObjectMap.put(3, ImmutableList.of(new Vec3D(0.5D, 0.313D, 0.625D), new Vec3D(0.375D, 0.44D, 0.5D), new Vec3D(0.56D, 0.5D, 0.44D)));
        int2ObjectMap.put(4, ImmutableList.of(new Vec3D(0.44D, 0.313D, 0.56D), new Vec3D(0.625D, 0.44D, 0.56D), new Vec3D(0.375D, 0.44D, 0.375D), new Vec3D(0.56D, 0.5D, 0.375D)));
        return Int2ObjectMaps.unmodifiable(int2ObjectMap);
    });
    private static final VoxelShape ONE_AABB = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 6.0D, 9.0D);
    private static final VoxelShape TWO_AABB = Block.box(5.0D, 0.0D, 6.0D, 11.0D, 6.0D, 9.0D);
    private static final VoxelShape THREE_AABB = Block.box(5.0D, 0.0D, 6.0D, 10.0D, 6.0D, 11.0D);
    private static final VoxelShape FOUR_AABB = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 10.0D);

    public BlockCandle(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(CANDLES, Integer.valueOf(1)).set(LIT, Boolean.valueOf(false)).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (player.getAbilities().mayBuild && player.getItemInHand(hand).isEmpty() && state.get(LIT)) {
            extinguish(player, state, world, pos);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    public boolean canBeReplaced(IBlockData state, BlockActionContext context) {
        return !context.isSneaking() && context.getItemStack().getItem() == this.getItem() && state.get(CANDLES) < 4 ? true : super.canBeReplaced(state, context);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = ctx.getWorld().getType(ctx.getClickPosition());
        if (blockState.is(this)) {
            return blockState.cycle(CANDLES);
        } else {
            Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
            boolean bl = fluidState.getType() == FluidTypes.WATER;
            return super.getPlacedState(ctx).set(WATERLOGGED, Boolean.valueOf(bl));
        }
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        switch(state.get(CANDLES)) {
        case 1:
        default:
            return ONE_AABB;
        case 2:
            return TWO_AABB;
        case 3:
            return THREE_AABB;
        case 4:
            return FOUR_AABB;
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(CANDLES, LIT, WATERLOGGED);
    }

    @Override
    public boolean place(GeneratorAccess world, BlockPosition pos, IBlockData state, Fluid fluidState) {
        if (!state.get(WATERLOGGED) && fluidState.getType() == FluidTypes.WATER) {
            IBlockData blockState = state.set(WATERLOGGED, Boolean.valueOf(true));
            if (state.get(LIT)) {
                extinguish((EntityHuman)null, blockState, world, pos);
            } else {
                world.setTypeAndData(pos, blockState, 3);
            }

            world.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(world));
            return true;
        } else {
            return false;
        }
    }

    public static boolean canLight(IBlockData state) {
        return state.is(TagsBlock.CANDLES, (statex) -> {
            return statex.hasProperty(LIT) && statex.hasProperty(WATERLOGGED);
        }) && !state.get(LIT) && !state.get(WATERLOGGED);
    }

    @Override
    protected Iterable<Vec3D> getParticleOffsets(IBlockData state) {
        return PARTICLE_OFFSETS.get(state.get(CANDLES).intValue());
    }

    @Override
    protected boolean canBeLit(IBlockData state) {
        return !state.get(WATERLOGGED) && super.canBeLit(state);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return Block.canSupportCenter(world, pos.below(), EnumDirection.UP);
    }
}
