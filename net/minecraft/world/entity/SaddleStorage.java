package net.minecraft.world.entity;

import java.util.Random;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;

public class SaddleStorage {
    private static final int MIN_BOOST_TIME = 140;
    private static final int MAX_BOOST_TIME = 700;
    private final DataWatcher entityData;
    private final DataWatcherObject<Integer> boostTimeAccessor;
    private final DataWatcherObject<Boolean> hasSaddleAccessor;
    public boolean boosting;
    public int boostTime;
    public int boostTimeTotal;

    public SaddleStorage(DataWatcher dataTracker, DataWatcherObject<Integer> boostTime, DataWatcherObject<Boolean> saddled) {
        this.entityData = dataTracker;
        this.boostTimeAccessor = boostTime;
        this.hasSaddleAccessor = saddled;
    }

    public void onSynced() {
        this.boosting = true;
        this.boostTime = 0;
        this.boostTimeTotal = this.entityData.get(this.boostTimeAccessor);
    }

    public boolean boost(Random random) {
        if (this.boosting) {
            return false;
        } else {
            this.boosting = true;
            this.boostTime = 0;
            this.boostTimeTotal = random.nextInt(841) + 140;
            this.entityData.set(this.boostTimeAccessor, this.boostTimeTotal);
            return true;
        }
    }

    public void addAdditionalSaveData(NBTTagCompound nbt) {
        nbt.setBoolean("Saddle", this.hasSaddle());
    }

    public void readAdditionalSaveData(NBTTagCompound nbt) {
        this.setSaddle(nbt.getBoolean("Saddle"));
    }

    public void setSaddle(boolean saddled) {
        this.entityData.set(this.hasSaddleAccessor, saddled);
    }

    public boolean hasSaddle() {
        return this.entityData.get(this.hasSaddleAccessor);
    }
}
