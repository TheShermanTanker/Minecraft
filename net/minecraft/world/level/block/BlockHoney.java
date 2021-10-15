package net.minecraft.world.level.block;

import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParamBlock;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.item.EntityTNTPrimed;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.entity.vehicle.EntityMinecartAbstract;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockHoney extends BlockHalfTransparent {
    private static final double SLIDE_STARTS_WHEN_VERTICAL_SPEED_IS_AT_LEAST = 0.13D;
    private static final double MIN_FALL_SPEED_TO_BE_CONSIDERED_SLIDING = 0.08D;
    private static final double THROTTLE_SLIDE_SPEED_TO = 0.05D;
    private static final int SLIDE_ADVANCEMENT_CHECK_INTERVAL = 20;
    protected static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D);

    public BlockHoney(BlockBase.Info settings) {
        super(settings);
    }

    private static boolean doesEntityDoHoneyBlockSlideEffects(Entity entity) {
        return entity instanceof EntityLiving || entity instanceof EntityMinecartAbstract || entity instanceof EntityTNTPrimed || entity instanceof EntityBoat;
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public void fallOn(World world, IBlockData state, BlockPosition pos, Entity entity, float fallDistance) {
        entity.playSound(SoundEffects.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
        if (!world.isClientSide) {
            world.broadcastEntityEffect(entity, (byte)54);
        }

        if (entity.causeFallDamage(fallDistance, 0.2F, DamageSource.FALL)) {
            entity.playSound(this.soundType.getFallSound(), this.soundType.getVolume() * 0.5F, this.soundType.getPitch() * 0.75F);
        }

    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (this.isSlidingDown(pos, entity)) {
            this.maybeDoSlideAchievement(entity, pos);
            this.doSlideMovement(entity);
            this.maybeDoSlideEffects(world, entity);
        }

        super.entityInside(state, world, pos, entity);
    }

    private boolean isSlidingDown(BlockPosition pos, Entity entity) {
        if (entity.isOnGround()) {
            return false;
        } else if (entity.locY() > (double)pos.getY() + 0.9375D - 1.0E-7D) {
            return false;
        } else if (entity.getMot().y >= -0.08D) {
            return false;
        } else {
            double d = Math.abs((double)pos.getX() + 0.5D - entity.locX());
            double e = Math.abs((double)pos.getZ() + 0.5D - entity.locZ());
            double f = 0.4375D + (double)(entity.getWidth() / 2.0F);
            return d + 1.0E-7D > f || e + 1.0E-7D > f;
        }
    }

    private void maybeDoSlideAchievement(Entity entity, BlockPosition pos) {
        if (entity instanceof EntityPlayer && entity.level.getTime() % 20L == 0L) {
            CriterionTriggers.HONEY_BLOCK_SLIDE.trigger((EntityPlayer)entity, entity.level.getType(pos));
        }

    }

    private void doSlideMovement(Entity entity) {
        Vec3D vec3 = entity.getMot();
        if (vec3.y < -0.13D) {
            double d = -0.05D / vec3.y;
            entity.setMot(new Vec3D(vec3.x * d, -0.05D, vec3.z * d));
        } else {
            entity.setMot(new Vec3D(vec3.x, -0.05D, vec3.z));
        }

        entity.fallDistance = 0.0F;
    }

    private void maybeDoSlideEffects(World world, Entity entity) {
        if (doesEntityDoHoneyBlockSlideEffects(entity)) {
            if (world.random.nextInt(5) == 0) {
                entity.playSound(SoundEffects.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
            }

            if (!world.isClientSide && world.random.nextInt(5) == 0) {
                world.broadcastEntityEffect(entity, (byte)53);
            }
        }

    }

    public static void showSlideParticles(Entity entity) {
        showParticles(entity, 5);
    }

    public static void showJumpParticles(Entity entity) {
        showParticles(entity, 10);
    }

    private static void showParticles(Entity entity, int count) {
        if (entity.level.isClientSide) {
            IBlockData blockState = Blocks.HONEY_BLOCK.getBlockData();

            for(int i = 0; i < count; ++i) {
                entity.level.addParticle(new ParticleParamBlock(Particles.BLOCK, blockState), entity.locX(), entity.locY(), entity.locZ(), 0.0D, 0.0D, 0.0D);
            }

        }
    }
}
