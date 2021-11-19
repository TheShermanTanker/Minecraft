package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockWetSponge extends Block {
    protected BlockWetSponge(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (world.getDimensionManager().isNether()) {
            world.setTypeAndData(pos, Blocks.SPONGE.getBlockData(), 3);
            world.triggerEffect(2009, pos, 0);
            world.playSound((EntityHuman)null, pos, SoundEffects.FIRE_EXTINGUISH, EnumSoundCategory.BLOCKS, 1.0F, (1.0F + world.getRandom().nextFloat() * 0.2F) * 0.7F);
        }

    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        EnumDirection direction = EnumDirection.getRandom(random);
        if (direction != EnumDirection.UP) {
            BlockPosition blockPos = pos.relative(direction);
            IBlockData blockState = world.getType(blockPos);
            if (!state.canOcclude() || !blockState.isFaceSturdy(world, blockPos, direction.opposite())) {
                double d = (double)pos.getX();
                double e = (double)pos.getY();
                double f = (double)pos.getZ();
                if (direction == EnumDirection.DOWN) {
                    e = e - 0.05D;
                    d += random.nextDouble();
                    f += random.nextDouble();
                } else {
                    e = e + random.nextDouble() * 0.8D;
                    if (direction.getAxis() == EnumDirection.EnumAxis.X) {
                        f += random.nextDouble();
                        if (direction == EnumDirection.EAST) {
                            ++d;
                        } else {
                            d += 0.05D;
                        }
                    } else {
                        d += random.nextDouble();
                        if (direction == EnumDirection.SOUTH) {
                            ++f;
                        } else {
                            f += 0.05D;
                        }
                    }
                }

                world.addParticle(Particles.DRIPPING_WATER, d, e, f, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
