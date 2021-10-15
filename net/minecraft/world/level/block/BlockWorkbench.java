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
import net.minecraft.world.inventory.ContainerWorkbench;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockWorkbench extends Block {
    private static final IChatBaseComponent CONTAINER_TITLE = new ChatMessage("container.crafting");

    protected BlockWorkbench(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            player.openContainer(state.getMenuProvider(world, pos));
            player.awardStat(StatisticList.INTERACT_WITH_CRAFTING_TABLE);
            return EnumInteractionResult.CONSUME;
        }
    }

    @Override
    public ITileInventory getInventory(IBlockData state, World world, BlockPosition pos) {
        return new TileInventory((syncId, inventory, player) -> {
            return new ContainerWorkbench(syncId, inventory, ContainerAccess.at(world, pos));
        }, CONTAINER_TITLE);
    }
}
