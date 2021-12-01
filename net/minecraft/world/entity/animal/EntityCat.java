package net.minecraft.world.entity.animal;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTameableAnimal;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalCatSitOnBed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowOwner;
import net.minecraft.world.entity.ai.goal.PathfinderGoalJumpOnBlock;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLeapAtTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalOcelotAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSit;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTempt;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalRandomTargetNonTamed;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.BlockBed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.AxisAlignedBB;

public class EntityCat extends EntityTameableAnimal {
    public static final double TEMPT_SPEED_MOD = 0.6D;
    public static final double WALK_SPEED_MOD = 0.8D;
    public static final double SPRINT_SPEED_MOD = 1.33D;
    private static final RecipeItemStack TEMPT_INGREDIENT = RecipeItemStack.of(Items.COD, Items.SALMON);
    private static final DataWatcherObject<Integer> DATA_TYPE_ID = DataWatcher.defineId(EntityCat.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Boolean> IS_LYING = DataWatcher.defineId(EntityCat.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> RELAX_STATE_ONE = DataWatcher.defineId(EntityCat.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Integer> DATA_COLLAR_COLOR = DataWatcher.defineId(EntityCat.class, DataWatcherRegistry.INT);
    public static final int TYPE_TABBY = 0;
    public static final int TYPE_BLACK = 1;
    public static final int TYPE_RED = 2;
    public static final int TYPE_SIAMESE = 3;
    public static final int TYPE_BRITISH = 4;
    public static final int TYPE_CALICO = 5;
    public static final int TYPE_PERSIAN = 6;
    public static final int TYPE_RAGDOLL = 7;
    public static final int TYPE_WHITE = 8;
    public static final int TYPE_JELLIE = 9;
    public static final int TYPE_ALL_BLACK = 10;
    private static final int NUMBER_OF_CAT_TYPES = 11;
    private static final int NUMBER_OF_CAT_TYPES_EXCEPT_ALL_BLACK = 10;
    public static final Map<Integer, MinecraftKey> TEXTURE_BY_TYPE = SystemUtils.make(Maps.newHashMap(), (map) -> {
        map.put(0, new MinecraftKey("textures/entity/cat/tabby.png"));
        map.put(1, new MinecraftKey("textures/entity/cat/black.png"));
        map.put(2, new MinecraftKey("textures/entity/cat/red.png"));
        map.put(3, new MinecraftKey("textures/entity/cat/siamese.png"));
        map.put(4, new MinecraftKey("textures/entity/cat/british_shorthair.png"));
        map.put(5, new MinecraftKey("textures/entity/cat/calico.png"));
        map.put(6, new MinecraftKey("textures/entity/cat/persian.png"));
        map.put(7, new MinecraftKey("textures/entity/cat/ragdoll.png"));
        map.put(8, new MinecraftKey("textures/entity/cat/white.png"));
        map.put(9, new MinecraftKey("textures/entity/cat/jellie.png"));
        map.put(10, new MinecraftKey("textures/entity/cat/all_black.png"));
    });
    private EntityCat.CatAvoidEntityGoal<EntityHuman> avoidPlayersGoal;
    @Nullable
    private PathfinderGoalTempt temptGoal;
    private float lieDownAmount;
    private float lieDownAmountO;
    private float lieDownAmountTail;
    private float lieDownAmountOTail;
    private float relaxStateOneAmount;
    private float relaxStateOneAmountO;

    public EntityCat(EntityTypes<? extends EntityCat> type, World world) {
        super(type, world);
    }

    public MinecraftKey getResourceLocation() {
        return TEXTURE_BY_TYPE.getOrDefault(this.getCatType(), TEXTURE_BY_TYPE.get(0));
    }

    @Override
    protected void initPathfinder() {
        this.temptGoal = new EntityCat.PathfinderGoalTemptChance(this, 0.6D, TEMPT_INGREDIENT, true);
        this.goalSelector.addGoal(1, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new PathfinderGoalSit(this));
        this.goalSelector.addGoal(2, new EntityCat.CatRelaxOnOwnerGoal(this));
        this.goalSelector.addGoal(3, this.temptGoal);
        this.goalSelector.addGoal(5, new PathfinderGoalCatSitOnBed(this, 1.1D, 8));
        this.goalSelector.addGoal(6, new PathfinderGoalFollowOwner(this, 1.0D, 10.0F, 5.0F, false));
        this.goalSelector.addGoal(7, new PathfinderGoalJumpOnBlock(this, 0.8D));
        this.goalSelector.addGoal(8, new PathfinderGoalLeapAtTarget(this, 0.3F));
        this.goalSelector.addGoal(9, new PathfinderGoalOcelotAttack(this));
        this.goalSelector.addGoal(10, new PathfinderGoalBreed(this, 0.8D));
        this.goalSelector.addGoal(11, new PathfinderGoalRandomStrollLand(this, 0.8D, 1.0000001E-5F));
        this.goalSelector.addGoal(12, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 10.0F));
        this.targetSelector.addGoal(1, new PathfinderGoalRandomTargetNonTamed<>(this, EntityRabbit.class, false, (Predicate<EntityLiving>)null));
        this.targetSelector.addGoal(1, new PathfinderGoalRandomTargetNonTamed<>(this, EntityTurtle.class, false, EntityTurtle.BABY_ON_LAND_SELECTOR));
    }

    public int getCatType() {
        return this.entityData.get(DATA_TYPE_ID);
    }

    public void setCatType(int type) {
        if (type < 0 || type >= 11) {
            type = this.random.nextInt(10);
        }

        this.entityData.set(DATA_TYPE_ID, type);
    }

    public void setLying(boolean sleeping) {
        this.entityData.set(IS_LYING, sleeping);
    }

    public boolean isLying() {
        return this.entityData.get(IS_LYING);
    }

    public void setRelaxStateOne(boolean headDown) {
        this.entityData.set(RELAX_STATE_ONE, headDown);
    }

    public boolean isRelaxStateOne() {
        return this.entityData.get(RELAX_STATE_ONE);
    }

    public EnumColor getCollarColor() {
        return EnumColor.fromColorIndex(this.entityData.get(DATA_COLLAR_COLOR));
    }

    public void setCollarColor(EnumColor color) {
        this.entityData.set(DATA_COLLAR_COLOR, color.getColorIndex());
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_TYPE_ID, 1);
        this.entityData.register(IS_LYING, false);
        this.entityData.register(RELAX_STATE_ONE, false);
        this.entityData.register(DATA_COLLAR_COLOR, EnumColor.RED.getColorIndex());
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("CatType", this.getCatType());
        nbt.setByte("CollarColor", (byte)this.getCollarColor().getColorIndex());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setCatType(nbt.getInt("CatType"));
        if (nbt.hasKeyOfType("CollarColor", 99)) {
            this.setCollarColor(EnumColor.fromColorIndex(nbt.getInt("CollarColor")));
        }

    }

    @Override
    public void mobTick() {
        if (this.getControllerMove().hasWanted()) {
            double d = this.getControllerMove().getSpeedModifier();
            if (d == 0.6D) {
                this.setPose(EntityPose.CROUCHING);
                this.setSprinting(false);
            } else if (d == 1.33D) {
                this.setPose(EntityPose.STANDING);
                this.setSprinting(true);
            } else {
                this.setPose(EntityPose.STANDING);
                this.setSprinting(false);
            }
        } else {
            this.setPose(EntityPose.STANDING);
            this.setSprinting(false);
        }

    }

    @Nullable
    @Override
    protected SoundEffect getSoundAmbient() {
        if (this.isTamed()) {
            if (this.isInLove()) {
                return SoundEffects.CAT_PURR;
            } else {
                return this.random.nextInt(4) == 0 ? SoundEffects.CAT_PURREOW : SoundEffects.CAT_AMBIENT;
            }
        } else {
            return SoundEffects.CAT_STRAY_AMBIENT;
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    public void hiss() {
        this.playSound(SoundEffects.CAT_HISS, this.getSoundVolume(), this.getVoicePitch());
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.CAT_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.CAT_DEATH;
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 10.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.3F).add(GenericAttributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void usePlayerItem(EntityHuman player, EnumHand hand, ItemStack stack) {
        if (this.isBreedItem(stack)) {
            this.playSound(SoundEffects.CAT_EAT, 1.0F, 1.0F);
        }

        super.usePlayerItem(player, hand, stack);
    }

    private float getAttackDamage() {
        return (float)this.getAttributeValue(GenericAttributes.ATTACK_DAMAGE);
    }

    @Override
    public boolean attackEntity(Entity target) {
        return target.damageEntity(DamageSource.mobAttack(this), this.getAttackDamage());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.temptGoal != null && this.temptGoal.isRunning() && !this.isTamed() && this.tickCount % 100 == 0) {
            this.playSound(SoundEffects.CAT_BEG_FOR_FOOD, 1.0F, 1.0F);
        }

        this.handleLieDown();
    }

    private void handleLieDown() {
        if ((this.isLying() || this.isRelaxStateOne()) && this.tickCount % 5 == 0) {
            this.playSound(SoundEffects.CAT_PURR, 0.6F + 0.4F * (this.random.nextFloat() - this.random.nextFloat()), 1.0F);
        }

        this.updateLieDownAmount();
        this.updateRelaxStateOneAmount();
    }

    private void updateLieDownAmount() {
        this.lieDownAmountO = this.lieDownAmount;
        this.lieDownAmountOTail = this.lieDownAmountTail;
        if (this.isLying()) {
            this.lieDownAmount = Math.min(1.0F, this.lieDownAmount + 0.15F);
            this.lieDownAmountTail = Math.min(1.0F, this.lieDownAmountTail + 0.08F);
        } else {
            this.lieDownAmount = Math.max(0.0F, this.lieDownAmount - 0.22F);
            this.lieDownAmountTail = Math.max(0.0F, this.lieDownAmountTail - 0.13F);
        }

    }

    private void updateRelaxStateOneAmount() {
        this.relaxStateOneAmountO = this.relaxStateOneAmount;
        if (this.isRelaxStateOne()) {
            this.relaxStateOneAmount = Math.min(1.0F, this.relaxStateOneAmount + 0.1F);
        } else {
            this.relaxStateOneAmount = Math.max(0.0F, this.relaxStateOneAmount - 0.13F);
        }

    }

    public float getLieDownAmount(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.lieDownAmountO, this.lieDownAmount);
    }

    public float getLieDownAmountTail(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.lieDownAmountOTail, this.lieDownAmountTail);
    }

    public float getRelaxStateOneAmount(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.relaxStateOneAmountO, this.relaxStateOneAmount);
    }

