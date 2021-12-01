package net.minecraft.world.level;

import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPositionTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.phys.AxisAlignedBB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class MobSpawnerAbstract {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int EVENT_SPAWN = 1;
    public int spawnDelay = 20;
    public SimpleWeightedRandomList<MobSpawnerData> spawnPotentials = SimpleWeightedRandomList.empty();
    public MobSpawnerData nextSpawnData = new MobSpawnerData();
    private double spin;
    private double oSpin;
    public int minSpawnDelay = 200;
    public int maxSpawnDelay = 800;
    public int spawnCount = 4;
    @Nullable
    private Entity displayEntity;
    public int maxNearbyEntities = 6;
    public int requiredPlayerRange = 16;
    public int spawnRange = 4;
    private final Random random = new Random();

    public void setMobName(EntityTypes<?> type) {
        this.nextSpawnData.getEntityToSpawn().setString("id", IRegistry.ENTITY_TYPE.getKey(type).toString());
    }

    public boolean isNearPlayer(World world, BlockPosition pos) {
        return world.isPlayerNearby((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, (double)this.requiredPlayerRange);
    }

    public void clientTick(World world, BlockPosition pos) {
        if (!this.isNearPlayer(world, pos)) {
            this.oSpin = this.spin;
        } else {
            double d = (double)pos.getX() + world.random.nextDouble();
            double e = (double)pos.getY() + world.random.nextDouble();
            double f = (double)pos.getZ() + world.random.nextDouble();
            world.addParticle(Particles.SMOKE, d, e, f, 0.0D, 0.0D, 0.0D);
            world.addParticle(Particles.FLAME, d, e, f, 0.0D, 0.0D, 0.0D);
            if (this.spawnDelay > 0) {
                --this.spawnDelay;
            }

            this.oSpin = this.spin;
            this.spin = (this.spin + (double)(1000.0F / ((float)this.spawnDelay + 200.0F))) % 360.0D;
        }

    }

    public void serverTick(WorldServer world, BlockPosition pos) {
        if (this.isNearPlayer(world, pos)) {
            if (this.spawnDelay == -1) {
                this.delay(world, pos);
            }

            if (this.spawnDelay > 0) {
                --this.spawnDelay;
            } else {
                boolean bl = false;

                for(int i = 0; i < this.spawnCount; ++i) {
                    NBTTagCompound compoundTag = this.nextSpawnData.getEntityToSpawn();
                    Optional<EntityTypes<?>> optional = EntityTypes.by(compoundTag);
                    if (optional.isEmpty()) {
                        this.delay(world, pos);
                        return;
                    }

                    NBTTagList listTag = compoundTag.getList("Pos", 6);
                    int j = listTag.size();
                    double d = j >= 1 ? listTag.getDouble(0) : (double)pos.getX() + (world.random.nextDouble() - world.random.nextDouble()) * (double)this.spawnRange + 0.5D;
                    double e = j >= 2 ? listTag.getDouble(1) : (double)(pos.getY() + world.random.nextInt(3) - 1);
                    double f = j >= 3 ? listTag.getDouble(2) : (double)pos.getZ() + (world.random.nextDouble() - world.random.nextDouble()) * (double)this.spawnRange + 0.5D;
                    if (world.noCollision(optional.get().getAABB(d, e, f))) {
                        BlockPosition blockPos = new BlockPosition(d, e, f);
                        if (this.nextSpawnData.getCustomSpawnRules().isPresent()) {
                            if (!optional.get().getCategory().isFriendly() && world.getDifficulty() == EnumDifficulty.PEACEFUL) {
                                continue;
                            }

                            SpawnData$CustomSpawnRules customSpawnRules = this.nextSpawnData.getCustomSpawnRules().get();
                            if (!customSpawnRules.blockLightLimit().isValueInRange(world.getBrightness(EnumSkyBlock.BLOCK, blockPos)) || !customSpawnRules.skyLightLimit().isValueInRange(world.getBrightness(EnumSkyBlock.SKY, blockPos))) {
                                continue;
                            }
                        } else if (!EntityPositionTypes.checkSpawnRules(optional.get(), world, EnumMobSpawn.SPAWNER, blockPos, world.getRandom())) {
                            continue;
                        }

                        Entity entity = EntityTypes.loadEntityRecursive(compoundTag, world, (entityx) -> {
                            entityx.setPositionRotation(d, e, f, entityx.getYRot(), entityx.getXRot());
                            return entityx;
                        });
                        if (entity == null) {
                            this.delay(world, pos);
                            return;
                        }

                        int k = world.getEntitiesOfClass(entity.getClass(), (new AxisAlignedBB((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1))).inflate((double)this.spawnRange)).size();
                        if (k >= this.maxNearbyEntities) {
                            this.delay(world, pos);
                            return;
                        }

                        entity.setPositionRotation(entity.locX(), entity.locY(), entity.locZ(), world.random.nextFloat() * 360.0F, 0.0F);
                        if (entity instanceof EntityInsentient) {
                            EntityInsentient mob = (EntityInsentient)entity;
                            if (this.nextSpawnData.getCustomSpawnRules().isEmpty() && !mob.checkSpawnRules(world, EnumMobSpawn.SPAWNER) || !mob.checkSpawnObstruction(world)) {
                                continue;
                            }

                            if (this.nextSpawnData.getEntityToSpawn().size() == 1 && this.nextSpawnData.getEntityToSpawn().hasKeyOfType("id", 8)) {
                                ((EntityInsentient)entity).prepare(world, world.getDamageScaler(entity.getChunkCoordinates()), EnumMobSpawn.SPAWNER, (GroupDataEntity)null, (NBTTagCompound)null);
                            }
                        }

                        if (!world.addAllEntitiesSafely(entity)) {
                            this.delay(world, pos);
                            return;
                        }

                        world.triggerEffect(2004, pos, 0);
                        if (entity instanceof EntityInsentient) {
                            ((EntityInsentient)entity).doSpawnEffect();
                        }

                        bl = true;
                    }
                }

                if (bl) {
                    this.delay(world, pos);
                }

            }
        }
    }

    public void delay(World world, BlockPosition pos) {
        if (this.maxSpawnDelay <= this.minSpawnDelay) {
            this.spawnDelay = this.minSpawnDelay;
        } else {
            this.spawnDelay = this.minSpawnDelay + this.random.nextInt(this.maxSpawnDelay - this.minSpawnDelay);
        }

        this.spawnPotentials.getRandom(this.random).ifPresent((wrapper) -> {
            this.setSpawnData(world, pos, wrapper.getData());
        });
        this.broadcastEvent(world, pos, 1);
    }

    public void load(@Nullable World world, BlockPosition pos, NBTTagCompound nbt) {
        this.spawnDelay = nbt.getShort("Delay");
        boolean bl = nbt.hasKeyOfType("SpawnPotentials", 9);
        boolean bl2 = nbt.hasKeyOfType("SpawnData", 10);
        if (!bl) {
            MobSpawnerData spawnData;
            if (bl2) {
                spawnData = MobSpawnerData.CODEC.parse(DynamicOpsNBT.INSTANCE, nbt.getCompound("SpawnData")).resultOrPartial((string) -> {
                    LOGGER.warn("Invalid SpawnData: {}", (Object)string);
                }).orElseGet(MobSpawnerData::new);
            } else {
                spawnData = new MobSpawnerData();
            }

            this.spawnPotentials = SimpleWeightedRandomList.single(spawnData);
            this.setSpawnData(world, pos, spawnData);
        } else {
            NBTTagList listTag = nbt.getList("SpawnPotentials", 10);
            this.spawnPotentials = MobSpawnerData.LIST_CODEC.parse(DynamicOpsNBT.INSTANCE, listTag).resultOrPartial((string) -> {
                LOGGER.warn("Invalid SpawnPotentials list: {}", (Object)string);
            }).orElseGet(SimpleWeightedRandomList::empty);
            if (bl2) {
                MobSpawnerData spawnData3 = MobSpawnerData.CODEC.parse(DynamicOpsNBT.INSTANCE, nbt.getCompound("SpawnData")).resultOrPartial((string) -> {
                    LOGGER.warn("Invalid SpawnData: {}", (Object)string);
                }).orElseGet(MobSpawnerData::new);
                this.setSpawnData(world, pos, spawnData3);
            } else {
                this.spawnPotentials.getRandom(this.random).ifPresent((wrapper) -> {
                    this.setSpawnData(world, pos, wrapper.getData());
                });
            }
        }

        if (nbt.hasKeyOfType("MinSpawnDelay", 99)) {
            this.minSpawnDelay = nbt.getShort("MinSpawnDelay");
            this.maxSpawnDelay = nbt.getShort("MaxSpawnDelay");
            this.spawnCount = nbt.getShort("SpawnCount");
        }

        if (nbt.hasKeyOfType("MaxNearbyEntities", 99)) {
            this.maxNearbyEntities = nbt.getShort("MaxNearbyEntities");
            this.requiredPlayerRange = nbt.getShort("RequiredPlayerRange");
        }

        if (nbt.hasKeyOfType("SpawnRange", 99)) {
            this.spawnRange = nbt.getShort("SpawnRange");
        }

        this.displayEntity = null;
    }

    public NBTTagCompound save(NBTTagCompound nbt) {
        nbt.setShort("Delay", (short)this.spawnDelay);
        nbt.setShort("MinSpawnDelay", (short)this.minSpawnDelay);
        nbt.setShort("MaxSpawnDelay", (short)this.maxSpawnDelay);
        nbt.setShort("SpawnCount", (short)this.spawnCount);
        nbt.setShort("MaxNearbyEntities", (short)this.maxNearbyEntities);
        nbt.setShort("RequiredPlayerRange", (short)this.requiredPlayerRange);
        nbt.setShort("SpawnRange", (short)this.spawnRange);
        nbt.set("SpawnData", MobSpawnerData.CODEC.encodeStart(DynamicOpsNBT.INSTANCE, this.nextSpawnData).result().orElseThrow(() -> {
            return new IllegalStateException("Invalid SpawnData");
        }));
        nbt.set("SpawnPotentials", MobSpawnerData.LIST_CODEC.encodeStart(DynamicOpsNBT.INSTANCE, this.spawnPotentials).result().orElseThrow());
        return nbt;
    }

    @Nullable
    public Entity getOrCreateDisplayEntity(World world) {
        if (this.displayEntity == null) {
            this.displayEntity = EntityTypes.loadEntityRecursive(this.nextSpawnData.getEntityToSpawn(), world, Function.identity());
            if (this.nextSpawnData.getEntityToSpawn().size() == 1 && this.nextSpawnData.getEntityToSpawn().hasKeyOfType("id", 8) && this.displayEntity instanceof EntityInsentient) {
            }
        }

        return this.displayEntity;
    }

    public boolean onEventTriggered(World level, int i) {
        if (i == 1) {
            if (level.isClientSide) {
                this.spawnDelay = this.minSpawnDelay;
            }

            return true;
        } else {
            return false;
        }
    }

    public void setSpawnData(@Nullable World world, BlockPosition pos, MobSpawnerData spawnEntry) {
        this.nextSpawnData = spawnEntry;
    }

    public abstract void broadcastEvent(World world, BlockPosition pos, int i);

    public double getSpin() {
        return this.spin;
    }

    public double getoSpin() {
        return this.oSpin;
    }
}
