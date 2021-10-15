package net.minecraft.world.level.storage;

import net.minecraft.core.BlockPosition;

public interface WorldDataMutable extends WorldData {
    void setXSpawn(int spawnX);

    void setYSpawn(int spawnY);

    void setZSpawn(int spawnZ);

    void setSpawnAngle(float angle);

    default void setSpawn(BlockPosition pos, float angle) {
        this.setXSpawn(pos.getX());
        this.setYSpawn(pos.getY());
        this.setZSpawn(pos.getZ());
        this.setSpawnAngle(angle);
    }
}
