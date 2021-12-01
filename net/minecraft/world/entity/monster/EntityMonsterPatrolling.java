package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.phys.Vec3D;

public abstract class EntityMonsterPatrolling extends EntityMonster {
    @Nullable
    private BlockPosition patrolTarget;
    private boolean patrolLeader;
    private boolean patrolling;

    protected EntityMonsterPatrolling(EntityTypes<? extends EntityMonsterPatrolling> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(4, new EntityMonsterPatrolling.LongDistancePatrolGoal<>(this, 0.7D, 0.595D));
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (this.patrolTarget != null) {
            nbt.set("PatrolTarget", GameProfileSerializer.writeBlockPos(this.patrolTarget));
        }

        nbt.setBoolean("PatrolLeader", this.patrolLeader);
        nbt.setBoolean("Patrolling", this.patrolling);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKey("PatrolTarget")) {
            this.patrolTarget = GameProfileSerializer.readBlockPos(nbt.getCompound("PatrolTarget"));
        }

        this.patrolLeader = nbt.getBoolean("PatrolLeader");
        this.patrolling = nbt.getBoolean("Patrolling");
    }

    @Override
    public double getMyRidingOffset() {
        return -0.45D;
    }

    public boolean canBeLeader() {
        return true;
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        if (spawnReason != EnumMobSpawn.PATROL && spawnReason != EnumMobSpawn.EVENT && spawnReason != EnumMobSpawn.STRUCTURE && this.random.nextFloat() < 0.06F && this.canBeLeader()) {
            this.patrolLeader = true;
        }

        if (this.isPatrolLeader()) {
            this.setSlot(EnumItemSlot.HEAD, Raid.getLeaderBannerInstance());
            this.setDropChance(EnumItemSlot.HEAD, 2.0F);
        }

        if (spawnReason == EnumMobSpawn.PATROL) {
            this.patrolling = true;
        }

        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    public static boolean checkPatrollingMonsterSpawnRules(EntityTypes<? extends EntityMonsterPatrolling> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return world.getBrightness(EnumSkyBlock.BLOCK, pos) > 8 ? false : checkAnyLightMonsterSpawnRules(type, world, spawnReason, pos, random);
    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return !this.patrolling || distanceSquared > 16384.0D;
    }

    public void setPatrolTarget(BlockPosition targetPos) {
        this.patrolTarget = targetPos;
        this.patrolling = true;
    }

    public BlockPosition getPatrolTarget() {
        return this.patrolTarget;
    }

    public boolean hasPatrolTarget() {
        return this.patrolTarget != null;
    }

    public void setPatrolLeader(boolean patrolLeader) {
        this.patrolLeader = patrolLeader;
        this.patrolling = true;
    }

    public boolean isPatrolLeader() {
        return this.patrolLeader;
    }

    public boolean canJoinPatrol() {
        return true;
    }

    public void findPatrolTarget() {
        this.patrolTarget = this.getChunkCoordinates().offset(-500 + this.random.nextInt(1000), 0, -500 + this.random.nextInt(1000));
        this.patrolling = true;
    }

    protected boolean isPatrolling() {
        return this.patrolling;
    }

    protected void setPatrolling(boolean patrolling) {
        this.patrolling = patrolling;
    }

    public static class LongDistancePatrolGoal<T extends EntityMonsterPatrolling> extends PathfinderGoal {
        private static final int NAVIGATION_FAILED_COOLDOWN = 200;
        private final T mob;
        private final double speedModifier;
        private final double leaderSpeedModifier;
        private long cooldownUntil;

        public LongDistancePatrolGoal(T entity, double leaderSpeed, double followSpeed) {
            this.mob = entity;
            this.speedModifier = leaderSpeed;
            this.leaderSpeedModifier = followSpeed;
            this.cooldownUntil = -1L;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            boolean bl = this.mob.level.getTime() < this.cooldownUntil;
            return this.mob.isPatrolling() && this.mob.getGoalTarget() == null && !this.mob.isVehicle() && this.mob.hasPatrolTarget() && !bl;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void tick() {
            boolean bl = this.mob.isPatrolLeader();
            NavigationAbstract pathNavigation = this.mob.getNavigation();
            if (pathNavigation.isDone()) {
                List<EntityMonsterPatrolling> list = this.findPatrolCompanions();
                if (this.mob.isPatrolling() && list.isEmpty()) {
                    this.mob.setPatrolling(false);
                } else if (bl && this.mob.getPatrolTarget().closerThan(this.mob.getPositionVector(), 10.0D)) {
                    this.mob.findPatrolTarget();
                } else {
                    Vec3D vec3 = Vec3D.atBottomCenterOf(this.mob.getPatrolTarget());
                    Vec3D vec32 = this.mob.getPositionVector();
                    Vec3D vec33 = vec32.subtract(vec3);
                    vec3 = vec33.yRot(90.0F).scale(0.4D).add(vec3);
                    Vec3D vec34 = vec3.subtract(vec32).normalize().scale(10.0D).add(vec32);
                    BlockPosition blockPos = new BlockPosition(vec34);
                    blockPos = this.mob.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, blockPos);
                    if (!pathNavigation.moveTo((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), bl ? this.leaderSpeedModifier : this.speedModifier)) {
                        this.moveRandomly();
                        this.cooldownUntil = this.mob.level.getTime() + 200L;
                    } else if (bl) {
                        for(EntityMonsterPatrolling patrollingMonster : list) {
                            patrollingMonster.setPatrolTarget(blockPos);
                        }
                    }
                }
            }

        }

        private List<EntityMonsterPatrolling> findPatrolCompanions() {
            return this.mob.level.getEntitiesOfClass(EntityMonsterPatrolling.class, this.mob.getBoundingBox().inflate(16.0D), (patrollingMonster) -> {
                return patrollingMonster.canJoinPatrol() && !patrollingMonster.is(this.mob);
            });
        }

        private boolean moveRandomly() {
            Random random = this.mob.getRandom();
            BlockPosition blockPos = this.mob.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, this.mob.getChunkCoordinates().offset(-8 + random.nextInt(16), 0, -8 + random.nextInt(16)));
            return this.mob.getNavigation().moveTo((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), this.speedModifier);
        }
    }
}
