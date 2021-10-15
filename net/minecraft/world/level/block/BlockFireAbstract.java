package net.minecraft.world.level.block;

import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.portal.BlockPortalShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public abstract class BlockFireAbstract extends Block {
    private static final int SECONDS_ON_FIRE = 8;
    private final float fireDamage;
    protected static final float AABB_OFFSET = 1.0F;
    protected static final VoxelShape DOWN_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public BlockFireAbstract(BlockBase.Info settings, float damage) {
        super(settings);
        this.fireDamage = damage;
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return getState(ctx.getWorld(), ctx.getClickPosition());
    }

    public static IBlockData getState(IBlockAccess world, BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        IBlockData blockState = world.getType(blockPos);
        return BlockSoulFire.canSurviveOnBlock(blockState) ? Blocks.SOUL_FIRE.getBlockData() : ((BlockFire)Blocks.FIRE).getPlacedState(world, pos);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return DOWN_AABB;
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (random.nextInt(24) == 0) {
            world.playLocalSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEffects.FIRE_AMBIENT, SoundCategory.BLOCKS, 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
        }

        BlockPosition blockPos = pos.below();
        IBlockData blockState = world.getType(blockPos);
        if (!this.canBurn(blockState) && !blockState.isFaceSturdy(world, blockPos, EnumDirection.UP)) {
            if (this.canBurn(world.getType(pos.west()))) {
                for(int j = 0; j < 2; ++j) {
                    double g = (double)pos.getX() + random.nextDouble() * (double)0.1F;
                    double h = (double)pos.getY() + random.nextDouble();
                    double k = (double)pos.getZ() + random.nextDouble();
                    world.addParticle(Particles.LARGE_SMOKE, g, h, k, 0.0D, 0.0D, 0.0D);
                }
            }

            if (this.canBurn(world.getType(pos.east()))) {
                for(int l = 0; l < 2; ++l) {
                    double m = (double)(pos.getX() + 1) - random.nextDouble() * (double)0.1F;
                    double n = (double)pos.getY() + random.nextDouble();
                    double o = (double)pos.getZ() + random.nextDouble();
                    world.addParticle(Particles.LARGE_SMOKE, m, n, o, 0.0D, 0.0D, 0.0D);
                }
            }

            if (this.canBurn(world.getType(pos.north()))) {
                for(int p = 0; p < 2; ++p) {
                    double q = (double)pos.getX() + random.nextDouble();
                    double r = (double)pos.getY() + random.nextDouble();
                    double s = (double)pos.getZ() + random.nextDouble() * (double)0.1F;
                    world.addParticle(Particles.LARGE_SMOKE, q, r, s, 0.0D, 0.0D, 0.0D);
                }
            }

            if (this.canBurn(world.getType(pos.south()))) {
                for(int t = 0; t < 2; ++t) {
                    double u = (double)pos.getX() + random.nextDouble();
                    double v = (double)pos.getY() + random.nextDouble();
                    double w = (double)(pos.getZ() + 1) - random.nextDouble() * (double)0.1F;
                    world.addParticle(Particles.LARGE_SMOKE, u, v, w, 0.0D, 0.0D, 0.0D);
                }
            }

            if (this.canBurn(world.getType(pos.above()))) {
                for(int x = 0; x < 2; ++x) {
                    double y = (double)pos.getX() + random.nextDouble();
                    double z = (double)(pos.getY() + 1) - random.nextDouble() * (double)0.1F;
                    double aa = (double)pos.getZ() + random.nextDouble();
                    world.addParticle(Particles.LARGE_SMOKE, y, z, aa, 0.0D, 0.0D, 0.0D);
                }
            }
        } else {
            for(int i = 0; i < 3; ++i) {
                double d = (double)pos.getX() + random.nextDouble();
                double e = (double)pos.getY() + random.nextDouble() * 0.5D + 0.5D;
                double f = (double)pos.getZ() + random.nextDouble();
                world.addParticle(Particles.LARGE_SMOKE, d, e, f, 0.0D, 0.0D, 0.0D);
            }
        }

    }

    protected abstract boolean canBurn(IBlockData state);

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (!entity.isFireProof()) {
            entity.setFireTicks(entity.getFireTicks() + 1);
            if (entity.getFireTicks() == 0) {
                entity.setOnFire(8);
            }

            entity.damageEntity(DamageSource.IN_FIRE, this.fireDamage);
        }

        super.entityInside(state, world, pos, entity);
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            if (inPortalDimension(world)) {
                Optional<BlockPortalShape> optional = BlockPortalShape.findEmptyPortalShape(world, pos, EnumDirection.EnumAxis.X);
                if (optional.isPresent()) {
                    optional.get().createPortal();
                    return;
                }
            }

            if (!state.canPlace(world, pos)) {
                world.removeBlock(pos, false);
            }

        }
    }

    private static boolean inPortalDimension(World world) {
        return world.getDimensionKey() == World.OVERWORLD || world.getDimensionKey() == World.NETHER;
    }

    @Override
    protected void spawnDestroyParticles(World world, EntityHuman player, BlockPosition pos, IBlockData state) {
    }

    @Override
    public void playerWillDestroy(World world, BlockPosition pos, IBlockData state, EntityHuman player) {
        if (!world.isClientSide()) {
            world.levelEvent((EntityHuman)null, 1009, pos, 0);
        }

        super.playerWillDestroy(world, pos, state, player);
    }

    public static boolean canBePlacedAt(World world, BlockPosition pos, EnumDirection direction) {
        IBlockData blockState = world.getType(pos);
        if (!blockState.isAir()) {
            return false;
        } else {
            return getState(world, pos).canPlace(world, pos) || isPortal(world, pos, direction);
        }
    }

    private static boolean isPortal(World world, BlockPosition pos, EnumDirection direction) {
        if (!inPortalDimension(world)) {
            return false;
        } else {
            BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();
            boolean bl = false;

            for(EnumDirection direction2 : EnumDirection.values()) {
                if (world.getType(mutableBlockPos.set(pos).move(direction2)).is(Blocks.OBSIDIAN)) {
                    bl = true;
                    break;
                }
            }

            if (!bl) {
                return false;
            } else {
                EnumDirection.EnumAxis axis = direction.getAxis().isHorizontal() ? direction.getCounterClockWise().getAxis() : EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomAxis(world.random);
                return BlockPortalShape.findEmptyPortalShape(world, pos, axis).isPresent();
            }
        }
    }
}
