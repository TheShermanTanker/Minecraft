package net.minecraft.world.level;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.PlayerChunkMap;
import net.minecraft.world.entity.EnumCreatureType;

public class LocalMobCapCalculator {
    private final Long2ObjectMap<List<EntityPlayer>> playersNearChunk = new Long2ObjectOpenHashMap<>();
    private final Map<EntityPlayer, LocalMobCapCalculator.MobCounts> playerMobCounts = Maps.newHashMap();
    private final PlayerChunkMap chunkMap;

    public LocalMobCapCalculator(PlayerChunkMap threadedAnvilChunkStorage) {
        this.chunkMap = threadedAnvilChunkStorage;
    }

    private List<EntityPlayer> getPlayersNear(ChunkCoordIntPair chunkPos) {
        return this.playersNearChunk.computeIfAbsent(chunkPos.pair(), (pos) -> {
            return this.chunkMap.getPlayersCloseForSpawning(chunkPos);
        });
    }

    public void addMob(ChunkCoordIntPair chunkPos, EnumCreatureType spawnGroup) {
        for(EntityPlayer serverPlayer : this.getPlayersNear(chunkPos)) {
            this.playerMobCounts.computeIfAbsent(serverPlayer, (player) -> {
                return new LocalMobCapCalculator.MobCounts();
            }).add(spawnGroup);
        }

    }

    public boolean canSpawn(EnumCreatureType spawnGroup, ChunkCoordIntPair chunkPos) {
        for(EntityPlayer serverPlayer : this.getPlayersNear(chunkPos)) {
            LocalMobCapCalculator.MobCounts mobCounts = this.playerMobCounts.get(serverPlayer);
            if (mobCounts == null || mobCounts.canSpawn(spawnGroup)) {
                return true;
            }
        }

        return false;
    }

    static class MobCounts {
        private final Object2IntMap<EnumCreatureType> counts = new Object2IntOpenHashMap<>(EnumCreatureType.values().length);

        public void add(EnumCreatureType spawnGroup) {
            this.counts.computeInt(spawnGroup, (group, density) -> {
                return density == null ? 1 : density + 1;
            });
        }

        public boolean canSpawn(EnumCreatureType spawnGroup) {
            return this.counts.getOrDefault(spawnGroup, 0) < spawnGroup.getMaxInstancesPerChunk();
        }
    }
}
