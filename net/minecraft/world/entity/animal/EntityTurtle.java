package net.minecraft.world.entity.animal;

import java.util.Random;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalGotoTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTempt;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationGuardian;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockTurtleEgg;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.Pathfinder;
import net.minecraft.world.level.pathfinder.PathfinderAmphibious;
import net.minecraft.world.phys.Vec3D;

public class EntityTurtle extends EntityAnimal {
    private static final DataWatcherObject<BlockPosition> HOME_POS = DataWatcher.defineId(EntityTurtle.class, DataWatcherRegistry.BLOCK_POS);
    private static final DataWatcherObject<Boolean> HAS_EGG = DataWatcher.defineId(EntityTurtle.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> LAYING_EGG = DataWatcher.defineId(EntityTurtle.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<BlockPosition> TRAVEL_POS = DataWatcher.defineId(EntityTurtle.class, DataWatcherRegistry.BLOCK_POS);
    private static final DataWatcherObject<Boolean> GOING_HOME = DataWatcher.defineId(EntityTurtle.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> TRAVELLING = DataWatcher.defineId(EntityTurtle.class, DataWatcherRegistry.BOOLEAN);
    public static final RecipeItemStack FOOD_ITEMS = RecipeItemStack.of(Blocks.SEAGRASS.getItem());
    int layEggCounter;
    public static final Predicate<EntityLiving> BABY_ON_LAND_SELECTOR = (entity) -> {
        return entity.isBaby() && !entity.isInWater();
    };

    public EntityTurtle(EntityTypes<? extends EntityTurtle> type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.DOOR_IRON_CLOSED, -1.0F);
        this.setPathfindingMalus(PathType.DOOR_WOOD_CLOSED, -1.0F);
        this.setPathfindingMalus(PathType.DOOR_OPEN, -1.0F);
        this.moveControl = new EntityTurtle.TurtleMoveControl(this);
        this.maxUpStep = 1.0F;
    }

    public void setHomePos(BlockPosition pos) {
        this.entityData.set(HOME_POS, pos);
    }

    public BlockPosition getHomePos() {
        return this.entityData.get(HOME_POS);
    }

    void setTravelPos(BlockPosition pos) {
        this.entityData.set(TRAVEL_POS, pos);
    }

    BlockPosition getTravelPos() {
        return this.entityData.get(TRAVEL_POS);
    }

    public boolean hasEgg() {
        return this.entityData.get(HAS_EGG);
    }

    public void setHasEgg(boolean hasEgg) {
        this.entityData.set(HAS_EGG, hasEgg);
    }

    public boolean isLayingEgg() {
        return this.entityData.get(LAYING_EGG);
    }

    void setLayingEgg(boolean diggingSand) {
        this.layEggCounter = diggingSand ? 1 : 0;
        this.entityData.set(LAYING_EGG, diggingSand);
    }

    public boolean isGoingHome() {
        return this.entityData.get(GOING_HOME);
    }

    public void setGoingHome(boolean landBound) {
        this.entityData.set(GOING_HOME, landBound);
    }

    public boolean isTravelling() {
        return this.entityData.get(TRAVELLING);
    }

    public void setTravelling(boolean travelling) {
        this.entityData.set(TRAVELLING, travelling);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(HOME_POS, BlockPosition.ZERO);
        this.entityData.register(HAS_EGG, false);
        this.entityData.register(TRAVEL_POS, BlockPosition.ZERO);
        this.entityData.register(GOING_HOME, false);
        this.entityData.register(TRAVELLING, false);
        this.entityData.register(LAYING_EGG, false);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("HomePosX", this.getHomePos().getX());
        nbt.setInt("HomePosY", this.getHomePos().getY());
        nbt.setInt("HomePosZ", this.getHomePos().getZ());
        nbt.setBoolean("HasEgg", this.hasEgg());
        nbt.setInt("TravelPosX", this.getTravelPos().getX());
        nbt.setInt("TravelPosY", this.getTravelPos().getY());
        nbt.setInt("TravelPosZ", this.getTravelPos().getZ());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        int i = nbt.getInt("HomePosX");
        int j = nbt.getInt("HomePosY");
        int k = nbt.getInt("HomePosZ");
        this.setHomePos(new BlockPosition(i, j, k));
        super.loadData(nbt);
        this.setHasEgg(nbt.getBoolean("HasEgg"));
        int l = nbt.getInt("TravelPosX");
        int m = nbt.getInt("TravelPosY");
        int n = nbt.getInt("TravelPosZ");
        this.setTravelPos(new BlockPosition(l, m, n));
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.setHomePos(this.getChunkCoordinates());
        this.setTravelPos(BlockPosition.ZERO);
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    public static boolean checkTurtleSpawnRules(EntityTypes<EntityTurtle> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return pos.getY() < world.getSeaLevel() + 4 && BlockTurtleEgg.onSand(world, pos) && isBrightEnoughToSpawn(world, pos);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(0, new EntityTurtle.TurtlePanicGoal(this, 1.2D));
        this.goalSelector.addGoal(1, new EntityTurtle.TurtleBreedGoal(this, 1.0D));
        this.goalSelector.addGoal(1, new EntityTurtle.TurtleLayEggGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new PathfinderGoalTempt(this, 1.1D, FOOD_ITEMS, false));
        this.goalSelector.addGoal(3, new EntityTurtle.TurtleGoToWaterGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new EntityTurtle.TurtleGoHomeGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new EntityTurtle.TurtleTravelGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(9, new EntityTurtle.TurtleRandomStrollGoal(this, 1.0D, 100));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 30.0D).add(GenericAttributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.WATER;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Nullable
    @Override
    protected SoundEffect getSoundAmbient() {
        return !this.isInWater() && this.onGround && !this.isBaby() ? SoundEffects.TURTLE_AMBIENT_LAND : super.getSoundAmbient();
    }

    @Override
    protected void playSwimSound(float volume) {
        super.playSwimSound(volume * 1.5F);
    }

    @Override
    protected SoundEffect getSoundSwim() {
        return SoundEffects.TURTLE_SWIM;
    }

    @Nullable
    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return this.isBaby() ? SoundEffects.TURTLE_HURT_BABY : SoundEffects.TURTLE_HURT;
    }

    @Nullable
    @Override
    public SoundEffect getSoundDeath() {
        return this.isBaby() ? SoundEffects.TURTLE_DEATH_BABY : SoundEffects.TURTLE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        SoundEffect soundEvent = this.isBaby() ? SoundEffects.TURTLE_SHAMBLE_BABY : SoundEffects.TURTLE_SHAMBLE;
        this.playSound(soundEvent, 0.15F, 1.0F);
    }

    @Override
    public boolean canFallInLove() {
        return super.canFallInLove() && !this.hasEgg();
    }

    @Override
    protected float nextStep() {
        return this.moveDist + 0.15F;
    }

    @Override
    public float getScale() {
        return this.isBaby() ? 0.3F : 1.0F;
    }

    @Override
    protected NavigationAbstract createNavigation(World world) {
        return new EntityTurtle.TurtlePathNavigation(this, world);
    }

    @Nullable
    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        return EntityTypes.TURTLE.create(world);
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return stack.is(Blocks.SEAGRASS.getItem());
    }

    @Override
    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        if (!this.isGoingHome() && world.getFluid(pos).is(TagsFluid.WATER)) {
            return 10.0F;
        } else {
            return BlockTurtleEgg.onSand(world, pos) ? 10.0F : world.getBrightness(pos) - 0.5F;
        }
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (this.isAlive() && this.isLayingEgg() && this.layEggCounter >= 1 && this.layEggCounter % 5 == 0) {
            BlockPosition blockPos = this.getChunkCoordinates();
            if (BlockTurtleEgg.onSand(this.level, blockPos)) {
                this.level.triggerEffect(2001, blockPos, Block.getCombinedId(this.level.getType(blockPos.below())));
            }
        }

    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!this.isBaby() && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            this.spawnAtLocation(Items.SCUTE, 1);
        }

    }

    @Override
    public void travel(Vec3D movementInput) {
        if (this.doAITick() && this.isInWater()) {
            this.moveRelative(0.1F, movementInput);
            this.move(EnumMoveType.SELF, this.getMot());
            this.setMot(this.getMot().scale(0.9D));
            if (this.getGoalTarget() == null && (!this.isGoingHome() || !this.getHomePos().closerThan(this.getPositionVector(), 20.0D))) {
                this.setMot(this.getMot().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(movementInput);
        }

    }

    @Override
    public boolean canBeLeashed(EntityHuman player) {
        return false;
    }

    @Override
    public void onLightningStrike(WorldServer world, EntityLightning lightning) {
        this.damageEntity(DamageSource.LIGHTNING_BOLT, Float.MAX_VALUE);
    }

    static class TurtleBreedGoal extends PathfinderGoalBreed {
        private final EntityTurtle turtle;

        TurtleBreedGoal(EntityTurtle turtle, double speed) {
            super(turtle, speed);
            this.turtle = turtle;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && !this.turtle.hasEgg();
        }

        @Override
        protected void breed() {
            EntityPlayer serverPlayer = this.animal.getBreedCause();
            if (serverPlayer == null && this.partner.getBreedCause() != null) {
                serverPlayer = this.partner.getBreedCause();
            }

            if (serverPlayer != null) {
                serverPlayer.awardStat(StatisticList.ANIMALS_BRED);
                CriterionTriggers.BRED_ANIMALS.trigger(serverPlayer, this.animal, this.partner, (EntityAgeable)null);
            }

            this.turtle.setHasEgg(true);
            this.animal.resetLove();
            this.partner.resetLove();
            Random random = this.animal.getRandom();
            if (this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                this.level.addEntity(new EntityExperienceOrb(this.level, this.animal.locX(), this.animal.locY(), this.animal.locZ(), random.nextInt(7) + 1));
            }

        }
    }

    static class TurtleGoHomeGoal extends PathfinderGoal {
        private final EntityTurtle turtle;
        private final double speedModifier;
        private boolean stuck;
        private int closeToHomeTryTicks;
        private static final int GIVE_UP_TICKS = 600;

        TurtleGoHomeGoal(EntityTurtle turtle, double speed) {
            this.turtle = turtle;
            this.speedModifier = speed;
        }

        @Override
        public boolean canUse() {
            if (this.turtle.isBaby()) {
                return false;
            } else if (this.turtle.hasEgg()) {
                return true;
            } else if (this.turtle.getRandom().nextInt(reducedTickDelay(700)) != 0) {
                return false;
            } else {
                return !this.turtle.getHomePos().closerThan(this.turtle.getPositionVector(), 64.0D);
            }
        }

        @Override
        public void start() {
            this.turtle.setGoingHome(true);
            this.stuck = false;
            this.closeToHomeTryTicks = 0;
        }

        @Override
        public void stop() {
            this.turtle.setGoingHome(false);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.turtle.getHomePos().closerThan(this.turtle.getPositionVector(), 7.0D) && !this.stuck && this.closeToHomeTryTicks <= this.adjustedTickDelay(600);
        }

        @Override
        public void tick() {
            BlockPosition blockPos = this.turtle.getHomePos();
            boolean bl = blockPos.closerThan(this.turtle.getPositionVector(), 16.0D);
            if (bl) {
                ++this.closeToHomeTryTicks;
            }

            if (this.turtle.getNavigation().isDone()) {
                Vec3D vec3 = Vec3D.atBottomCenterOf(blockPos);
                Vec3D vec32 = DefaultRandomPos.getPosTowards(this.turtle, 16, 3, vec3, (double)((float)Math.PI / 10F));
                if (vec32 == null) {
                    vec32 = DefaultRandomPos.getPosTowards(this.turtle, 8, 7, vec3, (double)((float)Math.PI / 2F));
                }

                if (vec32 != null && !bl && !this.turtle.level.getType(new BlockPosition(vec32)).is(Blocks.WATER)) {
                    vec32 = DefaultRandomPos.getPosTowards(this.turtle, 16, 5, vec3, (double)((float)Math.PI / 2F));
                }

                if (vec32 == null) {
                    this.stuck = true;
                    return;
                }

                this.turtle.getNavigation().moveTo(vec32.x, vec32.y, vec32.z, this.speedModifier);
            }

        }
    }

    static class TurtleGoToWaterGoal extends PathfinderGoalGotoTarget {
        private static final int GIVE_UP_TICKS = 1200;
        private final EntityTurtle turtle;

        TurtleGoToWaterGoal(EntityTurtle turtle, double speed) {
            super(turtle, turtle.isBaby() ? 2.0D : speed, 24);
            this.turtle = turtle;
            this.verticalSearchStart = -1;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.turtle.isInWater() && this.tryTicks <= 1200 && this.isValidTarget(this.turtle.level, this.blockPos);
        }

        @Override
        public boolean canUse() {
            if (this.turtle.isBaby() && !this.turtle.isInWater()) {
                return super.canUse();
            } else {
                return !this.turtle.isGoingHome() && !this.turtle.isInWater() && !this.turtle.hasEgg() ? super.canUse() : false;
            }
        }

        @Override
        public boolean shouldRecalculatePath() {
            return this.tryTicks % 160 == 0;
        }

        @Override
        protected boolean isValidTarget(IWorldReader world, BlockPosition pos) {
            return world.getType(pos).is(Blocks.WATER);
        }
    }

    static class TurtleLayEggGoal extends PathfinderGoalGotoTarget {
        private final EntityTurtle turtle;

        TurtleLayEggGoal(EntityTurtle turtle, double speed) {
            super(turtle, speed, 16);
            this.turtle = turtle;
        }

        @Override
        public boolean canUse() {
            return this.turtle.hasEgg() && this.turtle.getHomePos().closerThan(this.turtle.getPositionVector(), 9.0D) ? super.canUse() : false;
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.turtle.hasEgg() && this.turtle.getHomePos().closerThan(this.turtle.getPositionVector(), 9.0D);
        }

        @Override
        public void tick() {
            super.tick();
            BlockPosition blockPos = this.turtle.getChunkCoordinates();
            if (!this.turtle.isInWater() && this.isReachedTarget()) {
                if (this.turtle.layEggCounter < 1) {
                    this.turtle.setLayingEgg(true);
                } else if (this.turtle.layEggCounter > this.adjustedTickDelay(200)) {
                    World level = this.turtle.level;
                    level.playSound((EntityHuman)null, blockPos, SoundEffects.TURTLE_LAY_EGG, EnumSoundCategory.BLOCKS, 0.3F, 0.9F + level.random.nextFloat() * 0.2F);
                    level.setTypeAndData(this.blockPos.above(), Blocks.TURTLE_EGG.getBlockData().set(BlockTurtleEgg.EGGS, Integer.valueOf(this.turtle.random.nextInt(4) + 1)), 3);
                    this.turtle.setHasEgg(false);
                    this.turtle.setLayingEgg(false);
                    this.turtle.setLoveTicks(600);
                }

                if (this.turtle.isLayingEgg()) {
                    ++this.turtle.layEggCounter;
                }
            }

        }

        @Override
        protected boolean isValidTarget(IWorldReader world, BlockPosition pos) {
            return !world.isEmpty(pos.above()) ? false : BlockTurtleEgg.isSand(world, pos);
        }
    }

    static class TurtleMoveControl extends ControllerMove {
        private final EntityTurtle turtle;

        TurtleMoveControl(EntityTurtle turtle) {
            super(turtle);
            this.turtle = turtle;
        }

        private void updateSpeed() {
            if (this.turtle.isInWater()) {
                this.turtle.setMot(this.turtle.getMot().add(0.0D, 0.005D, 0.0D));
                if (!this.turtle.getHomePos().closerThan(this.turtle.getPositionVector(), 16.0D)) {
                    this.turtle.setSpeed(Math.max(this.turtle.getSpeed() / 2.0F, 0.08F));
                }

                if (this.turtle.isBaby()) {
                    this.turtle.setSpeed(Math.max(this.turtle.getSpeed() / 3.0F, 0.06F));
                }
            } else if (this.turtle.onGround) {
                this.turtle.setSpeed(Math.max(this.turtle.getSpeed() / 2.0F, 0.06F));
            }

        }

        @Override
        public void tick() {
            this.updateSpeed();
            if (this.operation == ControllerMove.Operation.MOVE_TO && !this.turtle.getNavigation().isDone()) {
                double d = this.wantedX - this.turtle.locX();
                double e = this.wantedY - this.turtle.locY();
                double f = this.wantedZ - this.turtle.locZ();
                double g = Math.sqrt(d * d + e * e + f * f);
                e = e / g;
                float h = (float)(MathHelper.atan2(f, d) * (double)(180F / (float)Math.PI)) - 90.0F;
                this.turtle.setYRot(this.rotlerp(this.turtle.getYRot(), h, 90.0F));
                this.turtle.yBodyRot = this.turtle.getYRot();
                float i = (float)(this.speedModifier * this.turtle.getAttributeValue(GenericAttributes.MOVEMENT_SPEED));
                this.turtle.setSpeed(MathHelper.lerp(0.125F, this.turtle.getSpeed(), i));
                this.turtle.setMot(this.turtle.getMot().add(0.0D, (double)this.turtle.getSpeed() * e * 0.1D, 0.0D));
            } else {
                this.turtle.setSpeed(0.0F);
            }
        }
    }

    static class TurtlePanicGoal extends PathfinderGoalPanic {
        TurtlePanicGoal(EntityTurtle turtle, double speed) {
            super(turtle, speed);
        }

        @Override
        public boolean canUse() {
            if (this.mob.getLastDamager() == null && !this.mob.isBurning()) {
                return false;
            } else {
                BlockPosition blockPos = this.lookForWater(this.mob.level, this.mob, 7);
                if (blockPos != null) {
                    this.posX = (double)blockPos.getX();
                    this.posY = (double)blockPos.getY();
                    this.posZ = (double)blockPos.getZ();
                    return true;
                } else {
                    return this.findRandomPosition();
                }
            }
        }
    }

    static class TurtlePathNavigation extends NavigationGuardian {
        TurtlePathNavigation(EntityTurtle owner, World world) {
            super(owner, world);
        }

        @Override
        protected boolean canUpdatePath() {
            return true;
        }

        @Override
        protected Pathfinder createPathFinder(int range) {
            this.nodeEvaluator = new PathfinderAmphibious(true);
            return new Pathfinder(this.nodeEvaluator, range);
        }

        @Override
        public boolean isStableDestination(BlockPosition pos) {
            if (this.mob instanceof EntityTurtle) {
                EntityTurtle turtle = (EntityTurtle)this.mob;
                if (turtle.isTravelling()) {
                    return this.level.getType(pos).is(Blocks.WATER);
                }
            }

            return !this.level.getType(pos.below()).isAir();
        }
    }

    static class TurtleRandomStrollGoal extends PathfinderGoalRandomStroll {
        private final EntityTurtle turtle;

        TurtleRandomStrollGoal(EntityTurtle turtle, double speed, int chance) {
            super(turtle, speed, chance);
            this.turtle = turtle;
        }

        @Override
        public boolean canUse() {
            return !this.mob.isInWater() && !this.turtle.isGoingHome() && !this.turtle.hasEgg() ? super.canUse() : false;
        }
    }

    static class TurtleTravelGoal extends PathfinderGoal {
        private final EntityTurtle turtle;
        private final double speedModifier;
        private boolean stuck;

        TurtleTravelGoal(EntityTurtle turtle, double speed) {
            this.turtle = turtle;
            this.speedModifier = speed;
        }

        @Override
        public boolean canUse() {
            return !this.turtle.isGoingHome() && !this.turtle.hasEgg() && this.turtle.isInWater();
        }

        @Override
        public void start() {
            int i = 512;
            int j = 4;
            Random random = this.turtle.random;
            int k = random.nextInt(1025) - 512;
            int l = random.nextInt(9) - 4;
            int m = random.nextInt(1025) - 512;
            if ((double)l + this.turtle.locY() > (double)(this.turtle.level.getSeaLevel() - 1)) {
                l = 0;
            }

            BlockPosition blockPos = new BlockPosition((double)k + this.turtle.locX(), (double)l + this.turtle.locY(), (double)m + this.turtle.locZ());
            this.turtle.setTravelPos(blockPos);
            this.turtle.setTravelling(true);
            this.stuck = false;
        }

        @Override
        public void tick() {
            if (this.turtle.getNavigation().isDone()) {
                Vec3D vec3 = Vec3D.atBottomCenterOf(this.turtle.getTravelPos());
                Vec3D vec32 = DefaultRandomPos.getPosTowards(this.turtle, 16, 3, vec3, (double)((float)Math.PI / 10F));
                if (vec32 == null) {
                    vec32 = DefaultRandomPos.getPosTowards(this.turtle, 8, 7, vec3, (double)((float)Math.PI / 2F));
                }

                if (vec32 != null) {
                    int i = MathHelper.floor(vec32.x);
                    int j = MathHelper.floor(vec32.z);
                    int k = 34;
                    if (!this.turtle.level.hasChunksAt(i - 34, j - 34, i + 34, j + 34)) {
                        vec32 = null;
                    }
                }

                if (vec32 == null) {
                    this.stuck = true;
                    return;
                }

                this.turtle.getNavigation().moveTo(vec32.x, vec32.y, vec32.z, this.speedModifier);
            }

        }

        @Override
        public boolean canContinueToUse() {
            return !this.turtle.getNavigation().isDone() && !this.stuck && !this.turtle.isGoingHome() && !this.turtle.isInLove() && !this.turtle.hasEgg();
        }

        @Override
        public void stop() {
            this.turtle.setTravelling(false);
            super.stop();
        }
    }
}
