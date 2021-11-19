package net.minecraft.world.level.levelgen;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.StatisticList;
import net.minecraft.stats.StatisticManagerServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.monster.EntityPhantom;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.MobSpawner;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;

public class MobSpawnerPhantom implements MobSpawner {
    private int nextTick;

    @Override
    public int tick(WorldServer world, boolean spawnMonsters, boolean spawnAnimals) {
        if (!spawnMonsters) {
            return 0;
        } else if (!world.getGameRules().getBoolean(GameRules.RULE_DOINSOMNIA)) {
            return 0;
        } else {
            Random random = world.random;
            --this.nextTick;
            if (this.nextTick > 0) {
                return 0;
            } else {
                this.nextTick += (60 + random.nextInt(60)) * 20;
                if (world.getSkyDarken() < 5 && world.getDimensionManager().hasSkyLight()) {
                    return 0;
                } else {
                    int i = 0;

                    for(EntityHuman player : world.getPlayers()) {
                        if (!player.isSpectator()) {
                            BlockPosition blockPos = player.getChunkCoordinates();
                            if (!world.getDimensionManager().hasSkyLight() || blockPos.getY() >= world.getSeaLevel() && world.canSeeSky(blockPos)) {
                                DifficultyDamageScaler difficultyInstance = world.getDamageScaler(blockPos);
                                if (difficultyInstance.isHarderThan(random.nextFloat() * 3.0F)) {
                                    StatisticManagerServer serverStatsCounter = ((EntityPlayer)player).getStatisticManager();
                                    int j = MathHelper.clamp(serverStatsCounter.getStatisticValue(StatisticList.CUSTOM.get(StatisticList.TIME_SINCE_REST)), 1, Integer.MAX_VALUE);
                                    int k = 24000;
                                    if (random.nextInt(j) >= 72000) {
                                        BlockPosition blockPos2 = blockPos.above(20 + random.nextInt(15)).east(-10 + random.nextInt(21)).south(-10 + random.nextInt(21));
                                        IBlockData blockState = world.getType(blockPos2);
                                        Fluid fluidState = world.getFluid(blockPos2);
                                        if (NaturalSpawner.isValidEmptySpawnBlock(world, blockPos2, blockState, fluidState, EntityTypes.PHANTOM)) {
                                            GroupDataEntity spawnGroupData = null;
                                            int l = 1 + random.nextInt(difficultyInstance.getDifficulty().getId() + 1);

                                            for(int m = 0; m < l; ++m) {
                                                EntityPhantom phantom = EntityTypes.PHANTOM.create(world);
                                                phantom.setPositionRotation(blockPos2, 0.0F, 0.0F);
                                                spawnGroupData = phantom.prepare(world, difficultyInstance, EnumMobSpawn.NATURAL, spawnGroupData, (NBTTagCompound)null);
                                                world.addAllEntities(phantom);
                                            }

                                            i += l;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    return i;
                }
            }
        }
    }
}
