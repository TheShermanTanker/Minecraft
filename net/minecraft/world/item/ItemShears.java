package net.minecraft.world.item;

import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemShears extends Item {
    public ItemShears(Item.Info settings) {
        super(settings);
    }

    @Override
    public boolean mineBlock(ItemStack stack, World world, IBlockData state, BlockPosition pos, EntityLiving miner) {
        if (!world.isClientSide && !state.is(TagsBlock.FIRE)) {
            stack.damage(1, miner, (e) -> {
                e.broadcastItemBreak(EnumItemSlot.MAINHAND);
            });
        }

        return !state.is(TagsBlock.LEAVES) && !state.is(Blocks.COBWEB) && !state.is(Blocks.GRASS) && !state.is(Blocks.FERN) && !state.is(Blocks.DEAD_BUSH) && !state.is(Blocks.HANGING_ROOTS) && !state.is(Blocks.VINE) && !state.is(Blocks.TRIPWIRE) && !state.is(TagsBlock.WOOL) ? super.mineBlock(stack, world, state, pos, miner) : true;
    }

    @Override
    public boolean canDestroySpecialBlock(IBlockData state) {
        return state.is(Blocks.COBWEB) || state.is(Blocks.REDSTONE_WIRE) || state.is(Blocks.TRIPWIRE);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, IBlockData state) {
        if (!state.is(Blocks.COBWEB) && !state.is(TagsBlock.LEAVES)) {
            if (state.is(TagsBlock.WOOL)) {
                return 5.0F;
            } else {
                return !state.is(Blocks.VINE) && !state.is(Blocks.GLOW_LICHEN) ? super.getDestroySpeed(stack, state) : 2.0F;
            }
        } else {
            return 15.0F;
        }
    }
}
