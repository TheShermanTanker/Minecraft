package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerStonecutter;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockStonecutter extends Block {
    private static final IChatBaseComponent CONTAINER_TITLE = new ChatMessage("container.stonecutter");
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D);

    public BlockStonecutter(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH));
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(FACING, ctx.getHorizontalDirection().opposite());
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            player.openContainer(state.getMenuProvider(world, pos));
            player.awardStat(StatisticList.INTERACT_WITH_STONECUTTER);
            return EnumInteractionResult.CONSUME;
        }
    }

    @Nullable
    @Override
    public ITileInventory getInventory(IBlockData state, World world, BlockPosition pos) {
        return new TileInventory((syncId, playerInventory, player) -> {
            return new ContainerStonecutter(syncId, playerInventory, ContainerAccess.at(world, pos));
        }, CONTAINER_TITLE);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return true;
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
        builder.add(FACING);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
