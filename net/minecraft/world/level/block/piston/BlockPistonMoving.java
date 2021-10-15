package net.minecraft.world.level.block.piston;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockTileEntity;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyPistonType;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockPistonMoving extends BlockTileEntity {
    public static final BlockStateDirection FACING = BlockPistonExtension.FACING;
    public static final BlockStateEnum<BlockPropertyPistonType> TYPE = BlockPistonExtension.TYPE;

    public BlockPistonMoving(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(TYPE, BlockPropertyPistonType.DEFAULT));
    }

    @Nullable
    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return null;
    }

    public static TileEntity newMovingBlockEntity(BlockPosition pos, IBlockData state, IBlockData pushedBlock, EnumDirection facing, boolean extending, boolean source) {
        return new TileEntityPiston(pos, state, pushedBlock, facing, extending, source);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return createTickerHelper(type, TileEntityTypes.PISTON, TileEntityPiston::tick);
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityPiston) {
                ((TileEntityPiston)blockEntity).finalTick();
            }

        }
    }

    @Override
    public void postBreak(GeneratorAccess world, BlockPosition pos, IBlockData state) {
        BlockPosition blockPos = pos.relative(state.get(FACING).opposite());
        IBlockData blockState = world.getType(blockPos);
        if (blockState.getBlock() instanceof BlockPiston && blockState.get(BlockPiston.EXTENDED)) {
            world.removeBlock(blockPos, false);
        }

    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (!world.isClientSide && world.getTileEntity(pos) == null) {
            world.removeBlock(pos, false);
            return EnumInteractionResult.CONSUME;
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    public List<ItemStack> getDrops(IBlockData state, LootTableInfo.Builder builder) {
        TileEntityPiston pistonMovingBlockEntity = this.getBlockEntity(builder.getLevel(), new BlockPosition(builder.getParameter(LootContextParameters.ORIGIN)));
        return pistonMovingBlockEntity == null ? Collections.emptyList() : pistonMovingBlockEntity.getMovedState().getDrops(builder);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        TileEntityPiston pistonMovingBlockEntity = this.getBlockEntity(world, pos);
        return pistonMovingBlockEntity != null ? pistonMovingBlockEntity.getCollisionShape(world, pos) : VoxelShapes.empty();
    }

    @Nullable
    private TileEntityPiston getBlockEntity(IBlockAccess world, BlockPosition pos) {
        TileEntity blockEntity = world.getTileEntity(pos);
        return blockEntity instanceof TileEntityPiston ? (TileEntityPiston)blockEntity : null;
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return ItemStack.EMPTY;
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
        builder.add(FACING, TYPE);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
