package net.minecraft.world.entity.monster;

import com.google.common.collect.Sets;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ISaddleable;
import net.minecraft.world.entity.ISteerable;
import net.minecraft.world.entity.SaddleStorage;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowParent;
import net.minecraft.world.entity.ai.goal.PathfinderGoalGotoTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTempt;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.vehicle.DismountUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.BlockFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.Pathfinder;
import net.minecraft.world.level.pathfinder.PathfinderNormal;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class EntityStrider extends EntityAnimal implements ISteerable, ISaddleable {
    private static final float SUFFOCATE_STEERING_MODIFIER = 0.23F;
    private static final float SUFFOCATE_SPEED_MODIFIER = 0.66F;
    private static final float STEERING_MODIFIER = 0.55F;
    private static final RecipeItemStack FOOD_ITEMS = RecipeItemStack.of(Items.WARPED_FUNGUS);
    private static final RecipeItemStack TEMPT_ITEMS = RecipeItemStack.of(Items.WARPED_FUNGUS, Items.WARPED_FUNGUS_ON_A_STICK);
    private static final DataWatcherObject<Integer> DATA_BOOST_TIME = DataWatcher.defineId(EntityStrider.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Boolean> DATA_SUFFOCATING = DataWatcher.defineId(EntityStrider.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> DATA_SADDLE_ID = DataWatcher.defineId(EntityStrider.class, DataWatcherRegistry.BOOLEAN);
    public final SaddleStorage steering = new SaddleStorage(this.entityData, DATA_BOOST_TIME, DATA_SADDLE_ID);
    private PathfinderGoalTempt temptGoal;
    private PathfinderGoalPanic panicGoal;

    public EntityStrider(EntityTypes<? extends EntityStrider> type, World world) {
        super(type, world);
        this.blocksBuilding = true;
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.LAVA, 0.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);
    }

    public static boolean checkStriderSpawnRules(EntityTypes<EntityStrider> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();

        do {
            mutableBlockPos.move(EnumDirection.UP);
        } while(world.getFluid(mutableBlockPos).is(TagsFluid.LAVA));

        return world.getType(mutableBlockPos).isAir();
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (DATA_BOOST_TIME.equals(data) && this.level.isClientSide) {
            this.steering.onSynced();
        }

        super.onSyncedDataUpdated(data);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_BOOST_TIME, 0);
        this.entityData.register(DATA_SUFFOCATING, false);
        this.entityData.register(DATA_SADDLE_ID, false);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        this.steering.addAdditionalSaveData(nbt);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.steering.readAdditionalSaveData(nbt);
    }

    @Override
    public boolean hasSaddle() {
        return this.steering.hasSaddle();
    }

    @Override
    public boolean canSaddle() {
        return this.isAlive() && !this.isBaby();
    }

    @Override
    public void saddle(@Nullable EnumSoundCategory sound) {
        this.steering.setSaddle(true);
        if (sound != null) {
            this.level.playSound((EntityHuman)null, this, SoundEffects.STRIDER_SADDLE, sound, 0.5F, 1.0F);
        }

    }

    @Override
    protected void initPathfinder() {
        this.panicGoal = new PathfinderGoalPanic(this, 1.65D);
        this.goalSelector.addGoal(1, this.panicGoal);
        this.goalSelector.addGoal(2, new PathfinderGoalBreed(this, 1.0D));
        this.temptGoal = new PathfinderGoalTempt(this, 1.4D, TEMPT_ITEMS, false);
        this.goalSelector.addGoal(3, this.temptGoal);
        this.goalSelector.addGoal(4, new EntityStrider.PathfinderGoalStriderFindLava(this, 1.5D));
        this.goalSelector.addGoal(5, new PathfinderGoalFollowParent(this, 1.1D));
        this.goalSelector.addGoal(7, new PathfinderGoalRandomStroll(this, 1.0D, 60));
        this.goalSelector.addGoal(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomLookaround(this));
        this.goalSelector.addGoal(9, new PathfinderGoalLookAtPlayer(this, EntityStrider.class, 8.0F));
    }

    public void setShivering(boolean cold) {
        this.entityData.set(DATA_SUFFOCATING, cold);
    }

    public boolean isShivering() {
        return this.getVehicle() instanceof EntityStrider ? ((EntityStrider)this.getVehicle()).isShivering() : this.entityData.get(DATA_SUFFOCATING);
    }

    @Override
    public boolean canStandOnFluid(FluidType fluid) {
        return fluid.is(TagsFluid.LAVA);
    }

    @Override
    public double getPassengersRidingOffset() {
        float f = Math.min(0.25F, this.animationSpeed);
        float g = this.animationPosition;
        return (double)this.getHeight() - 0.19D + (double)(0.12F * MathHelper.cos(g * 1.5F) * 2.0F * f);
    }

    @Override
    public boolean canBeControlledByRider() {
        Entity entity = this.getRidingPassenger();
        if (!(entity instanceof EntityHuman)) {
            return false;
        } else {
            EntityHuman player = (EntityHuman)entity;
            return player.getItemInMainHand().is(Items.WARPED_FUNGUS_ON_A_STICK) || player.getItemInOffHand().is(Items.WARPED_FUNGUS_ON_A_STICK);
        }
    }

    @Override
    public boolean checkSpawnObstruction(IWorldReader world) {
        return world.isUnobstructed(this);
    }

    @Nullable
    @Override
    public Entity getRidingPassenger() {
        return this.getFirstPassenger();
    }

    @Override
    public Vec3D getDismountLocationForPassenger(EntityLiving passenger) {
        Vec3D[] vec3s = new Vec3D[]{getCollisionHorizontalEscapeVector((double)this.getWidth(), (double)passenger.getWidth(), passenger.getYRot()), getCollisionHorizontalEscapeVector((double)this.getWidth(), (double)passenger.getWidth(), passenger.getYRot() - 22.5F), getCollisionHorizontalEscapeVector((double)this.getWidth(), (double)passenger.getWidth(), passenger.getYRot() + 22.5F), getCollisionHorizontalEscapeVector((double)this.getWidth(), (double)passenger.getWidth(), passenger.getYRot() - 45.0F), getCollisionHorizontalEscapeVector((double)this.getWidth(), (double)passenger.getWidth(), passenger.getYRot() + 45.0F)};
        Set<BlockPosition> set = Sets.newLinkedHashSet();
        double d = this.getBoundingBox().maxY;
        double e = this.getBoundingBox().minY - 0.5D;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(Vec3D vec3 : vec3s) {
            mutableBlockPos.set(this.locX() + vec3.x, d, this.locZ() + vec3.z);

            for(double f = d; f > e; --f) {
                set.add(mutableBlockPos.immutableCopy());
                mutableBlockPos.move(EnumDirection.DOWN);
            }
        }

        for(BlockPosition blockPos : set) {
            if (!this.level.getFluid(blockPos).is(TagsFluid.LAVA)) {
                double g = this.level.getBlockFloorHeight(blockPos);
                if (DismountUtil.isBlockFloorValid(g)) {
                    Vec3D vec32 = Vec3D.upFromBottomCenterOf(blockPos, g);

                    for(EntityPose pose : passenger.getDismountPoses()) {
                        AxisAlignedBB aABB = passenger.getLocalBoundsForPose(pose);
                        if (DismountUtil.canDismountTo(this.level, passenger, aABB.move(vec32))) {
                            passenger.setPose(pose);
                            return vec32;
                        }
                    }
                }
            }
        }

        return new Vec3D(this.locX(), this.getBoundingBox().maxY, this.locZ());
    }

    @Override
    public void travel(Vec3D movementInput) {
        this.setSpeed(this.getMoveSpeed());
        this.travel(this, this.steering, movementInput);
    }

    public float getMoveSpeed() {
        return (float)this.getAttributeValue(GenericAttributes.MOVEMENT_SPEED) * (this.isShivering() ? 0.66F : 1.0F);
    }

    @Override
    public float getSteeringSpeed() {
        return (float)this.getAttributeValue(GenericAttributes.MOVEMENT_SPEED) * (this.isShivering() ? 0.23F : 0.55F);
    }

    @Override
    public void travelWithInput(Vec3D movementInput) {
        super.travel(movementInput);
    }

    @Override
    protected float nextStep() {
        return this.moveDist + 0.6F;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(this.isInLava() ? SoundEffects.STRIDER_STEP_LAVA : SoundEffects.STRIDER_STEP, 1.0F, 1.0F);
    }

    @Override
    public boolean boost() {
        return this.steering.boost(this.getRandom());
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, IBlockData landedState, BlockPosition landedPosition) {
        this.checkBlockCollisions();
        if (this.isInLava()) {
            this.fallDistance = 0.0F;
        } else {
            super.checkFallDamage(heightDifference, onGround, landedState, landedPosition);
        }
    }

    @Override
    public void tick() {
        if (this.isBeingTempted() && this.random.nextInt(140) == 0) {
            this.playSound(SoundEffects.STRIDER_HAPPY, 1.0F, this.getVoicePitch());
        } else if (this.isPanicking() && this.random.nextInt(60) == 0) {
            this.playSound(SoundEffects.STRIDER_RETREAT, 1.0F, this.getVoicePitch());
        }

        IBlockData blockState = this.level.getType(this.getChunkCoordinates());
        IBlockData blockState2 = this.getBlockStateOn();
        boolean bl = blockState.is(TagsBlock.STRIDER_WARM_BLOCKS) || blockState2.is(TagsBlock.STRIDER_WARM_BLOCKS) || this.getFluidHeight(TagsFluid.LAVA) > 0.0D;
        this.setShivering(!bl);
        super.tick();
        this.floatStrider();
        this.checkBlockCollisions();
    }

    private boolean isPanicking() {
        return this.panicGoal != null && this.panicGoal.isRunning();
    }

    private boolean isBeingTempted() {
        return this.temptGoal != null && this.temptGoal.isRunning();
    }

    @Override
    protected boolean shouldPassengersInheritMalus() {
        return true;
    }

    private void floatStrider() {
        if (this.isInLava()) {
            VoxelShapeCollision collisionContext = VoxelShapeCollision.of(this);
            if (collisionContext.isAbove(BlockFluids.STABLE_SHAPE, this.getChunkCoordinates(), true) && !this.level.getFluid(this.getChunkCoordinates().above()).is(TagsFluid.LAVA)) {
                this.onGround = true;
            } else {
                this.setMot(this.getMot().scale(0.5D).add(0.0D, 0.05D, 0.0D));
            }
        }

    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MOVEMENT_SPEED, (double)0.175F).add(GenericAttributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return !this.isPanicking() && !this.isBeingTempted() ? SoundEffects.STRIDER_AMBIENT : null;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.STRIDER_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.STRIDER_DEATH;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.isVehicle() && !this.isEyeInFluid(TagsFluid.LAVA);
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public boolean isBurning() {
        return false;
    }

    @Override
    protected NavigationAbstract createNavigation(World world) {
        return new EntityStrider.NavigationStrider(this, world);
    }

    @Override
    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        if (world.getType(pos).getFluid().is(TagsFluid.LAVA)) {
            return 10.0F;
        } else {
            return this.isInLava() ? Float.NEGATIVE_INFINITY : 0.0F;
        }
    }

    @Override
    public EntityStrider getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        return EntityTypes.STRIDER.create(serverLevel);
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return FOOD_ITEMS.test(stack);
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.hasSaddle()) {
            this.spawnAtLocation(Items.SADDLE);
        }

    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        boolean bl = this.isBreedItem(player.getItemInHand(hand));
        if (!bl && this.hasSaddle() && !this.isVehicle() && !player.isSecondaryUseActive()) {
            if (!this.level.isClientSide) {
                player.startRiding(this);
            }

            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            EnumInteractionResult interactionResult = super.mobInteract(player, hand);
            if (!interactionResult.consumesAction()) {
                ItemStack itemStack = player.getItemInHand(hand);
                return itemStack.is(Items.SADDLE) ? itemStack.interactLivingEntity(player, this, hand) : EnumInteractionResult.PASS;
            } else {
                if (bl && !this.isSilent()) {
                    this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.STRIDER_EAT, this.getSoundCategory(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
                }

                return interactionResult;
            }
        }
    }

    @Override
    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double)(0.6F * this.getHeadHeight()), (double)(this.getWidth() * 0.4F));
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        if (this.isBaby()) {
            return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
        } else {
            Object var7;
            if (this.random.nextInt(30) == 0) {
                EntityInsentient mob = EntityTypes.ZOMBIFIED_PIGLIN.create(world.getLevel());
                var7 = this.spawnJockey(world, difficulty, mob, new EntityZombie.GroupDataZombie(EntityZombie.getSpawnAsBabyOdds(this.random), false));
                mob.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.WARPED_FUNGUS_ON_A_STICK));
                this.saddle((EnumSoundCategory)null);
            } else if (this.random.nextInt(10) == 0) {
                EntityAgeable ageableMob = EntityTypes.STRIDER.create(world.getLevel());
                ageableMob.setAgeRaw(-24000);
                var7 = this.spawnJockey(world, difficulty, ageableMob, (GroupDataEntity)null);
            } else {
                var7 = new EntityAgeable.GroupDataAgeable(0.5F);
            }

            return super.prepare(world, difficulty, spawnReason, (GroupDataEntity)var7, entityNbt);
        }
    }

    private GroupDataEntity spawnJockey(WorldAccess world, DifficultyDamageScaler difficulty, EntityInsentient rider, @Nullable GroupDataEntity entityData) {
        rider.setPositionRotation(this.locX(), this.locY(), this.locZ(), this.getYRot(), 0.0F);
        rider.prepare(world, difficulty, EnumMobSpawn.JOCKEY, entityData, (NBTTagCompound)null);
        rider.startRiding(this, true);
        return new EntityAgeable.GroupDataAgeable(0.0F);
    }

    static class NavigationStrider extends Navigation {
        NavigationStrider(EntityStrider entity, World world) {
            super(entity, world);
        }

        @Override
        protected Pathfinder createPathFinder(int range) {
            this.nodeEvaluator = new PathfinderNormal();
            return new Pathfinder(this.nodeEvaluator, range);
        }

        @Override
        protected boolean hasValidPathType(PathType pathType) {
            return pathType != PathType.LAVA && pathType != PathType.DAMAGE_FIRE && pathType != PathType.DANGER_FIRE ? super.hasValidPathType(pathType) : true;
        }

        @Override
        public boolean isStableDestination(BlockPosition pos) {
            return this.level.getType(pos).is(Blocks.LAVA) || super.isStableDestination(pos);
        }
    }

    static class PathfinderGoalStriderFindLava extends PathfinderGoalGotoTarget {
        private final EntityStrider strider;

        PathfinderGoalStriderFindLava(EntityStrider mob, double speed) {
            super(mob, speed, 8, 2);
            this.strider = mob;
        }

        @Override
        public BlockPosition getMoveToTarget() {
            return this.blockPos;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.strider.isInLava() && this.isValidTarget(this.strider.level, this.blockPos);
        }

        @Override
        public boolean canUse() {
            return !this.strider.isInLava() && super.canUse();
        }

        @Override
        public boolean shouldRecalculatePath() {
            return this.tryTicks % 20 == 0;
        }

        @Override
        protected boolean isValidTarget(IWorldReader world, BlockPosition pos) {
            return world.getType(pos).is(Blocks.LAVA) && world.getType(pos.above()).isPathfindable(world, pos, PathMode.LAND);
        }
    }
}
