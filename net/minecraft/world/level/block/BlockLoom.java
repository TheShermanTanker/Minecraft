package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerLoom;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockLoom extends BlockFacingHorizontal {
    private static final IChatBaseComponent CONTAINER_TITLE = new ChatMessage("container.loom");

    protected BlockLoom(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            player.openContainer(state.getMenuProvider(world, pos));
            player.awardStat(StatisticList.INTERACT_WITH_LOOM);
            return EnumInteractionResult.CONSUME;
        }
    }

    @Override
    public ITileInventory getInventory(IBlockData state, World world, BlockPosition pos) {
        return new TileInventory((syncId, inventory, player) -> {
            return new ContainerLoom(syncId, inventory, ContainerAccess.at(world, pos));
        }, CONTAINER_TITLE);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(FACING, ctx.getHorizontalDirection().opposite());
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING);
    }
}
