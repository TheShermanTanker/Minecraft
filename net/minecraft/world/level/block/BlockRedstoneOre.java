package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParamRedstone;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockRedstoneOre extends Block {
    public static final BlockStateBoolean LIT = BlockRedstoneTorch.LIT;

    public BlockRedstoneOre(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.getBlockData().set(LIT, Boolean.valueOf(false)));
    }

    @Override
    public void attack(IBlockData state, World world, BlockPosition pos, EntityHuman player) {
        interact(state, world, pos);
        super.attack(state, world, pos, player);
    }

    @Override
    public void stepOn(World world, BlockPosition pos, IBlockData state, Entity entity) {
        interact(state, world, pos);
        super.stepOn(world, pos, state, entity);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            playEffect(world, pos);
        } else {
            interact(state, world, pos);
        }

        ItemStack itemStack = player.getItemInHand(hand);
        return itemStack.getItem() instanceof ItemBlock && (new BlockActionContext(player, hand, itemStack, hit)).canPlace() ? EnumInteractionResult.PASS : EnumInteractionResult.SUCCESS;
    }

    private static void interact(IBlockData state, World world, BlockPosition pos) {
        playEffect(world, pos);
        if (!state.get(LIT)) {
            world.setTypeAndData(pos, state.set(LIT, Boolean.valueOf(true)), 3);
        }

    }

    @Override
    public boolean isTicking(IBlockData state) {
        return state.get(LIT);
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (state.get(LIT)) {
            world.setTypeAndData(pos, state.set(LIT, Boolean.valueOf(false)), 3);
        }

    }

    @Override
    public void dropNaturally(IBlockData state, WorldServer world, BlockPosition pos, ItemStack stack) {
        super.dropNaturally(state, world, pos, stack);
        if (EnchantmentManager.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) == 0) {
            int i = 1 + world.random.nextInt(5);
            this.dropExperience(world, pos, i);
        }

    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (state.get(LIT)) {
            playEffect(world, pos);
        }

    }

    private static void playEffect(World world, BlockPosition pos) {
        double d = 0.5625D;
        Random random = world.random;

        for(EnumDirection direction : EnumDirection.values()) {
            BlockPosition blockPos = pos.relative(direction);
            if (!world.getType(blockPos).isSolidRender(world, blockPos)) {
                EnumDirection.EnumAxis axis = direction.getAxis();
                double e = axis == EnumDirection.EnumAxis.X ? 0.5D + 0.5625D * (double)direction.getAdjacentX() : (double)random.nextFloat();
                double f = axis == EnumDirection.EnumAxis.Y ? 0.5D + 0.5625D * (double)direction.getAdjacentY() : (double)random.nextFloat();
                double g = axis == EnumDirection.EnumAxis.Z ? 0.5D + 0.5625D * (double)direction.getAdjacentZ() : (double)random.nextFloat();
                world.addParticle(ParticleParamRedstone.REDSTONE, (double)pos.getX() + e, (double)pos.getY() + f, (double)pos.getZ() + g, 0.0D, 0.0D, 0.0D);
            }
        }

    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(LIT);
    }
}
