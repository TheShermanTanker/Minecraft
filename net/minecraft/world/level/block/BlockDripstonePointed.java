package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.entity.projectile.EntityThrownTrident;
import net.minecraft.world.entity.projectile.IProjectile;
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
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockDripstonePointed extends Block implements Fallable, IBlockWaterlogged {
    public static final BlockStateDirection TIP_DIRECTION = BlockProperties.VERTICAL_DIRECTION;
    public static final BlockStateEnum<DripstoneThickness> THICKNESS = BlockProperties.DRIPSTONE_THICKNESS;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    private static final int MAX_SEARCH_LENGTH_WHEN_CHECKING_DRIP_TYPE = 11;
    private static final int MAX_SEARCH_LENGTH_WHEN_LOOKING_FOR_TIP_OF_FALLING_STALACTITE = Integer.MAX_VALUE;
    private static final int DELAY_BEFORE_FALLING = 2;
    private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK = 0.02F;
    private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK_IF_UNDER_LIQUID_SOURCE = 0.12F;
    private static final int MAX_SEARCH_LENGTH_BETWEEN_STALACTITE_TIP_AND_CAULDRON = 11;
    private static final float WATER_CAULDRON_FILL_PROBABILITY_PER_RANDOM_TICK = 0.17578125F;
    private static final float LAVA_CAULDRON_FILL_PROBABILITY_PER_RANDOM_TICK = 0.05859375F;
    private static final double MIN_TRIDENT_VELOCITY_TO_BREAK_DRIPSTONE = 0.6D;
    private static final float STALACTITE_DAMAGE_PER_FALL_DISTANCE_AND_SIZE = 1.0F;
    private static final int STALACTITE_MAX_DAMAGE = 40;
    private static final int MAX_STALACTITE_HEIGHT_FOR_DAMAGE_CALCULATION = 6;
    private static final float STALAGMITE_FALL_DISTANCE_OFFSET = 2.0F;
    private static final int STALAGMITE_FALL_DAMAGE_MODIFIER = 2;
    private static final float AVERAGE_DAYS_PER_GROWTH = 5.0F;
    private static final float GROWTH_PROBABILITY_PER_RANDOM_TICK = 0.011377778F;
    private static final int MAX_GROWTH_LENGTH = 7;
    private static final int MAX_STALAGMITE_SEARCH_RANGE_WHEN_GROWING = 10;
    private static final float STALACTITE_DRIP_START_PIXEL = 0.6875F;
    private static final VoxelShape TIP_MERGE_SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final VoxelShape TIP_SHAPE_UP = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 11.0D, 11.0D);
    private static final VoxelShape TIP_SHAPE_DOWN = Block.box(5.0D, 5.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final VoxelShape FRUSTUM_SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);
    private static final VoxelShape MIDDLE_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    private static final VoxelShape BASE_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final float MAX_HORIZONTAL_OFFSET = 0.125F;

    public BlockDripstonePointed(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(TIP_DIRECTION, EnumDirection.UP).set(THICKNESS, DripstoneThickness.TIP).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(TIP_DIRECTION, THICKNESS, WATERLOGGED);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return isValidPointedDripstonePlacement(world, pos, state.get(TIP_DIRECTION));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        if (direction != EnumDirection.UP && direction != EnumDirection.DOWN) {
            return state;
        } else {
            EnumDirection direction2 = state.get(TIP_DIRECTION);
            if (direction2 == EnumDirection.DOWN && world.getBlockTickList().hasScheduledTick(pos, this)) {
                return state;
            } else if (direction == direction2.opposite() && !this.canPlace(state, world, pos)) {
                if (direction2 == EnumDirection.DOWN) {
                    this.scheduleStalactiteFallTicks(state, world, pos);
                } else {
                    world.getBlockTickList().scheduleTick(pos, this, 1);
                }

                return state;
            } else {
                boolean bl = state.get(THICKNESS) == DripstoneThickness.TIP_MERGE;
                DripstoneThickness dripstoneThickness = calculateDripstoneThickness(world, pos, direction2, bl);
                return state.set(THICKNESS, dripstoneThickness);
            }
        }
    }

    @Override
    public void onProjectileHit(World world, IBlockData state, MovingObjectPositionBlock hit, IProjectile projectile) {
        BlockPosition blockPos = hit.getBlockPosition();
        if (!world.isClientSide && projectile.mayInteract(world, blockPos) && projectile instanceof EntityThrownTrident && projectile.getMot().length() > 0.6D) {
            world.destroyBlock(blockPos, true);
        }

    }

    @Override
    public void fallOn(World world, IBlockData state, BlockPosition pos, Entity entity, float fallDistance) {
        if (state.get(TIP_DIRECTION) == EnumDirection.UP && state.get(THICKNESS) == DripstoneThickness.TIP) {
            entity.causeFallDamage(fallDistance + 2.0F, 2.0F, DamageSource.STALAGMITE);
        } else {
            super.fallOn(world, state, pos, entity, fallDistance);
        }

    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (canDrip(state)) {
            float f = random.nextFloat();
            if (!(f > 0.12F)) {
                getFluidAboveStalactite(world, pos, state).filter((fluid) -> {
                    return f < 0.02F || canFillCauldron(fluid);
                }).ifPresent((fluid) -> {
                    spawnDripParticle(world, pos, state, fluid);
                });
            }
        }
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (isStalagmite(state) && !this.canPlace(state, world, pos)) {
            world.destroyBlock(pos, true);
        } else {
            spawnFallingStalactite(state, world, pos);
        }

    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        maybeFillCauldron(state, world, pos, random.nextFloat());
        if (random.nextFloat() < 0.011377778F && isStalactiteStartPos(state, world, pos)) {
            growStalactiteOrStalagmiteIfPossible(state, world, pos, random);
        }

    }

    @VisibleForTesting
    public static void maybeFillCauldron(IBlockData state, WorldServer world, BlockPosition pos, float dripChance) {
        if (!(dripChance > 0.17578125F) || !(dripChance > 0.05859375F)) {
            if (isStalactiteStartPos(state, world, pos)) {
                FluidType fluid = getCauldronFillFluidType(world, pos);
                float f;
                if (fluid == FluidTypes.WATER) {
                    f = 0.17578125F;
                } else {
                    if (fluid != FluidTypes.LAVA) {
                        return;
                    }

                    f = 0.05859375F;
                }

                if (!(dripChance >= f)) {
                    BlockPosition blockPos = findTip(state, world, pos, 11, false);
                    if (blockPos != null) {
                        BlockPosition blockPos2 = findFillableCauldronBelowStalactiteTip(world, blockPos, fluid);
                        if (blockPos2 != null) {
                            world.triggerEffect(1504, blockPos, 0);
                            int i = blockPos.getY() - blockPos2.getY();
                            int j = 50 + i;
                            IBlockData blockState = world.getType(blockPos2);
                            world.getBlockTicks().scheduleTick(blockPos2, blockState.getBlock(), j);
                        }
                    }
                }
            }
        }
    }

    @Override
    public EnumPistonReaction getPushReaction(IBlockData state) {
        return EnumPistonReaction.DESTROY;
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        GeneratorAccess levelAccessor = ctx.getWorld();
        BlockPosition blockPos = ctx.getClickPosition();
        EnumDirection direction = ctx.getNearestLookingVerticalDirection().opposite();
        EnumDirection direction2 = calculateTipDirection(levelAccessor, blockPos, direction);
        if (direction2 == null) {
            return null;
        } else {
            boolean bl = !ctx.isSneaking();
            DripstoneThickness dripstoneThickness = calculateDripstoneThickness(levelAccessor, blockPos, direction2, bl);
            return dripstoneThickness == null ? null : this.getBlockData().set(TIP_DIRECTION, direction2).set(THICKNESS, dripstoneThickness).set(WATERLOGGED, Boolean.valueOf(levelAccessor.getFluid(blockPos).getType() == FluidTypes.WATER));
        }
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public VoxelShape getOcclusionShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        DripstoneThickness dripstoneThickness = state.get(THICKNESS);
        VoxelShape voxelShape;
        if (dripstoneThickness == DripstoneThickness.TIP_MERGE) {
            voxelShape = TIP_MERGE_SHAPE;
        } else if (dripstoneThickness == DripstoneThickness.TIP) {
            if (state.get(TIP_DIRECTION) == EnumDirection.DOWN) {
                voxelShape = TIP_SHAPE_DOWN;
            } else {
                voxelShape = TIP_SHAPE_UP;
            }
        } else if (dripstoneThickness == DripstoneThickness.FRUSTUM) {
            voxelShape = FRUSTUM_SHAPE;
        } else if (dripstoneThickness == DripstoneThickness.MIDDLE) {
            voxelShape = MIDDLE_SHAPE;
        } else {
            voxelShape = BASE_SHAPE;
        }

        Vec3D vec3 = state.getOffset(world, pos);
        return voxelShape.move(vec3.x, 0.0D, vec3.z);
    }

    @Override
    public boolean isCollisionShapeFullBlock(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return false;
    }

    @Override
    public BlockBase.EnumRandomOffset getOffsetType() {
        return BlockBase.EnumRandomOffset.XZ;
    }

    @Override
    public float getMaxHorizontalOffset() {
        return 0.125F;
    }

    @Override
    public void onBrokenAfterFall(World world, BlockPosition pos, EntityFallingBlock fallingBlockEntity) {
        if (!fallingBlockEntity.isSilent()) {
            world.triggerEffect(1045, pos, 0);
        }

    }

    @Override
    public DamageSource getFallDamageSource() {
        return DamageSource.FALLING_STALACTITE;
    }

    @Override
    public Predicate<Entity> getHurtsEntitySelector() {
        return IEntitySelector.NO_CREATIVE_OR_SPECTATOR.and(IEntitySelector.LIVING_ENTITY_STILL_ALIVE);
    }

    private void scheduleStalactiteFallTicks(IBlockData state, GeneratorAccess world, BlockPosition pos) {
        BlockPosition blockPos = findTip(state, world, pos, Integer.MAX_VALUE, true);
        if (blockPos != null) {
            BlockPosition.MutableBlockPosition mutableBlockPos = blockPos.mutable();

            while(isStalactite(world.getType(mutableBlockPos))) {
                world.getBlockTickList().scheduleTick(mutableBlockPos, this, 2);
                mutableBlockPos.move(EnumDirection.UP);
            }

        }
    }

    private static int getStalactiteSizeFromTip(WorldServer world, BlockPosition pos, int range) {
        int i = 1;
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable().move(EnumDirection.UP);

        while(i < range && isStalactite(world.getType(mutableBlockPos))) {
            ++i;
            mutableBlockPos.move(EnumDirection.UP);
        }

        return i;
    }

    private static void spawnFallingStalactite(IBlockData state, WorldServer world, BlockPosition pos) {
        Vec3D vec3 = Vec3D.atBottomCenterOf(pos);
        EntityFallingBlock fallingBlockEntity = new EntityFallingBlock(world, vec3.x, vec3.y, vec3.z, state);
        if (isTip(state, true)) {
            int i = getStalactiteSizeFromTip(world, pos, 6);
            float f = 1.0F * (float)i;
            fallingBlockEntity.setHurtsEntities(f, 40);
        }

        world.addEntity(fallingBlockEntity);
    }

    @VisibleForTesting
    public static void growStalactiteOrStalagmiteIfPossible(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        IBlockData blockState = world.getType(pos.above(1));
        IBlockData blockState2 = world.getType(pos.above(2));
        if (canGrow(blockState, blockState2)) {
            BlockPosition blockPos = findTip(state, world, pos, 7, false);
            if (blockPos != null) {
                IBlockData blockState3 = world.getType(blockPos);
                if (canDrip(blockState3) && canTipGrow(blockState3, world, blockPos)) {
                    if (random.nextBoolean()) {
                        grow(world, blockPos, EnumDirection.DOWN);
                    } else {
                        growStalagmiteBelow(world, blockPos);
                    }

                }
            }
        }
    }

    private static void growStalagmiteBelow(WorldServer world, BlockPosition pos) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();

        for(int i = 0; i < 10; ++i) {
            mutableBlockPos.move(EnumDirection.DOWN);
            IBlockData blockState = world.getType(mutableBlockPos);
            if (!blockState.getFluid().isEmpty()) {
                return;
            }

            if (isUnmergedTipWithDirection(blockState, EnumDirection.UP) && canTipGrow(blockState, world, mutableBlockPos)) {
                grow(world, mutableBlockPos, EnumDirection.UP);
                return;
            }

            if (isValidPointedDripstonePlacement(world, mutableBlockPos, EnumDirection.UP) && !world.isWaterAt(mutableBlockPos.below())) {
                grow(world, mutableBlockPos.below(), EnumDirection.UP);
                return;
            }
        }

    }

    private static void grow(WorldServer world, BlockPosition pos, EnumDirection direction) {
        BlockPosition blockPos = pos.relative(direction);
        IBlockData blockState = world.getType(blockPos);
        if (isUnmergedTipWithDirection(blockState, direction.opposite())) {
            createMergedTips(blockState, world, blockPos);
        } else if (blockState.isAir() || blockState.is(Blocks.WATER)) {
            createDripstone(world, blockPos, direction, DripstoneThickness.TIP);
        }

    }

    private static void createDripstone(GeneratorAccess world, BlockPosition pos, EnumDirection direction, DripstoneThickness thickness) {
        IBlockData blockState = Blocks.POINTED_DRIPSTONE.getBlockData().set(TIP_DIRECTION, direction).set(THICKNESS, thickness).set(WATERLOGGED, Boolean.valueOf(world.getFluid(pos).getType() == FluidTypes.WATER));
        world.setTypeAndData(pos, blockState, 3);
    }

    private static void createMergedTips(IBlockData state, GeneratorAccess world, BlockPosition pos) {
        BlockPosition blockPos2;
        BlockPosition blockPos;
        if (state.get(TIP_DIRECTION) == EnumDirection.UP) {
            blockPos = pos;
            blockPos2 = pos.above();
        } else {
            blockPos2 = pos;
            blockPos = pos.below();
        }

        createDripstone(world, blockPos2, EnumDirection.DOWN, DripstoneThickness.TIP_MERGE);
        createDripstone(world, blockPos, EnumDirection.UP, DripstoneThickness.TIP_MERGE);
    }

    public static void spawnDripParticle(World world, BlockPosition pos, IBlockData state) {
        getFluidAboveStalactite(world, pos, state).ifPresent((fluid) -> {
            spawnDripParticle(world, pos, state, fluid);
        });
    }

    private static void spawnDripParticle(World world, BlockPosition pos, IBlockData state, FluidType fluid) {
        Vec3D vec3 = state.getOffset(world, pos);
        double d = 0.0625D;
        double e = (double)pos.getX() + 0.5D + vec3.x;
        double f = (double)((float)(pos.getY() + 1) - 0.6875F) - 0.0625D;
        double g = (double)pos.getZ() + 0.5D + vec3.z;
        FluidType fluid2 = getDripFluid(world, fluid);
        ParticleParam particleOptions = fluid2.is(TagsFluid.LAVA) ? Particles.DRIPPING_DRIPSTONE_LAVA : Particles.DRIPPING_DRIPSTONE_WATER;
        world.addParticle(particleOptions, e, f, g, 0.0D, 0.0D, 0.0D);
    }

    @Nullable
    private static BlockPosition findTip(IBlockData state, GeneratorAccess world, BlockPosition pos, int range, boolean allowMerged) {
        if (isTip(state, allowMerged)) {
            return pos;
        } else {
            EnumDirection direction = state.get(TIP_DIRECTION);
            Predicate<IBlockData> predicate = (statex) -> {
                return statex.is(Blocks.POINTED_DRIPSTONE) && statex.get(TIP_DIRECTION) == direction;
            };
            return findBlockVertical(world, pos, direction.getAxisDirection(), predicate, (statex) -> {
                return isTip(statex, allowMerged);
            }, range).orElse((BlockPosition)null);
        }
    }

    @Nullable
    private static EnumDirection calculateTipDirection(IWorldReader world, BlockPosition pos, EnumDirection direction) {
        EnumDirection direction2;
        if (isValidPointedDripstonePlacement(world, pos, direction)) {
            direction2 = direction;
        } else {
            if (!isValidPointedDripstonePlacement(world, pos, direction.opposite())) {
                return null;
            }

            direction2 = direction.opposite();
        }

        return direction2;
    }

    private static DripstoneThickness calculateDripstoneThickness(IWorldReader world, BlockPosition pos, EnumDirection direction, boolean tryMerge) {
        EnumDirection direction2 = direction.opposite();
        IBlockData blockState = world.getType(pos.relative(direction));
        if (isPointedDripstoneWithDirection(blockState, direction2)) {
            return !tryMerge && blockState.get(THICKNESS) != DripstoneThickness.TIP_MERGE ? DripstoneThickness.TIP : DripstoneThickness.TIP_MERGE;
        } else if (!isPointedDripstoneWithDirection(blockState, direction)) {
            return DripstoneThickness.TIP;
        } else {
            DripstoneThickness dripstoneThickness = blockState.get(THICKNESS);
            if (dripstoneThickness != DripstoneThickness.TIP && dripstoneThickness != DripstoneThickness.TIP_MERGE) {
                IBlockData blockState2 = world.getType(pos.relative(direction2));
                return !isPointedDripstoneWithDirection(blockState2, direction) ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
            } else {
                return DripstoneThickness.FRUSTUM;
            }
        }
    }

    public static boolean canDrip(IBlockData state) {
        return isStalactite(state) && state.get(THICKNESS) == DripstoneThickness.TIP && !state.get(WATERLOGGED);
    }

    private static boolean canTipGrow(IBlockData state, WorldServer world, BlockPosition pos) {
        EnumDirection direction = state.get(TIP_DIRECTION);
        BlockPosition blockPos = pos.relative(direction);
        IBlockData blockState = world.getType(blockPos);
        if (!blockState.getFluid().isEmpty()) {
            return false;
        } else {
            return blockState.isAir() ? true : isUnmergedTipWithDirection(blockState, direction.opposite());
        }
    }

    private static Optional<BlockPosition> findRootBlock(World world, BlockPosition pos, IBlockData state, int range) {
        EnumDirection direction = state.get(TIP_DIRECTION);
        Predicate<IBlockData> predicate = (statex) -> {
            return statex.is(Blocks.POINTED_DRIPSTONE) && statex.get(TIP_DIRECTION) == direction;
        };
        return findBlockVertical(world, pos, direction.opposite().getAxisDirection(), predicate, (statex) -> {
            return !statex.is(Blocks.POINTED_DRIPSTONE);
        }, range);
    }

    private static boolean isValidPointedDripstonePlacement(IWorldReader world, BlockPosition pos, EnumDirection direction) {
        BlockPosition blockPos = pos.relative(direction.opposite());
        IBlockData blockState = world.getType(blockPos);
        return blockState.isFaceSturdy(world, blockPos, direction) || isPointedDripstoneWithDirection(blockState, direction);
    }

    private static boolean isTip(IBlockData state, boolean allowMerged) {
        if (!state.is(Blocks.POINTED_DRIPSTONE)) {
            return false;
        } else {
            DripstoneThickness dripstoneThickness = state.get(THICKNESS);
            return dripstoneThickness == DripstoneThickness.TIP || allowMerged && dripstoneThickness == DripstoneThickness.TIP_MERGE;
        }
    }

    private static boolean isUnmergedTipWithDirection(IBlockData state, EnumDirection direction) {
        return isTip(state, false) && state.get(TIP_DIRECTION) == direction;
    }

    private static boolean isStalactite(IBlockData state) {
        return isPointedDripstoneWithDirection(state, EnumDirection.DOWN);
    }

    private static boolean isStalagmite(IBlockData state) {
        return isPointedDripstoneWithDirection(state, EnumDirection.UP);
    }

    private static boolean isStalactiteStartPos(IBlockData state, IWorldReader world, BlockPosition pos) {
        return isStalactite(state) && !world.getType(pos.above()).is(Blocks.POINTED_DRIPSTONE);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    private static boolean isPointedDripstoneWithDirection(IBlockData state, EnumDirection direction) {
        return state.is(Blocks.POINTED_DRIPSTONE) && state.get(TIP_DIRECTION) == direction;
    }

    @Nullable
    private static BlockPosition findFillableCauldronBelowStalactiteTip(World world, BlockPosition pos, FluidType fluid) {
        Predicate<IBlockData> predicate = (state) -> {
            return state.getBlock() instanceof BlockCauldronAbstract && ((BlockCauldronAbstract)state.getBlock()).canReceiveStalactiteDrip(fluid);
        };
        return findBlockVertical(world, pos, EnumDirection.DOWN.getAxisDirection(), BlockBase.BlockData::isAir, predicate, 11).orElse((BlockPosition)null);
    }

    @Nullable
    public static BlockPosition findStalactiteTipAboveCauldron(World world, BlockPosition pos) {
        return findBlockVertical(world, pos, EnumDirection.UP.getAxisDirection(), BlockBase.BlockData::isAir, BlockDripstonePointed::canDrip, 11).orElse((BlockPosition)null);
    }

    public static FluidType getCauldronFillFluidType(World world, BlockPosition pos) {
        return getFluidAboveStalactite(world, pos, world.getType(pos)).filter(BlockDripstonePointed::canFillCauldron).orElse(FluidTypes.EMPTY);
    }

    private static Optional<FluidType> getFluidAboveStalactite(World world, BlockPosition pos, IBlockData state) {
        return !isStalactite(state) ? Optional.empty() : findRootBlock(world, pos, state, 11).map((posx) -> {
            return world.getFluid(posx.above()).getType();
        });
    }

    private static boolean canFillCauldron(FluidType fluid) {
        return fluid == FluidTypes.LAVA || fluid == FluidTypes.WATER;
    }

    private static boolean canGrow(IBlockData dripstoneBlockState, IBlockData waterState) {
        return dripstoneBlockState.is(Blocks.DRIPSTONE_BLOCK) && waterState.is(Blocks.WATER) && waterState.getFluid().isSource();
    }

    private static FluidType getDripFluid(World world, FluidType fluid) {
        if (fluid.isSame(FluidTypes.EMPTY)) {
            return world.getDimensionManager().isNether() ? FluidTypes.LAVA : FluidTypes.WATER;
        } else {
            return fluid;
        }
    }

    private static Optional<BlockPosition> findBlockVertical(GeneratorAccess world, BlockPosition pos, EnumDirection.EnumAxisDirection direction, Predicate<IBlockData> continuePredicate, Predicate<IBlockData> stopPredicate, int range) {
        EnumDirection direction2 = EnumDirection.get(direction, EnumDirection.EnumAxis.Y);
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();

        for(int i = 1; i < range; ++i) {
            mutableBlockPos.move(direction2);
            IBlockData blockState = world.getType(mutableBlockPos);
            if (stopPredicate.test(blockState)) {
                return Optional.of(mutableBlockPos.immutableCopy());
            }

            if (world.isOutsideBuildHeight(mutableBlockPos.getY()) || !continuePredicate.test(blockState)) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
