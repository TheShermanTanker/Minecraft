package net.minecraft.world.entity.monster;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityTippedArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;

public class EntitySkeletonStray extends EntitySkeletonAbstract {
    public EntitySkeletonStray(EntityTypes<? extends EntitySkeletonStray> type, World world) {
        super(type, world);
    }

    public static boolean checkStraySpawnRules(EntityTypes<EntitySkeletonStray> type, WorldAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        BlockPosition blockPos = pos;

        do {
            blockPos = blockPos.above();
        } while(world.getType(blockPos).is(Blocks.POWDER_SNOW));

        return checkMonsterSpawnRules(type, world, spawnReason, pos, random) && (spawnReason == EnumMobSpawn.SPAWNER || world.canSeeSky(blockPos.below()));
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.STRAY_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.STRAY_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.STRAY_DEATH;
    }

    @Override
    SoundEffect getStepSound() {
        return SoundEffects.STRAY_STEP;
    }

    @Override
    protected EntityArrow getArrow(ItemStack arrow, float damageModifier) {
        EntityArrow abstractArrow = super.getArrow(arrow, damageModifier);
        if (abstractArrow instanceof EntityTippedArrow) {
            ((EntityTippedArrow)abstractArrow).addEffect(new MobEffect(MobEffects.MOVEMENT_SLOWDOWN, 600));
        }

        return abstractArrow;
    }
}
