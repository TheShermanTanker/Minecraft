package net.minecraft.world.item.enchantment;

import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class EnchantmentFrostWalker extends Enchantment {
    public EnchantmentFrostWalker(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.ARMOR_FEET, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return level * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 15;
    }

    @Override
    public boolean isTreasure() {
        return true;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }

    public static void onEntityMoved(EntityLiving entity, World world, BlockPosition blockPos, int level) {
        if (entity.isOnGround()) {
            IBlockData blockState = Blocks.FROSTED_ICE.getBlockData();
            float f = (float)Math.min(16, 2 + level);
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(BlockPosition blockPos2 : BlockPosition.betweenClosed(blockPos.offset((double)(-f), -1.0D, (double)(-f)), blockPos.offset((double)f, -1.0D, (double)f))) {
                if (blockPos2.closerThan(entity.getPositionVector(), (double)f)) {
                    mutableBlockPos.set(blockPos2.getX(), blockPos2.getY() + 1, blockPos2.getZ());
                    IBlockData blockState2 = world.getType(mutableBlockPos);
                    if (blockState2.isAir()) {
                        IBlockData blockState3 = world.getType(blockPos2);
                        if (blockState3.getMaterial() == Material.WATER && blockState3.get(BlockFluids.LEVEL) == 0 && blockState.canPlace(world, blockPos2) && world.isUnobstructed(blockState, blockPos2, VoxelShapeCollision.empty())) {
                            world.setTypeUpdate(blockPos2, blockState);
                            world.getBlockTickList().scheduleTick(blockPos2, Blocks.FROSTED_ICE, MathHelper.nextInt(entity.getRandom(), 60, 120));
                        }
                    }
                }
            }

        }
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        return super.checkCompatibility(other) && other != Enchantments.DEPTH_STRIDER;
    }
}
