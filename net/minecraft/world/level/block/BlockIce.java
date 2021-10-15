package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.material.Material;

public class BlockIce extends BlockHalfTransparent {
    public BlockIce(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public void playerDestroy(World world, EntityHuman player, BlockPosition pos, IBlockData state, @Nullable TileEntity blockEntity, ItemStack stack) {
        super.playerDestroy(world, player, pos, state, blockEntity, stack);
        if (EnchantmentManager.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) == 0) {
            if (world.getDimensionManager().isNether()) {
                world.removeBlock(pos, false);
                return;
            }

            Material material = world.getType(pos.below()).getMaterial();
            if (material.isSolid() || material.isLiquid()) {
                world.setTypeUpdate(pos, Blocks.WATER.getBlockData());
            }
        }

    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (world.getBrightness(EnumSkyBlock.BLOCK, pos) > 11 - state.getLightBlock(world, pos)) {
            this.melt(state, world, pos);
        }

    }

    protected void melt(IBlockData state, World world, BlockPosition pos) {
        if (world.getDimensionManager().isNether()) {
            world.removeBlock(pos, false);
        } else {
            world.setTypeUpdate(pos, Blocks.WATER.getBlockData());
            world.neighborChanged(pos, Blocks.WATER, pos);
        }
    }

    @Override
    public EnumPistonReaction getPushReaction(IBlockData state) {
        return EnumPistonReaction.NORMAL;
    }
}
