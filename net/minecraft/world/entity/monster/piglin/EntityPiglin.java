package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.monster.ICrossbow;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.ItemProjectileWeapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class EntityPiglin extends EntityPiglinAbstract implements ICrossbow, InventoryCarrier {
    private static final DataWatcherObject<Boolean> DATA_BABY_ID = DataWatcher.defineId(EntityPiglin.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> DATA_IS_CHARGING_CROSSBOW = DataWatcher.defineId(EntityPiglin.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> DATA_IS_DANCING = DataWatcher.defineId(EntityPiglin.class, DataWatcherRegistry.BOOLEAN);
    private static final UUID SPEED_MODIFIER_BABY_UUID = UUID.fromString("766bfa64-11f3-11ea-8d71-362b9e155667");
    private static final AttributeModifier SPEED_MODIFIER_BABY = new AttributeModifier(SPEED_MODIFIER_BABY_UUID, "Baby speed boost", (double)0.2F, AttributeModifier.Operation.MULTIPLY_BASE);
    private static final int MAX_HEALTH = 16;
    private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.35F;
    private static final int ATTACK_DAMAGE = 5;
    private static final float CROSSBOW_POWER = 1.6F;
    private static final float CHANCE_OF_WEARING_EACH_ARMOUR_ITEM = 0.1F;
    private static final int MAX_PASSENGERS_ON_ONE_HOGLIN = 3;
    private static final float PROBABILITY_OF_SPAWNING_AS_BABY = 0.2F;
    private static final float BABY_EYE_HEIGHT_ADJUSTMENT = 0.81F;
    private static final double PROBABILITY_OF_SPAWNING_WITH_CROSSBOW_INSTEAD_OF_SWORD = 0.5D;
    public final InventorySubcontainer inventory = new InventorySubcontainer(8);
    public boolean cannotHunt;
    protected static final ImmutableList<SensorType<? extends Sensor<? super EntityPiglin>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.HURT_BY, SensorType.PIGLIN_SPECIFIC_SENSOR);
    protected static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.LOOK_TARGET, MemoryModuleType.DOORS_TO_CLOSE, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, MemoryModuleType.NEARBY_ADULT_PIGLINS, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryModuleType.INTERACTION_TARGET, MemoryModuleType.PATH, MemoryModuleType.ANGRY_AT, MemoryModuleType.UNIVERSAL_ANGER, MemoryModuleType.AVOID_TARGET, MemoryModuleType.ADMIRING_ITEM, MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM, MemoryModuleType.ADMIRING_DISABLED, MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM, MemoryModuleType.CELEBRATE_LOCATION, MemoryModuleType.DANCING, MemoryModuleType.HUNTED_RECENTLY, MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, MemoryModuleType.RIDE_TARGET, MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, MemoryModuleType.ATE_RECENTLY, MemoryModuleType.NEAREST_REPELLENT);

    public EntityPiglin(EntityTypes<? extends EntityPiglinAbstract> type, World world) {
        super(type, world);
        this.xpReward = 5;
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (this.isBaby()) {
            nbt.setBoolean("IsBaby", true);
        }

        if (this.cannotHunt) {
            nbt.setBoolean("CannotHunt", true);
        }

        nbt.set("Inventory", this.inventory.createTag());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setBaby(nbt.getBoolean("IsBaby"));
        this.setCannotHunt(nbt.getBoolean("CannotHunt"));
        this.inventory.fromTag(nbt.getList("Inventory", 10));
    }

    @VisibleForDebug
    @Override
    public IInventory getInventory() {
        return this.inventory;
    }

    @Override
    protected void dropDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropDeathLoot(source, lootingMultiplier, allowDrops);
        this.inventory.removeAllItems().forEach(this::spawnAtLocation);
    }

    protected ItemStack addToInventory(ItemStack stack) {
        return this.inventory.addItem(stack);
    }

    protected boolean canAddToInventory(ItemStack stack) {
        return this.inventory.canAddItem(stack);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_BABY_ID, false);
        this.entityData.register(DATA_IS_CHARGING_CROSSBOW, false);
        this.entityData.register(DATA_IS_DANCING, false);
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        super.onSyncedDataUpdated(data);
        if (DATA_BABY_ID.equals(data)) {
            this.updateSize();
        }

    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MAX_HEALTH, 16.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.35F).add(GenericAttributes.ATTACK_DAMAGE, 5.0D);
    }

    public static boolean checkPiglinSpawnRules(EntityTypes<EntityPiglin> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return !world.getType(pos.below()).is(Blocks.NETHER_WART_BLOCK);
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        if (spawnReason != EnumMobSpawn.STRUCTURE) {
            if (world.getRandom().nextFloat() < 0.2F) {
                this.setBaby(true);
            } else if (this.isAdult()) {
                this.setSlot(EnumItemSlot.MAINHAND, this.createSpawnWeapon());
            }
        }

        PiglinAI.initMemories(this);
        this.populateDefaultEquipmentSlots(difficulty);
        this.populateDefaultEquipmentEnchantments(difficulty);
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return !this.isPersistent();
    }

    @Override
    protected void populateDefaultEquipmentSlots(DifficultyDamageScaler difficulty) {
        if (this.isAdult()) {
            this.maybeWearArmor(EnumItemSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            this.maybeWearArmor(EnumItemSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
            this.maybeWearArmor(EnumItemSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
            this.maybeWearArmor(EnumItemSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
        }

    }

    private void maybeWearArmor(EnumItemSlot slot, ItemStack stack) {
        if (this.level.random.nextFloat() < 0.1F) {
            this.setSlot(slot, stack);
        }

    }

    @Override
    protected BehaviorController.Provider<EntityPiglin> brainProvider() {
        return BehaviorController.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected BehaviorController<?> makeBrain(Dynamic<?> dynamic) {
        return PiglinAI.makeBrain(this, this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public BehaviorController<EntityPiglin> getBehaviorController() {
        return super.getBehaviorController();
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        EnumInteractionResult interactionResult = super.mobInteract(player, hand);
        if (interactionResult.consumesAction()) {
            return interactionResult;
        } else if (!this.level.isClientSide) {
            return PiglinAI.mobInteract(this, player, hand);
        } else {
            boolean bl = PiglinAI.canAdmire(this, player.getItemInHand(hand)) && this.getArmPose() != EntityPiglinArmPose.ADMIRING_ITEM;
            return bl ? EnumInteractionResult.SUCCESS : EnumInteractionResult.PASS;
        }
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return this.isBaby() ? 0.93F : 1.74F;
    }

    @Override
    public double getPassengersRidingOffset() {
        return (double)this.getHeight() * 0.92D;
    }

    @Override
    public void setBaby(boolean baby) {
        this.getDataWatcher().set(DATA_BABY_ID, baby);
        if (!this.level.isClientSide) {
            AttributeModifiable attributeInstance = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);
            attributeInstance.removeModifier(SPEED_MODIFIER_BABY);
            if (baby) {
                attributeInstance.addTransientModifier(SPEED_MODIFIER_BABY);
            }
        }

    }

    @Override
    public boolean isBaby() {
        return this.getDataWatcher().get(DATA_BABY_ID);
    }

    private void setCannotHunt(boolean cannotHunt) {
        this.cannotHunt = cannotHunt;
    }

    @Override
    protected boolean canHunt() {
        return !this.cannotHunt;
    }

    @Override
    protected void mobTick() {
        this.level.getMethodProfiler().enter("piglinBrain");
        this.getBehaviorController().tick((WorldServer)this.level, this);
        this.level.getMethodProfiler().exit();
        PiglinAI.updateActivity(this);
        super.mobTick();
    }

    @Override
    protected int getExpValue(EntityHuman player) {
        return this.xpReward;
    }

    @Override
    protected void finishConversion(WorldServer world) {
        PiglinAI.cancelAdmiring(this);
        this.inventory.removeAllItems().forEach(this::spawnAtLocation);
        super.finishConversion(world);
    }

    private ItemStack createSpawnWeapon() {
        return (double)this.random.nextFloat() < 0.5D ? new ItemStack(Items.CROSSBOW) : new ItemStack(Items.GOLDEN_SWORD);
    }

    private boolean isChargingCrossbow() {
        return this.entityData.get(DATA_IS_CHARGING_CROSSBOW);
    }

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(DATA_IS_CHARGING_CROSSBOW, charging);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public EntityPiglinArmPose getArmPose() {
        if (this.isDancing()) {
            return EntityPiglinArmPose.DANCING;
        } else if (PiglinAI.isLovedItem(this.getItemInOffHand())) {
            return EntityPiglinArmPose.ADMIRING_ITEM;
        } else if (this.isAggressive() && this.isHoldingMeleeWeapon()) {
            return EntityPiglinArmPose.ATTACKING_WITH_MELEE_WEAPON;
        } else if (this.isChargingCrossbow()) {
            return EntityPiglinArmPose.CROSSBOW_CHARGE;
        } else {
            return this.isAggressive() && this.isHolding(Items.CROSSBOW) ? EntityPiglinArmPose.CROSSBOW_HOLD : EntityPiglinArmPose.DEFAULT;
        }
    }

    public boolean isDancing() {
        return this.entityData.get(DATA_IS_DANCING);
    }

    public void setDancing(boolean dancing) {
        this.entityData.set(DATA_IS_DANCING, dancing);
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        boolean bl = super.damageEntity(source, amount);
        if (this.level.isClientSide) {
            return false;
        } else {
            if (bl && source.getEntity() instanceof EntityLiving) {
                PiglinAI.wasHurtBy(this, (EntityLiving)source.getEntity());
            }

            return bl;
        }
    }

    @Override
    public void performRangedAttack(EntityLiving target, float pullProgress) {
        this.performCrossbowAttack(this, 1.6F);
    }

    @Override
    public void shootCrossbowProjectile(EntityLiving target, ItemStack crossbow, IProjectile projectile, float multiShotSpray) {
        this.shootCrossbowProjectile(this, target, projectile, multiShotSpray, 1.6F);
    }

    @Override
    public boolean canFireProjectileWeapon(ItemProjectileWeapon weapon) {
        return weapon == Items.CROSSBOW;
    }

    protected void holdInMainHand(ItemStack stack) {
        this.setItemSlotAndDropWhenKilled(EnumItemSlot.MAINHAND, stack);
    }

    protected void holdInOffHand(ItemStack stack) {
        if (stack.is(PiglinAI.BARTERING_ITEM)) {
            this.setSlot(EnumItemSlot.OFFHAND, stack);
            this.setGuaranteedDrop(EnumItemSlot.OFFHAND);
        } else {
            this.setItemSlotAndDropWhenKilled(EnumItemSlot.OFFHAND, stack);
        }

    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && this.canPickupLoot() && PiglinAI.wantsToPickup(this, stack);
    }

    protected boolean canReplaceCurrentItem(ItemStack stack) {
        EnumItemSlot equipmentSlot = EntityInsentient.getEquipmentSlotForItem(stack);
        ItemStack itemStack = this.getEquipment(equipmentSlot);
        return this.canReplaceCurrentItem(stack, itemStack);
    }

    @Override
    protected boolean canReplaceCurrentItem(ItemStack newStack, ItemStack oldStack) {
        if (EnchantmentManager.hasBindingCurse(oldStack)) {
            return false;
        } else {
            boolean bl = PiglinAI.isLovedItem(newStack) || newStack.is(Items.CROSSBOW);
            boolean bl2 = PiglinAI.isLovedItem(oldStack) || oldStack.is(Items.CROSSBOW);
            if (bl && !bl2) {
                return true;
            } else if (!bl && bl2) {
                return false;
            } else {
                return this.isAdult() && !newStack.is(Items.CROSSBOW) && oldStack.is(Items.CROSSBOW) ? false : super.canReplaceCurrentItem(newStack, oldStack);
            }
        }
    }

    @Override
    protected void pickUpItem(EntityItem item) {
        this.onItemPickup(item);
        PiglinAI.pickUpItem(this, item);
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        if (this.isBaby() && entity.getEntityType() == EntityTypes.HOGLIN) {
            entity = this.getTopPassenger(entity, 3);
        }

        return super.startRiding(entity, force);
    }

    private Entity getTopPassenger(Entity entity, int maxLevel) {
        List<Entity> list = entity.getPassengers();
        return maxLevel != 1 && !list.isEmpty() ? this.getTopPassenger(list.get(0), maxLevel - 1) : entity;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return this.level.isClientSide ? null : PiglinAI.getSoundForCurrentActivity(this).orElse((SoundEffect)null);
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.PIGLIN_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.PIGLIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.PIGLIN_STEP, 0.15F, 1.0F);
    }

    protected void playSound(SoundEffect sound) {
        this.playSound(sound, this.getSoundVolume(), this.getVoicePitch());
    }

    @Override
    protected void playConvertedSound() {
        this.playSound(SoundEffects.PIGLIN_CONVERTED_TO_ZOMBIFIED);
    }
}
