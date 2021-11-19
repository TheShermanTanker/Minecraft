package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParamDustColorTransition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockSculkSensor extends BlockTileEntity implements IBlockWaterlogged {
    public static final int ACTIVE_TICKS = 40;
    public static final int COOLDOWN_TICKS = 1;
    public static final Object2IntMap<GameEvent> VIBRATION_STRENGTH_FOR_EVENT = Object2IntMaps.unmodifiable(SystemUtils.make(new Object2IntOpenHashMap<>(), (map) -> {
        map.put(GameEvent.STEP, 1);
        map.put(GameEvent.FLAP, 2);
        map.put(GameEvent.SWIM, 3);
        map.put(GameEvent.ELYTRA_FREE_FALL, 4);
        map.put(GameEvent.HIT_GROUND, 5);
        map.put(GameEvent.SPLASH, 6);
        map.put(GameEvent.WOLF_SHAKING, 6);
        map.put(GameEvent.MINECART_MOVING, 6);
        map.put(GameEvent.RING_BELL, 6);
        map.put(GameEvent.BLOCK_CHANGE, 6);
        map.put(GameEvent.PROJECTILE_SHOOT, 7);
        map.put(GameEvent.DRINKING_FINISH, 7);
        map.put(GameEvent.PRIME_FUSE, 7);
        map.put(GameEvent.PROJECTILE_LAND, 8);
        map.put(GameEvent.EAT, 8);
        map.put(GameEvent.MOB_INTERACT, 8);
        map.put(GameEvent.ENTITY_DAMAGED, 8);
        map.put(GameEvent.EQUIP, 9);
        map.put(GameEvent.SHEAR, 9);
        map.put(GameEvent.RAVAGER_ROAR, 9);
        map.put(GameEvent.BLOCK_CLOSE, 10);
        map.put(GameEvent.BLOCK_UNSWITCH, 10);
        map.put(GameEvent.BLOCK_UNPRESS, 10);
        map.put(GameEvent.BLOCK_DETACH, 10);
        map.put(GameEvent.DISPENSE_FAIL, 10);
        map.put(GameEvent.BLOCK_OPEN, 11);
        map.put(GameEvent.BLOCK_SWITCH, 11);
        map.put(GameEvent.BLOCK_PRESS, 11);
        map.put(GameEvent.BLOCK_ATTACH, 11);
        map.put(GameEvent.ENTITY_PLACE, 12);
        map.put(GameEvent.BLOCK_PLACE, 12);
        map.put(GameEvent.FLUID_PLACE, 12);
        map.put(GameEvent.ENTITY_KILLED, 13);
        map.put(GameEvent.BLOCK_DESTROY, 13);
        map.put(GameEvent.FLUID_PICKUP, 13);
        map.put(GameEvent.FISHING_ROD_REEL_IN, 14);
        map.put(GameEvent.CONTAINER_CLOSE, 14);
        map.put(GameEvent.PISTON_CONTRACT, 14);
        map.put(GameEvent.SHULKER_CLOSE, 14);
        map.put(GameEvent.PISTON_EXTEND, 15);
        map.put(GameEvent.CONTAINER_OPEN, 15);
        map.put(GameEvent.FISHING_ROD_CAST, 15);
        map.put(GameEvent.EXPLODE, 15);
        map.put(GameEvent.LIGHTNING_STRIKE, 15);
        map.put(GameEvent.SHULKER_OPEN, 15);
    }));
    public static final BlockStateEnum<SculkSensorPhase> PHASE = BlockProperties.SCULK_SENSOR_PHASE;
    public static final BlockStateInteger POWER = BlockProperties.POWER;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    private final int listenerRange;

    public BlockSculkSensor(BlockBase.Info settings, int range) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(PHASE, SculkSensorPhase.INACTIVE).set(POWER, Integer.valueOf(0)).set(WATERLOGGED, Boolean.valueOf(false)));
        this.listenerRange = range;
    }

    public int getListenerRange() {
        return this.listenerRange;
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        BlockPosition blockPos = ctx.getClickPosition();
        Fluid fluidState = ctx.getWorld().getFluid(blockPos);
        return this.getBlockData().set(WATERLOGGED, Boolean.valueOf(fluidState.getType() == FluidTypes.WATER));
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (getPhase(state) != SculkSensorPhase.ACTIVE) {
            if (getPhase(state) == SculkSensorPhase.COOLDOWN) {
                world.setTypeAndData(pos, state.set(PHASE, SculkSensorPhase.INACTIVE), 3);
            }

        } else {
            deactivate(world, pos, state);
        }
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!world.isClientSide() && !state.is(oldState.getBlock())) {
            if (state.get(POWER) > 0 && !world.getBlockTickList().hasScheduledTick(pos, this)) {
                world.setTypeAndData(pos, state.set(POWER, Integer.valueOf(0)), 18);
            }

            world.getBlockTickList().scheduleTick(new BlockPosition(pos), state.getBlock(), 1);
        }
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (getPhase(state) == SculkSensorPhase.ACTIVE) {
                updateNeighbours(world, pos);
            }

            super.remove(state, world, pos, newState, moved);
        }
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    private static void updateNeighbours(World world, BlockPosition pos) {
        world.applyPhysics(pos, Blocks.SCULK_SENSOR);
        world.applyPhysics(pos.relative(EnumDirection.UP.opposite()), Blocks.SCULK_SENSOR);
    }

    @Nullable
    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new SculkSensorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> GameEventListener getListener(World world, T blockEntity) {
        return blockEntity instanceof SculkSensorBlockEntity ? ((SculkSensorBlockEntity)blockEntity).getListener() : null;
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return !world.isClientSide ? createTickerHelper(type, TileEntityTypes.SCULK_SENSOR, (worldx, pos, statex, blockEntity) -> {
            blockEntity.getListener().tick(worldx);
        }) : null;
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.get(POWER);
    }

    public static SculkSensorPhase getPhase(IBlockData state) {
        return state.get(PHASE);
    }

    public static boolean canActivate(IBlockData state) {
        return getPhase(state) == SculkSensorPhase.INACTIVE;
    }

    public static void deactivate(World world, BlockPosition pos, IBlockData state) {
        world.setTypeAndData(pos, state.set(PHASE, SculkSensorPhase.COOLDOWN).set(POWER, Integer.valueOf(0)), 3);
        world.getBlockTickList().scheduleTick(new BlockPosition(pos), state.getBlock(), 1);
        if (!state.get(WATERLOGGED)) {
            world.playSound((EntityHuman)null, pos, SoundEffects.SCULK_CLICKING_STOP, EnumSoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.2F + 0.8F);
        }

        updateNeighbours(world, pos);
    }

    public static void activate(World world, BlockPosition pos, IBlockData state, int power) {
        world.setTypeAndData(pos, state.set(PHASE, SculkSensorPhase.ACTIVE).set(POWER, Integer.valueOf(power)), 3);
        world.getBlockTickList().scheduleTick(new BlockPosition(pos), state.getBlock(), 40);
        updateNeighbours(world, pos);
        if (!state.get(WATERLOGGED)) {
            world.playSound((EntityHuman)null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEffects.SCULK_CLICKING, EnumSoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.2F + 0.8F);
        }

    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (getPhase(state) == SculkSensorPhase.ACTIVE) {
            EnumDirection direction = EnumDirection.getRandom(random);
            if (direction != EnumDirection.UP && direction != EnumDirection.DOWN) {
                double d = (double)pos.getX() + 0.5D + (direction.getAdjacentX() == 0 ? 0.5D - random.nextDouble() : (double)direction.getAdjacentX() * 0.6D);
                double e = (double)pos.getY() + 0.25D;
                double f = (double)pos.getZ() + 0.5D + (direction.getAdjacentZ() == 0 ? 0.5D - random.nextDouble() : (double)direction.getAdjacentZ() * 0.6D);
                double g = (double)random.nextFloat() * 0.04D;
                world.addParticle(ParticleParamDustColorTransition.SCULK_TO_REDSTONE, d, e, f, 0.0D, g, 0.0D);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(PHASE, POWER, WATERLOGGED);
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof SculkSensorBlockEntity) {
            SculkSensorBlockEntity sculkSensorBlockEntity = (SculkSensorBlockEntity)blockEntity;
            return getPhase(state) == SculkSensorPhase.ACTIVE ? sculkSensorBlockEntity.getLastVibrationFrequency() : 0;
        } else {
            return 0;
        }
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    @Override
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return true;
    }
}
