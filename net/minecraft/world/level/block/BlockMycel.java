package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockMycel extends BlockDirtSnowSpreadable {
    public BlockMycel(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        super.animateTick(state, world, pos, random);
        if (random.nextInt(10) == 0) {
            world.addParticle(Particles.MYCELIUM, (double)pos.getX() + random.nextDouble(), (double)pos.getY() + 1.1D, (double)pos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
        }

    }
}
