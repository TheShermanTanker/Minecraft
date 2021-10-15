package net.minecraft.world.entity.monster;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.phys.Vec3D;

public class EntityMagmaCube extends EntitySlime {
    public EntityMagmaCube(EntityTypes<? extends EntityMagmaCube> type, World world) {
        super(type, world);
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MOVEMENT_SPEED, (double)0.2F);
    }

    public static boolean checkMagmaCubeSpawnRules(EntityTypes<EntityMagmaCube> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return world.getDifficulty() != EnumDifficulty.PEACEFUL;
    }

    @Override
    public boolean checkSpawnObstruction(IWorldReader world) {
        return world.isUnobstructed(this) && !world.containsLiquid(this.getBoundingBox());
    }

    @Override
    public void setSize(int size, boolean heal) {
        super.setSize(size, heal);
        this.getAttributeInstance(GenericAttributes.ARMOR).setValue((double)(size * 3));
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    protected ParticleParam getParticleType() {
        return Particles.FLAME;
    }

    @Override
    protected MinecraftKey getDefaultLootTable() {
        return this.isTiny() ? LootTables.EMPTY : this.getEntityType().getDefaultLootTable();
    }

    @Override
    public boolean isBurning() {
        return false;
    }

    @Override
    protected int getJumpDelay() {
        return super.getJumpDelay() * 4;
    }

    @Override
    protected void decreaseSquish() {
        this.targetSquish *= 0.9F;
    }

    @Override
    protected void jump() {
        Vec3D vec3 = this.getMot();
        this.setMot(vec3.x, (double)(this.getJumpPower() + (float)this.getSize() * 0.1F), vec3.z);
        this.hasImpulse = true;
    }

    @Override
    protected void jumpInLiquid(Tag<FluidType> fluid) {
        if (fluid == TagsFluid.LAVA) {
            Vec3D vec3 = this.getMot();
            this.setMot(vec3.x, (double)(0.22F + (float)this.getSize() * 0.05F), vec3.z);
            this.hasImpulse = true;
        } else {
            super.jumpInLiquid(fluid);
        }

    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected boolean isDealsDamage() {
        return this.doAITick();
    }

    @Override
    protected float getAttackDamage() {
        return super.getAttackDamage() + 2.0F;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return this.isTiny() ? SoundEffects.MAGMA_CUBE_HURT_SMALL : SoundEffects.MAGMA_CUBE_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return this.isTiny() ? SoundEffects.MAGMA_CUBE_DEATH_SMALL : SoundEffects.MAGMA_CUBE_DEATH;
    }

    @Override
    protected SoundEffect getSoundSquish() {
        return this.isTiny() ? SoundEffects.MAGMA_CUBE_SQUISH_SMALL : SoundEffects.MAGMA_CUBE_SQUISH;
    }

    @Override
    protected SoundEffect getSoundJump() {
        return SoundEffects.MAGMA_CUBE_JUMP;
    }
}
