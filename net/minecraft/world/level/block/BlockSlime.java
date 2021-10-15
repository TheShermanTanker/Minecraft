package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

public class BlockSlime extends BlockHalfTransparent {
    public BlockSlime(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public void fallOn(World world, IBlockData state, BlockPosition pos, Entity entity, float fallDistance) {
        if (entity.isSuppressingBounce()) {
            super.fallOn(world, state, pos, entity, fallDistance);
        } else {
            entity.causeFallDamage(fallDistance, 0.0F, DamageSource.FALL);
        }

    }

    @Override
    public void updateEntityAfterFallOn(IBlockAccess world, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(world, entity);
        } else {
            this.bounceUp(entity);
        }

    }

    private void bounceUp(Entity entity) {
        Vec3D vec3 = entity.getMot();
        if (vec3.y < 0.0D) {
            double d = entity instanceof EntityLiving ? 1.0D : 0.8D;
            entity.setMot(vec3.x, -vec3.y * d, vec3.z);
        }

    }

    @Override
    public void stepOn(World world, BlockPosition pos, IBlockData state, Entity entity) {
        double d = Math.abs(entity.getMot().y);
        if (d < 0.1D && !entity.isSteppingCarefully()) {
            double e = 0.4D + d * 0.2D;
            entity.setMot(entity.getMot().multiply(e, 1.0D, e));
        }

        super.stepOn(world, pos, state, entity);
    }
}
