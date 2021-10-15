package net.minecraft.world.level.levelgen;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.monster.EntityMonsterPatrolling;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.MobSpawner;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.IBlockData;

public class MobSpawnerPatrol implements MobSpawner {
    private int nextTick;

    @Override
    public int tick(WorldServer world, boolean spawnMonsters, boolean spawnAnimals) {
        if (!spawnMonsters) {
            return 0;
        } else if (!world.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
            return 0;
        } else {
            Random random = world.random;
            --this.nextTick;
            if (this.nextTick > 0) {
                return 0;
            } else {
                this.nextTick += 12000 + random.nextInt(1200);
                long l = world.getDayTime() / 24000L;
                if (l >= 5L && world.isDay()) {
                    if (random.nextInt(5) != 0) {
                        return 0;
                    } else {
                        int i = world.getPlayers().size();
                        if (i < 1) {
                            return 0;
                        } else {
                            EntityHuman player = world.getPlayers().get(random.nextInt(i));
                            if (player.isSpectator()) {
                                return 0;
                            } else if (world.isCloseToVillage(player.getChunkCoordinates(), 2)) {
                                return 0;
                            } else {
                                int j = (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
                                int k = (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
                                BlockPosition.MutableBlockPosition mutableBlockPos = player.getChunkCoordinates().mutable().move(j, 0, k);
                                int m = 10;
                                if (!world.hasChunksAt(mutableBlockPos.getX() - 10, mutableBlockPos.getZ() - 10, mutableBlockPos.getX() + 10, mutableBlockPos.getZ() + 10)) {
                                    return 0;
                                } else {
                                    BiomeBase biome = world.getBiome(mutableBlockPos);
                                    BiomeBase.Geography biomeCategory = biome.getBiomeCategory();
                                    if (biomeCategory == BiomeBase.Geography.MUSHROOM) {
                                        return 0;
                                    } else {
                                        int n = 0;
                                        int o = (int)Math.ceil((double)world.getDamageScaler(mutableBlockPos).getEffectiveDifficulty()) + 1;

                                        for(int p = 0; p < o; ++p) {
                                            ++n;
                                            mutableBlockPos.setY(world.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, mutableBlockPos).getY());
                                            if (p == 0) {
                                                if (!this.spawnPatrolMember(world, mutableBlockPos, random, true)) {
                                                    break;
                                                }
                                            } else {
                                                this.spawnPatrolMember(world, mutableBlockPos, random, false);
                                            }

                                            mutableBlockPos.setX(mutableBlockPos.getX() + random.nextInt(5) - random.nextInt(5));
                                            mutableBlockPos.setZ(mutableBlockPos.getZ() + random.nextInt(5) - random.nextInt(5));
                                        }

                                        return n;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    return 0;
                }
            }
        }
    }

    private boolean spawnPatrolMember(WorldServer world, BlockPosition pos, Random random, boolean captain) {
        IBlockData blockState = world.getType(pos);
        if (!NaturalSpawner.isValidEmptySpawnBlock(world, pos, blockState, blockState.getFluid(), EntityTypes.PILLAGER)) {
            return false;
        } else if (!EntityMonsterPatrolling.checkPatrollingMonsterSpawnRules(EntityTypes.PILLAGER, world, EnumMobSpawn.PATROL, pos, random)) {
            return false;
        } else {
            EntityMonsterPatrolling patrollingMonster = EntityTypes.PILLAGER.create(world);
            if (patrollingMonster != null) {
                if (captain) {
                    patrollingMonster.setPatrolLeader(true);
                    patrollingMonster.findPatrolTarget();
                }

                patrollingMonster.setPosition((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
                patrollingMonster.prepare(world, world.getDamageScaler(pos), EnumMobSpawn.PATROL, (GroupDataEntity)null, (NBTTagCompound)null);
                world.addAllEntities(patrollingMonster);
                return true;
            } else {
                return false;
            }
        }
    }
}
