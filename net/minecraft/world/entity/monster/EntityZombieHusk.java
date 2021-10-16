package net.minecraft.world.entity.monster;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;

public class EntityZombieHusk extends EntityZombie {
    public EntityZombieHusk(EntityTypes<? extends EntityZombieHusk> type, World world) {
        super(type, world);
    }

    public static boolean checkHuskSpawnRules(EntityTypes<EntityZombieHusk> type, WorldAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return checkMonsterSpawnRules(type, world, spawnReason, pos, random) && (spawnReason == EnumMobSpawn.SPAWNER || world.canSeeSky(pos));
    }

    @Override
    public boolean isSunSensitive() {
        return false;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.HUSK_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.HUSK_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.HUSK_DEATH;
    }

    @Override
    protected SoundEffect getSoundStep() {
        return SoundEffects.HUSK_STEP;
    }

    @Override
    public boolean attackEntity(Entity target) {
        boolean bl = super.attackEntity(target);
        if (bl && this.getItemInMainHand().isEmpty() && target instanceof EntityLiving) {
            float f = this.level.getDamageScaler(this.getChunkCoordinates()).getEffectiveDifficulty();
            ((EntityLiving)target).addEffect(new MobEffect(MobEffects.HUNGER, 140 * (int)f), this);
        }

        return bl;
    }

    @Override
    protected boolean convertsInWater() {
        return true;
    }

    @Override
    protected void doUnderWaterConversion() {
        this.convertToZombieType(EntityTypes.ZOMBIE);
        if (!this.isSilent()) {
            this.level.triggerEffect((EntityHuman)null, 1041, this.getChunkCoordinates(), 0);
        }

    }

    @Override
    protected ItemStack getSkull() {
        return ItemStack.EMPTY;
    }
}
