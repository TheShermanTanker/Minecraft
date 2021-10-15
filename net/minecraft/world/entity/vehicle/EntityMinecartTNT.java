package net.minecraft.world.entity.vehicle;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;

public class EntityMinecartTNT extends EntityMinecartAbstract {
    private static final byte EVENT_PRIME = 10;
    private int fuse = -1;

    public EntityMinecartTNT(EntityTypes<? extends EntityMinecartTNT> type, World world) {
        super(type, world);
    }

    public EntityMinecartTNT(World world, double x, double y, double z) {
        super(EntityTypes.TNT_MINECART, world, x, y, z);
    }

    @Override
    public EntityMinecartAbstract.EnumMinecartType getMinecartType() {
        return EntityMinecartAbstract.EnumMinecartType.TNT;
    }

    @Override
    public IBlockData getDefaultDisplayBlockState() {
        return Blocks.TNT.getBlockData();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.fuse > 0) {
            --this.fuse;
            this.level.addParticle(Particles.SMOKE, this.locX(), this.locY() + 0.5D, this.locZ(), 0.0D, 0.0D, 0.0D);
        } else if (this.fuse == 0) {
            this.explode(this.getMot().horizontalDistanceSqr());
        }

        if (this.horizontalCollision) {
            double d = this.getMot().horizontalDistanceSqr();
            if (d >= (double)0.01F) {
                this.explode(d);
            }
        }

    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        Entity entity = source.getDirectEntity();
        if (entity instanceof EntityArrow) {
            EntityArrow abstractArrow = (EntityArrow)entity;
            if (abstractArrow.isBurning()) {
                this.explode(abstractArrow.getMot().lengthSqr());
            }
        }

        return super.damageEntity(source, amount);
    }

    @Override
    public void destroy(DamageSource damageSource) {
        double d = this.getMot().horizontalDistanceSqr();
        if (!damageSource.isFire() && !damageSource.isExplosion() && !(d >= (double)0.01F)) {
            super.destroy(damageSource);
            if (!damageSource.isExplosion() && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                this.spawnAtLocation(Blocks.TNT);
            }

        } else {
            if (this.fuse < 0) {
                this.primeFuse();
                this.fuse = this.random.nextInt(20) + this.random.nextInt(20);
            }

        }
    }

    protected void explode(double velocity) {
        if (!this.level.isClientSide) {
            double d = Math.sqrt(velocity);
            if (d > 5.0D) {
                d = 5.0D;
            }

            this.level.explode(this, this.locX(), this.locY(), this.locZ(), (float)(4.0D + this.random.nextDouble() * 1.5D * d), Explosion.Effect.BREAK);
            this.die();
        }

    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (fallDistance >= 3.0F) {
            float f = fallDistance / 10.0F;
            this.explode((double)(f * f));
        }

        return super.causeFallDamage(fallDistance, damageMultiplier, damageSource);
    }

    @Override
    public void activateMinecart(int x, int y, int z, boolean powered) {
        if (powered && this.fuse < 0) {
            this.primeFuse();
        }

    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 10) {
            this.primeFuse();
        } else {
            super.handleEntityEvent(status);
        }

    }

    public void primeFuse() {
        this.fuse = 80;
        if (!this.level.isClientSide) {
            this.level.broadcastEntityEffect(this, (byte)10);
            if (!this.isSilent()) {
                this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }

    }

    public int getFuse() {
        return this.fuse;
    }

    public boolean isPrimed() {
        return this.fuse > -1;
    }

    @Override
    public float getBlockExplosionResistance(Explosion explosion, IBlockAccess world, BlockPosition pos, IBlockData blockState, Fluid fluidState, float max) {
        return !this.isPrimed() || !blockState.is(TagsBlock.RAILS) && !world.getType(pos.above()).is(TagsBlock.RAILS) ? super.getBlockExplosionResistance(explosion, world, pos, blockState, fluidState, max) : 0.0F;
    }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, IBlockAccess world, BlockPosition pos, IBlockData state, float explosionPower) {
        return !this.isPrimed() || !state.is(TagsBlock.RAILS) && !world.getType(pos.above()).is(TagsBlock.RAILS) ? super.shouldBlockExplode(explosion, world, pos, state, explosionPower) : false;
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("TNTFuse", 99)) {
            this.fuse = nbt.getInt("TNTFuse");
        }

    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("TNTFuse", this.fuse);
    }
}
