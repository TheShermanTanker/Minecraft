package net.minecraft.world.entity.animal;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFishSchool;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;

public abstract class EntityFishSchool extends EntityFish {
    private EntityFishSchool leader;
    private int schoolSize = 1;

    public EntityFishSchool(EntityTypes<? extends EntityFishSchool> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(5, new PathfinderGoalFishSchool(this));
    }

    @Override
    public int getMaxSpawnGroup() {
        return this.getMaxSchoolSize();
    }

    public int getMaxSchoolSize() {
        return super.getMaxSpawnGroup();
    }

    @Override
    protected boolean canRandomSwim() {
        return !this.isFollower();
    }

    public boolean isFollower() {
        return this.leader != null && this.leader.isAlive();
    }

    public EntityFishSchool startFollowing(EntityFishSchool groupLeader) {
        this.leader = groupLeader;
        groupLeader.addFollower();
        return groupLeader;
    }

    public void stopFollowing() {
        this.leader.removeFollower();
        this.leader = null;
    }

    private void addFollower() {
        ++this.schoolSize;
    }

    private void removeFollower() {
        --this.schoolSize;
    }

    public boolean canBeFollowed() {
        return this.hasFollowers() && this.schoolSize < this.getMaxSchoolSize();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.hasFollowers() && this.level.random.nextInt(200) == 1) {
            List<? extends EntityFish> list = this.level.getEntitiesOfClass(this.getClass(), this.getBoundingBox().grow(8.0D, 8.0D, 8.0D));
            if (list.size() <= 1) {
                this.schoolSize = 1;
            }
        }

    }

    public boolean hasFollowers() {
        return this.schoolSize > 1;
    }

    public boolean inRangeOfLeader() {
        return this.distanceToSqr(this.leader) <= 121.0D;
    }

    public void pathToLeader() {
        if (this.isFollower()) {
            this.getNavigation().moveTo(this.leader, 1.0D);
        }

    }

    public void addFollowers(Stream<? extends EntityFishSchool> fish) {
        fish.limit((long)(this.getMaxSchoolSize() - this.schoolSize)).filter((fishx) -> {
            return fishx != this;
        }).forEach((fishx) -> {
            fishx.startFollowing(this);
        });
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
        if (entityData == null) {
            entityData = new EntityFishSchool.SchoolSpawnGroupData(this);
        } else {
            this.startFollowing(((EntityFishSchool.SchoolSpawnGroupData)entityData).leader);
        }

        return entityData;
    }

    public static class SchoolSpawnGroupData implements GroupDataEntity {
        public final EntityFishSchool leader;

        public SchoolSpawnGroupData(EntityFishSchool leader) {
            this.leader = leader;
        }
    }
}
