package net.minecraft.world.entity.npc;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityPositionTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.animal.EntityCat;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.MobSpawner;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.phys.AxisAlignedBB;

public class MobSpawnerCat implements MobSpawner {
    private static final int TICK_DELAY = 1200;
    private int nextTick;

    @Override
    public int tick(WorldServer world, boolean spawnMonsters, boolean spawnAnimals) {
        if (spawnAnimals && world.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            --this.nextTick;
            if (this.nextTick > 0) {
                return 0;
            } else {
                this.nextTick = 1200;
                EntityHuman player = world.getRandomPlayer();
                if (player == null) {
                    return 0;
                } else {
                    Random random = world.random;
                    int i = (8 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
                    int j = (8 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
                    BlockPosition blockPos = player.getChunkCoordinates().offset(i, 0, j);
                    int k = 10;
                    if (!world.hasChunksAt(blockPos.getX() - 10, blockPos.getZ() - 10, blockPos.getX() + 10, blockPos.getZ() + 10)) {
                        return 0;
                    } else {
                        if (NaturalSpawner.isSpawnPositionOk(EntityPositionTypes.Surface.ON_GROUND, world, blockPos, EntityTypes.CAT)) {
                            if (world.isCloseToVillage(blockPos, 2)) {
                                return this.spawnInVillage(world, blockPos);
                            }

                            if (world.getStructureManager().getStructureWithPieceAt(blockPos, StructureGenerator.SWAMP_HUT).isValid()) {
                                return this.spawnInHut(world, blockPos);
                            }
                        }

                        return 0;
                    }
                }
            }
        } else {
            return 0;
        }
    }

    private int spawnInVillage(WorldServer world, BlockPosition pos) {
        int i = 48;
        if (world.getPoiManager().getCountInRange(VillagePlaceType.HOME.getPredicate(), pos, 48, VillagePlace.Occupancy.IS_OCCUPIED) > 4L) {
            List<EntityCat> list = world.getEntitiesOfClass(EntityCat.class, (new AxisAlignedBB(pos)).grow(48.0D, 8.0D, 48.0D));
            if (list.size() < 5) {
                return this.spawnCat(pos, world);
            }
        }

        return 0;
    }

    private int spawnInHut(WorldServer world, BlockPosition pos) {
        int i = 16;
        List<EntityCat> list = world.getEntitiesOfClass(EntityCat.class, (new AxisAlignedBB(pos)).grow(16.0D, 8.0D, 16.0D));
        return list.size() < 1 ? this.spawnCat(pos, world) : 0;
    }

    private int spawnCat(BlockPosition pos, WorldServer world) {
        EntityCat cat = EntityTypes.CAT.create(world);
        if (cat == null) {
            return 0;
        } else {
            cat.prepare(world, world.getDamageScaler(pos), EnumMobSpawn.NATURAL, (GroupDataEntity)null, (NBTTagCompound)null);
            cat.setPositionRotation(pos, 0.0F, 0.0F);
            world.addAllEntities(cat);
            return 1;
        }
    }
}
