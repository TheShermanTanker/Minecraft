package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public abstract class BlockCandleAbstract extends Block {
    public static final int LIGHT_PER_CANDLE = 3;
    public static final BlockStateBoolean LIT = BlockProperties.LIT;

    protected BlockCandleAbstract(BlockBase.Info settings) {
        super(settings);
    }

    protected abstract Iterable<Vec3D> getParticleOffsets(IBlockData state);

    public static boolean isLit(IBlockData state) {
        return state.hasProperty(LIT) && (state.is(TagsBlock.CANDLES) || state.is(TagsBlock.CANDLE_CAKES)) && state.get(LIT);
    }

    @Override
    public void onProjectileHit(World world, IBlockData state, MovingObjectPositionBlock hit, IProjectile projectile) {
        if (!world.isClientSide && projectile.isBurning() && this.canBeLit(state)) {
            setLit(world, state, hit.getBlockPosition(), true);
        }

    }

    protected boolean canBeLit(IBlockData state) {
        return !state.get(LIT);
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (state.get(LIT)) {
            this.getParticleOffsets(state).forEach((offset) -> {
                addParticlesAndSound(world, offset.add((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()), random);
            });
        }
    }

    private static void addParticlesAndSound(World world, Vec3D vec3d, Random random) {
        float f = random.nextFloat();
        if (f < 0.3F) {
            world.addParticle(Particles.SMOKE, vec3d.x, vec3d.y, vec3d.z, 0.0D, 0.0D, 0.0D);
            if (f < 0.17F) {
                world.playLocalSound(vec3d.x + 0.5D, vec3d.y + 0.5D, vec3d.z + 0.5D, SoundEffects.CANDLE_AMBIENT, SoundCategory.BLOCKS, 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
            }
        }

        world.addParticle(Particles.SMALL_FLAME, vec3d.x, vec3d.y, vec3d.z, 0.0D, 0.0D, 0.0D);
    }

    public static void extinguish(@Nullable EntityHuman player, IBlockData state, GeneratorAccess world, BlockPosition pos) {
        setLit(world, state, pos, false);
        if (state.getBlock() instanceof BlockCandleAbstract) {
            ((BlockCandleAbstract)state.getBlock()).getParticleOffsets(state).forEach((offset) -> {
                world.addParticle(Particles.SMOKE, (double)pos.getX() + offset.getX(), (double)pos.getY() + offset.getY(), (double)pos.getZ() + offset.getZ(), 0.0D, (double)0.1F, 0.0D);
            });
        }

        world.playSound((EntityHuman)null, pos, SoundEffects.CANDLE_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }

    private static void setLit(GeneratorAccess world, IBlockData state, BlockPosition pos, boolean lit) {
        world.setTypeAndData(pos, state.set(LIT, Boolean.valueOf(lit)), 11);
    }
}
