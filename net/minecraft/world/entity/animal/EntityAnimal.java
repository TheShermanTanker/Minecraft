package net.minecraft.world.entity.animal;

import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockLightAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;

public abstract class EntityAnimal extends EntityAgeable {
    static final int PARENT_AGE_AFTER_BREEDING = 6000;
    public int inLove;
    @Nullable
    public UUID loveCause;

    protected EntityAnimal(EntityTypes<? extends EntityAnimal> type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
    }

    @Override
    protected void mobTick() {
        if (this.getAge() != 0) {
            this.inLove = 0;
        }

        super.mobTick();
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (this.getAge() != 0) {
            this.inLove = 0;
        }

        if (this.inLove > 0) {
            --this.inLove;
            if (this.inLove % 10 == 0) {
                double d = this.random.nextGaussian() * 0.02D;
                double e = this.random.nextGaussian() * 0.02D;
                double f = this.random.nextGaussian() * 0.02D;
                this.level.addParticle(Particles.HEART, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d, e, f);
            }
        }

    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else {
            this.inLove = 0;
            return super.damageEntity(source, amount);
        }
    }

    @Override
    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        return world.getType(pos.below()).is(Blocks.GRASS_BLOCK) ? 10.0F : world.getBrightness(pos) - 0.5F;
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("InLove", this.inLove);
        if (this.loveCause != null) {
            nbt.putUUID("LoveCause", this.loveCause);
        }

    }

    @Override
    public double getMyRidingOffset() {
        return 0.14D;
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.inLove = nbt.getInt("InLove");
        this.loveCause = nbt.hasUUID("LoveCause") ? nbt.getUUID("LoveCause") : null;
    }

    public static boolean checkAnimalSpawnRules(EntityTypes<? extends EntityAnimal> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return world.getType(pos.below()).is(TagsBlock.ANIMALS_SPAWNABLE_ON) && isBrightEnoughToSpawn(world, pos);
    }

    protected static boolean isBrightEnoughToSpawn(IBlockLightAccess world, BlockPosition pos) {
        return world.getLightLevel(pos, 0) > 8;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return false;
    }

    @Override
    protected int getExpValue(EntityHuman player) {
        return 1 + this.level.random.nextInt(3);
    }

    public boolean isBreedItem(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.isBreedItem(itemStack)) {
            int i = this.getAge();
            if (!this.level.isClientSide && i == 0 && this.canFallInLove()) {
                this.usePlayerItem(player, hand, itemStack);
                this.setInLove(player);
                this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
                return EnumInteractionResult.SUCCESS;
            }

            if (this.isBaby()) {
                this.usePlayerItem(player, hand, itemStack);
                this.setAge((int)((float)(-i / 20) * 0.1F), true);
                this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
                return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
            }

            if (this.level.isClientSide) {
                return EnumInteractionResult.CONSUME;
            }
        }

        return super.mobInteract(player, hand);
    }

    protected void usePlayerItem(EntityHuman player, EnumHand hand, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.subtract(1);
        }

    }

    public boolean canFallInLove() {
        return this.inLove <= 0;
    }

    public void setInLove(@Nullable EntityHuman player) {
        this.inLove = 600;
        if (player != null) {
            this.loveCause = player.getUniqueID();
        }

        this.level.broadcastEntityEffect(this, (byte)18);
    }

    public void setLoveTicks(int loveTicks) {
        this.inLove = loveTicks;
    }

    public int getInLoveTime() {
        return this.inLove;
    }

    @Nullable
    public EntityPlayer getBreedCause() {
        if (this.loveCause == null) {
            return null;
        } else {
            EntityHuman player = this.level.getPlayerByUUID(this.loveCause);
            return player instanceof EntityPlayer ? (EntityPlayer)player : null;
        }
    }

    public boolean isInLove() {
        return this.inLove > 0;
    }

    public void resetLove() {
        this.inLove = 0;
    }

    public boolean mate(EntityAnimal other) {
        if (other == this) {
            return false;
        } else if (other.getClass() != this.getClass()) {
            return false;
        } else {
            return this.isInLove() && other.isInLove();
        }
    }

    public void spawnChildFromBreeding(WorldServer world, EntityAnimal other) {
        EntityAgeable ageableMob = this.createChild(world, other);
        if (ageableMob != null) {
            EntityPlayer serverPlayer = this.getBreedCause();
            if (serverPlayer == null && other.getBreedCause() != null) {
                serverPlayer = other.getBreedCause();
            }

            if (serverPlayer != null) {
                serverPlayer.awardStat(StatisticList.ANIMALS_BRED);
                CriterionTriggers.BRED_ANIMALS.trigger(serverPlayer, this, other, ageableMob);
            }

            this.setAgeRaw(6000);
            other.setAgeRaw(6000);
            this.resetLove();
            other.resetLove();
            ageableMob.setBaby(true);
            ageableMob.setPositionRotation(this.locX(), this.locY(), this.locZ(), 0.0F, 0.0F);
            world.addAllEntities(ageableMob);
            world.broadcastEntityEffect(this, (byte)18);
            if (world.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                world.addEntity(new EntityExperienceOrb(world, this.locX(), this.locY(), this.locZ(), this.getRandom().nextInt(7) + 1));
            }

        }
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 18) {
            for(int i = 0; i < 7; ++i) {
                double d = this.random.nextGaussian() * 0.02D;
                double e = this.random.nextGaussian() * 0.02D;
                double f = this.random.nextGaussian() * 0.02D;
                this.level.addParticle(Particles.HEART, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d, e, f);
            }
        } else {
            super.handleEntityEvent(status);
        }

    }
}
