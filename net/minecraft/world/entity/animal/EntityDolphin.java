package net.minecraft.world.entity.animal;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerLookSmoothSwim;
import net.minecraft.world.entity.ai.control.ControllerMoveSmoothSwim;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreath;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowBoat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomSwim;
import net.minecraft.world.entity.ai.goal.PathfinderGoalWater;
import net.minecraft.world.entity.ai.goal.PathfinderGoalWaterJump;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationGuardian;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.EntityGuardian;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.Vec3D;

public class EntityDolphin extends EntityWaterAnimal {
    private static final DataWatcherObject<BlockPosition> TREASURE_POS = DataWatcher.defineId(EntityDolphin.class, DataWatcherRegistry.BLOCK_POS);
    private static final DataWatcherObject<Boolean> GOT_FISH = DataWatcher.defineId(EntityDolphin.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Integer> MOISTNESS_LEVEL = DataWatcher.defineId(EntityDolphin.class, DataWatcherRegistry.INT);
    static final PathfinderTargetCondition SWIM_WITH_PLAYER_TARGETING = PathfinderTargetCondition.forNonCombat().range(10.0D).ignoreLineOfSight();
    public static final int TOTAL_AIR_SUPPLY = 4800;
    private static final int TOTAL_MOISTNESS_LEVEL = 2400;
    public static final Predicate<EntityItem> ALLOWED_ITEMS = (item) -> {
        return !item.hasPickUpDelay() && item.isAlive() && item.isInWater();
    };

    public EntityDolphin(EntityTypes<? extends EntityDolphin> type, World world) {
        super(type, world);
        this.moveControl = new ControllerMoveSmoothSwim(this, 85, 10, 0.02F, 0.1F, true);
        this.lookControl = new ControllerLookSmoothSwim(this, 10);
        this.setCanPickupLoot(true);
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.setAirTicks(this.getMaxAirSupply());
        this.setXRot(0.0F);
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return false;
    }

    @Override
    protected void handleAirSupply(int air) {
    }

    public void setTreasurePos(BlockPosition treasurePos) {
        this.entityData.set(TREASURE_POS, treasurePos);
    }

    public BlockPosition getTreasurePos() {
        return this.entityData.get(TREASURE_POS);
    }

    public boolean gotFish() {
        return this.entityData.get(GOT_FISH);
    }

    public void setGotFish(boolean hasFish) {
        this.entityData.set(GOT_FISH, hasFish);
    }

    public int getMoistness() {
        return this.entityData.get(MOISTNESS_LEVEL);
    }

    public void setMoistness(int moistness) {
        this.entityData.set(MOISTNESS_LEVEL, moistness);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(TREASURE_POS, BlockPosition.ZERO);
        this.entityData.register(GOT_FISH, false);
        this.entityData.register(MOISTNESS_LEVEL, 2400);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("TreasurePosX", this.getTreasurePos().getX());
        nbt.setInt("TreasurePosY", this.getTreasurePos().getY());
        nbt.setInt("TreasurePosZ", this.getTreasurePos().getZ());
        nbt.setBoolean("GotFish", this.gotFish());
        nbt.setInt("Moistness", this.getMoistness());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        int i = nbt.getInt("TreasurePosX");
        int j = nbt.getInt("TreasurePosY");
        int k = nbt.getInt("TreasurePosZ");
        this.setTreasurePos(new BlockPosition(i, j, k));
        super.loadData(nbt);
        this.setGotFish(nbt.getBoolean("GotFish"));
        this.setMoistness(nbt.getInt("Moistness"));
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(0, new PathfinderGoalBreath(this));
        this.goalSelector.addGoal(0, new PathfinderGoalWater(this));
        this.goalSelector.addGoal(1, new EntityDolphin.DolphinSwimToTreasureGoal(this));
        this.goalSelector.addGoal(2, new EntityDolphin.DolphinSwimWithPlayerGoal(this, 4.0D));
        this.goalSelector.addGoal(4, new PathfinderGoalRandomSwim(this, 1.0D, 10));
        this.goalSelector.addGoal(4, new PathfinderGoalRandomLookaround(this));
        this.goalSelector.addGoal(5, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.addGoal(5, new PathfinderGoalWaterJump(this, 10));
        this.goalSelector.addGoal(6, new PathfinderGoalMeleeAttack(this, (double)1.2F, true));
        this.goalSelector.addGoal(8, new EntityDolphin.PlayWithItemsGoal());
        this.goalSelector.addGoal(8, new PathfinderGoalFollowBoat(this));
        this.goalSelector.addGoal(9, new PathfinderGoalAvoidTarget<>(this, EntityGuardian.class, 8.0F, 1.0D, 1.0D));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this, EntityGuardian.class)).setAlertOthers());
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 10.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)1.2F).add(GenericAttributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected NavigationAbstract createNavigation(World world) {
        return new NavigationGuardian(this, world);
    }

    @Override
    public boolean attackEntity(Entity target) {
        boolean bl = target.damageEntity(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(GenericAttributes.ATTACK_DAMAGE)));
        if (bl) {
            this.doEnchantDamageEffects(this, target);
            this.playSound(SoundEffects.DOLPHIN_ATTACK, 1.0F, 1.0F);
        }

        return bl;
    }

    @Override
    public int getMaxAirSupply() {
        return 4800;
    }

    @Override
    protected int increaseAirSupply(int air) {
        return this.getMaxAirSupply();
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 0.3F;
    }

    @Override
    public int getMaxHeadXRot() {
        return 1;
    }

    @Override
    public int getMaxHeadYRot() {
        return 1;
    }

    @Override
    protected boolean canRide(Entity entity) {
        return true;
    }

    @Override
    public boolean canTakeItem(ItemStack stack) {
        EnumItemSlot equipmentSlot = EntityInsentient.getEquipmentSlotForItem(stack);
        if (!this.getEquipment(equipmentSlot).isEmpty()) {
            return false;
        } else {
            return equipmentSlot == EnumItemSlot.MAINHAND && super.canTakeItem(stack);
        }
    }

    @Override
    protected void pickUpItem(EntityItem item) {
        if (this.getEquipment(EnumItemSlot.MAINHAND).isEmpty()) {
            ItemStack itemStack = item.getItemStack();
            if (this.canPickup(itemStack)) {
                this.onItemPickup(item);
                this.setSlot(EnumItemSlot.MAINHAND, itemStack);
                this.handDropChances[EnumItemSlot.MAINHAND.getIndex()] = 2.0F;
                this.receive(item, itemStack.getCount());
                item.die();
            }
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (this.isNoAI()) {
            this.setAirTicks(this.getMaxAirSupply());
        } else {
            if (this.isInWaterRainOrBubble()) {
                this.setMoistness(2400);
            } else {
                this.setMoistness(this.getMoistness() - 1);
                if (this.getMoistness() <= 0) {
                    this.damageEntity(DamageSource.DRY_OUT, 1.0F);
                }

                if (this.onGround) {
                    this.setMot(this.getMot().add((double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F), 0.5D, (double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F)));
                    this.setYRot(this.random.nextFloat() * 360.0F);
                    this.onGround = false;
                    this.hasImpulse = true;
                }
            }

            if (this.level.isClientSide && this.isInWater() && this.getMot().lengthSqr() > 0.03D) {
                Vec3D vec3 = this.getViewVector(0.0F);
                float f = MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F)) * 0.3F;
                float g = MathHelper.sin(this.getYRot() * ((float)Math.PI / 180F)) * 0.3F;
                float h = 1.2F - this.random.nextFloat() * 0.7F;

                for(int i = 0; i < 2; ++i) {
                    this.level.addParticle(Particles.DOLPHIN, this.locX() - vec3.x * (double)h + (double)f, this.locY() - vec3.y, this.locZ() - vec3.z * (double)h + (double)g, 0.0D, 0.0D, 0.0D);
                    this.level.addParticle(Particles.DOLPHIN, this.locX() - vec3.x * (double)h - (double)f, this.locY() - vec3.y, this.locZ() - vec3.z * (double)h - (double)g, 0.0D, 0.0D, 0.0D);
                }
            }

        }
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 38) {
            this.addParticlesAroundSelf(Particles.HAPPY_VILLAGER);
        } else {
            super.handleEntityEvent(status);
        }

    }

    private void addParticlesAroundSelf(ParticleParam parameters) {
        for(int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.01D;
            double e = this.random.nextGaussian() * 0.01D;
            double f = this.random.nextGaussian() * 0.01D;
            this.level.addParticle(parameters, this.getRandomX(1.0D), this.getRandomY() + 0.2D, this.getRandomZ(1.0D), d, e, f);
        }

    }

    @Override
    protected EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.isEmpty() && itemStack.is(TagsItem.FISHES)) {
            if (!this.level.isClientSide) {
                this.playSound(SoundEffects.DOLPHIN_EAT, 1.0F, 1.0F);
            }

            this.setGotFish(true);
            if (!player.getAbilities().instabuild) {
                itemStack.subtract(1);
            }

            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            return super.mobInteract(player, hand);
        }
    }

    public static boolean checkDolphinSpawnRules(EntityTypes<EntityDolphin> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        if (pos.getY() > 45 && pos.getY() < world.getSeaLevel()) {
            Optional<ResourceKey<BiomeBase>> optional = world.getBiomeName(pos);
            return (!Objects.equals(optional, Optional.of(Biomes.OCEAN)) || !Objects.equals(optional, Optional.of(Biomes.DEEP_OCEAN))) && world.getFluid(pos).is(TagsFluid.WATER);
        } else {
            return false;
        }
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.DOLPHIN_HURT;
    }

    @Nullable
    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.DOLPHIN_DEATH;
    }

    @Nullable
    @Override
    protected SoundEffect getSoundAmbient() {
        return this.isInWater() ? SoundEffects.DOLPHIN_AMBIENT_WATER : SoundEffects.DOLPHIN_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundSplash() {
        return SoundEffects.DOLPHIN_SPLASH;
    }

    @Override
    protected SoundEffect getSoundSwim() {
        return SoundEffects.DOLPHIN_SWIM;
    }

    protected boolean closeToNextPos() {
        BlockPosition blockPos = this.getNavigation().getTargetPos();
        return blockPos != null ? blockPos.closerThan(this.getPositionVector(), 12.0D) : false;
    }

    @Override
    public void travel(Vec3D movementInput) {
        if (this.doAITick() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), movementInput);
            this.move(EnumMoveType.SELF, this.getMot());
            this.setMot(this.getMot().scale(0.9D));
            if (this.getGoalTarget() == null) {
                this.setMot(this.getMot().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(movementInput);
        }

    }

    @Override
    public boolean canBeLeashed(EntityHuman player) {
        return true;
    }

    static class DolphinSwimToTreasureGoal extends PathfinderGoal {
        private final EntityDolphin dolphin;
        private boolean stuck;

        DolphinSwimToTreasureGoal(EntityDolphin dolphin) {
            this.dolphin = dolphin;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public boolean canUse() {
            return this.dolphin.gotFish() && this.dolphin.getAirTicks() >= 100;
        }

        @Override
        public boolean canContinueToUse() {
            BlockPosition blockPos = this.dolphin.getTreasurePos();
            return !(new BlockPosition((double)blockPos.getX(), this.dolphin.locY(), (double)blockPos.getZ())).closerThan(this.dolphin.getPositionVector(), 4.0D) && !this.stuck && this.dolphin.getAirTicks() >= 100;
        }

        @Override
        public void start() {
            if (this.dolphin.level instanceof WorldServer) {
                WorldServer serverLevel = (WorldServer)this.dolphin.level;
                this.stuck = false;
                this.dolphin.getNavigation().stop();
                BlockPosition blockPos = this.dolphin.getChunkCoordinates();
                StructureGenerator<?> structureFeature = (double)serverLevel.random.nextFloat() >= 0.5D ? StructureGenerator.OCEAN_RUIN : StructureGenerator.SHIPWRECK;
                BlockPosition blockPos2 = serverLevel.findNearestMapFeature(structureFeature, blockPos, 50, false);
                if (blockPos2 == null) {
                    StructureGenerator<?> structureFeature2 = structureFeature.equals(StructureGenerator.OCEAN_RUIN) ? StructureGenerator.SHIPWRECK : StructureGenerator.OCEAN_RUIN;
                    BlockPosition blockPos3 = serverLevel.findNearestMapFeature(structureFeature2, blockPos, 50, false);
                    if (blockPos3 == null) {
                        this.stuck = true;
                        return;
                    }

                    this.dolphin.setTreasurePos(blockPos3);
                } else {
                    this.dolphin.setTreasurePos(blockPos2);
                }

                serverLevel.broadcastEntityEffect(this.dolphin, (byte)38);
            }
        }

        @Override
        public void stop() {
            BlockPosition blockPos = this.dolphin.getTreasurePos();
            if ((new BlockPosition((double)blockPos.getX(), this.dolphin.locY(), (double)blockPos.getZ())).closerThan(this.dolphin.getPositionVector(), 4.0D) || this.stuck) {
                this.dolphin.setGotFish(false);
            }

        }

        @Override
        public void tick() {
            World level = this.dolphin.level;
            if (this.dolphin.closeToNextPos() || this.dolphin.getNavigation().isDone()) {
                Vec3D vec3 = Vec3D.atCenterOf(this.dolphin.getTreasurePos());
                Vec3D vec32 = DefaultRandomPos.getPosTowards(this.dolphin, 16, 1, vec3, (double)((float)Math.PI / 8F));
                if (vec32 == null) {
                    vec32 = DefaultRandomPos.getPosTowards(this.dolphin, 8, 4, vec3, (double)((float)Math.PI / 2F));
                }

                if (vec32 != null) {
                    BlockPosition blockPos = new BlockPosition(vec32);
                    if (!level.getFluid(blockPos).is(TagsFluid.WATER) || !level.getType(blockPos).isPathfindable(level, blockPos, PathMode.WATER)) {
                        vec32 = DefaultRandomPos.getPosTowards(this.dolphin, 8, 5, vec3, (double)((float)Math.PI / 2F));
                    }
                }

                if (vec32 == null) {
                    this.stuck = true;
                    return;
                }

                this.dolphin.getControllerLook().setLookAt(vec32.x, vec32.y, vec32.z, (float)(this.dolphin.getMaxHeadYRot() + 20), (float)this.dolphin.getMaxHeadXRot());
                this.dolphin.getNavigation().moveTo(vec32.x, vec32.y, vec32.z, 1.3D);
                if (level.random.nextInt(80) == 0) {
                    level.broadcastEntityEffect(this.dolphin, (byte)38);
                }
            }

        }
    }

    static class DolphinSwimWithPlayerGoal extends PathfinderGoal {
        private final EntityDolphin dolphin;
        private final double speedModifier;
        private EntityHuman player;

        DolphinSwimWithPlayerGoal(EntityDolphin dolphin, double speed) {
            this.dolphin = dolphin;
            this.speedModifier = speed;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean canUse() {
            this.player = this.dolphin.level.getNearestPlayer(EntityDolphin.SWIM_WITH_PLAYER_TARGETING, this.dolphin);
            if (this.player == null) {
                return false;
            } else {
                return this.player.isSwimming() && this.dolphin.getGoalTarget() != this.player;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.player != null && this.player.isSwimming() && this.dolphin.distanceToSqr(this.player) < 256.0D;
        }

        @Override
        public void start() {
            this.player.addEffect(new MobEffect(MobEffectList.DOLPHINS_GRACE, 100), this.dolphin);
        }

        @Override
        public void stop() {
            this.player = null;
            this.dolphin.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.dolphin.getControllerLook().setLookAt(this.player, (float)(this.dolphin.getMaxHeadYRot() + 20), (float)this.dolphin.getMaxHeadXRot());
            if (this.dolphin.distanceToSqr(this.player) < 6.25D) {
                this.dolphin.getNavigation().stop();
            } else {
                this.dolphin.getNavigation().moveTo(this.player, this.speedModifier);
            }

            if (this.player.isSwimming() && this.player.level.random.nextInt(6) == 0) {
                this.player.addEffect(new MobEffect(MobEffectList.DOLPHINS_GRACE, 100), this.dolphin);
            }

        }
    }

    class PlayWithItemsGoal extends PathfinderGoal {
        private int cooldown;

        @Override
        public boolean canUse() {
            if (this.cooldown > EntityDolphin.this.tickCount) {
                return false;
            } else {
                List<EntityItem> list = EntityDolphin.this.level.getEntitiesOfClass(EntityItem.class, EntityDolphin.this.getBoundingBox().grow(8.0D, 8.0D, 8.0D), EntityDolphin.ALLOWED_ITEMS);
                return !list.isEmpty() || !EntityDolphin.this.getEquipment(EnumItemSlot.MAINHAND).isEmpty();
            }
        }

        @Override
        public void start() {
            List<EntityItem> list = EntityDolphin.this.level.getEntitiesOfClass(EntityItem.class, EntityDolphin.this.getBoundingBox().grow(8.0D, 8.0D, 8.0D), EntityDolphin.ALLOWED_ITEMS);
            if (!list.isEmpty()) {
                EntityDolphin.this.getNavigation().moveTo(list.get(0), (double)1.2F);
                EntityDolphin.this.playSound(SoundEffects.DOLPHIN_PLAY, 1.0F, 1.0F);
            }

            this.cooldown = 0;
        }

        @Override
        public void stop() {
            ItemStack itemStack = EntityDolphin.this.getEquipment(EnumItemSlot.MAINHAND);
            if (!itemStack.isEmpty()) {
                this.drop(itemStack);
                EntityDolphin.this.setSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
                this.cooldown = EntityDolphin.this.tickCount + EntityDolphin.this.random.nextInt(100);
            }

        }

        @Override
        public void tick() {
            List<EntityItem> list = EntityDolphin.this.level.getEntitiesOfClass(EntityItem.class, EntityDolphin.this.getBoundingBox().grow(8.0D, 8.0D, 8.0D), EntityDolphin.ALLOWED_ITEMS);
            ItemStack itemStack = EntityDolphin.this.getEquipment(EnumItemSlot.MAINHAND);
            if (!itemStack.isEmpty()) {
                this.drop(itemStack);
                EntityDolphin.this.setSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
            } else if (!list.isEmpty()) {
                EntityDolphin.this.getNavigation().moveTo(list.get(0), (double)1.2F);
            }

        }

        private void drop(ItemStack stack) {
            if (!stack.isEmpty()) {
                double d = EntityDolphin.this.getHeadY() - (double)0.3F;
                EntityItem itemEntity = new EntityItem(EntityDolphin.this.level, EntityDolphin.this.locX(), d, EntityDolphin.this.locZ(), stack);
                itemEntity.setPickupDelay(40);
                itemEntity.setThrower(EntityDolphin.this.getUniqueID());
                float f = 0.3F;
                float g = EntityDolphin.this.random.nextFloat() * ((float)Math.PI * 2F);
                float h = 0.02F * EntityDolphin.this.random.nextFloat();
                itemEntity.setMot((double)(0.3F * -MathHelper.sin(EntityDolphin.this.getYRot() * ((float)Math.PI / 180F)) * MathHelper.cos(EntityDolphin.this.getXRot() * ((float)Math.PI / 180F)) + MathHelper.cos(g) * h), (double)(0.3F * MathHelper.sin(EntityDolphin.this.getXRot() * ((float)Math.PI / 180F)) * 1.5F), (double)(0.3F * MathHelper.cos(EntityDolphin.this.getYRot() * ((float)Math.PI / 180F)) * MathHelper.cos(EntityDolphin.this.getXRot() * ((float)Math.PI / 180F)) + MathHelper.sin(g) * h));
                EntityDolphin.this.level.addEntity(itemEntity);
            }
        }
    }
}
