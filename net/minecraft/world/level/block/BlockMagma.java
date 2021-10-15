package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockMagma extends Block {
    private static final int BUBBLE_COLUMN_CHECK_DELAY = 20;

    public BlockMagma(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public void stepOn(World world, BlockPosition pos, IBlockData state, Entity entity) {
        if (!entity.isFireProof() && entity instanceof EntityLiving && !EnchantmentManager.hasFrostWalker((EntityLiving)entity)) {
            entity.damageEntity(DamageSource.HOT_FLOOR, 1.0F);
        }

        super.stepOn(world, pos, state, entity);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        BlockBubbleColumn.updateColumn(world, pos.above(), state);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (direction == EnumDirection.UP && neighborState.is(Blocks.WATER)) {
            world.getBlockTickList().scheduleTick(pos, this, 20);
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        BlockPosition blockPos = pos.above();
        if (world.getFluid(pos).is(TagsFluid.WATER)) {
            world.playSound((EntityHuman)null, pos, SoundEffects.FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
            world.sendParticles(Particles.LARGE_SMOKE, (double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.25D, (double)blockPos.getZ() + 0.5D, 8, 0.5D, 0.25D, 0.5D, 0.0D);
        }

    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        world.getBlockTickList().scheduleTick(pos, this, 20);
    }
}
