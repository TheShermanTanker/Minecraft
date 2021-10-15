package net.minecraft.world.entity;

import javax.annotation.Nullable;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;

public abstract class EntityAgeable extends EntityCreature {
    private static final DataWatcherObject<Boolean> DATA_BABY_ID = DataWatcher.defineId(EntityAgeable.class, DataWatcherRegistry.BOOLEAN);
    public static final int BABY_START_AGE = -24000;
    private static final int FORCED_AGE_PARTICLE_TICKS = 40;
    protected int age;
    protected int forcedAge;
    protected int forcedAgeTimer;

    protected EntityAgeable(EntityTypes<? extends EntityAgeable> type, World world) {
        super(type, world);
    }

    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        if (entityData == null) {
            entityData = new EntityAgeable.GroupDataAgeable(true);
        }

        EntityAgeable.GroupDataAgeable ageableMobGroupData = (EntityAgeable.GroupDataAgeable)entityData;
        if (ageableMobGroupData.isShouldSpawnBaby() && ageableMobGroupData.getGroupSize() > 0 && this.random.nextFloat() <= ageableMobGroupData.getBabySpawnChance()) {
            this.setAgeRaw(-24000);
        }

        ageableMobGroupData.increaseGroupSizeByOne();
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Nullable
    public abstract EntityAgeable createChild(WorldServer world, EntityAgeable entity);

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_BABY_ID, false);
    }

    public boolean canBreed() {
        return false;
    }

    public int getAge() {
        if (this.level.isClientSide) {
            return this.entityData.get(DATA_BABY_ID) ? -1 : 1;
        } else {
            return this.age;
        }
    }

    public void setAge(int age, boolean overGrow) {
        int i = this.getAge();
        i = i + age * 20;
        if (i > 0) {
            i = 0;
        }

        int k = i - i;
        this.setAgeRaw(i);
        if (overGrow) {
            this.forcedAge += k;
            if (this.forcedAgeTimer == 0) {
                this.forcedAgeTimer = 40;
            }
        }

        if (this.getAge() == 0) {
            this.setAgeRaw(this.forcedAge);
        }

    }

    public void setAge(int age) {
        this.setAge(age, false);
    }

    public void setAgeRaw(int age) {
        int i = this.age;
        this.age = age;
        if (i < 0 && age >= 0 || i >= 0 && age < 0) {
            this.entityData.set(DATA_BABY_ID, age < 0);
            this.ageBoundaryReached();
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("Age", this.getAge());
        nbt.setInt("ForcedAge", this.forcedAge);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setAgeRaw(nbt.getInt("Age"));
        this.forcedAge = nbt.getInt("ForcedAge");
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (DATA_BABY_ID.equals(data)) {
            this.updateSize();
        }

        super.onSyncedDataUpdated(data);
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (this.level.isClientSide) {
            if (this.forcedAgeTimer > 0) {
                if (this.forcedAgeTimer % 4 == 0) {
                    this.level.addParticle(Particles.HAPPY_VILLAGER, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), 0.0D, 0.0D, 0.0D);
                }

                --this.forcedAgeTimer;
            }
        } else if (this.isAlive()) {
            int i = this.getAge();
            if (i < 0) {
                ++i;
                this.setAgeRaw(i);
            } else if (i > 0) {
                --i;
                this.setAgeRaw(i);
            }
        }

    }

    protected void ageBoundaryReached() {
    }

    @Override
    public boolean isBaby() {
        return this.getAge() < 0;
    }

    @Override
    public void setBaby(boolean baby) {
        this.setAgeRaw(baby ? -24000 : 0);
    }

    public static class GroupDataAgeable implements GroupDataEntity {
        private int groupSize;
        private final boolean shouldSpawnBaby;
        private final float babySpawnChance;

        private GroupDataAgeable(boolean babyAllowed, float babyChance) {
            this.shouldSpawnBaby = babyAllowed;
            this.babySpawnChance = babyChance;
        }

        public GroupDataAgeable(boolean babyAllowed) {
            this(babyAllowed, 0.05F);
        }

        public GroupDataAgeable(float babyChance) {
            this(true, babyChance);
        }

        public int getGroupSize() {
            return this.groupSize;
        }

        public void increaseGroupSizeByOne() {
            ++this.groupSize;
        }

        public boolean isShouldSpawnBaby() {
            return this.shouldSpawnBaby;
        }

        public float getBabySpawnChance() {
            return this.babySpawnChance;
        }
    }
}
