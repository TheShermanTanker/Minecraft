package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockAmethyst extends Block {
    public BlockAmethyst(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public void onProjectileHit(World world, IBlockData state, MovingObjectPositionBlock hit, IProjectile projectile) {
        if (!world.isClientSide) {
            BlockPosition blockPos = hit.getBlockPosition();
            world.playSound((EntityHuman)null, blockPos, SoundEffects.AMETHYST_BLOCK_HIT, EnumSoundCategory.BLOCKS, 1.0F, 0.5F + world.random.nextFloat() * 1.2F);
            world.playSound((EntityHuman)null, blockPos, SoundEffects.AMETHYST_BLOCK_CHIME, EnumSoundCategory.BLOCKS, 1.0F, 0.5F + world.random.nextFloat() * 1.2F);
        }

    }
}
