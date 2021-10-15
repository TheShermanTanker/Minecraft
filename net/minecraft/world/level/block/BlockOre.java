package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockOre extends Block {
    private final UniformInt xpRange;

    public BlockOre(BlockBase.Info settings) {
        this(settings, UniformInt.of(0, 0));
    }

    public BlockOre(BlockBase.Info settings, UniformInt experienceDropped) {
        super(settings);
        this.xpRange = experienceDropped;
    }

    @Override
    public void dropNaturally(IBlockData state, WorldServer world, BlockPosition pos, ItemStack stack) {
        super.dropNaturally(state, world, pos, stack);
        if (EnchantmentManager.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) == 0) {
            int i = this.xpRange.sample(world.random);
            if (i > 0) {
                this.dropExperience(world, pos, i);
            }
        }

    }
}
