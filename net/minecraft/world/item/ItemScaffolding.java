package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockScaffolding;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemScaffolding extends ItemBlock {
    public ItemScaffolding(Block block, Item.Info settings) {
        super(block, settings);
    }

    @Nullable
    @Override
    public BlockActionContext updatePlacementContext(BlockActionContext context) {
        BlockPosition blockPos = context.getClickPosition();
        World level = context.getWorld();
        IBlockData blockState = level.getType(blockPos);
        Block block = this.getBlock();
        if (!blockState.is(block)) {
            return BlockScaffolding.getDistance(level, blockPos) == 7 ? null : context;
        } else {
            EnumDirection direction;
            if (context.isSneaking()) {
                direction = context.isInside() ? context.getClickedFace().opposite() : context.getClickedFace();
            } else {
                direction = context.getClickedFace() == EnumDirection.UP ? context.getHorizontalDirection() : EnumDirection.UP;
            }

            int i = 0;
            BlockPosition.MutableBlockPosition mutableBlockPos = blockPos.mutable().move(direction);

            while(i < 7) {
                if (!level.isClientSide && !level.isValidLocation(mutableBlockPos)) {
                    EntityHuman player = context.getEntity();
                    int j = level.getMaxBuildHeight();
                    if (player instanceof EntityPlayer && mutableBlockPos.getY() >= j) {
                        ((EntityPlayer)player).sendMessage((new ChatMessage("build.tooHigh", j - 1)).withStyle(EnumChatFormat.RED), ChatMessageType.GAME_INFO, SystemUtils.NIL_UUID);
                    }
                    break;
                }

                blockState = level.getType(mutableBlockPos);
                if (!blockState.is(this.getBlock())) {
                    if (blockState.canBeReplaced(context)) {
                        return BlockActionContext.at(context, mutableBlockPos, direction);
                    }
                    break;
                }

                mutableBlockPos.move(direction);
                if (direction.getAxis().isHorizontal()) {
                    ++i;
                }
            }

            return null;
        }
    }

    @Override
    protected boolean isCheckCollisions() {
        return false;
    }
}
