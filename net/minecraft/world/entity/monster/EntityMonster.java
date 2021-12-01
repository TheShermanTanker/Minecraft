package net.minecraft.world.entity.monster;

import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.LivingEntity$Fallsounds;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemProjectileWeapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;

public abstract class EntityMonster extends EntityCreature implements IMonster {
    protected EntityMonster(EntityTypes<? extends EntityMonster> type, World world) {
        super(type, world);
        this.xpReward = 5;
    }

    @Override
    public EnumSoundCategory getSoundCategory() {
        return EnumSoundCategory.HOSTILE;
    }

    @Override
    public void movementTick() {
        this.updateSwingTime();
        this.updateNoActionTime();
        super.movementTick();
    }

    protected void updateNoActionTime() {
        float f = this.getBrightness();
        if (f > 0.5F) {
            this.noActionTime += 2;
        }

    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    protected SoundEffect getSoundSwim() {
        return SoundEffects.HOSTILE_SWIM;
    }

    @Override
    protected SoundEffect getSoundSplash() {
        return SoundEffects.HOSTILE_SPLASH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.HOSTILE_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.HOSTILE_DEATH;
    }

    @Override
    public LivingEntity$Fallsounds getFallSounds() {
        return new LivingEntity$Fallsounds(SoundEffects.HOSTILE_SMALL_FALL, SoundEffects.HOSTILE_BIG_FALL);
    }

    @Override
    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        return 0.5F - world.getBrightness(pos);
    }

    public static boolean isDarkEnoughToSpawn(WorldAccess world, BlockPosition pos, Random random) {
        if (world.getBrightness(EnumSkyBlock.SKY, pos) > random.nextInt(32)) {
            return false;
        } else if (world.getBrightness(EnumSkyBlock.BLOCK, pos) > 0) {
            return false;
        } else {
            int i = world.getLevel().isThundering() ? world.getMaxLocalRawBrightness(pos, 10) : world.getLightLevel(pos);
            return i <= random.nextInt(8);
        }
    }

    public static boolean checkMonsterSpawnRules(EntityTypes<? extends EntityMonster> type, WorldAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return world.getDifficulty() != EnumDifficulty.PEACEFUL && isDarkEnoughToSpawn(world, pos, random) && checkMobSpawnRules(type, world, spawnReason, pos, random);
    }

    public static boolean checkAnyLightMonsterSpawnRules(EntityTypes<? extends EntityMonster> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return world.getDifficulty() != EnumDifficulty.PEACEFUL && checkMobSpawnRules(type, world, spawnReason, pos, random);
    }

    public static AttributeProvider.Builder createMonsterAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.ATTACK_DAMAGE);
    }

    @Override
    protected boolean isDropExperience() {
        return true;
    }

    @Override
    protected boolean shouldDropLoot() {
        return true;
    }

    public boolean isPreventingPlayerRest(EntityHuman player) {
        return true;
    }

    @Override
    public ItemStack getProjectile(ItemStack stack) {
        if (stack.getItem() instanceof ItemProjectileWeapon) {
            Predicate<ItemStack> predicate = ((ItemProjectileWeapon)stack.getItem()).getSupportedHeldProjectiles();
            ItemStack itemStack = ItemProjectileWeapon.getHeldProjectile(this, predicate);
            return itemStack.isEmpty() ? new ItemStack(Items.ARROW) : itemStack;
        } else {
            return ItemStack.EMPTY;
        }
    }
}
