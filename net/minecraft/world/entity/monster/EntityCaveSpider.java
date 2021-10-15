package net.minecraft.world.entity.monster;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;

public class EntityCaveSpider extends EntitySpider {
    public EntityCaveSpider(EntityTypes<? extends EntityCaveSpider> type, World world) {
        super(type, world);
    }

    public static AttributeProvider.Builder createCaveSpider() {
        return EntitySpider.createAttributes().add(GenericAttributes.MAX_HEALTH, 12.0D);
    }

    @Override
    public boolean attackEntity(Entity target) {
        if (super.attackEntity(target)) {
            if (target instanceof EntityLiving) {
                int i = 0;
                if (this.level.getDifficulty() == EnumDifficulty.NORMAL) {
                    i = 7;
                } else if (this.level.getDifficulty() == EnumDifficulty.HARD) {
                    i = 15;
                }

                if (i > 0) {
                    ((EntityLiving)target).addEffect(new MobEffect(MobEffects.POISON, i * 20, 0), this);
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        return entityData;
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 0.45F;
    }
}
