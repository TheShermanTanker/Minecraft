package net.minecraft.world.entity.animal;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomSwim;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationGuardian;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

public abstract class EntityFish extends EntityWaterAnimal implements IBucketable {
    private static final DataWatcherObject<Boolean> FROM_BUCKET = DataWatcher.defineId(EntityFish.class, DataWatcherRegistry.BOOLEAN);

    public EntityFish(EntityTypes<? extends EntityFish> type, World world) {
        super(type, world);
        this.moveControl = new EntityFish.ControllerMoveFish(this);
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height * 0.65F;
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 3.0D);
    }

    @Override
    public boolean isSpecialPersistence() {
        return super.isSpecialPersistence() || this.isFromBucket();
    }

    public static boolean checkFishSpawnRules(EntityTypes<? extends EntityFish> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return world.getType(pos).is(Blocks.WATER) && world.getType(pos.above()).is(Blocks.WATER);
    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    @Override
    public int getMaxSpawnGroup() {
        return 8;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(FROM_BUCKET, false);
    }

    @Override
    public boolean isFromBucket() {
        return this.entityData.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        this.entityData.set(FROM_BUCKET, fromBucket);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setBoolean("FromBucket", this.isFromBucket());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setFromBucket(nbt.getBoolean("FromBucket"));
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(0, new PathfinderGoalPanic(this, 1.25D));
        this.goalSelector.addGoal(2, new PathfinderGoalAvoidTarget<>(this, EntityHuman.class, 8.0F, 1.6D, 1.4D, IEntitySelector.NO_SPECTATORS::test));
        this.goalSelector.addGoal(4, new EntityFish.PathfinderGoalFishSwim(this));
    }

    @Override
    protected NavigationAbstract createNavigation(World world) {
        return new NavigationGuardian(this, world);
    }

    @Override
    public void travel(Vec3D movementInput) {
        if (this.doAITick() && this.isInWater()) {
            this.moveRelative(0.01F, movementInput);
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
    public void movementTick() {
        if (!this.isInWater() && this.onGround && this.verticalCollision) {
            this.setMot(this.getMot().add((double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.05F), (double)0.4F, (double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.05F)));
            this.onGround = false;
            this.hasImpulse = true;
            this.playSound(this.getSoundFlop(), this.getSoundVolume(), this.getVoicePitch());
        }

        super.movementTick();
    }

    @Override
    protected EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        return IBucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
    }

    @Override
    public void setBucketName(ItemStack stack) {
        IBucketable.saveDefaultDataToBucketTag(this, stack);
    }

    @Override
    public void loadFromBucketTag(NBTTagCompound nbt) {
        IBucketable.loadDefaultDataFromBucketTag(this, nbt);
    }

    @Override
    public SoundEffect getPickupSound() {
        return SoundEffects.BUCKET_FILL_FISH;
    }

    protected boolean canRandomSwim() {
        return true;
    }

    protected abstract SoundEffect getSoundFlop();

    @Override
    protected SoundEffect getSoundSwim() {
        return SoundEffects.FISH_SWIM;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
    }

    static class ControllerMoveFish extends ControllerMove {
        private final EntityFish fish;

        ControllerMoveFish(EntityFish owner) {
            super(owner);
            this.fish = owner;
        }

        @Override
        public void tick() {
            if (this.fish.isEyeInFluid(TagsFluid.WATER)) {
                this.fish.setMot(this.fish.getMot().add(0.0D, 0.005D, 0.0D));
            }

            if (this.operation == ControllerMove.Operation.MOVE_TO && !this.fish.getNavigation().isDone()) {
                float f = (float)(this.speedModifier * this.fish.getAttributeValue(GenericAttributes.MOVEMENT_SPEED));
                this.fish.setSpeed(MathHelper.lerp(0.125F, this.fish.getSpeed(), f));
                double d = this.wantedX - this.fish.locX();
                double e = this.wantedY - this.fish.locY();
                double g = this.wantedZ - this.fish.locZ();
                if (e != 0.0D) {
                    double h = Math.sqrt(d * d + e * e + g * g);
                    this.fish.setMot(this.fish.getMot().add(0.0D, (double)this.fish.getSpeed() * (e / h) * 0.1D, 0.0D));
                }

                if (d != 0.0D || g != 0.0D) {
                    float i = (float)(MathHelper.atan2(g, d) * (double)(180F / (float)Math.PI)) - 90.0F;
                    this.fish.setYRot(this.rotlerp(this.fish.getYRot(), i, 90.0F));
                    this.fish.yBodyRot = this.fish.getYRot();
                }

            } else {
                this.fish.setSpeed(0.0F);
            }
        }
    }

    static class PathfinderGoalFishSwim extends PathfinderGoalRandomSwim {
        private final EntityFish fish;

        public PathfinderGoalFishSwim(EntityFish fish) {
            super(fish, 1.0D, 40);
            this.fish = fish;
        }

        @Override
        public boolean canUse() {
            return this.fish.canRandomSwim() && super.canUse();
        }
    }
}
