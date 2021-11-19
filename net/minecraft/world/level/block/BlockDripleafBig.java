package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockDripleafBig extends BlockFacingHorizontal implements IBlockFragilePlantElement, IBlockWaterlogged {
    private static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    private static final BlockStateEnum<Tilt> TILT = BlockProperties.TILT;
    private static final int NO_TICK = -1;
    private static final Object2IntMap<Tilt> DELAY_UNTIL_NEXT_TILT_STATE = SystemUtils.make(new Object2IntArrayMap<>(), (delays) -> {
        delays.defaultReturnValue(-1);
        delays.put(Tilt.UNSTABLE, 10);
        delays.put(Tilt.PARTIAL, 10);
        delays.put(Tilt.FULL, 100);
    });
    private static final int MAX_GEN_HEIGHT = 5;
    private static final int STEM_WIDTH = 6;
    private static final int ENTITY_DETECTION_MIN_Y = 11;
    private static final int LOWEST_LEAF_TOP = 13;
    private static final Map<Tilt, VoxelShape> LEAF_SHAPES = ImmutableMap.of(Tilt.NONE, Block.box(0.0D, 11.0D, 0.0D, 16.0D, 15.0D, 16.0D), Tilt.UNSTABLE, Block.box(0.0D, 11.0D, 0.0D, 16.0D, 15.0D, 16.0D), Tilt.PARTIAL, Block.box(0.0D, 11.0D, 0.0D, 16.0D, 13.0D, 16.0D), Tilt.FULL, VoxelShapes.empty());
    private static final VoxelShape STEM_SLICER = Block.box(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final Map<EnumDirection, VoxelShape> STEM_SHAPES = ImmutableMap.of(EnumDirection.NORTH, VoxelShapes.joinUnoptimized(BlockDripleafStemBig.NORTH_SHAPE, STEM_SLICER, OperatorBoolean.ONLY_FIRST), EnumDirection.SOUTH, VoxelShapes.joinUnoptimized(BlockDripleafStemBig.SOUTH_SHAPE, STEM_SLICER, OperatorBoolean.ONLY_FIRST), EnumDirection.EAST, VoxelShapes.joinUnoptimized(BlockDripleafStemBig.EAST_SHAPE, STEM_SLICER, OperatorBoolean.ONLY_FIRST), EnumDirection.WEST, VoxelShapes.joinUnoptimized(BlockDripleafStemBig.WEST_SHAPE, STEM_SLICER, OperatorBoolean.ONLY_FIRST));
    private final Map<IBlockData, VoxelShape> shapesCache;

    protected BlockDripleafBig(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(WATERLOGGED, Boolean.valueOf(false)).set(FACING, EnumDirection.NORTH).set(TILT, Tilt.NONE));
        this.shapesCache = this.getShapeForEachState(BlockDripleafBig::calculateShape);
    }

    private static VoxelShape calculateShape(IBlockData state) {
        return VoxelShapes.or(LEAF_SHAPES.get(state.get(TILT)), STEM_SHAPES.get(state.get(FACING)));
    }

    public static void placeWithRandomHeight(GeneratorAccess world, Random random, BlockPosition pos, EnumDirection direction) {
        int i = MathHelper.nextInt(random, 2, 5);
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();
        int j = 0;

        while(j < i && canPlaceAt(world, mutableBlockPos, world.getType(mutableBlockPos))) {
            ++j;
            mutableBlockPos.move(EnumDirection.UP);
        }

        int k = pos.getY() + j - 1;
        mutableBlockPos.setY(pos.getY());

        while(mutableBlockPos.getY() < k) {
            BlockDripleafStemBig.place(world, mutableBlockPos, world.getFluid(mutableBlockPos), direction);
            mutableBlockPos.move(EnumDirection.UP);
        }

        place(world, mutableBlockPos, world.getFluid(mutableBlockPos), direction);
    }

    private static boolean canReplace(IBlockData state) {
        return state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.SMALL_DRIPLEAF);
    }

    protected static boolean canPlaceAt(IWorldHeightAccess world, BlockPosition pos, IBlockData state) {
        return !world.isOutsideWorld(pos) && canReplace(state);
    }

    protected static boolean place(GeneratorAccess world, BlockPosition pos, Fluid fluidState, EnumDirection direction) {
        IBlockData blockState = Blocks.BIG_DRIPLEAF.getBlockData().set(WATERLOGGED, Boolean.valueOf(fluidState.isSourceOfType(FluidTypes.WATER))).set(FACING, direction);
        return world.setTypeAndData(pos, blockState, 3);
    }

    @Override
    public void onProjectileHit(World world, IBlockData state, MovingObjectPositionBlock hit, IProjectile projectile) {
        this.setTiltAndScheduleTick(state, world, hit.getBlockPosition(), Tilt.FULL, SoundEffects.BIG_DRIPLEAF_TILT_DOWN);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        IBlockData blockState = world.getType(blockPos);
        return blockState.is(Blocks.BIG_DRIPLEAF_STEM) || blockState.is(this) || blockState.isFaceSturdy(world, blockPos, EnumDirection.UP);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction == EnumDirection.DOWN && !state.canPlace(world, pos)) {
            return Blocks.AIR.getBlockData();
        } else {
            if (state.get(WATERLOGGED)) {
                world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
            }

            return direction == EnumDirection.UP && neighborState.is(this) ? Blocks.BIG_DRIPLEAF_STEM.withPropertiesOf(state) : super.updateState(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        IBlockData blockState = world.getType(pos.above());
        return canReplace(blockState);
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        BlockPosition blockPos = pos.above();
        IBlockData blockState = world.getType(blockPos);
        if (canPlaceAt(world, blockPos, blockState)) {
            EnumDirection direction = state.get(FACING);
            BlockDripleafStemBig.place(world, pos, state.getFluid(), direction);
            place(world, blockPos, blockState.getFluid(), direction);
        }

    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (!world.isClientSide) {
            if (state.get(TILT) == Tilt.NONE && canEntityTilt(pos, entity) && !world.isBlockIndirectlyPowered(pos)) {
                this.setTiltAndScheduleTick(state, world, pos, Tilt.UNSTABLE, (SoundEffect)null);
            }

        }
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (world.isBlockIndirectlyPowered(pos)) {
            resetTilt(state, world, pos);
        } else {
            Tilt tilt = state.get(TILT);
            if (tilt == Tilt.UNSTABLE) {
                this.setTiltAndScheduleTick(state, world, pos, Tilt.PARTIAL, SoundEffects.BIG_DRIPLEAF_TILT_DOWN);
            } else if (tilt == Tilt.PARTIAL) {
                this.setTiltAndScheduleTick(state, world, pos, Tilt.FULL, SoundEffects.BIG_DRIPLEAF_TILT_DOWN);
            } else if (tilt == Tilt.FULL) {
                resetTilt(state, world, pos);
            }

        }
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (world.isBlockIndirectlyPowered(pos)) {
            resetTilt(state, world, pos);
        }

    }

    private static void playTiltSound(World world, BlockPosition pos, SoundEffect soundEvent) {
        float f = MathHelper.randomBetween(world.random, 0.8F, 1.2F);
        world.playSound((EntityHuman)null, pos, soundEvent, EnumSoundCategory.BLOCKS, 1.0F, f);
    }

    private static boolean canEntityTilt(BlockPosition pos, Entity entity) {
        return entity.isOnGround() && entity.getPositionVector().y > (double)((float)pos.getY() + 0.6875F);
    }

    private void setTiltAndScheduleTick(IBlockData state, World world, BlockPosition pos, Tilt tilt, @Nullable SoundEffect sound) {
        setTilt(state, world, pos, tilt);
        if (sound != null) {
            playTiltSound(world, pos, sound);
        }

        int i = DELAY_UNTIL_NEXT_TILT_STATE.getInt(tilt);
        if (i != -1) {
            world.getBlockTickList().scheduleTick(pos, this, i);
        }

    }

    private static void resetTilt(IBlockData state, World world, BlockPosition pos) {
        setTilt(state, world, pos, Tilt.NONE);
        if (state.get(TILT) != Tilt.NONE) {
            playTiltSound(world, pos, SoundEffects.BIG_DRIPLEAF_TILT_UP);
        }

    }

    private static void setTilt(IBlockData state, World world, BlockPosition pos, Tilt tilt) {
        world.setTypeAndData(pos, state.set(TILT, tilt), 2);
        if (tilt.causesVibration()) {
            world.gameEvent(GameEvent.BLOCK_CHANGE, pos);
        }

    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return LEAF_SHAPES.get(state.get(TILT));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.shapesCache.get(state);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        IBlockData blockState = ctx.getWorld().getType(ctx.getClickPosition().below());
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        boolean bl = blockState.is(Blocks.BIG_DRIPLEAF) || blockState.is(Blocks.BIG_DRIPLEAF_STEM);
        return this.getBlockData().set(WATERLOGGED, Boolean.valueOf(fluidState.isSourceOfType(FluidTypes.WATER))).set(FACING, bl ? blockState.get(FACING) : ctx.getHorizontalDirection().opposite());
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(WATERLOGGED, FACING, TILT);
    }
}
