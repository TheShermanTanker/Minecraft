package net.minecraft.world.level;

import net.minecraft.server.level.WorldServer;

public interface MobSpawner {
    int tick(WorldServer world, boolean spawnMonsters, boolean spawnAnimals);
}
