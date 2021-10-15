package net.minecraft.world.level;

import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;

public class ExplosionDamageCalculatorEntity extends ExplosionDamageCalculator {
    private final Entity source;

    public ExplosionDamageCalculatorEntity(Entity entity) {
        this.source = entity;
    }

    @Override
    public Optional<Float> getBlockExplosionResistance(Explosion explosion, IBlockAccess world, BlockPosition pos, IBlockData blockState, Fluid fluidState) {
        return super.getBlockExplosionResistance(explosion, world, pos, blockState, fluidState).map((float_) -> {
            return this.source.getBlockExplosionResistance(explosion, world, pos, blockState, fluidState, float_);
        });
    }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, IBlockAccess world, BlockPosition pos, IBlockData state, float power) {
        return this.source.shouldBlockExplode(explosion, world, pos, state, power);
    }
}
