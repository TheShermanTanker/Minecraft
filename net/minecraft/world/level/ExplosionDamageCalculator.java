package net.minecraft.world.level;

import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;

public class ExplosionDamageCalculator {
    public Optional<Float> getBlockExplosionResistance(Explosion explosion, IBlockAccess world, BlockPosition pos, IBlockData blockState, Fluid fluidState) {
        return blockState.isAir() && fluidState.isEmpty() ? Optional.empty() : Optional.of(Math.max(blockState.getBlock().getDurability(), fluidState.getExplosionResistance()));
    }

    public boolean shouldBlockExplode(Explosion explosion, IBlockAccess world, BlockPosition pos, IBlockData state, float power) {
        return true;
    }
}
