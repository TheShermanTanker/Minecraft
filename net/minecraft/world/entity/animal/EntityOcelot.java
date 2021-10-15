package net.minecraft.world.entity.animal;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
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
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLeapAtTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalOcelotAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTempt;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

public class EntityOcelot extends EntityAnimal {
    public static final double CROUCH_SPEED_MOD = 0.6D;
    public static final double WALK_SPEED_MOD = 0.8D;
    public static final double SPRINT_SPEED_MOD = 1.33D;
    private static final RecipeItemStack TEMPT_INGREDIENT = RecipeItemStack.of(Items.COD, Items.SALMON);
    private static final DataWatcherObject<Boolean> DATA_TRUSTING = DataWatcher.defineId(EntityOcelot.class, DataWatcherRegistry.BOOLEAN);
    private EntityOcelot.OcelotAvoidEntityGoal<EntityHuman> ocelotAvoidPlayersGoal;
    private EntityOcelot.OcelotTemptGoal temptGoal;

    public EntityOcelot(EntityTypes<? extends EntityOcelot> type, World world) {
        super(type, world);
        this.reassessTrustingGoals();
    }

    public boolean isTrusting() {
        return this.entityData.get(DATA_TRUSTING);
    }

    public void setTrusting(boolean trusting) {
        this.entityData.set(DATA_TRUSTING, trusting);
        this.reassessTrustingGoals();
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setBoolean("Trusting", this.isTrusting());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setTrusting(nbt.getBoolean("Trusting"));
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_TRUSTING, false);
    }

    @Override
    protected void initPathfinder() {
        this.temptGoal = new EntityOcelot.OcelotTemptGoal(this, 0.6D, TEMPT_INGREDIENT, true);
        this.goalSelector.addGoal(1, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(3, this.temptGoal);
        this.goalSelector.addGoal(7, new PathfinderGoalLeapAtTarget(this, 0.3F));
        this.goalSelector.addGoal(8, new PathfinderGoalOcelotAttack(this));
        this.goalSelector.addGoal(9, new PathfinderGoalBreed(this, 0.8D));
        this.goalSelector.addGoal(10, new PathfinderGoalRandomStrollLand(this, 0.8D, 1.0000001E-5F));
        this.goalSelector.addGoal(11, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 10.0F));
        this.targetSelector.addGoal(1, new PathfinderGoalNearestAttackableTarget<>(this, EntityChicken.class, false));
        this.targetSelector.addGoal(1, new PathfinderGoalNearestAttackableTarget<>(this, EntityTurtle.class, 10, false, false, EntityTurtle.BABY_ON_LAND_SELECTOR));
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

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return !this.isTrusting() && this.tickCount > 2400;
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 10.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.3F).add(GenericAttributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Nullable
    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.OCELOT_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 900;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.OCELOT_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.OCELOT_DEATH;
    }

    private float getAttackDamage() {
        return (float)this.getAttributeValue(GenericAttributes.ATTACK_DAMAGE);
    }

    @Override
    public boolean attackEntity(Entity target) {
        return target.damageEntity(DamageSource.mobAttack(this), this.getAttackDamage());
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if ((this.temptGoal == null || this.temptGoal.isRunning()) && !this.isTrusting() && this.isBreedItem(itemStack) && player.distanceToSqr(this) < 9.0D) {
            this.usePlayerItem(player, hand, itemStack);
            if (!this.level.isClientSide) {
                if (this.random.nextInt(3) == 0) {
                    this.setTrusting(true);
                    this.spawnTrustingParticles(true);
                    this.level.broadcastEntityEffect(this, (byte)41);
                } else {
                    this.spawnTrustingParticles(false);
                    this.level.broadcastEntityEffect(this, (byte)40);
                }
            }

            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            return super.mobInteract(player, hand);
        }
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 41) {
            this.spawnTrustingParticles(true);
        } else if (status == 40) {
            this.spawnTrustingParticles(false);
        } else {
            super.handleEntityEvent(status);
        }

    }

    private void spawnTrustingParticles(boolean positive) {
        ParticleParam particleOptions = Particles.HEART;
        if (!positive) {
            particleOptions = Particles.SMOKE;
        }

        for(int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02D;
            double e = this.random.nextGaussian() * 0.02D;
            double f = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(particleOptions, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d, e, f);
        }

    }

    protected void reassessTrustingGoals() {
        if (this.ocelotAvoidPlayersGoal == null) {
            this.ocelotAvoidPlayersGoal = new EntityOcelot.OcelotAvoidEntityGoal<>(this, EntityHuman.class, 16.0F, 0.8D, 1.33D);
        }

        this.goalSelector.removeGoal(this.ocelotAvoidPlayersGoal);
        if (!this.isTrusting()) {
            this.goalSelector.addGoal(4, this.ocelotAvoidPlayersGoal);
        }

    }

    @Override
    public EntityOcelot getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        return EntityTypes.OCELOT.create(serverLevel);
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return TEMPT_INGREDIENT.test(stack);
    }

    public static boolean checkOcelotSpawnRules(EntityTypes<EntityOcelot> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return random.nextInt(3) != 0;
    }

    @Override
    public boolean checkSpawnObstruction(IWorldReader world) {
        if (world.isUnobstructed(this) && !world.containsLiquid(this.getBoundingBox())) {
            BlockPosition blockPos = this.getChunkCoordinates();
            if (blockPos.getY() < world.getSeaLevel()) {
                return false;
            }

            IBlockData blockState = world.getType(blockPos.below());
            if (blockState.is(Blocks.GRASS_BLOCK) || blockState.is(TagsBlock.LEAVES)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        if (entityData == null) {
            entityData = new EntityAgeable.GroupDataAgeable(1.0F);
        }

        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double)(0.5F * this.getHeadHeight()), (double)(this.getWidth() * 0.4F));
    }

    @Override
    public boolean isSteppingCarefully() {
        return this.getPose() == EntityPose.CROUCHING || super.isSteppingCarefully();
    }

    static class OcelotAvoidEntityGoal<T extends EntityLiving> extends PathfinderGoalAvoidTarget<T> {
        private final EntityOcelot ocelot;

        public OcelotAvoidEntityGoal(EntityOcelot ocelot, Class<T> fleeFromType, float distance, double slowSpeed, double fastSpeed) {
            super(ocelot, fleeFromType, distance, slowSpeed, fastSpeed, IEntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
            this.ocelot = ocelot;
        }

        @Override
        public boolean canUse() {
            return !this.ocelot.isTrusting() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !this.ocelot.isTrusting() && super.canContinueToUse();
        }
    }

    static class OcelotTemptGoal extends PathfinderGoalTempt {
        private final EntityOcelot ocelot;

        public OcelotTemptGoal(EntityOcelot ocelot, double speed, RecipeItemStack food, boolean canBeScared) {
            super(ocelot, speed, food, canBeScared);
            this.ocelot = ocelot;
        }

        @Override
        protected boolean canScare() {
            return super.canScare() && !this.ocelot.isTrusting();
        }
    }
}
