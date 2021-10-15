package net.minecraft.world.entity.npc;

import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityPositionTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.animal.horse.EntityLlamaTrader;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.MobSpawner;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.storage.IWorldDataServer;

public class MobSpawnerTrader implements MobSpawner {
    private static final int DEFAULT_TICK_DELAY = 1200;
    public static final int DEFAULT_SPAWN_DELAY = 24000;
    private static final int MIN_SPAWN_CHANCE = 25;
    private static final int MAX_SPAWN_CHANCE = 75;
    private static final int SPAWN_CHANCE_INCREASE = 25;
    private static final int SPAWN_ONE_IN_X_CHANCE = 10;
    private static final int NUMBER_OF_SPAWN_ATTEMPTS = 10;
    private final Random random = new Random();
    private final IWorldDataServer serverLevelData;
    private int tickDelay;
    private int spawnDelay;
    private int spawnChance;

    public MobSpawnerTrader(IWorldDataServer properties) {
        this.serverLevelData = properties;
        this.tickDelay = 1200;
        this.spawnDelay = properties.getWanderingTraderSpawnDelay();
        this.spawnChance = properties.getWanderingTraderSpawnChance();
        if (this.spawnDelay == 0 && this.spawnChance == 0) {
            this.spawnDelay = 24000;
            properties.setWanderingTraderSpawnDelay(this.spawnDelay);
            this.spawnChance = 25;
            properties.setWanderingTraderSpawnChance(this.spawnChance);
        }

    }

    @Override
    public int tick(WorldServer world, boolean spawnMonsters, boolean spawnAnimals) {
        if (!world.getGameRules().getBoolean(GameRules.RULE_DO_TRADER_SPAWNING)) {
            return 0;
        } else if (--this.tickDelay > 0) {
            return 0;
        } else {
            this.tickDelay = 1200;
            this.spawnDelay -= 1200;
            this.serverLevelData.setWanderingTraderSpawnDelay(this.spawnDelay);
            if (this.spawnDelay > 0) {
                return 0;
            } else {
                this.spawnDelay = 24000;
                if (!world.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
                    return 0;
                } else {
                    int i = this.spawnChance;
                    this.spawnChance = MathHelper.clamp(this.spawnChance + 25, 25, 75);
                    this.serverLevelData.setWanderingTraderSpawnChance(this.spawnChance);
                    if (this.random.nextInt(100) > i) {
                        return 0;
                    } else if (this.spawn(world)) {
                        this.spawnChance = 25;
                        return 1;
                    } else {
                        return 0;
                    }
                }
            }
        }
    }

    private boolean spawn(WorldServer world) {
        EntityHuman player = world.getRandomPlayer();
        if (player == null) {
            return true;
        } else if (this.random.nextInt(10) != 0) {
            return false;
        } else {
            BlockPosition blockPos = player.getChunkCoordinates();
            int i = 48;
            VillagePlace poiManager = world.getPoiManager();
            Optional<BlockPosition> optional = poiManager.find(VillagePlaceType.MEETING.getPredicate(), (pos) -> {
                return true;
            }, blockPos, 48, VillagePlace.Occupancy.ANY);
            BlockPosition blockPos2 = optional.orElse(blockPos);
            BlockPosition blockPos3 = this.findSpawnPositionNear(world, blockPos2, 48);
            if (blockPos3 != null && this.hasEnoughSpace(world, blockPos3)) {
                if (world.getBiomeName(blockPos3).equals(Optional.of(Biomes.THE_VOID))) {
                    return false;
                }

                EntityVillagerTrader wanderingTrader = EntityTypes.WANDERING_TRADER.spawnCreature(world, (NBTTagCompound)null, (IChatBaseComponent)null, (EntityHuman)null, blockPos3, EnumMobSpawn.EVENT, false, false);
                if (wanderingTrader != null) {
                    for(int j = 0; j < 2; ++j) {
                        this.tryToSpawnLlamaFor(world, wanderingTrader, 4);
                    }

                    this.serverLevelData.setWanderingTraderId(wanderingTrader.getUniqueID());
                    wanderingTrader.setDespawnDelay(48000);
                    wanderingTrader.setWanderTarget(blockPos2);
                    wanderingTrader.restrictTo(blockPos2, 16);
                    return true;
                }
            }

            return false;
        }
    }

    private void tryToSpawnLlamaFor(WorldServer world, EntityVillagerTrader wanderingTrader, int range) {
        BlockPosition blockPos = this.findSpawnPositionNear(world, wanderingTrader.getChunkCoordinates(), range);
        if (blockPos != null) {
            EntityLlamaTrader traderLlama = EntityTypes.TRADER_LLAMA.spawnCreature(world, (NBTTagCompound)null, (IChatBaseComponent)null, (EntityHuman)null, blockPos, EnumMobSpawn.EVENT, false, false);
            if (traderLlama != null) {
                traderLlama.setLeashHolder(wanderingTrader, true);
            }
        }
    }

    @Nullable
    private BlockPosition findSpawnPositionNear(IWorldReader world, BlockPosition pos, int range) {
        BlockPosition blockPos = null;

        for(int i = 0; i < 10; ++i) {
            int j = pos.getX() + this.random.nextInt(range * 2) - range;
            int k = pos.getZ() + this.random.nextInt(range * 2) - range;
            int l = world.getHeight(HeightMap.Type.WORLD_SURFACE, j, k);
            BlockPosition blockPos2 = new BlockPosition(j, l, k);
            if (NaturalSpawner.isSpawnPositionOk(EntityPositionTypes.Surface.ON_GROUND, world, blockPos2, EntityTypes.WANDERING_TRADER)) {
                blockPos = blockPos2;
                break;
            }
        }

        return blockPos;
    }

    private boolean hasEnoughSpace(IBlockAccess world, BlockPosition pos) {
        for(BlockPosition blockPos : BlockPosition.betweenClosed(pos, pos.offset(1, 2, 1))) {
            if (!world.getType(blockPos).getCollisionShape(world, blockPos).isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
