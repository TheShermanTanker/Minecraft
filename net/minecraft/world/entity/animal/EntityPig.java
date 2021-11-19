package net.minecraft.world.entity.animal;

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
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.ISaddleable;
import net.minecraft.world.entity.ISteerable;
import net.minecraft.world.entity.SaddleStorage;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowParent;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTempt;
import net.minecraft.world.entity.monster.EntityPigZombie;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.vehicle.DismountUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class EntityPig extends EntityAnimal implements ISteerable, ISaddleable {
    private static final DataWatcherObject<Boolean> DATA_SADDLE_ID = DataWatcher.defineId(EntityPig.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Integer> DATA_BOOST_TIME = DataWatcher.defineId(EntityPig.class, DataWatcherRegistry.INT);
    private static final RecipeItemStack FOOD_ITEMS = RecipeItemStack.of(Items.CARROT, Items.POTATO, Items.BEETROOT);
    public final SaddleStorage steering = new SaddleStorage(this.entityData, DATA_BOOST_TIME, DATA_SADDLE_ID);

    public EntityPig(EntityTypes<? extends EntityPig> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new PathfinderGoalPanic(this, 1.25D));
        this.goalSelector.addGoal(3, new PathfinderGoalBreed(this, 1.0D));
        this.goalSelector.addGoal(4, new PathfinderGoalTempt(this, 1.2D, RecipeItemStack.of(Items.CARROT_ON_A_STICK), false));
        this.goalSelector.addGoal(4, new PathfinderGoalTempt(this, 1.2D, FOOD_ITEMS, false));
        this.goalSelector.addGoal(5, new PathfinderGoalFollowParent(this, 1.1D));
        this.goalSelector.addGoal(6, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.goalSelector.addGoal(7, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomLookaround(this));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 10.0D).add(GenericAttributes.MOVEMENT_SPEED, 0.25D);
    }

    @Nullable
    @Override
    public Entity getRidingPassenger() {
        return this.getFirstPassenger();
    }

    @Override
    public boolean canBeControlledByRider() {
        Entity entity = this.getRidingPassenger();
        if (!(entity instanceof EntityHuman)) {
            return false;
        } else {
            EntityHuman player = (EntityHuman)entity;
            return player.getItemInMainHand().is(Items.CARROT_ON_A_STICK) || player.getItemInOffHand().is(Items.CARROT_ON_A_STICK);
        }
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
        this.entityData.register(DATA_SADDLE_ID, false);
        this.entityData.register(DATA_BOOST_TIME, 0);
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
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.PIG_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.PIG_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.PIG_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.PIG_STEP, 0.15F, 1.0F);
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
                return interactionResult;
            }
        }
    }

    @Override
    public boolean canSaddle() {
        return this.isAlive() && !this.isBaby();
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.hasSaddle()) {
            this.spawnAtLocation(Items.SADDLE);
        }

    }

    @Override
    public boolean hasSaddle() {
        return this.steering.hasSaddle();
    }

    @Override
    public void saddle(@Nullable EnumSoundCategory sound) {
        this.steering.setSaddle(true);
        if (sound != null) {
            this.level.playSound((EntityHuman)null, this, SoundEffects.PIG_SADDLE, sound, 0.5F, 1.0F);
        }

    }

    @Override
    public Vec3D getDismountLocationForPassenger(EntityLiving passenger) {
        EnumDirection direction = this.getAdjustedDirection();
        if (direction.getAxis() == EnumDirection.EnumAxis.Y) {
            return super.getDismountLocationForPassenger(passenger);
        } else {
            int[][] is = DismountUtil.offsetsForDirection(direction);
            BlockPosition blockPos = this.getChunkCoordinates();
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(EntityPose pose : passenger.getDismountPoses()) {
                AxisAlignedBB aABB = passenger.getLocalBoundsForPose(pose);

                for(int[] js : is) {
                    mutableBlockPos.set(blockPos.getX() + js[0], blockPos.getY(), blockPos.getZ() + js[1]);
                    double d = this.level.getBlockFloorHeight(mutableBlockPos);
                    if (DismountUtil.isBlockFloorValid(d)) {
                        Vec3D vec3 = Vec3D.upFromBottomCenterOf(mutableBlockPos, d);
                        if (DismountUtil.canDismountTo(this.level, passenger, aABB.move(vec3))) {
                            passenger.setPose(pose);
                            return vec3;
                        }
                    }
                }
            }

            return super.getDismountLocationForPassenger(passenger);
        }
    }

    @Override
    public void onLightningStrike(WorldServer world, EntityLightning lightning) {
        if (world.getDifficulty() != EnumDifficulty.PEACEFUL) {
            EntityPigZombie zombifiedPiglin = EntityTypes.ZOMBIFIED_PIGLIN.create(world);
            zombifiedPiglin.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            zombifiedPiglin.setPositionRotation(this.locX(), this.locY(), this.locZ(), this.getYRot(), this.getXRot());
            zombifiedPiglin.setNoAI(this.isNoAI());
            zombifiedPiglin.setBaby(this.isBaby());
            if (this.hasCustomName()) {
                zombifiedPiglin.setCustomName(this.getCustomName());
                zombifiedPiglin.setCustomNameVisible(this.getCustomNameVisible());
            }

            zombifiedPiglin.setPersistent();
            world.addEntity(zombifiedPiglin);
            this.die();
        } else {
            super.onLightningStrike(world, lightning);
        }

    }

    @Override
    public void travel(Vec3D movementInput) {
        this.travel(this, this.steering, movementInput);
    }

    @Override
    public float getSteeringSpeed() {
        return (float)this.getAttributeValue(GenericAttributes.MOVEMENT_SPEED) * 0.225F;
    }

    @Override
    public void travelWithInput(Vec3D movementInput) {
        super.travel(movementInput);
    }

    @Override
    public boolean boost() {
        return this.steering.boost(this.getRandom());
    }

    @Override
    public EntityPig getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        return EntityTypes.PIG.create(serverLevel);
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return FOOD_ITEMS.test(stack);
    }

    @Override
    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double)(0.6F * this.getHeadHeight()), (double)(this.getWidth() * 0.4F));
    }
}
