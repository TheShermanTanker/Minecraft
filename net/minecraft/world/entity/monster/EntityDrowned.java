package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalArrowAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalGotoTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.PathfinderGoalZombieAttack;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.navigation.NavigationGuardian;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.animal.EntityTurtle;
import net.minecraft.world.entity.animal.axolotl.EntityAxolotl;
import net.minecraft.world.entity.npc.EntityVillagerAbstract;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3D;

public class EntityDrowned extends EntityZombie implements IRangedEntity {
    public static final float NAUTILUS_SHELL_CHANCE = 0.03F;
    boolean searchingForLand;
    public final NavigationGuardian waterNavigation;
    public final Navigation groundNavigation;

    public EntityDrowned(EntityTypes<? extends EntityDrowned> type, World world) {
        super(type, world);
        this.maxUpStep = 1.0F;
        this.moveControl = new EntityDrowned.ControllerMoveDrowned(this);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.waterNavigation = new NavigationGuardian(this, world);
        this.groundNavigation = new Navigation(this, world);
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(1, new EntityDrowned.PathfinderGoalDrownedFindWater(this, 1.0D));
        this.goalSelector.addGoal(2, new EntityDrowned.PathfinderGoalDrownedTridentAttack(this, 1.0D, 40, 10.0F));
        this.goalSelector.addGoal(2, new EntityDrowned.PathfinderGoalDrownedNearestAttackableTarget(this, 1.0D, false));
        this.goalSelector.addGoal(5, new EntityDrowned.PathfinderGoalNearestBeach(this, 1.0D));
        this.goalSelector.addGoal(6, new EntityDrowned.PathfinderGoalDrownedSwim(this, 1.0D, this.level.getSeaLevel()));
        this.goalSelector.addGoal(7, new PathfinderGoalRandomStroll(this, 1.0D));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this, EntityDrowned.class)).setAlertOthers(EntityPigZombie.class));
        this.targetSelector.addGoal(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, 10, true, false, this::okTarget));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityVillagerAbstract.class, false));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityIronGolem.class, true));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityAxolotl.class, true, false));
        this.targetSelector.addGoal(5, new PathfinderGoalNearestAttackableTarget<>(this, EntityTurtle.class, 10, true, false, EntityTurtle.BABY_ON_LAND_SELECTOR));
    }

    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        entityData = super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
        if (this.getEquipment(EnumItemSlot.OFFHAND).isEmpty() && this.random.nextFloat() < 0.03F) {
            this.setSlot(EnumItemSlot.OFFHAND, new ItemStack(Items.NAUTILUS_SHELL));
            this.handDropChances[EnumItemSlot.OFFHAND.getIndex()] = 2.0F;
        }

        return entityData;
    }

    public static boolean checkDrownedSpawnRules(EntityTypes<EntityDrowned> type, WorldAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        Optional<ResourceKey<BiomeBase>> optional = world.getBiomeName(pos);
        boolean bl = world.getDifficulty() != EnumDifficulty.PEACEFUL && isDarkEnoughToSpawn(world, pos, random) && (spawnReason == EnumMobSpawn.SPAWNER || world.getFluid(pos).is(TagsFluid.WATER));
        if (!Objects.equals(optional, Optional.of(Biomes.RIVER)) && !Objects.equals(optional, Optional.of(Biomes.FROZEN_RIVER))) {
            return random.nextInt(40) == 0 && isDeepEnoughToSpawn(world, pos) && bl;
        } else {
            return random.nextInt(15) == 0 && bl;
        }
    }

    private static boolean isDeepEnoughToSpawn(GeneratorAccess world, BlockPosition pos) {
        return pos.getY() < world.getSeaLevel() - 5;
    }

    @Override
    public boolean supportsBreakDoorGoal() {
        return false;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return this.isInWater() ? SoundEffects.DROWNED_AMBIENT_WATER : SoundEffects.DROWNED_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return this.isInWater() ? SoundEffects.DROWNED_HURT_WATER : SoundEffects.DROWNED_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return this.isInWater() ? SoundEffects.DROWNED_DEATH_WATER : SoundEffects.DROWNED_DEATH;
    }

    @Override
    protected SoundEffect getSoundStep() {
        return SoundEffects.DROWNED_STEP;
    }

    @Override
    protected SoundEffect getSoundSwim() {
        return SoundEffects.DROWNED_SWIM;
    }

    @Override
    protected ItemStack getSkull() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void populateDefaultEquipmentSlots(DifficultyDamageScaler difficulty) {
        if ((double)this.random.nextFloat() > 0.9D) {
            int i = this.random.nextInt(16);
            if (i < 10) {
                this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.TRIDENT));
            } else {
                this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
            }
        }

    }

    @Override
    protected boolean canReplaceCurrentItem(ItemStack newStack, ItemStack oldStack) {
        if (oldStack.is(Items.NAUTILUS_SHELL)) {
            return false;
        } else if (oldStack.is(Items.TRIDENT)) {
            if (newStack.is(Items.TRIDENT)) {
                return newStack.getDamage() < oldStack.getDamage();
            } else {
                return false;
            }
        } else {
            return newStack.is(Items.TRIDENT) ? true : super.canReplaceCurrentItem(newStack, oldStack);
        }
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean checkSpawnObstruction(IWorldReader world) {
        return world.isUnobstructed(this);
    }

    public boolean okTarget(@Nullable EntityLiving target) {
        if (target != null) {
            return !this.level.isDay() || target.isInWater();
        } else {
            return false;
        }
    }

    @Override
    public boolean isPushedByFluid() {
        return !this.isSwimming();
    }

    boolean wantsToSwim() {
        if (this.searchingForLand) {
            return true;
        } else {
            EntityLiving livingEntity = this.getGoalTarget();
            return livingEntity != null && livingEntity.isInWater();
        }
    }

    @Override
    public void travel(Vec3D movementInput) {
        if (this.doAITick() && this.isInWater() && this.wantsToSwim()) {
            this.moveRelative(0.01F, movementInput);
            this.move(EnumMoveType.SELF, this.getMot());
            this.setMot(this.getMot().scale(0.9D));
        } else {
            super.travel(movementInput);
        }

    }

    @Override
    public void updateSwimming() {
        if (!this.level.isClientSide) {
            if (this.doAITick() && this.isInWater() && this.wantsToSwim()) {
                this.navigation = this.waterNavigation;
                this.setSwimming(true);
            } else {
                this.navigation = this.groundNavigation;
                this.setSwimming(false);
            }
        }

    }

    protected boolean closeToNextPos() {
        PathEntity path = this.getNavigation().getPath();
        if (path != null) {
            BlockPosition blockPos = path.getTarget();
            if (blockPos != null) {
                double d = this.distanceToSqr((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ());
                if (d < 4.0D) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void performRangedAttack(EntityLiving target, float pullProgress) {
        EntityThrownTrident thrownTrident = new EntityThrownTrident(this.level, this, new ItemStack(Items.TRIDENT));
        double d = target.locX() - this.locX();
        double e = target.getY(0.3333333333333333D) - thrownTrident.locY();
        double f = target.locZ() - this.locZ();
        double g = Math.sqrt(d * d + f * f);
        thrownTrident.shoot(d, e + g * (double)0.2F, f, 1.6F, (float)(14 - this.level.getDifficulty().getId() * 4));
        this.playSound(SoundEffects.DROWNED_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level.addEntity(thrownTrident);
    }

    public void setSearchingForLand(boolean targetingUnderwater) {
        this.searchingForLand = targetingUnderwater;
    }

    static class ControllerMoveDrowned extends ControllerMove {
        private final EntityDrowned drowned;

        public ControllerMoveDrowned(EntityDrowned drowned) {
            super(drowned);
            this.drowned = drowned;
        }

        @Override
        public void tick() {
            EntityLiving livingEntity = this.drowned.getGoalTarget();
            if (this.drowned.wantsToSwim() && this.drowned.isInWater()) {
                if (livingEntity != null && livingEntity.locY() > this.drowned.locY() || this.drowned.searchingForLand) {
                    this.drowned.setMot(this.drowned.getMot().add(0.0D, 0.002D, 0.0D));
                }

                if (this.operation != ControllerMove.Operation.MOVE_TO || this.drowned.getNavigation().isDone()) {
                    this.drowned.setSpeed(0.0F);
                    return;
                }

                double d = this.wantedX - this.drowned.locX();
                double e = this.wantedY - this.drowned.locY();
                double f = this.wantedZ - this.drowned.locZ();
                double g = Math.sqrt(d * d + e * e + f * f);
                e = e / g;
                float h = (float)(MathHelper.atan2(f, d) * (double)(180F / (float)Math.PI)) - 90.0F;
                this.drowned.setYRot(this.rotlerp(this.drowned.getYRot(), h, 90.0F));
                this.drowned.yBodyRot = this.drowned.getYRot();
                float i = (float)(this.speedModifier * this.drowned.getAttributeValue(GenericAttributes.MOVEMENT_SPEED));
                float j = MathHelper.lerp(0.125F, this.drowned.getSpeed(), i);
                this.drowned.setSpeed(j);
                this.drowned.setMot(this.drowned.getMot().add((double)j * d * 0.005D, (double)j * e * 0.1D, (double)j * f * 0.005D));
            } else {
                if (!this.drowned.onGround) {
                    this.drowned.setMot(this.drowned.getMot().add(0.0D, -0.008D, 0.0D));
                }

                super.tick();
            }

        }
    }

    static class PathfinderGoalDrownedFindWater extends PathfinderGoal {
        private final EntityCreature mob;
        private double wantedX;
        private double wantedY;
        private double wantedZ;
        private final double speedModifier;
        private final World level;

        public PathfinderGoalDrownedFindWater(EntityCreature mob, double speed) {
            this.mob = mob;
            this.speedModifier = speed;
            this.level = mob.level;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.level.isDay()) {
                return false;
            } else if (this.mob.isInWater()) {
                return false;
            } else {
                Vec3D vec3 = this.getWaterPos();
                if (vec3 == null) {
                    return false;
                } else {
                    this.wantedX = vec3.x;
                    this.wantedY = vec3.y;
                    this.wantedZ = vec3.z;
                    return true;
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !this.mob.getNavigation().isDone();
        }

        @Override
        public void start() {
            this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
        }

        @Nullable
        private Vec3D getWaterPos() {
            Random random = this.mob.getRandom();
            BlockPosition blockPos = this.mob.getChunkCoordinates();

            for(int i = 0; i < 10; ++i) {
                BlockPosition blockPos2 = blockPos.offset(random.nextInt(20) - 10, 2 - random.nextInt(8), random.nextInt(20) - 10);
                if (this.level.getType(blockPos2).is(Blocks.WATER)) {
                    return Vec3D.atBottomCenterOf(blockPos2);
                }
            }

            return null;
        }
    }

    static class PathfinderGoalDrownedNearestAttackableTarget extends PathfinderGoalZombieAttack {
        private final EntityDrowned drowned;

        public PathfinderGoalDrownedNearestAttackableTarget(EntityDrowned drowned, double speed, boolean pauseWhenMobIdle) {
            super(drowned, speed, pauseWhenMobIdle);
            this.drowned = drowned;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && this.drowned.okTarget(this.drowned.getGoalTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.drowned.okTarget(this.drowned.getGoalTarget());
        }
    }

    static class PathfinderGoalDrownedSwim extends PathfinderGoal {
        private final EntityDrowned drowned;
        private final double speedModifier;
        private final int seaLevel;
        private boolean stuck;

        public PathfinderGoalDrownedSwim(EntityDrowned drowned, double speed, int minY) {
            this.drowned = drowned;
            this.speedModifier = speed;
            this.seaLevel = minY;
        }

        @Override
        public boolean canUse() {
            return !this.drowned.level.isDay() && this.drowned.isInWater() && this.drowned.locY() < (double)(this.seaLevel - 2);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() && !this.stuck;
        }

        @Override
        public void tick() {
            if (this.drowned.locY() < (double)(this.seaLevel - 1) && (this.drowned.getNavigation().isDone() || this.drowned.closeToNextPos())) {
                Vec3D vec3 = DefaultRandomPos.getPosTowards(this.drowned, 4, 8, new Vec3D(this.drowned.locX(), (double)(this.seaLevel - 1), this.drowned.locZ()), (double)((float)Math.PI / 2F));
                if (vec3 == null) {
                    this.stuck = true;
                    return;
                }

                this.drowned.getNavigation().moveTo(vec3.x, vec3.y, vec3.z, this.speedModifier);
            }

        }

        @Override
        public void start() {
            this.drowned.setSearchingForLand(true);
            this.stuck = false;
        }

        @Override
        public void stop() {
            this.drowned.setSearchingForLand(false);
        }
    }

    static class PathfinderGoalDrownedTridentAttack extends PathfinderGoalArrowAttack {
        private final EntityDrowned drowned;

        public PathfinderGoalDrownedTridentAttack(IRangedEntity mob, double mobSpeed, int intervalTicks, float maxShootRange) {
            super(mob, mobSpeed, intervalTicks, maxShootRange);
            this.drowned = (EntityDrowned)mob;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && this.drowned.getItemInMainHand().is(Items.TRIDENT);
        }

        @Override
        public void start() {
            super.start();
            this.drowned.setAggressive(true);
            this.drowned.startUsingItem(EnumHand.MAIN_HAND);
        }

        @Override
        public void stop() {
            super.stop();
            this.drowned.clearActiveItem();
            this.drowned.setAggressive(false);
        }
    }

    static class PathfinderGoalNearestBeach extends PathfinderGoalGotoTarget {
        private final EntityDrowned drowned;

        public PathfinderGoalNearestBeach(EntityDrowned drowned, double speed) {
            super(drowned, speed, 8, 2);
            this.drowned = drowned;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && !this.drowned.level.isDay() && this.drowned.isInWater() && this.drowned.locY() >= (double)(this.drowned.level.getSeaLevel() - 3);
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse();
        }

        @Override
        protected boolean isValidTarget(IWorldReader world, BlockPosition pos) {
            BlockPosition blockPos = pos.above();
            return world.isEmpty(blockPos) && world.isEmpty(blockPos.above()) ? world.getType(pos).entityCanStandOn(world, pos, this.drowned) : false;
        }

        @Override
        public void start() {
            this.drowned.setSearchingForLand(false);
            this.drowned.navigation = this.drowned.groundNavigation;
            super.start();
        }

        @Override
        public void stop() {
            super.stop();
        }
    }
}
