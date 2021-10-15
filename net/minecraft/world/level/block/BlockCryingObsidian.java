package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockCryingObsidian extends Block {
    public BlockCryingObsidian(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (random.nextInt(5) == 0) {
            EnumDirection direction = EnumDirection.getRandom(random);
            if (direction != EnumDirection.UP) {
                BlockPosition blockPos = pos.relative(direction);
                IBlockData blockState = world.getType(blockPos);
                if (!state.canOcclude() || !blockState.isFaceSturdy(world, blockPos, direction.opposite())) {
                    double d = direction.getAdjacentX() == 0 ? random.nextDouble() : 0.5D + (double)direction.getAdjacentX() * 0.6D;
                    double e = direction.getAdjacentY() == 0 ? random.nextDouble() : 0.5D + (double)direction.getAdjacentY() * 0.6D;
                    double f = direction.getAdjacentZ() == 0 ? random.nextDouble() : 0.5D + (double)direction.getAdjacentZ() * 0.6D;
                    world.addParticle(Particles.DRIPPING_OBSIDIAN_TEAR, (double)pos.getX() + d, (double)pos.getY() + e, (double)pos.getZ() + f, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }
}
