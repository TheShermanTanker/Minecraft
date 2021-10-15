package net.minecraft.world.level;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPositionTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.GenLayerZoomerBiome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.Vec3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class NaturalSpawner {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MIN_SPAWN_DISTANCE = 24;
    public static final int SPAWN_DISTANCE_CHUNK = 8;
    public static final int SPAWN_DISTANCE_BLOCK = 128;
    static final int MAGIC_NUMBER = (int)Math.pow(17.0D, 2.0D);
    private static final EnumCreatureType[] SPAWNING_CATEGORIES = Stream.of(EnumCreatureType.values()).filter((spawnGroup) -> {
        return spawnGroup != EnumCreatureType.MISC;
    }).toArray((i) -> {
        return new EnumCreatureType[i];
    });

    private NaturalSpawner() {
    }

    public static NaturalSpawner.SpawnState createState(int spawningChunkCount, Iterable<Entity> entities, NaturalSpawner.ChunkGetter chunkSource) {
        NaturalSpawnerPotentials potentialCalculator = new NaturalSpawnerPotentials();
        Object2IntOpenHashMap<EnumCreatureType> object2IntOpenHashMap = new Object2IntOpenHashMap<>();
        Iterator var5 = entities.iterator();

        while(true) {
            Entity entity;
            EntityInsentient mob;
            do {
                if (!var5.hasNext()) {
                    return new NaturalSpawner.SpawnState(spawningChunkCount, object2IntOpenHashMap, potentialCalculator);
                }

                entity = (Entity)var5.next();
                if (!(entity instanceof EntityInsentient)) {
                    break;
                }

                mob = (EntityInsentient)entity;
            } while(mob.isPersistent() || mob.isSpecialPersistence());

            EnumCreatureType mobCategory = entity.getEntityType().getCategory();
            if (mobCategory != EnumCreatureType.MISC) {
                BlockPosition blockPos = entity.getChunkCoordinates();
                long l = ChunkCoordIntPair.pair(SectionPosition.blockToSectionCoord(blockPos.getX()), SectionPosition.blockToSectionCoord(blockPos.getZ()));
                chunkSource.query(l, (chunk) -> {
                    BiomeSettingsMobs.MobSpawnCost mobSpawnCost = getRoughBiome(blockPos, chunk).getMobSettings().getMobSpawnCost(entity.getEntityType());
                    if (mobSpawnCost != null) {
                        potentialCalculator.addCharge(entity.getChunkCoordinates(), mobSpawnCost.getCharge());
                    }

                    object2IntOpenHashMap.addTo(mobCategory, 1);
                });
            }
        }
    }

    static BiomeBase getRoughBiome(BlockPosition pos, IChunkAccess chunk) {
        return GenLayerZoomerBiome.INSTANCE.getBiome(0L, pos.getX(), pos.getY(), pos.getZ(), chunk.getBiomeIndex());
    }

    public static void spawnForChunk(WorldServer world, Chunk chunk, NaturalSpawner.SpawnState info, boolean spawnAnimals, boolean spawnMonsters, boolean rareSpawn) {
        world.getMethodProfiler().enter("spawner");

        for(EnumCreatureType mobCategory : SPAWNING_CATEGORIES) {
            if ((spawnAnimals || !mobCategory.isFriendly()) && (spawnMonsters || mobCategory.isFriendly()) && (rareSpawn || !mobCategory.isPersistent()) && info.canSpawnForCategory(mobCategory)) {
                spawnCategoryForChunk(mobCategory, world, chunk, info::canSpawn, info::afterSpawn);
            }
        }

        world.getMethodProfiler().exit();
    }

    public static void spawnCategoryForChunk(EnumCreatureType group, WorldServer world, Chunk chunk, NaturalSpawner.SpawnPredicate checker, NaturalSpawner.AfterSpawnCallback runner) {
        BlockPosition blockPos = getRandomPosition(world, chunk);
        if (blockPos.getY() >= world.getMinBuildHeight() + 1) {
            spawnCategoryForPosition(group, world, chunk, blockPos, checker, runner);
        }
    }

    @VisibleForDebug
    public static void spawnCategoryForPosition(EnumCreatureType group, WorldServer world, BlockPosition pos) {
        spawnCategoryForPosition(group, world, world.getChunk(pos), pos, (type, posx, chunk) -> {
            return true;
        }, (entity, chunk) -> {
        });
    }

    public static void spawnCategoryForPosition(EnumCreatureType group, WorldServer world, IChunkAccess chunk, BlockPosition pos, NaturalSpawner.SpawnPredicate checker, NaturalSpawner.AfterSpawnCallback runner) {
        StructureManager structureFeatureManager = world.getStructureManager();
        ChunkGenerator chunkGenerator = world.getChunkSource().getChunkGenerator();
        int i = pos.getY();
        IBlockData blockState = chunk.getType(pos);
        if (!blockState.isOccluding(chunk, pos)) {
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
            int j = 0;

            for(int k = 0; k < 3; ++k) {
                int l = pos.getX();
                int m = pos.getZ();
                int n = 6;
                BiomeSettingsMobs.SpawnerData spawnerData = null;
                GroupDataEntity spawnGroupData = null;
                int o = MathHelper.ceil(world.random.nextFloat() * 4.0F);
                int p = 0;

                for(int q = 0; q < o; ++q) {
                    l += world.random.nextInt(6) - world.random.nextInt(6);
                    m += world.random.nextInt(6) - world.random.nextInt(6);
                    mutableBlockPos.set(l, i, m);
                    double d = (double)l + 0.5D;
                    double e = (double)m + 0.5D;
                    EntityHuman player = world.getNearestPlayer(d, (double)i, e, -1.0D, false);
                    if (player != null) {
                        double f = player.distanceToSqr(d, (double)i, e);
                        if (isRightDistanceToPlayerAndSpawnPoint(world, chunk, mutableBlockPos, f)) {
                            if (spawnerData == null) {
                                Optional<BiomeSettingsMobs.SpawnerData> optional = getRandomSpawnMobAt(world, structureFeatureManager, chunkGenerator, group, world.random, mutableBlockPos);
                                if (!optional.isPresent()) {
                                    break;
                                }

                                spawnerData = optional.get();
                                o = spawnerData.minCount + world.random.nextInt(1 + spawnerData.maxCount - spawnerData.minCount);
                            }

                            if (isValidSpawnPostitionForType(world, group, structureFeatureManager, chunkGenerator, spawnerData, mutableBlockPos, f) && checker.test(spawnerData.type, mutableBlockPos, chunk)) {
                                EntityInsentient mob = getMobForSpawn(world, spawnerData.type);
                                if (mob == null) {
                                    return;
                                }

                                mob.setPositionRotation(d, (double)i, e, world.random.nextFloat() * 360.0F, 0.0F);
                                if (isValidPositionForMob(world, mob, f)) {
                                    spawnGroupData = mob.prepare(world, world.getDamageScaler(mob.getChunkCoordinates()), EnumMobSpawn.NATURAL, spawnGroupData, (NBTTagCompound)null);
                                    ++j;
                                    ++p;
                                    world.addAllEntities(mob);
                                    runner.run(mob, chunk);
                                    if (j >= mob.getMaxSpawnGroup()) {
                                        return;
                                    }

                                    if (mob.isMaxGroupSizeReached(p)) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private static boolean isRightDistanceToPlayerAndSpawnPoint(WorldServer world, IChunkAccess chunk, BlockPosition.MutableBlockPosition pos, double squaredDistance) {
        if (squaredDistance <= 576.0D) {
            return false;
        } else if (world.getSpawn().closerThan(new Vec3D((double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D), 24.0D)) {
            return false;
        } else {
            return Objects.equals(new ChunkCoordIntPair(pos), chunk.getPos()) || world.isPositionEntityTicking(pos);
        }
    }

    private static boolean isValidSpawnPostitionForType(WorldServer world, EnumCreatureType group, StructureManager structureAccessor, ChunkGenerator chunkGenerator, BiomeSettingsMobs.SpawnerData spawnEntry, BlockPosition.MutableBlockPosition pos, double squaredDistance) {
        EntityTypes<?> entityType = spawnEntry.type;
        if (entityType.getCategory() == EnumCreatureType.MISC) {
            return false;
        } else if (!entityType.canSpawnFarFromPlayer() && squaredDistance > (double)(entityType.getCategory().getDespawnDistance() * entityType.getCategory().getDespawnDistance())) {
            return false;
        } else if (entityType.canSummon() && canSpawnMobAt(world, structureAccessor, chunkGenerator, group, spawnEntry, pos)) {
            EntityPositionTypes.Surface type = EntityPositionTypes.getPlacementType(entityType);
            if (!isSpawnPositionOk(type, world, pos, entityType)) {
                return false;
            } else if (!EntityPositionTypes.checkSpawnRules(entityType, world, EnumMobSpawn.NATURAL, pos, world.random)) {
                return false;
            } else {
                return world.noCollision(entityType.getAABB((double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D));
            }
        } else {
            return false;
        }
    }

    @Nullable
    private static EntityInsentient getMobForSpawn(WorldServer world, EntityTypes<?> type) {
        try {
            Entity entity = type.create(world);
            if (!(entity instanceof EntityInsentient)) {
                throw new IllegalStateException("Trying to spawn a non-mob: " + IRegistry.ENTITY_TYPE.getKey(type));
            } else {
                return (EntityInsentient)entity;
            }
        } catch (Exception var4) {
            LOGGER.warn("Failed to create mob", (Throwable)var4);
            return null;
        }
    }

    private static boolean isValidPositionForMob(WorldServer world, EntityInsentient entity, double squaredDistance) {
        if (squaredDistance > (double)(entity.getEntityType().getCategory().getDespawnDistance() * entity.getEntityType().getCategory().getDespawnDistance()) && entity.isTypeNotPersistent(squaredDistance)) {
            return false;
        } else {
            return entity.checkSpawnRules(world, EnumMobSpawn.NATURAL) && entity.checkSpawnObstruction(world);
        }
    }

    private static Optional<BiomeSettingsMobs.SpawnerData> getRandomSpawnMobAt(WorldServer world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, EnumCreatureType spawnGroup, Random random, BlockPosition pos) {
        BiomeBase biome = world.getBiome(pos);
        return spawnGroup == EnumCreatureType.WATER_AMBIENT && biome.getBiomeCategory() == BiomeBase.Geography.RIVER && random.nextFloat() < 0.98F ? Optional.empty() : mobsAt(world, structureAccessor, chunkGenerator, spawnGroup, pos, biome).getRandom(random);
    }

    private static boolean canSpawnMobAt(WorldServer world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, EnumCreatureType spawnGroup, BiomeSettingsMobs.SpawnerData spawnEntry, BlockPosition pos) {
        return mobsAt(world, structureAccessor, chunkGenerator, spawnGroup, pos, (BiomeBase)null).unwrap().contains(spawnEntry);
    }

    private static WeightedRandomList<BiomeSettingsMobs.SpawnerData> mobsAt(WorldServer world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, EnumCreatureType spawnGroup, BlockPosition pos, @Nullable BiomeBase biome) {
        return spawnGroup == EnumCreatureType.MONSTER && world.getType(pos.below()).is(Blocks.NETHER_BRICKS) && structureAccessor.getStructureAt(pos, false, StructureGenerator.NETHER_BRIDGE).isValid() ? StructureGenerator.NETHER_BRIDGE.getSpecialEnemies() : chunkGenerator.getMobsFor(biome != null ? biome : world.getBiome(pos), structureAccessor, spawnGroup, pos);
    }

    private static BlockPosition getRandomPosition(World world, Chunk chunk) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        int i = chunkPos.getMinBlockX() + world.random.nextInt(16);
        int j = chunkPos.getMinBlockZ() + world.random.nextInt(16);
        int k = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, i, j) + 1;
        int l = MathHelper.randomBetweenInclusive(world.random, world.getMinBuildHeight(), k);
        return new BlockPosition(i, l, j);
    }

    public static boolean isValidEmptySpawnBlock(IBlockAccess blockView, BlockPosition pos, IBlockData state, Fluid fluidState, EntityTypes<?> entityType) {
        if (state.isCollisionShapeFullBlock(blockView, pos)) {
            return false;
        } else if (state.isPowerSource()) {
            return false;
        } else if (!fluidState.isEmpty()) {
            return false;
        } else if (state.is(TagsBlock.PREVENT_MOB_SPAWNING_INSIDE)) {
            return false;
        } else {
            return !entityType.isBlockDangerous(state);
        }
    }

    public static boolean isSpawnPositionOk(EntityPositionTypes.Surface location, IWorldReader world, BlockPosition pos, @Nullable EntityTypes<?> entityType) {
        if (location == EntityPositionTypes.Surface.NO_RESTRICTIONS) {
            return true;
        } else if (entityType != null && world.getWorldBorder().isWithinBounds(pos)) {
            IBlockData blockState = world.getType(pos);
            Fluid fluidState = world.getFluid(pos);
            BlockPosition blockPos = pos.above();
            BlockPosition blockPos2 = pos.below();
            switch(location) {
            case IN_WATER:
                return fluidState.is(TagsFluid.WATER) && world.getFluid(blockPos2).is(TagsFluid.WATER) && !world.getType(blockPos).isOccluding(world, blockPos);
            case IN_LAVA:
                return fluidState.is(TagsFluid.LAVA);
            case ON_GROUND:
            default:
                IBlockData blockState2 = world.getType(blockPos2);
                if (!blockState2.isValidSpawn(world, blockPos2, entityType)) {
                    return false;
                } else {
                    return isValidEmptySpawnBlock(world, pos, blockState, fluidState, entityType) && isValidEmptySpawnBlock(world, blockPos, world.getType(blockPos), world.getFluid(blockPos), entityType);
                }
            }
        } else {
            return false;
        }
    }

    public static void spawnMobsForChunkGeneration(WorldAccess world, BiomeBase biome, ChunkCoordIntPair chunkPos, Random random) {
        BiomeSettingsMobs mobSpawnSettings = biome.getMobSettings();
        WeightedRandomList<BiomeSettingsMobs.SpawnerData> weightedRandomList = mobSpawnSettings.getMobs(EnumCreatureType.CREATURE);
        if (!weightedRandomList.isEmpty()) {
            int i = chunkPos.getMinBlockX();
            int j = chunkPos.getMinBlockZ();

            while(random.nextFloat() < mobSpawnSettings.getCreatureProbability()) {
                Optional<BiomeSettingsMobs.SpawnerData> optional = weightedRandomList.getRandom(random);
                if (optional.isPresent()) {
                    BiomeSettingsMobs.SpawnerData spawnerData = optional.get();
                    int k = spawnerData.minCount + random.nextInt(1 + spawnerData.maxCount - spawnerData.minCount);
                    GroupDataEntity spawnGroupData = null;
                    int l = i + random.nextInt(16);
                    int m = j + random.nextInt(16);
                    int n = l;
                    int o = m;

                    for(int p = 0; p < k; ++p) {
                        boolean bl = false;

                        for(int q = 0; !bl && q < 4; ++q) {
                            BlockPosition blockPos = getTopNonCollidingPos(world, spawnerData.type, l, m);
                            if (spawnerData.type.canSummon() && isSpawnPositionOk(EntityPositionTypes.getPlacementType(spawnerData.type), world, blockPos, spawnerData.type)) {
                                float f = spawnerData.type.getWidth();
                                double d = MathHelper.clamp((double)l, (double)i + (double)f, (double)i + 16.0D - (double)f);
                                double e = MathHelper.clamp((double)m, (double)j + (double)f, (double)j + 16.0D - (double)f);
                                if (!world.noCollision(spawnerData.type.getAABB(d, (double)blockPos.getY(), e)) || !EntityPositionTypes.checkSpawnRules(spawnerData.type, world, EnumMobSpawn.CHUNK_GENERATION, new BlockPosition(d, (double)blockPos.getY(), e), world.getRandom())) {
                                    continue;
                                }

                                Entity entity;
                                try {
                                    entity = spawnerData.type.create(world.getLevel());
                                } catch (Exception var27) {
                                    LOGGER.warn("Failed to create mob", (Throwable)var27);
                                    continue;
                                }

                                entity.setPositionRotation(d, (double)blockPos.getY(), e, random.nextFloat() * 360.0F, 0.0F);
                                if (entity instanceof EntityInsentient) {
                                    EntityInsentient mob = (EntityInsentient)entity;
                                    if (mob.checkSpawnRules(world, EnumMobSpawn.CHUNK_GENERATION) && mob.checkSpawnObstruction(world)) {
                                        spawnGroupData = mob.prepare(world, world.getDamageScaler(mob.getChunkCoordinates()), EnumMobSpawn.CHUNK_GENERATION, spawnGroupData, (NBTTagCompound)null);
                                        world.addAllEntities(mob);
                                        bl = true;
                                    }
                                }
                            }

                            l += random.nextInt(5) - random.nextInt(5);

                            for(m += random.nextInt(5) - random.nextInt(5); l < i || l >= i + 16 || m < j || m >= j + 16; m = o + random.nextInt(5) - random.nextInt(5)) {
                                l = n + random.nextInt(5) - random.nextInt(5);
                            }
                        }
                    }
                }
            }

        }
    }

    private static BlockPosition getTopNonCollidingPos(IWorldReader world, EntityTypes<?> entityType, int x, int z) {
        int i = world.getHeight(EntityPositionTypes.getHeightmapType(entityType), x, z);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(x, i, z);
        if (world.getDimensionManager().hasCeiling()) {
            do {
                mutableBlockPos.move(EnumDirection.DOWN);
            } while(!world.getType(mutableBlockPos).isAir());

            do {
                mutableBlockPos.move(EnumDirection.DOWN);
            } while(world.getType(mutableBlockPos).isAir() && mutableBlockPos.getY() > world.getMinBuildHeight());
        }

        if (EntityPositionTypes.getPlacementType(entityType) == EntityPositionTypes.Surface.ON_GROUND) {
            BlockPosition blockPos = mutableBlockPos.below();
            if (world.getType(blockPos).isPathfindable(world, blockPos, PathMode.LAND)) {
                return blockPos;
            }
        }

        return mutableBlockPos.immutableCopy();
    }

    @FunctionalInterface
    public interface AfterSpawnCallback {
        void run(EntityInsentient entity, IChunkAccess chunk);
    }

    @FunctionalInterface
    public interface ChunkGetter {
        void query(long pos, Consumer<Chunk> chunkConsumer);
    }

    @FunctionalInterface
    public interface SpawnPredicate {
        boolean test(EntityTypes<?> type, BlockPosition pos, IChunkAccess chunk);
    }

    public static class SpawnState {
        private final int spawnableChunkCount;
        private final Object2IntOpenHashMap<EnumCreatureType> mobCategoryCounts;
        private final NaturalSpawnerPotentials spawnPotential;
        private final Object2IntMap<EnumCreatureType> unmodifiableMobCategoryCounts;
        @Nullable
        private BlockPosition lastCheckedPos;
        @Nullable
        private EntityTypes<?> lastCheckedType;
        private double lastCharge;

        SpawnState(int spawningChunkCount, Object2IntOpenHashMap<EnumCreatureType> groupToCount, NaturalSpawnerPotentials densityField) {
            this.spawnableChunkCount = spawningChunkCount;
            this.mobCategoryCounts = groupToCount;
            this.spawnPotential = densityField;
            this.unmodifiableMobCategoryCounts = Object2IntMaps.unmodifiable(groupToCount);
        }

        private boolean canSpawn(EntityTypes<?> type, BlockPosition pos, IChunkAccess chunk) {
            this.lastCheckedPos = pos;
            this.lastCheckedType = type;
            BiomeSettingsMobs.MobSpawnCost mobSpawnCost = NaturalSpawner.getRoughBiome(pos, chunk).getMobSettings().getMobSpawnCost(type);
            if (mobSpawnCost == null) {
                this.lastCharge = 0.0D;
                return true;
            } else {
                double d = mobSpawnCost.getCharge();
                this.lastCharge = d;
                double e = this.spawnPotential.getPotentialEnergyChange(pos, d);
                return e <= mobSpawnCost.getEnergyBudget();
            }
        }

        private void afterSpawn(EntityInsentient entity, IChunkAccess chunk) {
            EntityTypes<?> entityType = entity.getEntityType();
            BlockPosition blockPos = entity.getChunkCoordinates();
            double d;
            if (blockPos.equals(this.lastCheckedPos) && entityType == this.lastCheckedType) {
                d = this.lastCharge;
            } else {
                BiomeSettingsMobs.MobSpawnCost mobSpawnCost = NaturalSpawner.getRoughBiome(blockPos, chunk).getMobSettings().getMobSpawnCost(entityType);
                if (mobSpawnCost != null) {
                    d = mobSpawnCost.getCharge();
                } else {
                    d = 0.0D;
                }
            }

            this.spawnPotential.addCharge(blockPos, d);
            this.mobCategoryCounts.addTo(entityType.getCategory(), 1);
        }

        public int getSpawnableChunkCount() {
            return this.spawnableChunkCount;
        }

        public Object2IntMap<EnumCreatureType> getMobCategoryCounts() {
            return this.unmodifiableMobCategoryCounts;
        }

        boolean canSpawnForCategory(EnumCreatureType group) {
            int i = group.getMaxInstancesPerChunk() * this.spawnableChunkCount / NaturalSpawner.MAGIC_NUMBER;
            return this.mobCategoryCounts.getInt(group) < i;
        }
    }
}
