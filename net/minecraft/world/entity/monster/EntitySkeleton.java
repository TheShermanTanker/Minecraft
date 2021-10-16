package net.minecraft.world.entity.monster;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class EntitySkeleton extends EntitySkeletonAbstract {
    public static final DataWatcherObject<Boolean> DATA_STRAY_CONVERSION_ID = DataWatcher.defineId(EntitySkeleton.class, DataWatcherRegistry.BOOLEAN);
    public static final String CONVERSION_TAG = "StrayConversionTime";
    private int inPowderSnowTime;
    public int conversionTime;

    public EntitySkeleton(EntityTypes<? extends EntitySkeleton> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.getDataWatcher().register(DATA_STRAY_CONVERSION_ID, false);
    }

    public boolean isStrayConverting() {
        return this.getDataWatcher().get(DATA_STRAY_CONVERSION_ID);
    }

    public void setFreezeConverting(boolean converting) {
        this.entityData.set(DATA_STRAY_CONVERSION_ID, converting);
    }

    @Override
    public boolean isShaking() {
        return this.isStrayConverting();
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide && this.isAlive() && !this.isNoAI()) {
            if (this.isStrayConverting()) {
                --this.conversionTime;
                if (this.conversionTime < 0) {
                    this.doFreezeConversion();
                }
            } else if (this.isInPowderSnow) {
                ++this.inPowderSnowTime;
                if (this.inPowderSnowTime >= 140) {
                    this.startStrayConversion(300);
                }
            } else {
                this.inPowderSnowTime = -1;
            }
        }

        super.tick();
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("StrayConversionTime", this.isStrayConverting() ? this.conversionTime : -1);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("StrayConversionTime", 99) && nbt.getInt("StrayConversionTime") > -1) {
            this.startStrayConversion(nbt.getInt("StrayConversionTime"));
        }

    }

    public void startStrayConversion(int time) {
        this.conversionTime = time;
        this.entityData.set(DATA_STRAY_CONVERSION_ID, true);
    }

    protected void doFreezeConversion() {
        this.convertTo(EntityTypes.STRAY, true);
        if (!this.isSilent()) {
            this.level.triggerEffect((EntityHuman)null, 1048, this.getChunkCoordinates(), 0);
        }

    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.SKELETON_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.SKELETON_DEATH;
    }

    @Override
    SoundEffect getStepSound() {
        return SoundEffects.SKELETON_STEP;
    }

    @Override
    protected void dropDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropDeathLoot(source, lootingMultiplier, allowDrops);
        Entity entity = source.getEntity();
        if (entity instanceof EntityCreeper) {
            EntityCreeper creeper = (EntityCreeper)entity;
            if (creeper.canCauseHeadDrop()) {
                creeper.setCausedHeadDrop();
                this.spawnAtLocation(Items.SKELETON_SKULL);
            }
        }

    }
}