    @Override
    public EntityCat getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        EntityCat cat = EntityTypes.CAT.create(serverLevel);
        if (ageableMob instanceof EntityCat) {
            if (this.random.nextBoolean()) {
                cat.setCatType(this.getCatType());
            } else {
                cat.setCatType(((EntityCat)ageableMob).getCatType());
            }

            if (this.isTamed()) {
                cat.setOwnerUUID(this.getOwnerUUID());
                cat.setTamed(true);
                if (this.random.nextBoolean()) {
                    cat.setCollarColor(this.getCollarColor());
                } else {
                    cat.setCollarColor(((EntityCat)ageableMob).getCollarColor());
                }
            }
        }

        return cat;
    }

    @Override
    public boolean mate(EntityAnimal other) {
        if (!this.isTamed()) {
            return false;
        } else if (!(other instanceof EntityCat)) {
            return false;
        } else {
            EntityCat cat = (EntityCat)other;
            return cat.isTamed() && super.mate(other);
        }
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        entityData = super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
        if (world.getMoonBrightness() > 0.9F) {
            this.setCatType(this.random.nextInt(11));
        } else {
            this.setCatType(this.random.nextInt(10));
        }

        World level = world.getLevel();
        if (level instanceof WorldServer && ((WorldServer)level).getStructureManager().getStructureWithPieceAt(this.getChunkCoordinates(), StructureGenerator.SWAMP_HUT).isValid()) {
            this.setCatType(10);
            this.setPersistent();
        }

        return entityData;
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        Item item = itemStack.getItem();
        if (this.level.isClientSide) {
            if (this.isTamed() && this.isOwnedBy(player)) {
                return EnumInteractionResult.SUCCESS;
            } else {
                return !this.isBreedItem(itemStack) || !(this.getHealth() < this.getMaxHealth()) && this.isTamed() ? EnumInteractionResult.PASS : EnumInteractionResult.SUCCESS;
            }
        } else {
            if (this.isTamed()) {
                if (this.isOwnedBy(player)) {
                    if (!(item instanceof ItemDye)) {
                        if (item.isFood() && this.isBreedItem(itemStack) && this.getHealth() < this.getMaxHealth()) {
                            this.usePlayerItem(player, hand, itemStack);
                            this.heal((float)item.getFoodInfo().getNutrition());
                            return EnumInteractionResult.CONSUME;
                        }

                        EnumInteractionResult interactionResult = super.mobInteract(player, hand);
                        if (!interactionResult.consumesAction() || this.isBaby()) {
                            this.setWillSit(!this.isWillSit());
                        }

                        return interactionResult;
                    }

                    EnumColor dyeColor = ((ItemDye)item).getDyeColor();
                    if (dyeColor != this.getCollarColor()) {
                        this.setCollarColor(dyeColor);
                        if (!player.getAbilities().instabuild) {
                            itemStack.subtract(1);
                        }

                        this.setPersistent();
                        return EnumInteractionResult.CONSUME;
                    }
                }
            } else if (this.isBreedItem(itemStack)) {
                this.usePlayerItem(player, hand, itemStack);
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.setWillSit(true);
                    this.level.broadcastEntityEffect(this, (byte)7);
                } else {
                    this.level.broadcastEntityEffect(this, (byte)6);
                }

                this.setPersistent();
                return EnumInteractionResult.CONSUME;
            }

            EnumInteractionResult interactionResult2 = super.mobInteract(player, hand);
            if (interactionResult2.consumesAction()) {
                this.setPersistent();
            }

            return interactionResult2;
        }
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return TEMPT_INGREDIENT.test(stack);
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height * 0.5F;
    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return !this.isTamed() && this.tickCount > 2400;
    }

    @Override
    protected void reassessTameGoals() {
        if (this.avoidPlayersGoal == null) {
            this.avoidPlayersGoal = new EntityCat.CatAvoidEntityGoal<>(this, EntityHuman.class, 16.0F, 0.8D, 1.33D);
        }

        this.goalSelector.removeGoal(this.avoidPlayersGoal);
        if (!this.isTamed()) {
            this.goalSelector.addGoal(4, this.avoidPlayersGoal);
        }

    }

    @Override
    public boolean isSteppingCarefully() {
        return this.getPose() == EntityPose.CROUCHING || super.isSteppingCarefully();
    }

    static class CatAvoidEntityGoal<T extends EntityLiving> extends PathfinderGoalAvoidTarget<T> {
        private final EntityCat cat;

        public CatAvoidEntityGoal(EntityCat cat, Class<T> fleeFromType, float distance, double slowSpeed, double fastSpeed) {
            super(cat, fleeFromType, distance, slowSpeed, fastSpeed, IEntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            return !this.cat.isTamed() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !this.cat.isTamed() && super.canContinueToUse();
        }
    }

    static class CatRelaxOnOwnerGoal extends PathfinderGoal {
        private final EntityCat cat;
        @Nullable
        private EntityHuman ownerPlayer;
        @Nullable
        private BlockPosition goalPos;
        private int onBedTicks;

        public CatRelaxOnOwnerGoal(EntityCat cat) {
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            if (!this.cat.isTamed()) {
                return false;
            } else if (this.cat.isWillSit()) {
                return false;
            } else {
                EntityLiving livingEntity = this.cat.getOwner();
                if (livingEntity instanceof EntityHuman) {
                    this.ownerPlayer = (EntityHuman)livingEntity;
                    if (!livingEntity.isSleeping()) {
                        return false;
                    }

                    if (this.cat.distanceToSqr(this.ownerPlayer) > 100.0D) {
                        return false;
                    }

                    BlockPosition blockPos = this.ownerPlayer.getChunkCoordinates();
                    IBlockData blockState = this.cat.level.getType(blockPos);
                    if (blockState.is(TagsBlock.BEDS)) {
                        this.goalPos = blockState.getOptionalValue(BlockBed.FACING).map((direction) -> {
                            return blockPos.relative(direction.opposite());
                        }).orElseGet(() -> {
                            return new BlockPosition(blockPos);
                        });
                        return !this.spaceIsOccupied();
                    }
                }

                return false;
            }
        }

        private boolean spaceIsOccupied() {
            for(EntityCat cat : this.cat.level.getEntitiesOfClass(EntityCat.class, (new AxisAlignedBB(this.goalPos)).inflate(2.0D))) {
                if (cat != this.cat && (cat.isLying() || cat.isRelaxStateOne())) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.cat.isTamed() && !this.cat.isWillSit() && this.ownerPlayer != null && this.ownerPlayer.isSleeping() && this.goalPos != null && !this.spaceIsOccupied();
        }

        @Override
        public void start() {
            if (this.goalPos != null) {
                this.cat.setSitting(false);
                this.cat.getNavigation().moveTo((double)this.goalPos.getX(), (double)this.goalPos.getY(), (double)this.goalPos.getZ(), (double)1.1F);
            }

        }

        @Override
        public void stop() {
            this.cat.setLying(false);
            float f = this.cat.level.getTimeOfDay(1.0F);
            if (this.ownerPlayer.getSleepTimer() >= 100 && (double)f > 0.77D && (double)f < 0.8D && (double)this.cat.level.getRandom().nextFloat() < 0.7D) {
                this.giveMorningGift();
            }

            this.onBedTicks = 0;
            this.cat.setRelaxStateOne(false);
            this.cat.getNavigation().stop();
        }

        private void giveMorningGift() {
            Random random = this.cat.getRandom();
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
            mutableBlockPos.set(this.cat.getChunkCoordinates());
            this.cat.randomTeleport((double)(mutableBlockPos.getX() + random.nextInt(11) - 5), (double)(mutableBlockPos.getY() + random.nextInt(5) - 2), (double)(mutableBlockPos.getZ() + random.nextInt(11) - 5), false);
            mutableBlockPos.set(this.cat.getChunkCoordinates());
            LootTable lootTable = this.cat.level.getMinecraftServer().getLootTableRegistry().getLootTable(LootTables.CAT_MORNING_GIFT);
            LootTableInfo.Builder builder = (new LootTableInfo.Builder((WorldServer)this.cat.level)).set(LootContextParameters.ORIGIN, this.cat.getPositionVector()).set(LootContextParameters.THIS_ENTITY, this.cat).withRandom(random);

            for(ItemStack itemStack : lootTable.populateLoot(builder.build(LootContextParameterSets.GIFT))) {
                this.cat.level.addEntity(new EntityItem(this.cat.level, (double)mutableBlockPos.getX() - (double)MathHelper.sin(this.cat.yBodyRot * ((float)Math.PI / 180F)), (double)mutableBlockPos.getY(), (double)mutableBlockPos.getZ() + (double)MathHelper.cos(this.cat.yBodyRot * ((float)Math.PI / 180F)), itemStack));
            }

        }

        @Override
        public void tick() {
            if (this.ownerPlayer != null && this.goalPos != null) {
                this.cat.setSitting(false);
                this.cat.getNavigation().moveTo((double)this.goalPos.getX(), (double)this.goalPos.getY(), (double)this.goalPos.getZ(), (double)1.1F);
                if (this.cat.distanceToSqr(this.ownerPlayer) < 2.5D) {
                    ++this.onBedTicks;
                    if (this.onBedTicks > this.adjustedTickDelay(16)) {
                        this.cat.setLying(true);
                        this.cat.setRelaxStateOne(false);
                    } else {
                        this.cat.lookAt(this.ownerPlayer, 45.0F, 45.0F);
                        this.cat.setRelaxStateOne(true);
                    }
                } else {
                    this.cat.setLying(false);
                }
            }

        }
    }

    static class PathfinderGoalTemptChance extends PathfinderGoalTempt {
        @Nullable
        private EntityHuman selectedPlayer;
        private final EntityCat cat;

        public PathfinderGoalTemptChance(EntityCat cat, double speed, RecipeItemStack food, boolean canBeScared) {
            super(cat, speed, food, canBeScared);
            this.cat = cat;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.selectedPlayer == null && this.mob.getRandom().nextInt(this.adjustedTickDelay(600)) == 0) {
                this.selectedPlayer = this.player;
            } else if (this.mob.getRandom().nextInt(this.adjustedTickDelay(500)) == 0) {
                this.selectedPlayer = null;
            }

        }

        @Override
        protected boolean canScare() {
            return this.selectedPlayer != null && this.selectedPlayer.equals(this.player) ? false : super.canScare();
        }

        @Override
        public boolean canUse() {
            return super.canUse() && !this.cat.isTamed();
        }
    }
}
