package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InventoryUtils;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityFurnace;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public abstract class BlockFurnace extends BlockTileEntity {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateBoolean LIT = BlockProperties.LIT;

    protected BlockFurnace(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(LIT, Boolean.valueOf(false)));
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            this.openContainer(world, pos, player);
            return EnumInteractionResult.CONSUME;
        }
    }

    protected abstract void openContainer(World world, BlockPosition pos, EntityHuman player);

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(FACING, ctx.getHorizontalDirection().opposite());
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        if (itemStack.hasName()) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityFurnace) {
                ((TileEntityFurnace)blockEntity).setCustomName(itemStack.getName());
            }
        }

    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityFurnace) {
                if (world instanceof WorldServer) {
                    InventoryUtils.dropInventory(world, pos, (TileEntityFurnace)blockEntity);
                    ((TileEntityFurnace)blockEntity).getRecipesToAwardAndPopExperience((WorldServer)world, Vec3D.atCenterOf(pos));
                }

                world.updateAdjacentComparators(pos, this);
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
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
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
        builder.add(FACING, LIT);
    }

    @Nullable
    protected static <T extends TileEntity> BlockEntityTicker<T> createFurnaceTicker(World world, TileEntityTypes<T> givenType, TileEntityTypes<? extends TileEntityFurnace> expectedType) {
        return world.isClientSide ? null : createTickerHelper(givenType, expectedType, TileEntityFurnace::serverTick);
    }
}
