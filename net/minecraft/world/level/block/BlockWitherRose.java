package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectBase;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockWitherRose extends BlockFlowers {
    public BlockWitherRose(MobEffectBase effect, BlockBase.Info settings) {
        super(effect, 8, settings);
    }

    @Override
    protected boolean mayPlaceOn(IBlockData floor, IBlockAccess world, BlockPosition pos) {
        return super.mayPlaceOn(floor, world, pos) || floor.is(Blocks.NETHERRACK) || floor.is(Blocks.SOUL_SAND) || floor.is(Blocks.SOUL_SOIL);
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        VoxelShape voxelShape = this.getShape(state, world, pos, VoxelShapeCollision.empty());
        Vec3D vec3 = voxelShape.getBoundingBox().getCenter();
        double d = (double)pos.getX() + vec3.x;
        double e = (double)pos.getZ() + vec3.z;

        for(int i = 0; i < 3; ++i) {
            if (random.nextBoolean()) {
                world.addParticle(Particles.SMOKE, d + random.nextDouble() / 5.0D, (double)pos.getY() + (0.5D - random.nextDouble()), e + random.nextDouble() / 5.0D, 0.0D, 0.0D, 0.0D);
            }
        }

    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (!world.isClientSide && world.getDifficulty() != EnumDifficulty.PEACEFUL) {
            if (entity instanceof EntityLiving) {
                EntityLiving livingEntity = (EntityLiving)entity;
                if (!livingEntity.isInvulnerable(DamageSource.WITHER)) {
                    livingEntity.addEffect(new MobEffect(MobEffectList.WITHER, 40));
                }
            }

        }
    }
}
