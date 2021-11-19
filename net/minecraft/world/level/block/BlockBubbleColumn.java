package net.minecraft.world.level.block;

import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockBubbleColumn extends Block implements IFluidSource {
    public static final BlockStateBoolean DRAG_DOWN = BlockProperties.DRAG;
    private static final int CHECK_PERIOD = 5;

    public BlockBubbleColumn(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(DRAG_DOWN, Boolean.valueOf(true)));
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        IBlockData blockState = world.getType(pos.above());
        if (blockState.isAir()) {
            entity.onAboveBubbleCol(state.get(DRAG_DOWN));
            if (!world.isClientSide) {
                WorldServer serverLevel = (WorldServer)world;

                for(int i = 0; i < 2; ++i) {
                    serverLevel.sendParticles(Particles.SPLASH, (double)pos.getX() + world.random.nextDouble(), (double)(pos.getY() + 1), (double)pos.getZ() + world.random.nextDouble(), 1, 0.0D, 0.0D, 0.0D, 1.0D);
                    serverLevel.sendParticles(Particles.BUBBLE, (double)pos.getX() + world.random.nextDouble(), (double)(pos.getY() + 1), (double)pos.getZ() + world.random.nextDouble(), 1, 0.0D, 0.01D, 0.0D, 0.2D);
                }
            }
        } else {
            entity.onInsideBubbleColumn(state.get(DRAG_DOWN));
        }

    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        updateColumn(world, pos, state, world.getType(pos.below()));
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return FluidTypes.WATER.getSource(false);
    }

    public static void updateColumn(GeneratorAccess world, BlockPosition pos, IBlockData state) {
        updateColumn(world, pos, world.getType(pos), state);
    }

    public static void updateColumn(GeneratorAccess world, BlockPosition pos, IBlockData water, IBlockData bubbleSource) {
        if (canExistIn(water)) {
            IBlockData blockState = getColumnState(bubbleSource);
            world.setTypeAndData(pos, blockState, 2);
            BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable().move(EnumDirection.UP);

            while(canExistIn(world.getType(mutableBlockPos))) {
                if (!world.setTypeAndData(mutableBlockPos, blockState, 2)) {
                    return;
                }

                mutableBlockPos.move(EnumDirection.UP);
            }

        }
    }

    private static boolean canExistIn(IBlockData state) {
        return state.is(Blocks.BUBBLE_COLUMN) || state.is(Blocks.WATER) && state.getFluid().getAmount() >= 8 && state.getFluid().isSource();
    }

    private static IBlockData getColumnState(IBlockData state) {
        if (state.is(Blocks.BUBBLE_COLUMN)) {
            return state;
        } else if (state.is(Blocks.SOUL_SAND)) {
            return Blocks.BUBBLE_COLUMN.getBlockData().set(DRAG_DOWN, Boolean.valueOf(false));
        } else {
            return state.is(Blocks.MAGMA_BLOCK) ? Blocks.BUBBLE_COLUMN.getBlockData().set(DRAG_DOWN, Boolean.valueOf(true)) : Blocks.WATER.getBlockData();
        }
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        double d = (double)pos.getX();
        double e = (double)pos.getY();
        double f = (double)pos.getZ();
        if (state.get(DRAG_DOWN)) {
            world.addAlwaysVisibleParticle(Particles.CURRENT_DOWN, d + 0.5D, e + 0.8D, f, 0.0D, 0.0D, 0.0D);
            if (random.nextInt(200) == 0) {
                world.playLocalSound(d, e, f, SoundEffects.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, EnumSoundCategory.BLOCKS, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
            }
        } else {
            world.addAlwaysVisibleParticle(Particles.BUBBLE_COLUMN_UP, d + 0.5D, e, f + 0.5D, 0.0D, 0.04D, 0.0D);
            world.addAlwaysVisibleParticle(Particles.BUBBLE_COLUMN_UP, d + (double)random.nextFloat(), e + (double)random.nextFloat(), f + (double)random.nextFloat(), 0.0D, 0.04D, 0.0D);
            if (random.nextInt(200) == 0) {
                world.playLocalSound(d, e, f, SoundEffects.BUBBLE_COLUMN_UPWARDS_AMBIENT, EnumSoundCategory.BLOCKS, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
            }
        }

    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        if (!state.canPlace(world, pos) || direction == EnumDirection.DOWN || direction == EnumDirection.UP && !neighborState.is(Blocks.BUBBLE_COLUMN) && canExistIn(neighborState)) {
            world.getBlockTickList().scheduleTick(pos, this, 5);
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos.below());
        return blockState.is(Blocks.BUBBLE_COLUMN) || blockState.is(Blocks.MAGMA_BLOCK) || blockState.is(Blocks.SOUL_SAND);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return VoxelShapes.empty();
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(DRAG_DOWN);
    }

    @Override
    public ItemStack removeFluid(GeneratorAccess world, BlockPosition pos, IBlockData state) {
        world.setTypeAndData(pos, Blocks.AIR.getBlockData(), 11);
        return new ItemStack(Items.WATER_BUCKET);
    }

    @Override
    public Optional<SoundEffect> getPickupSound() {
        return FluidTypes.WATER.getPickupSound();
    }
}
