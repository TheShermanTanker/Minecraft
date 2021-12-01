package net.minecraft.world.entity.animal;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParamBlock;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.util.TimeRange;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.IEntityAngerable;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMoveTowardsTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalOfferFlower;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalStrollVillage;
import net.minecraft.world.entity.ai.goal.PathfinderGoalStrollVillageGolem;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalDefendVillage;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalUniversalAngerReset;
import net.minecraft.world.entity.monster.EntityCreeper;
import net.minecraft.world.entity.monster.IMonster;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.Vec3D;

public class EntityIronGolem extends EntityGolem implements IEntityAngerable {
    protected static final DataWatcherObject<Byte> DATA_FLAGS_ID = DataWatcher.defineId(EntityIronGolem.class, DataWatcherRegistry.BYTE);
    private static final int IRON_INGOT_HEAL_AMOUNT = 25;
    private int attackAnimationTick;
    private int offerFlowerTick;
    private static final IntProviderUniform PERSISTENT_ANGER_TIME = TimeRange.rangeOfSeconds(20, 39);
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;

    public EntityIronGolem(EntityTypes<? extends EntityIronGolem> type, World world) {
        super(type, world);
        this.maxUpStep = 1.0F;
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(1, new PathfinderGoalMeleeAttack(this, 1.0D, true));
        this.goalSelector.addGoal(2, new PathfinderGoalMoveTowardsTarget(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(2, new PathfinderGoalStrollVillage(this, 0.6D, false));
        this.goalSelector.addGoal(4, new PathfinderGoalStrollVillageGolem(this, 0.6D));
        this.goalSelector.addGoal(5, new PathfinderGoalOfferFlower(this));
        this.goalSelector.addGoal(7, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, new PathfinderGoalDefendVillage(this));
        this.targetSelector.addGoal(2, new PathfinderGoalHurtByTarget(this));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityInsentient.class, 5, false, false, (entity) -> {
            return entity instanceof IMonster && !(entity instanceof EntityCreeper);
        }));
        this.targetSelector.addGoal(4, new PathfinderGoalUniversalAngerReset<>(this, false));
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_FLAGS_ID, (byte)0);
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 100.0D).add(GenericAttributes.MOVEMENT_SPEED, 0.25D).add(GenericAttributes.KNOCKBACK_RESISTANCE, 1.0D).add(GenericAttributes.ATTACK_DAMAGE, 15.0D);
    }

    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    @Override
    protected void doPush(Entity entity) {
        if (entity instanceof IMonster && !(entity instanceof EntityCreeper) && this.getRandom().nextInt(20) == 0) {
            this.setGoalTarget((EntityLiving)entity);
        }

        super.doPush(entity);
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (this.attackAnimationTick > 0) {
            --this.attackAnimationTick;
        }

        if (this.offerFlowerTick > 0) {
            --this.offerFlowerTick;
        }

        if (this.getMot().horizontalDistanceSqr() > (double)2.5000003E-7F && this.random.nextInt(5) == 0) {
            int i = MathHelper.floor(this.locX());
            int j = MathHelper.floor(this.locY() - (double)0.2F);
            int k = MathHelper.floor(this.locZ());
            IBlockData blockState = this.level.getType(new BlockPosition(i, j, k));
            if (!blockState.isAir()) {
                this.level.addParticle(new ParticleParamBlock(Particles.BLOCK, blockState), this.locX() + ((double)this.random.nextFloat() - 0.5D) * (double)this.getWidth(), this.locY() + 0.1D, this.locZ() + ((double)this.random.nextFloat() - 0.5D) * (double)this.getWidth(), 4.0D * ((double)this.random.nextFloat() - 0.5D), 0.5D, ((double)this.random.nextFloat() - 0.5D) * 4.0D);
            }
        }

        if (!this.level.isClientSide) {
            this.updatePersistentAnger((WorldServer)this.level, true);
        }

    }

    @Override
    public boolean canAttackType(EntityTypes<?> type) {
        if (this.isPlayerCreated() && type == EntityTypes.PLAYER) {
            return false;
        } else {
            return type == EntityTypes.CREEPER ? false : super.canAttackType(type);
        }
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setBoolean("PlayerCreated", this.isPlayerCreated());
        this.addPersistentAngerSaveData(nbt);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setPlayerCreated(nbt.getBoolean("PlayerCreated"));
        this.readPersistentAngerSaveData(this.level, nbt);
    }

    @Override
    public void anger() {
        this.setAnger(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Override
    public void setAnger(int ticks) {
        this.remainingPersistentAngerTime = ticks;
    }

    @Override
    public int getAnger() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setAngerTarget(@Nullable UUID uuid) {
        this.persistentAngerTarget = uuid;
    }

    @Nullable
    @Override
    public UUID getAngerTarget() {
        return this.persistentAngerTarget;
    }

    private float getAttackDamage() {
        return (float)this.getAttributeValue(GenericAttributes.ATTACK_DAMAGE);
    }

    @Override
    public boolean attackEntity(Entity target) {
        this.attackAnimationTick = 10;
        this.level.broadcastEntityEffect(this, (byte)4);
        float f = this.getAttackDamage();
        float g = (int)f > 0 ? f / 2.0F + (float)this.random.nextInt((int)f) : f;
        boolean bl = target.damageEntity(DamageSource.mobAttack(this), g);
        if (bl) {
            target.setMot(target.getMot().add(0.0D, (double)0.4F, 0.0D));
            this.doEnchantDamageEffects(this, target);
        }

        this.playSound(SoundEffects.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
        return bl;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        EntityIronGolem.CrackLevel crackiness = this.getCrackiness();
        boolean bl = super.damageEntity(source, amount);
        if (bl && this.getCrackiness() != crackiness) {
            this.playSound(SoundEffects.IRON_GOLEM_DAMAGE, 1.0F, 1.0F);
        }

        return bl;
    }

    public EntityIronGolem.CrackLevel getCrackiness() {
        return EntityIronGolem.CrackLevel.byFraction(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 4) {
            this.attackAnimationTick = 10;
            this.playSound(SoundEffects.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
        } else if (status == 11) {
            this.offerFlowerTick = 400;
        } else if (status == 34) {
            this.offerFlowerTick = 0;
        } else {
            super.handleEntityEvent(status);
        }

    }

    public int getAttackAnimationTick() {
        return this.attackAnimationTick;
    }

    public void offerFlower(boolean lookingAtVillager) {
        if (lookingAtVillager) {
            this.offerFlowerTick = 400;
            this.level.broadcastEntityEffect(this, (byte)11);
        } else {
            this.offerFlowerTick = 0;
            this.level.broadcastEntityEffect(this, (byte)34);
        }

    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.IRON_GOLEM_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.IRON_GOLEM_DEATH;
    }

    @Override
    protected EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.is(Items.IRON_INGOT)) {
            return EnumInteractionResult.PASS;
        } else {
            float f = this.getHealth();
            this.heal(25.0F);
            if (this.getHealth() == f) {
                return EnumInteractionResult.PASS;
            } else {
                float g = 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F;
                this.playSound(SoundEffects.IRON_GOLEM_REPAIR, 1.0F, g);
                this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
                if (!player.getAbilities().instabuild) {
                    itemStack.subtract(1);
                }

                return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
            }
        }
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }

    public int getOfferFlowerTick() {
        return this.offerFlowerTick;
    }

    public boolean isPlayerCreated() {
        return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
    }

    public void setPlayerCreated(boolean playerCreated) {
        byte b = this.entityData.get(DATA_FLAGS_ID);
        if (playerCreated) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(b | 1));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(b & -2));
        }

    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
    }

    @Override
    public boolean checkSpawnObstruction(IWorldReader world) {
        BlockPosition blockPos = this.getChunkCoordinates();
        BlockPosition blockPos2 = blockPos.below();
        IBlockData blockState = world.getType(blockPos2);
        if (!blockState.entityCanStandOn(world, blockPos2, this)) {
            return false;
        } else {
            for(int i = 1; i < 3; ++i) {
                BlockPosition blockPos3 = blockPos.above(i);
                IBlockData blockState2 = world.getType(blockPos3);
                if (!NaturalSpawner.isValidEmptySpawnBlock(world, blockPos3, blockState2, blockState2.getFluid(), EntityTypes.IRON_GOLEM)) {
                    return false;
                }
            }

            return NaturalSpawner.isValidEmptySpawnBlock(world, blockPos, world.getType(blockPos), FluidTypes.EMPTY.defaultFluidState(), EntityTypes.IRON_GOLEM) && world.isUnobstructed(this);
        }
    }

    @Override
    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double)(0.875F * this.getHeadHeight()), (double)(this.getWidth() * 0.4F));
    }

    public static enum CrackLevel {
        NONE(1.0F),
        LOW(0.75F),
        MEDIUM(0.5F),
        HIGH(0.25F);

        private static final List<EntityIronGolem.CrackLevel> BY_DAMAGE = Stream.of(values()).sorted(Comparator.comparingDouble((crackiness) -> {
            return (double)crackiness.fraction;
        })).collect(ImmutableList.toImmutableList());
        private final float fraction;

        private CrackLevel(float maxHealthFraction) {
            this.fraction = maxHealthFraction;
        }

        public static EntityIronGolem.CrackLevel byFraction(float healthFraction) {
            for(EntityIronGolem.CrackLevel crackiness : BY_DAMAGE) {
                if (healthFraction < crackiness.fraction) {
                    return crackiness;
                }
            }

            return NONE;
        }
    }
}
