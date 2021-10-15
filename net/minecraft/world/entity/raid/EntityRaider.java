package net.minecraft.world.entity.raid;

import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRaid;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.animal.EntityWolf;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.EntityIllagerAbstract;
import net.minecraft.world.entity.monster.EntityMonsterPatrolling;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.phys.Vec3D;

public abstract class EntityRaider extends EntityMonsterPatrolling {
    protected static final DataWatcherObject<Boolean> IS_CELEBRATING = DataWatcher.defineId(EntityRaider.class, DataWatcherRegistry.BOOLEAN);
    static final Predicate<EntityItem> ALLOWED_ITEMS = (itemEntity) -> {
        return !itemEntity.hasPickUpDelay() && itemEntity.isAlive() && ItemStack.matches(itemEntity.getItemStack(), Raid.getLeaderBannerInstance());
    };
    @Nullable
    protected Raid raid;
    private int wave;
    private boolean canJoinRaid;
    private int ticksOutsideRaid;

    protected EntityRaider(EntityTypes<? extends EntityRaider> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(1, new EntityRaider.ObtainRaidLeaderBannerGoal<>(this));
        this.goalSelector.addGoal(3, new PathfinderGoalRaid<>(this));
        this.goalSelector.addGoal(4, new EntityRaider.RaiderMoveThroughVillageGoal(this, (double)1.05F, 1));
        this.goalSelector.addGoal(5, new EntityRaider.RaiderCelebration(this));
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(IS_CELEBRATING, false);
    }

    public abstract void applyRaidBuffs(int wave, boolean unused);

    public boolean isCanJoinRaid() {
        return this.canJoinRaid;
    }

    public void setCanJoinRaid(boolean ableToJoinRaid) {
        this.canJoinRaid = ableToJoinRaid;
    }

    @Override
    public void movementTick() {
        if (this.level instanceof WorldServer && this.isAlive()) {
            Raid raid = this.getCurrentRaid();
            if (this.isCanJoinRaid()) {
                if (raid == null) {
                    if (this.level.getTime() % 20L == 0L) {
                        Raid raid2 = ((WorldServer)this.level).getRaidAt(this.getChunkCoordinates());
                        if (raid2 != null && PersistentRaid.canJoinRaid(this, raid2)) {
                            raid2.joinRaid(raid2.getGroupsSpawned(), this, (BlockPosition)null, true);
                        }
                    }
                } else {
                    EntityLiving livingEntity = this.getGoalTarget();
                    if (livingEntity != null && (livingEntity.getEntityType() == EntityTypes.PLAYER || livingEntity.getEntityType() == EntityTypes.IRON_GOLEM)) {
                        this.noActionTime = 0;
                    }
                }
            }
        }

        super.movementTick();
    }

    @Override
    protected void updateNoActionTime() {
        this.noActionTime += 2;
    }

    @Override
    public void die(DamageSource source) {
        if (this.level instanceof WorldServer) {
            Entity entity = source.getEntity();
            Raid raid = this.getCurrentRaid();
            if (raid != null) {
                if (this.isPatrolLeader()) {
                    raid.removeLeader(this.getWave());
                }

                if (entity != null && entity.getEntityType() == EntityTypes.PLAYER) {
                    raid.addHeroOfTheVillage(entity);
                }

                raid.removeFromRaid(this, false);
            }

            if (this.isPatrolLeader() && raid == null && ((WorldServer)this.level).getRaidAt(this.getChunkCoordinates()) == null) {
                ItemStack itemStack = this.getEquipment(EnumItemSlot.HEAD);
                EntityHuman player = null;
                if (entity instanceof EntityHuman) {
                    player = (EntityHuman)entity;
                } else if (entity instanceof EntityWolf) {
                    EntityWolf wolf = (EntityWolf)entity;
                    EntityLiving livingEntity = wolf.getOwner();
                    if (wolf.isTamed() && livingEntity instanceof EntityHuman) {
                        player = (EntityHuman)livingEntity;
                    }
                }

                if (!itemStack.isEmpty() && ItemStack.matches(itemStack, Raid.getLeaderBannerInstance()) && player != null) {
                    MobEffect mobEffectInstance = player.getEffect(MobEffects.BAD_OMEN);
                    int i = 1;
                    if (mobEffectInstance != null) {
                        i += mobEffectInstance.getAmplifier();
                        player.removeEffectNoUpdate(MobEffects.BAD_OMEN);
                    } else {
                        --i;
                    }

                    i = MathHelper.clamp(i, 0, 4);
                    MobEffect mobEffectInstance2 = new MobEffect(MobEffects.BAD_OMEN, 120000, i, false, false, true);
                    if (!this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
                        player.addEffect(mobEffectInstance2);
                    }
                }
            }
        }

        super.die(source);
    }

