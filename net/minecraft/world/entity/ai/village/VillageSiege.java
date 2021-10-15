package net.minecraft.world.entity.ai.village;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.monster.EntityZombie;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.MobSpawner;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.phys.Vec3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VillageSiege implements MobSpawner {
    private static final Logger LOGGER = LogManager.getLogger();
    private boolean hasSetupSiege;
    private VillageSiege.State siegeState = VillageSiege.State.SIEGE_DONE;
    private int zombiesToSpawn;
    private int nextSpawnTime;
    private int spawnX;
    private int spawnY;
    private int spawnZ;

    @Override
    public int tick(WorldServer world, boolean spawnMonsters, boolean spawnAnimals) {
        if (!world.isDay() && spawnMonsters) {
            float f = world.getTimeOfDay(0.0F);
            if ((double)f == 0.5D) {
                this.siegeState = world.random.nextInt(10) == 0 ? VillageSiege.State.SIEGE_TONIGHT : VillageSiege.State.SIEGE_DONE;
            }

            if (this.siegeState == VillageSiege.State.SIEGE_DONE) {
                return 0;
            } else {
                if (!this.hasSetupSiege) {
                    if (!this.tryToSetupSiege(world)) {
                        return 0;
                    }

                    this.hasSetupSiege = true;
                }

                if (this.nextSpawnTime > 0) {
                    --this.nextSpawnTime;
                    return 0;
                } else {
                    this.nextSpawnTime = 2;
                    if (this.zombiesToSpawn > 0) {
                        this.trySpawn(world);
                        --this.zombiesToSpawn;
                    } else {
                        this.siegeState = VillageSiege.State.SIEGE_DONE;
                    }

                    return 1;
                }
            }
        } else {
            this.siegeState = VillageSiege.State.SIEGE_DONE;
            this.hasSetupSiege = false;
            return 0;
        }
    }

    private boolean tryToSetupSiege(WorldServer world) {
        for(EntityHuman player : world.getPlayers()) {
            if (!player.isSpectator()) {
                BlockPosition blockPos = player.getChunkCoordinates();
                if (world.isVillage(blockPos) && world.getBiome(blockPos).getBiomeCategory() != BiomeBase.Geography.MUSHROOM) {
                    for(int i = 0; i < 10; ++i) {
                        float f = world.random.nextFloat() * ((float)Math.PI * 2F);
                        this.spawnX = blockPos.getX() + MathHelper.floor(MathHelper.cos(f) * 32.0F);
                        this.spawnY = blockPos.getY();
                        this.spawnZ = blockPos.getZ() + MathHelper.floor(MathHelper.sin(f) * 32.0F);
                        if (this.findRandomSpawnPos(world, new BlockPosition(this.spawnX, this.spawnY, this.spawnZ)) != null) {
                            this.nextSpawnTime = 0;
                            this.zombiesToSpawn = 20;
                            break;
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    private void trySpawn(WorldServer world) {
        Vec3D vec3 = this.findRandomSpawnPos(world, new BlockPosition(this.spawnX, this.spawnY, this.spawnZ));
        if (vec3 != null) {
            EntityZombie zombie;
            try {
                zombie = new EntityZombie(world);
                zombie.prepare(world, world.getDamageScaler(zombie.getChunkCoordinates()), EnumMobSpawn.EVENT, (GroupDataEntity)null, (NBTTagCompound)null);
            } catch (Exception var5) {
                LOGGER.warn("Failed to create zombie for village siege at {}", vec3, var5);
                return;
            }

            zombie.setPositionRotation(vec3.x, vec3.y, vec3.z, world.random.nextFloat() * 360.0F, 0.0F);
            world.addAllEntities(zombie);
        }
    }

    @Nullable
    private Vec3D findRandomSpawnPos(WorldServer world, BlockPosition pos) {
        for(int i = 0; i < 10; ++i) {
            int j = pos.getX() + world.random.nextInt(16) - 8;
            int k = pos.getZ() + world.random.nextInt(16) - 8;
            int l = world.getHeight(HeightMap.Type.WORLD_SURFACE, j, k);
            BlockPosition blockPos = new BlockPosition(j, l, k);
            if (world.isVillage(blockPos) && EntityMonster.checkMonsterSpawnRules(EntityTypes.ZOMBIE, world, EnumMobSpawn.EVENT, blockPos, world.random)) {
                return Vec3D.atBottomCenterOf(blockPos);
            }
        }

        return null;
    }

    static enum State {
        SIEGE_CAN_ACTIVATE,
        SIEGE_TONIGHT,
        SIEGE_DONE;
    }
}
