package net.minecraft.world.entity.monster;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntityPositionTypes;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreakDoor;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMoveThroughVillage;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRemoveBlock;
import net.minecraft.world.entity.ai.goal.PathfinderGoalZombieAttack;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.util.PathfinderGoalUtil;
import net.minecraft.world.entity.animal.EntityChicken;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.animal.EntityTurtle;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.EntityVillagerAbstract;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class EntityZombie extends EntityMonster {
    private static final UUID SPEED_MODIFIER_BABY_UUID = UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D836");
    private static final AttributeModifier SPEED_MODIFIER_BABY = new AttributeModifier(SPEED_MODIFIER_BABY_UUID, "Baby speed boost", 0.5D, AttributeModifier.Operation.MULTIPLY_BASE);
    private static final DataWatcherObject<Boolean> DATA_BABY_ID = DataWatcher.defineId(EntityZombie.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Integer> DATA_SPECIAL_TYPE_ID = DataWatcher.defineId(EntityZombie.class, DataWatcherRegistry.INT);
    public static final DataWatcherObject<Boolean> DATA_DROWNED_CONVERSION_ID = DataWatcher.defineId(EntityZombie.class, DataWatcherRegistry.BOOLEAN);
    public static final float ZOMBIE_LEADER_CHANCE = 0.05F;
    public static final int REINFORCEMENT_ATTEMPTS = 50;
    public static final int REINFORCEMENT_RANGE_MAX = 40;
    public static final int REINFORCEMENT_RANGE_MIN = 7;
    private static final float BREAK_DOOR_CHANCE = 0.1F;
    public static final Predicate<EnumDifficulty> DOOR_BREAKING_PREDICATE = (difficulty) -> {
        return difficulty == EnumDifficulty.HARD;
    };
    private final PathfinderGoalBreakDoor breakDoorGoal = new PathfinderGoalBreakDoor(this, DOOR_BREAKING_PREDICATE);
    private boolean canBreakDoors;
    private int inWaterTime;
    public int conversionTime;

    public EntityZombie(EntityTypes<? extends EntityZombie> type, World world) {
        super(type, world);
    }

    public EntityZombie(World world) {
        this(EntityTypes.ZOMBIE, world);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(4, new EntityZombie.ZombieAttackTurtleEggGoal(this, 1.0D, 3));
        this.goalSelector.addGoal(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomLookaround(this));
        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(2, new PathfinderGoalZombieAttack(this, 1.0D, false));
        this.goalSelector.addGoal(6, new PathfinderGoalMoveThroughVillage(this, 1.0D, true, 4, this::canBreakDoors));
        this.goalSelector.addGoal(7, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this)).setAlertOthers(EntityPigZombie.class));
        this.targetSelector.addGoal(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityVillagerAbstract.class, false));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityIronGolem.class, true));
        this.targetSelector.addGoal(5, new PathfinderGoalNearestAttackableTarget<>(this, EntityTurtle.class, 10, true, false, EntityTurtle.BABY_ON_LAND_SELECTOR));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.FOLLOW_RANGE, 35.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.23F).add(GenericAttributes.ATTACK_DAMAGE, 3.0D).add(GenericAttributes.ARMOR, 2.0D).add(GenericAttributes.SPAWN_REINFORCEMENTS_CHANCE);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.getDataWatcher().register(DATA_BABY_ID, false);
        this.getDataWatcher().register(DATA_SPECIAL_TYPE_ID, 0);
        this.getDataWatcher().register(DATA_DROWNED_CONVERSION_ID, false);
    }

    public boolean isDrownConverting() {
        return this.getDataWatcher().get(DATA_DROWNED_CONVERSION_ID);
    }

    public boolean canBreakDoors() {
        return this.canBreakDoors;
    }

    public void setCanBreakDoors(boolean canBreakDoors) {
        if (this.supportsBreakDoorGoal() && PathfinderGoalUtil.hasGroundPathNavigation(this)) {
            if (this.canBreakDoors != canBreakDoors) {
                this.canBreakDoors = canBreakDoors;
                ((Navigation)this.getNavigation()).setCanOpenDoors(canBreakDoors);
                if (canBreakDoors) {
                    this.goalSelector.addGoal(1, this.breakDoorGoal);
                } else {
                    this.goalSelector.removeGoal(this.breakDoorGoal);
                }
            }
        } else if (this.canBreakDoors) {
            this.goalSelector.removeGoal(this.breakDoorGoal);
            this.canBreakDoors = false;
        }

    }

    protected boolean supportsBreakDoorGoal() {
        return true;
    }

    @Override
    public boolean isBaby() {
        return this.getDataWatcher().get(DATA_BABY_ID);
    }

    @Override
    protected int getExpValue(EntityHuman player) {
        if (this.isBaby()) {
            this.xpReward = (int)((float)this.xpReward * 2.5F);
        }

        return super.getExpValue(player);
    }

    @Override
    public void setBaby(boolean baby) {
        this.getDataWatcher().set(DATA_BABY_ID, baby);
        if (this.level != null && !this.level.isClientSide) {
            AttributeModifiable attributeInstance = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);
            attributeInstance.removeModifier(SPEED_MODIFIER_BABY);
            if (baby) {
                attributeInstance.addTransientModifier(SPEED_MODIFIER_BABY);
            }
        }

    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (DATA_BABY_ID.equals(data)) {
            this.updateSize();
        }

        super.onSyncedDataUpdated(data);
    }

    protected boolean convertsInWater() {
        return true;
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide && this.isAlive() && !this.isNoAI()) {
            if (this.isDrownConverting()) {
                --this.conversionTime;
                if (this.conversionTime < 0) {
                    this.doUnderWaterConversion();
                }
            } else if (this.convertsInWater()) {
                if (this.isEyeInFluid(TagsFluid.WATER)) {
                    ++this.inWaterTime;
                    if (this.inWaterTime >= 600) {
                        this.startDrownedConversion(300);
                    }
                } else {
                    this.inWaterTime = -1;
                }
            }
        }

        super.tick();
    }

    @Override
    public void movementTick() {
        if (this.isAlive()) {
            boolean bl = this.isSunSensitive() && this.isSunBurnTick();
            if (bl) {
                ItemStack itemStack = this.getEquipment(EnumItemSlot.HEAD);
                if (!itemStack.isEmpty()) {
                    if (itemStack.isDamageableItem()) {
                        itemStack.setDamage(itemStack.getDamage() + this.random.nextInt(2));
                        if (itemStack.getDamage() >= itemStack.getMaxDamage()) {
                            this.broadcastItemBreak(EnumItemSlot.HEAD);
                            this.setSlot(EnumItemSlot.HEAD, ItemStack.EMPTY);
                        }
                    }

                    bl = false;
                }

                if (bl) {
                    this.setOnFire(8);
                }
            }
        }

        super.movementTick();
    }

    public void startDrownedConversion(int ticksUntilWaterConversion) {
        this.conversionTime = ticksUntilWaterConversion;
        this.getDataWatcher().set(DATA_DROWNED_CONVERSION_ID, true);
    }

    protected void doUnderWaterConversion() {
        this.convertToZombieType(EntityTypes.DROWNED);
        if (!this.isSilent()) {
            this.level.levelEvent((EntityHuman)null, 1040, this.getChunkCoordinates(), 0);
        }

    }

    protected void convertToZombieType(EntityTypes<? extends EntityZombie> entityType) {
        EntityZombie zombie = this.convertTo(entityType, true);
        if (zombie != null) {
            zombie.handleAttributes(zombie.level.getDamageScaler(zombie.getChunkCoordinates()).getSpecialMultiplier());
            zombie.setCanBreakDoors(zombie.supportsBreakDoorGoal() && this.canBreakDoors());
        }

    }

    public boolean isSunSensitive() {
        return true;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (!super.damageEntity(source, amount)) {
            return false;
        } else if (!(this.level instanceof WorldServer)) {
            return false;
        } else {
            WorldServer serverLevel = (WorldServer)this.level;
            EntityLiving livingEntity = this.getGoalTarget();
            if (livingEntity == null && source.getEntity() instanceof EntityLiving) {
                livingEntity = (EntityLiving)source.getEntity();
            }

            if (livingEntity != null && this.level.getDifficulty() == EnumDifficulty.HARD && (double)this.random.nextFloat() < this.getAttributeValue(GenericAttributes.SPAWN_REINFORCEMENTS_CHANCE) && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
                int i = MathHelper.floor(this.locX());
                int j = MathHelper.floor(this.locY());
                int k = MathHelper.floor(this.locZ());
                EntityZombie zombie = new EntityZombie(this.level);

                for(int l = 0; l < 50; ++l) {
                    int m = i + MathHelper.nextInt(this.random, 7, 40) * MathHelper.nextInt(this.random, -1, 1);
                    int n = j + MathHelper.nextInt(this.random, 7, 40) * MathHelper.nextInt(this.random, -1, 1);
                    int o = k + MathHelper.nextInt(this.random, 7, 40) * MathHelper.nextInt(this.random, -1, 1);
                    BlockPosition blockPos = new BlockPosition(m, n, o);
                    EntityTypes<?> entityType = zombie.getEntityType();
                    EntityPositionTypes.Surface type = EntityPositionTypes.getPlacementType(entityType);
                    if (NaturalSpawner.isSpawnPositionOk(type, this.level, blockPos, entityType) && EntityPositionTypes.checkSpawnRules(entityType, serverLevel, EnumMobSpawn.REINFORCEMENT, blockPos, this.level.random)) {
                        zombie.setPosition((double)m, (double)n, (double)o);
                        if (!this.level.isPlayerNearby((double)m, (double)n, (double)o, 7.0D) && this.level.isUnobstructed(zombie) && this.level.getCubes(zombie) && !this.level.containsLiquid(zombie.getBoundingBox())) {
                            zombie.setGoalTarget(livingEntity);
                            zombie.prepare(serverLevel, this.level.getDamageScaler(zombie.getChunkCoordinates()), EnumMobSpawn.REINFORCEMENT, (GroupDataEntity)null, (NBTTagCompound)null);
                            serverLevel.addAllEntities(zombie);
                            this.getAttributeInstance(GenericAttributes.SPAWN_REINFORCEMENTS_CHANCE).addPermanentModifier(new AttributeModifier("Zombie reinforcement caller charge", (double)-0.05F, AttributeModifier.Operation.ADDITION));
                            zombie.getAttributeInstance(GenericAttributes.SPAWN_REINFORCEMENTS_CHANCE).addPermanentModifier(new AttributeModifier("Zombie reinforcement callee charge", (double)-0.05F, AttributeModifier.Operation.ADDITION));
                            break;
                        }
                    }
                }
            }

            return true;
        }
    }

    @Override
    public boolean attackEntity(Entity target) {
        boolean bl = super.attackEntity(target);
        if (bl) {
            float f = this.level.getDamageScaler(this.getChunkCoordinates()).getEffectiveDifficulty();
            if (this.getItemInMainHand().isEmpty() && this.isBurning() && this.random.nextFloat() < f * 0.3F) {
                target.setOnFire(2 * (int)f);
            }
        }

        return bl;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.ZOMBIE_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.ZOMBIE_DEATH;
    }

    protected SoundEffect getSoundStep() {
        return SoundEffects.ZOMBIE_STEP;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(this.getSoundStep(), 0.15F, 1.0F);
    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.UNDEAD;
    }

    @Override
    protected void populateDefaultEquipmentSlots(DifficultyDamageScaler difficulty) {
        super.populateDefaultEquipmentSlots(difficulty);
        if (this.random.nextFloat() < (this.level.getDifficulty() == EnumDifficulty.HARD ? 0.05F : 0.01F)) {
            int i = this.random.nextInt(3);
            if (i == 0) {
                this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            } else {
                this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
            }
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setBoolean("IsBaby", this.isBaby());
        nbt.setBoolean("CanBreakDoors", this.canBreakDoors());
        nbt.setInt("InWaterTime", this.isInWater() ? this.inWaterTime : -1);
        nbt.setInt("DrownedConversionTime", this.isDrownConverting() ? this.conversionTime : -1);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setBaby(nbt.getBoolean("IsBaby"));
        this.setCanBreakDoors(nbt.getBoolean("CanBreakDoors"));
        this.inWaterTime = nbt.getInt("InWaterTime");
        if (nbt.hasKeyOfType("DrownedConversionTime", 99) && nbt.getInt("DrownedConversionTime") > -1) {
            this.startDrownedConversion(nbt.getInt("DrownedConversionTime"));
        }

    }

    @Override
    public void killed(WorldServer world, EntityLiving other) {
        super.killed(world, other);
        if ((world.getDifficulty() == EnumDifficulty.NORMAL || world.getDifficulty() == EnumDifficulty.HARD) && other instanceof EntityVillager) {
            if (world.getDifficulty() != EnumDifficulty.HARD && this.random.nextBoolean()) {
                return;
            }

            EntityVillager villager = (EntityVillager)other;
            EntityZombieVillager zombieVillager = villager.convertTo(EntityTypes.ZOMBIE_VILLAGER, false);
            zombieVillager.prepare(world, world.getDamageScaler(zombieVillager.getChunkCoordinates()), EnumMobSpawn.CONVERSION, new EntityZombie.GroupDataZombie(false, true), (NBTTagCompound)null);
            zombieVillager.setVillagerData(villager.getVillagerData());
            zombieVillager.setGossips(villager.getGossips().store(DynamicOpsNBT.INSTANCE).getValue());
            zombieVillager.setOffers(villager.getOffers().createTag());
            zombieVillager.setVillagerXp(villager.getExperience());
            if (!this.isSilent()) {
                world.levelEvent((EntityHuman)null, 1026, this.getChunkCoordinates(), 0);
            }
        }

    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return this.isBaby() ? 0.93F : 1.74F;
    }

    @Override
    public boolean canPickup(ItemStack stack) {
        return stack.is(Items.EGG) && this.isBaby() && this.isPassenger() ? false : super.canPickup(stack);
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return stack.is(Items.GLOW_INK_SAC) ? false : super.wantsToPickUp(stack);
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        entityData = super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
        float f = difficulty.getSpecialMultiplier();
        this.setCanPickupLoot(this.random.nextFloat() < 0.55F * f);
        if (entityData == null) {
            entityData = new EntityZombie.GroupDataZombie(getSpawnAsBabyOdds(world.getRandom()), true);
        }

        if (entityData instanceof EntityZombie.GroupDataZombie) {
            EntityZombie.GroupDataZombie zombieGroupData = (EntityZombie.GroupDataZombie)entityData;
            if (zombieGroupData.isBaby) {
                this.setBaby(true);
                if (zombieGroupData.canSpawnJockey) {
                    if ((double)world.getRandom().nextFloat() < 0.05D) {
                        List<EntityChicken> list = world.getEntitiesOfClass(EntityChicken.class, this.getBoundingBox().grow(5.0D, 3.0D, 5.0D), IEntitySelector.ENTITY_NOT_BEING_RIDDEN);
                        if (!list.isEmpty()) {
                            EntityChicken chicken = list.get(0);
                            chicken.setChickenJockey(true);
                            this.startRiding(chicken);
                        }
                    } else if ((double)world.getRandom().nextFloat() < 0.05D) {
                        EntityChicken chicken2 = EntityTypes.CHICKEN.create(this.level);
                        chicken2.setPositionRotation(this.locX(), this.locY(), this.locZ(), this.getYRot(), 0.0F);
                        chicken2.prepare(world, difficulty, EnumMobSpawn.JOCKEY, (GroupDataEntity)null, (NBTTagCompound)null);
                        chicken2.setChickenJockey(true);
                        this.startRiding(chicken2);
                        world.addEntity(chicken2);
                    }
                }
            }

            this.setCanBreakDoors(this.supportsBreakDoorGoal() && this.random.nextFloat() < f * 0.1F);
            this.populateDefaultEquipmentSlots(difficulty);
            this.populateDefaultEquipmentEnchantments(difficulty);
        }

        if (this.getEquipment(EnumItemSlot.HEAD).isEmpty()) {
            LocalDate localDate = LocalDate.now();
            int i = localDate.get(ChronoField.DAY_OF_MONTH);
            int j = localDate.get(ChronoField.MONTH_OF_YEAR);
            if (j == 10 && i == 31 && this.random.nextFloat() < 0.25F) {
                this.setSlot(EnumItemSlot.HEAD, new ItemStack(this.random.nextFloat() < 0.1F ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
                this.armorDropChances[EnumItemSlot.HEAD.getIndex()] = 0.0F;
            }
        }

        this.handleAttributes(f);
        return entityData;
    }

    public static boolean getSpawnAsBabyOdds(Random random) {
        return random.nextFloat() < 0.05F;
    }

    protected void handleAttributes(float chanceMultiplier) {
        this.randomizeReinforcementsChance();
        this.getAttributeInstance(GenericAttributes.KNOCKBACK_RESISTANCE).addPermanentModifier(new AttributeModifier("Random spawn bonus", this.random.nextDouble() * (double)0.05F, AttributeModifier.Operation.ADDITION));
        double d = this.random.nextDouble() * 1.5D * (double)chanceMultiplier;
        if (d > 1.0D) {
            this.getAttributeInstance(GenericAttributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("Random zombie-spawn bonus", d, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        if (this.random.nextFloat() < chanceMultiplier * 0.05F) {
            this.getAttributeInstance(GenericAttributes.SPAWN_REINFORCEMENTS_CHANCE).addPermanentModifier(new AttributeModifier("Leader zombie bonus", this.random.nextDouble() * 0.25D + 0.5D, AttributeModifier.Operation.ADDITION));
            this.getAttributeInstance(GenericAttributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("Leader zombie bonus", this.random.nextDouble() * 3.0D + 1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            this.setCanBreakDoors(this.supportsBreakDoorGoal());
        }

    }

    protected void randomizeReinforcementsChance() {
        this.getAttributeInstance(GenericAttributes.SPAWN_REINFORCEMENTS_CHANCE).setValue(this.random.nextDouble() * (double)0.1F);
    }

    @Override
    public double getMyRidingOffset() {
        return this.isBaby() ? 0.0D : -0.45D;
    }

    @Override
    protected void dropDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropDeathLoot(source, lootingMultiplier, allowDrops);
        Entity entity = source.getEntity();
        if (entity instanceof EntityCreeper) {
            EntityCreeper creeper = (EntityCreeper)entity;
            if (creeper.canCauseHeadDrop()) {
                ItemStack itemStack = this.getSkull();
                if (!itemStack.isEmpty()) {
                    creeper.setCausedHeadDrop();
                    this.spawnAtLocation(itemStack);
                }
            }
        }

    }

    protected ItemStack getSkull() {
        return new ItemStack(Items.ZOMBIE_HEAD);
    }

    public static class GroupDataZombie implements GroupDataEntity {
        public final boolean isBaby;
        public final boolean canSpawnJockey;

        public GroupDataZombie(boolean baby, boolean tryChickenJockey) {
            this.isBaby = baby;
            this.canSpawnJockey = tryChickenJockey;
        }
    }

    class ZombieAttackTurtleEggGoal extends PathfinderGoalRemoveBlock {
        ZombieAttackTurtleEggGoal(EntityCreature mob, double speed, int maxYDifference) {
            super(Blocks.TURTLE_EGG, mob, speed, maxYDifference);
        }

        @Override
        public void playDestroyProgressSound(GeneratorAccess world, BlockPosition pos) {
            world.playSound((EntityHuman)null, pos, SoundEffects.ZOMBIE_DESTROY_EGG, SoundCategory.HOSTILE, 0.5F, 0.9F + EntityZombie.this.random.nextFloat() * 0.2F);
        }

        @Override
        public void playBreakSound(World world, BlockPosition pos) {
            world.playSound((EntityHuman)null, pos, SoundEffects.TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + world.random.nextFloat() * 0.2F);
        }

        @Override
        public double acceptedDistance() {
            return 1.14D;
        }
    }
}