    @Override
    public boolean canJoinPatrol() {
        return !this.hasActiveRaid();
    }

    public void setCurrentRaid(@Nullable Raid raid) {
        this.raid = raid;
    }

    @Nullable
    public Raid getCurrentRaid() {
        return this.raid;
    }

    public boolean hasActiveRaid() {
        return this.getCurrentRaid() != null && this.getCurrentRaid().isActive();
    }

    public void setWave(int wave) {
        this.wave = wave;
    }

    public int getWave() {
        return this.wave;
    }

    public boolean isCelebrating() {
        return this.entityData.get(IS_CELEBRATING);
    }

    public void setCelebrating(boolean celebrating) {
        this.entityData.set(IS_CELEBRATING, celebrating);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("Wave", this.wave);
        nbt.setBoolean("CanJoinRaid", this.canJoinRaid);
        if (this.raid != null) {
            nbt.setInt("RaidId", this.raid.getId());
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.wave = nbt.getInt("Wave");
        this.canJoinRaid = nbt.getBoolean("CanJoinRaid");
        if (nbt.hasKeyOfType("RaidId", 3)) {
            if (this.level instanceof WorldServer) {
                this.raid = ((WorldServer)this.level).getPersistentRaid().get(nbt.getInt("RaidId"));
            }

            if (this.raid != null) {
                this.raid.addWaveMob(this.wave, this, false);
                if (this.isPatrolLeader()) {
                    this.raid.setLeader(this.wave, this);
                }
            }
        }

    }

    @Override
    protected void pickUpItem(EntityItem item) {
        ItemStack itemStack = item.getItemStack();
        boolean bl = this.hasActiveRaid() && this.getCurrentRaid().getLeader(this.getWave()) != null;
        if (this.hasActiveRaid() && !bl && ItemStack.matches(itemStack, Raid.getLeaderBannerInstance())) {
            EnumItemSlot equipmentSlot = EnumItemSlot.HEAD;
            ItemStack itemStack2 = this.getEquipment(equipmentSlot);
            double d = (double)this.getEquipmentDropChance(equipmentSlot);
            if (!itemStack2.isEmpty() && (double)Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d) {
                this.spawnAtLocation(itemStack2);
            }

            this.onItemPickup(item);
            this.setSlot(equipmentSlot, itemStack);
            this.receive(item, itemStack.getCount());
            item.die();
            this.getCurrentRaid().setLeader(this.getWave(), this);
            this.setPatrolLeader(true);
        } else {
            super.pickUpItem(item);
        }

    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return this.getCurrentRaid() == null ? super.isTypeNotPersistent(distanceSquared) : false;
    }

    @Override
    public boolean isSpecialPersistence() {
        return super.isSpecialPersistence() || this.getCurrentRaid() != null;
    }

    public int getTicksOutsideRaid() {
        return this.ticksOutsideRaid;
    }

    public void setTicksOutsideRaid(int counter) {
        this.ticksOutsideRaid = counter;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.hasActiveRaid()) {
            this.getCurrentRaid().updateProgress();
        }

        return super.damageEntity(source, amount);
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.setCanJoinRaid(this.getEntityType() != EntityTypes.WITCH || spawnReason != EnumMobSpawn.NATURAL);
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    public abstract SoundEffect getCelebrateSound();

    public class HoldGroundAttackGoal extends PathfinderGoal {
        private final EntityRaider mob;
        private final float hostileRadiusSqr;
        public final PathfinderTargetCondition shoutTargeting = PathfinderTargetCondition.forNonCombat().range(8.0D).ignoreLineOfSight().ignoreInvisibilityTesting();

        public HoldGroundAttackGoal(EntityIllagerAbstract illager, float distance) {
            this.mob = illager;
            this.hostileRadiusSqr = distance * distance;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean canUse() {
            EntityLiving livingEntity = this.mob.getLastDamager();
            return this.mob.getCurrentRaid() == null && this.mob.isPatrolling() && this.mob.getGoalTarget() != null && !this.mob.isAggressive() && (livingEntity == null || livingEntity.getEntityType() != EntityTypes.PLAYER);
        }

        @Override
        public void start() {
            super.start();
            this.mob.getNavigation().stop();

            for(EntityRaider raider : this.mob.level.getNearbyEntities(EntityRaider.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().grow(8.0D, 8.0D, 8.0D))) {
                raider.setGoalTarget(this.mob.getGoalTarget());
            }

        }

        @Override
        public void stop() {
            super.stop();
            EntityLiving livingEntity = this.mob.getGoalTarget();
            if (livingEntity != null) {
                for(EntityRaider raider : this.mob.level.getNearbyEntities(EntityRaider.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().grow(8.0D, 8.0D, 8.0D))) {
                    raider.setGoalTarget(livingEntity);
                    raider.setAggressive(true);
                }

                this.mob.setAggressive(true);
            }

        }

        @Override
        public void tick() {
            EntityLiving livingEntity = this.mob.getGoalTarget();
            if (livingEntity != null) {
                if (this.mob.distanceToSqr(livingEntity) > (double)this.hostileRadiusSqr) {
                    this.mob.getControllerLook().setLookAt(livingEntity, 30.0F, 30.0F);
                    if (this.mob.random.nextInt(50) == 0) {
                        this.mob.playAmbientSound();
                    }
                } else {
                    this.mob.setAggressive(true);
                }

                super.tick();
            }
        }
    }

    public class ObtainRaidLeaderBannerGoal<T extends EntityRaider> extends PathfinderGoal {
        private final T mob;

        public ObtainRaidLeaderBannerGoal(T actor) {
            this.mob = actor;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            Raid raid = this.mob.getCurrentRaid();
            if (this.mob.hasActiveRaid() && !this.mob.getCurrentRaid().isOver() && this.mob.canBeLeader() && !ItemStack.matches(this.mob.getEquipment(EnumItemSlot.HEAD), Raid.getLeaderBannerInstance())) {
                EntityRaider raider = raid.getLeader(this.mob.getWave());
                if (raider == null || !raider.isAlive()) {
                    List<EntityItem> list = this.mob.level.getEntitiesOfClass(EntityItem.class, this.mob.getBoundingBox().grow(16.0D, 8.0D, 16.0D), EntityRaider.ALLOWED_ITEMS);
                    if (!list.isEmpty()) {
                        return this.mob.getNavigation().moveTo(list.get(0), (double)1.15F);
                    }
                }

                return false;
            } else {
                return false;
            }
        }

        @Override
        public void tick() {
            if (this.mob.getNavigation().getTargetPos().closerThan(this.mob.getPositionVector(), 1.414D)) {
                List<EntityItem> list = this.mob.level.getEntitiesOfClass(EntityItem.class, this.mob.getBoundingBox().grow(4.0D, 4.0D, 4.0D), EntityRaider.ALLOWED_ITEMS);
                if (!list.isEmpty()) {
                    this.mob.pickUpItem(list.get(0));
                }
            }

        }
    }

    public class RaiderCelebration extends PathfinderGoal {
        private final EntityRaider mob;

        RaiderCelebration(EntityRaider raider) {
            this.mob = raider;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            Raid raid = this.mob.getCurrentRaid();
            return this.mob.isAlive() && this.mob.getGoalTarget() == null && raid != null && raid.isLoss();
        }

        @Override
        public void start() {
            this.mob.setCelebrating(true);
            super.start();
        }

        @Override
        public void stop() {
            this.mob.setCelebrating(false);
            super.stop();
        }

        @Override
        public void tick() {
            if (!this.mob.isSilent() && this.mob.random.nextInt(100) == 0) {
                EntityRaider.this.playSound(EntityRaider.this.getCelebrateSound(), EntityRaider.this.getSoundVolume(), EntityRaider.this.getVoicePitch());
            }

            if (!this.mob.isPassenger() && this.mob.random.nextInt(50) == 0) {
                this.mob.getControllerJump().jump();
            }

            super.tick();
        }
    }

    static class RaiderMoveThroughVillageGoal extends PathfinderGoal {
        private final EntityRaider raider;
        private final double speedModifier;
        private BlockPosition poiPos;
        private final List<BlockPosition> visited = Lists.newArrayList();
        private final int distanceToPoi;
        private boolean stuck;

        public RaiderMoveThroughVillageGoal(EntityRaider raider, double speed, int distance) {
            this.raider = raider;
            this.speedModifier = speed;
            this.distanceToPoi = distance;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            this.updateVisited();
            return this.isValidRaid() && this.hasSuitablePoi() && this.raider.getGoalTarget() == null;
        }

        private boolean isValidRaid() {
            return this.raider.hasActiveRaid() && !this.raider.getCurrentRaid().isOver();
        }

        private boolean hasSuitablePoi() {
            WorldServer serverLevel = (WorldServer)this.raider.level;
            BlockPosition blockPos = this.raider.getChunkCoordinates();
            Optional<BlockPosition> optional = serverLevel.getPoiManager().getRandom((poiType) -> {
                return poiType == VillagePlaceType.HOME;
            }, this::hasNotVisited, VillagePlace.Occupancy.ANY, blockPos, 48, this.raider.random);
            if (!optional.isPresent()) {
                return false;
            } else {
                this.poiPos = optional.get().immutableCopy();
                return true;
            }
        }

        @Override
        public boolean canContinueToUse() {
            if (this.raider.getNavigation().isDone()) {
                return false;
            } else {
                return this.raider.getGoalTarget() == null && !this.poiPos.closerThan(this.raider.getPositionVector(), (double)(this.raider.getWidth() + (float)this.distanceToPoi)) && !this.stuck;
            }
        }

        @Override
        public void stop() {
            if (this.poiPos.closerThan(this.raider.getPositionVector(), (double)this.distanceToPoi)) {
                this.visited.add(this.poiPos);
            }

        }

        @Override
        public void start() {
            super.start();
            this.raider.setNoActionTime(0);
            this.raider.getNavigation().moveTo((double)this.poiPos.getX(), (double)this.poiPos.getY(), (double)this.poiPos.getZ(), this.speedModifier);
            this.stuck = false;
        }

        @Override
        public void tick() {
            if (this.raider.getNavigation().isDone()) {
                Vec3D vec3 = Vec3D.atBottomCenterOf(this.poiPos);
                Vec3D vec32 = DefaultRandomPos.getPosTowards(this.raider, 16, 7, vec3, (double)((float)Math.PI / 10F));
                if (vec32 == null) {
                    vec32 = DefaultRandomPos.getPosTowards(this.raider, 8, 7, vec3, (double)((float)Math.PI / 2F));
                }

                if (vec32 == null) {
                    this.stuck = true;
                    return;
                }

                this.raider.getNavigation().moveTo(vec32.x, vec32.y, vec32.z, this.speedModifier);
            }

        }

        private boolean hasNotVisited(BlockPosition pos) {
            for(BlockPosition blockPos : this.visited) {
                if (Objects.equals(pos, blockPos)) {
                    return false;
                }
            }

            return true;
        }

        private void updateVisited() {
            if (this.visited.size() > 2) {
                this.visited.remove(0);
            }

        }
    }
}
