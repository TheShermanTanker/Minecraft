package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.monster.piglin.PiglinAI;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.InventoryEnderChest;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.entity.TileEntityEnderChest;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockEnderChest extends BlockChestAbstract<TileEntityEnderChest> implements IBlockWaterlogged {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    protected static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    private static final IChatBaseComponent CONTAINER_TITLE = new ChatMessage("container.enderchest");

    protected BlockEnderChest(BlockBase.Info settings) {
        super(settings, () -> {
            return TileEntityTypes.ENDER_CHEST;
        });
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public DoubleBlockFinder.Result<? extends TileEntityChest> combine(IBlockData state, World world, BlockPosition pos, boolean ignoreBlocked) {
        return DoubleBlockFinder.Combiner::acceptNone;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        Fluid fluidState = ctx.getWorld().getFluid(ctx.getClickPosition());
        return this.getBlockData().set(FACING, ctx.getHorizontalDirection().opposite()).set(WATERLOGGED, Boolean.valueOf(fluidState.getType() == FluidTypes.WATER));
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        InventoryEnderChest playerEnderChestContainer = player.getEnderChest();
        TileEntity blockEntity = world.getTileEntity(pos);
        if (playerEnderChestContainer != null && blockEntity instanceof TileEntityEnderChest) {
            BlockPosition blockPos = pos.above();
            if (world.getType(blockPos).isOccluding(world, blockPos)) {
                return EnumInteractionResult.sidedSuccess(world.isClientSide);
            } else if (world.isClientSide) {
                return EnumInteractionResult.SUCCESS;
            } else {
                TileEntityEnderChest enderChestBlockEntity = (TileEntityEnderChest)blockEntity;
                playerEnderChestContainer.setActiveChest(enderChestBlockEntity);
                player.openContainer(new TileInventory((syncId, inventory, playerx) -> {
                    return ContainerChest.threeRows(syncId, inventory, playerEnderChestContainer);
                }, CONTAINER_TITLE));
                player.awardStat(StatisticList.OPEN_ENDERCHEST);
                PiglinAI.angerNearbyPiglins(player, true);
                return EnumInteractionResult.CONSUME;
            }
        } else {
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityEnderChest(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return world.isClientSide ? createTickerHelper(type, TileEntityTypes.ENDER_CHEST, TileEntityEnderChest::lidAnimateTick) : null;
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        for(int i = 0; i < 3; ++i) {
            int j = random.nextInt(2) * 2 - 1;
            int k = random.nextInt(2) * 2 - 1;
            double d = (double)pos.getX() + 0.5D + 0.25D * (double)j;
            double e = (double)((float)pos.getY() + random.nextFloat());
            double f = (double)pos.getZ() + 0.5D + 0.25D * (double)k;
            double g = (double)(random.nextFloat() * (float)j);
            double h = ((double)random.nextFloat() - 0.5D) * 0.125D;
            double l = (double)(random.nextFloat() * (float)k);
            world.addParticle(Particles.PORTAL, d, e, f, g, h, l);
        }

    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityEnderChest) {
            ((TileEntityEnderChest)blockEntity).recheckOpen();
        }

    }
}
