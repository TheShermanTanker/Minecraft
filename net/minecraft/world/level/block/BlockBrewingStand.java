package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InventoryUtils;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityBrewingStand;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockBrewingStand extends BlockTileEntity {
    public static final BlockStateBoolean[] HAS_BOTTLE = new BlockStateBoolean[]{BlockProperties.HAS_BOTTLE_0, BlockProperties.HAS_BOTTLE_1, BlockProperties.HAS_BOTTLE_2};
    protected static final VoxelShape SHAPE = VoxelShapes.or(Block.box(1.0D, 0.0D, 1.0D, 15.0D, 2.0D, 15.0D), Block.box(7.0D, 0.0D, 7.0D, 9.0D, 14.0D, 9.0D));

    public BlockBrewingStand(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(HAS_BOTTLE[0], Boolean.valueOf(false)).set(HAS_BOTTLE[1], Boolean.valueOf(false)).set(HAS_BOTTLE[2], Boolean.valueOf(false)));
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityBrewingStand(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return world.isClientSide ? null : createTickerHelper(type, TileEntityTypes.BREWING_STAND, TileEntityBrewingStand::serverTick);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityBrewingStand) {
                player.openContainer((TileEntityBrewingStand)blockEntity);
                player.awardStat(StatisticList.INTERACT_WITH_BREWINGSTAND);
            }

            return EnumInteractionResult.CONSUME;
        }
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        if (itemStack.hasName()) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityBrewingStand) {
                ((TileEntityBrewingStand)blockEntity).setCustomName(itemStack.getName());
            }
        }

    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        double d = (double)pos.getX() + 0.4D + (double)random.nextFloat() * 0.2D;
        double e = (double)pos.getY() + 0.7D + (double)random.nextFloat() * 0.3D;
        double f = (double)pos.getZ() + 0.4D + (double)random.nextFloat() * 0.2D;
        world.addParticle(Particles.SMOKE, d, e, f, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityBrewingStand) {
                InventoryUtils.dropInventory(world, pos, (TileEntityBrewingStand)blockEntity);
            }

            super.remove(state, world, pos, newState, moved);
        }
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return Container.getRedstoneSignalFromBlockEntity(world.getTileEntity(pos));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(HAS_BOTTLE[0], HAS_BOTTLE[1], HAS_BOTTLE[2]);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
