package net.minecraft.world.entity;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.EntitySquid;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;

public class EntityGlowSquid extends EntitySquid {
    private static final DataWatcherObject<Integer> DATA_DARK_TICKS_REMAINING = DataWatcher.defineId(EntityGlowSquid.class, DataWatcherRegistry.INT);

    public EntityGlowSquid(EntityTypes<? extends EntityGlowSquid> type, World world) {
        super(type, world);
    }

    @Override
    protected ParticleParam getInkParticle() {
        return Particles.GLOW_SQUID_INK;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_DARK_TICKS_REMAINING, 0);
    }

    @Override
    protected SoundEffect getSquirtSound() {
        return SoundEffects.GLOW_SQUID_SQUIRT;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.GLOW_SQUID_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.GLOW_SQUID_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.GLOW_SQUID_DEATH;
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("DarkTicksRemaining", this.getDarkTicksRemaining());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setDarkTicksRemaining(nbt.getInt("DarkTicksRemaining"));
    }

    @Override
    public void movementTick() {
        super.movementTick();
        int i = this.getDarkTicksRemaining();
        if (i > 0) {
            this.setDarkTicksRemaining(i - 1);
        }

        this.level.addParticle(Particles.GLOW, this.getRandomX(0.6D), this.getRandomY(), this.getRandomZ(0.6D), 0.0D, 0.0D, 0.0D);
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        boolean bl = super.damageEntity(source, amount);
        if (bl) {
            this.setDarkTicksRemaining(100);
        }

        return bl;
    }

    public void setDarkTicksRemaining(int ticks) {
        this.entityData.set(DATA_DARK_TICKS_REMAINING, ticks);
    }

    public int getDarkTicksRemaining() {
        return this.entityData.get(DATA_DARK_TICKS_REMAINING);
    }

    public static boolean checkGlowSquideSpawnRules(EntityTypes<? extends EntityLiving> type, WorldAccess world, EnumMobSpawn reason, BlockPosition pos, Random random) {
        return world.getType(pos).is(Blocks.WATER) && pos.getY() <= world.getSeaLevel() - 33;
    }
}
