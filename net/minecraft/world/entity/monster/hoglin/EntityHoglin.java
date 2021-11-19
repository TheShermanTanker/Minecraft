package net.minecraft.world.entity.monster.hoglin;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.monster.EntityZoglin;
import net.minecraft.world.entity.monster.IMonster;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class EntityHoglin extends EntityAnimal implements IMonster, IOglin {
    private static final DataWatcherObject<Boolean> DATA_IMMUNE_TO_ZOMBIFICATION = DataWatcher.defineId(EntityHoglin.class, DataWatcherRegistry.BOOLEAN);
    private static final float PROBABILITY_OF_SPAWNING_AS_BABY = 0.2F;
    private static final int MAX_HEALTH = 40;
    private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.3F;
    private static final int ATTACK_KNOCKBACK = 1;
    private static final float KNOCKBACK_RESISTANCE = 0.6F;
    private static final int ATTACK_DAMAGE = 6;
    private static final float BABY_ATTACK_DAMAGE = 0.5F;
    private static final int CONVERSION_TIME = 300;
    private int attackAnimationRemainingTicks;
    public int timeInOverworld;
    public boolean cannotBeHunted;
    protected static final ImmutableList<? extends SensorType<? extends Sensor<? super EntityHoglin>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ADULT, SensorType.HOGLIN_SPECIFIC_SENSOR);
    protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.BREED_TARGET, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.LOOK_TARGET, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, MemoryModuleType.AVOID_TARGET, MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS, MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.NEAREST_REPELLENT, MemoryModuleType.PACIFIED);

    public EntityHoglin(EntityTypes<? extends EntityHoglin> type, World world) {
        super(type, world);
        this.xpReward = 5;
    }

    @Override
    public boolean canBeLeashed(EntityHuman player) {
        return !this.isLeashed();
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MAX_HEALTH, 40.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.3F).add(GenericAttributes.KNOCKBACK_RESISTANCE, (double)0.6F).add(GenericAttributes.ATTACK_KNOCKBACK, 1.0D).add(GenericAttributes.ATTACK_DAMAGE, 6.0D);
    }

    @Override
    public boolean attackEntity(Entity target) {
        if (!(target instanceof EntityLiving)) {
            return false;
        } else {
            this.attackAnimationRemainingTicks = 10;
            this.level.broadcastEntityEffect(this, (byte)4);
            this.playSound(SoundEffects.HOGLIN_ATTACK, 1.0F, this.getVoicePitch());
            HoglinAI.onHitTarget(this, (EntityLiving)target);
            return IOglin.hurtAndThrowTarget(this, (EntityLiving)target);
        }
    }

    @Override
    protected void blockedByShield(EntityLiving target) {
        if (this.isAdult()) {
            IOglin.throwTarget(this, target);
        }

    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        boolean bl = super.damageEntity(source, amount);
        if (this.level.isClientSide) {
            return false;
        } else {
            if (bl && source.getEntity() instanceof EntityLiving) {
                HoglinAI.wasHurtBy(this, (EntityLiving)source.getEntity());
            }

            return bl;
        }
    }

    @Override
    protected BehaviorController.Provider<EntityHoglin> brainProvider() {
        return BehaviorController.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected BehaviorController<?> makeBrain(Dynamic<?> dynamic) {
        return HoglinAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public BehaviorController<EntityHoglin> getBehaviorController() {
        return super.getBehaviorController();
    }

    @Override
    protected void mobTick() {
        this.level.getMethodProfiler().enter("hoglinBrain");
        this.getBehaviorController().tick((WorldServer)this.level, this);
        this.level.getMethodProfiler().exit();
        HoglinAI.updateActivity(this);
        if (this.isConverting()) {
            ++this.timeInOverworld;
            if (this.timeInOverworld > 300) {
                this.playSound(SoundEffects.HOGLIN_CONVERTED_TO_ZOMBIFIED);
                this.finishConversion((WorldServer)this.level);
            }
        } else {
            this.timeInOverworld = 0;
        }

    }

    @Override
    public void movementTick() {
        if (this.attackAnimationRemainingTicks > 0) {
            --this.attackAnimationRemainingTicks;
        }

        super.movementTick();
    }

    @Override
    protected void ageBoundaryReached() {
        if (this.isBaby()) {
            this.xpReward = 3;
            this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(0.5D);
        } else {
            this.xpReward = 5;
            this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(6.0D);
        }

    }

    public static boolean checkHoglinSpawnRules(EntityTypes<EntityHoglin> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return !world.getType(pos.below()).is(Blocks.NETHER_WART_BLOCK);
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        if (world.getRandom().nextFloat() < 0.2F) {
            this.setBaby(true);
        }

        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return !this.isPersistent();
    }

    @Override
    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        if (HoglinAI.isPosNearNearestRepellent(this, pos)) {
            return -1.0F;
        } else {
            return world.getType(pos.below()).is(Blocks.CRIMSON_NYLIUM) ? 10.0F : 0.0F;
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return (double)this.getHeight() - (this.isBaby() ? 0.2D : 0.15D);
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        EnumInteractionResult interactionResult = super.mobInteract(player, hand);
        if (interactionResult.consumesAction()) {
            this.setPersistent();
        }

        return interactionResult;
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 4) {
            this.attackAnimationRemainingTicks = 10;
            this.playSound(SoundEffects.HOGLIN_ATTACK, 1.0F, this.getVoicePitch());
        } else {
            super.handleEntityEvent(status);
        }

    }

    @Override
    public int getAttackAnimationRemainingTicks() {
        return this.attackAnimationRemainingTicks;
    }

    @Override
    protected boolean isDropExperience() {
        return true;
    }

    @Override
    protected int getExpValue(EntityHuman player) {
        return this.xpReward;
    }

    private void finishConversion(WorldServer word) {
        EntityZoglin zoglin = this.convertTo(EntityTypes.ZOGLIN, true);
        if (zoglin != null) {
            zoglin.addEffect(new MobEffect(MobEffectList.CONFUSION, 200, 0));
        }

    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return stack.is(Items.CRIMSON_FUNGUS);
    }

    public boolean isAdult() {
        return !this.isBaby();
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_IMMUNE_TO_ZOMBIFICATION, false);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (this.isImmuneToZombification()) {
            nbt.setBoolean("IsImmuneToZombification", true);
        }

        nbt.setInt("TimeInOverworld", this.timeInOverworld);
        if (this.cannotBeHunted) {
            nbt.setBoolean("CannotBeHunted", true);
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setImmuneToZombification(nbt.getBoolean("IsImmuneToZombification"));
        this.timeInOverworld = nbt.getInt("TimeInOverworld");
        this.setCannotBeHunted(nbt.getBoolean("CannotBeHunted"));
    }

    public void setImmuneToZombification(boolean immuneToZombification) {
        this.getDataWatcher().set(DATA_IMMUNE_TO_ZOMBIFICATION, immuneToZombification);
    }

    public boolean isImmuneToZombification() {
        return this.getDataWatcher().get(DATA_IMMUNE_TO_ZOMBIFICATION);
    }

    public boolean isConverting() {
        return !this.level.getDimensionManager().isPiglinSafe() && !this.isImmuneToZombification() && !this.isNoAI();
    }

    private void setCannotBeHunted(boolean cannotBeHunted) {
        this.cannotBeHunted = cannotBeHunted;
    }

    public boolean canBeHunted() {
        return this.isAdult() && !this.cannotBeHunted;
    }

    @Nullable
    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        EntityHoglin hoglin = EntityTypes.HOGLIN.create(world);
        if (hoglin != null) {
            hoglin.setPersistent();
        }

        return hoglin;
    }

    @Override
    public boolean canFallInLove() {
        return !HoglinAI.isPacified(this) && super.canFallInLove();
    }

    @Override
    public EnumSoundCategory getSoundCategory() {
        return EnumSoundCategory.HOSTILE;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return this.level.isClientSide ? null : HoglinAI.getSoundForCurrentActivity(this).orElse((SoundEffect)null);
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.HOGLIN_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.HOGLIN_DEATH;
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
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.HOGLIN_STEP, 0.15F, 1.0F);
    }

    protected void playSound(SoundEffect sound) {
        this.playSound(sound, this.getSoundVolume(), this.getVoicePitch());
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        PacketDebug.sendEntityBrain(this);
    }
}
