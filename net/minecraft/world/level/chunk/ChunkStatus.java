package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.LightEngineThreaded;
import net.minecraft.server.level.PlayerChunk;
import net.minecraft.server.level.RegionLimitedWorldAccess;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class ChunkStatus {
    private static final EnumSet<HeightMap.Type> PRE_FEATURES = EnumSet.of(HeightMap.Type.OCEAN_FLOOR_WG, HeightMap.Type.WORLD_SURFACE_WG);
    private static final EnumSet<HeightMap.Type> POST_FEATURES = EnumSet.of(HeightMap.Type.OCEAN_FLOOR, HeightMap.Type.WORLD_SURFACE, HeightMap.Type.MOTION_BLOCKING, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES);
    private static final ChunkStatus.LoadingTask PASSTHROUGH_LOAD_TASK = (targetStatus, world, structureManager, lightingProvider, function, chunk) -> {
        if (chunk instanceof ProtoChunk && !chunk.getChunkStatus().isOrAfter(targetStatus)) {
            ((ProtoChunk)chunk).setStatus(targetStatus);
        }

        return CompletableFuture.completedFuture(Either.left(chunk));
    };
    public static final ChunkStatus EMPTY = registerSimple("empty", (ChunkStatus)null, -1, PRE_FEATURES, ChunkStatus.Type.PROTOCHUNK, (chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
    });
    public static final ChunkStatus STRUCTURE_STARTS = register("structure_starts", EMPTY, 0, PRE_FEATURES, ChunkStatus.Type.PROTOCHUNK, (targetStatus, executor, world, chunkGenerator, structureManager, threadedLevelLightEngine, function, list, chunkAccess) -> {
        if (!chunkAccess.getChunkStatus().isOrAfter(targetStatus)) {
            if (world.getMinecraftServer().getSaveData().getGeneratorSettings().shouldGenerateMapFeatures()) {
                chunkGenerator.createStructures(world.registryAccess(), world.getStructureManager(), chunkAccess, structureManager, world.getSeed());
            }

            if (chunkAccess instanceof ProtoChunk) {
                ((ProtoChunk)chunkAccess).setStatus(targetStatus);
            }
        }

        return CompletableFuture.completedFuture(Either.left(chunkAccess));
    });
    public static final ChunkStatus STRUCTURE_REFERENCES = registerSimple("structure_references", STRUCTURE_STARTS, 8, PRE_FEATURES, ChunkStatus.Type.PROTOCHUNK, (chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
        RegionLimitedWorldAccess worldGenRegion = new RegionLimitedWorldAccess(serverLevel, list, chunkStatus, -1);
        chunkGenerator.storeStructures(worldGenRegion, serverLevel.getStructureManager().forWorldGenRegion(worldGenRegion), chunkAccess);
    });
    public static final ChunkStatus BIOMES = registerSimple("biomes", STRUCTURE_REFERENCES, 0, PRE_FEATURES, ChunkStatus.Type.PROTOCHUNK, (chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
        chunkGenerator.createBiomes(serverLevel.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY), chunkAccess);
    });
    public static final ChunkStatus NOISE = register("noise", BIOMES, 8, PRE_FEATURES, ChunkStatus.Type.PROTOCHUNK, (chunkStatus, executor, world, chunkGenerator, structureManager, threadedLevelLightEngine, function, list, chunkAccess) -> {
        if (!chunkAccess.getChunkStatus().isOrAfter(chunkStatus)) {
            RegionLimitedWorldAccess worldGenRegion = new RegionLimitedWorldAccess(world, list, chunkStatus, 0);
            return chunkGenerator.buildNoise(executor, world.getStructureManager().forWorldGenRegion(worldGenRegion), chunkAccess).thenApply((chunkAccessx) -> {
                if (chunkAccessx instanceof ProtoChunk) {
                    ((ProtoChunk)chunkAccessx).setStatus(chunkStatus);
                }

                return Either.left(chunkAccessx);
            });
        } else {
            return CompletableFuture.completedFuture(Either.left(chunkAccess));
        }
    });
    public static final ChunkStatus SURFACE = registerSimple("surface", NOISE, 0, PRE_FEATURES, ChunkStatus.Type.PROTOCHUNK, (chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
        chunkGenerator.buildBase(new RegionLimitedWorldAccess(serverLevel, list, chunkStatus, 0), chunkAccess);
    });
    public static final ChunkStatus CARVERS = registerSimple("carvers", SURFACE, 0, PRE_FEATURES, ChunkStatus.Type.PROTOCHUNK, (chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
        chunkGenerator.doCarving(serverLevel.getSeed(), serverLevel.getBiomeManager(), chunkAccess, WorldGenStage.Features.AIR);
    });
    public static final ChunkStatus LIQUID_CARVERS = registerSimple("liquid_carvers", CARVERS, 0, POST_FEATURES, ChunkStatus.Type.PROTOCHUNK, (chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
        chunkGenerator.doCarving(serverLevel.getSeed(), serverLevel.getBiomeManager(), chunkAccess, WorldGenStage.Features.LIQUID);
    });
    public static final ChunkStatus FEATURES = register("features", LIQUID_CARVERS, 8, POST_FEATURES, ChunkStatus.Type.PROTOCHUNK, (status, executor, serverLevel, chunkGenerator, structureManager, threadedLevelLightEngine, function, list, chunkAccess) -> {
        ProtoChunk protoChunk = (ProtoChunk)chunkAccess;
        protoChunk.setLightEngine(threadedLevelLightEngine);
        if (!chunkAccess.getChunkStatus().isOrAfter(status)) {
            HeightMap.primeHeightmaps(chunkAccess, EnumSet.of(HeightMap.Type.MOTION_BLOCKING, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, HeightMap.Type.OCEAN_FLOOR, HeightMap.Type.WORLD_SURFACE));
            RegionLimitedWorldAccess worldGenRegion = new RegionLimitedWorldAccess(serverLevel, list, status, 1);
            chunkGenerator.addDecorations(worldGenRegion, serverLevel.getStructureManager().forWorldGenRegion(worldGenRegion));
            protoChunk.setStatus(status);
        }

        return CompletableFuture.completedFuture(Either.left(chunkAccess));
    });
    public static final ChunkStatus LIGHT = register("light", FEATURES, 1, POST_FEATURES, ChunkStatus.Type.PROTOCHUNK, (targetStatus, executor, serverLevel, chunkGenerator, structureManager, threadedLevelLightEngine, function, list, chunkAccess) -> {
        return lightChunk(targetStatus, threadedLevelLightEngine, chunkAccess);
    }, (status, world, structureManager, lightingProvider, function, chunk) -> {
        return lightChunk(status, lightingProvider, chunk);
    });
    public static final ChunkStatus SPAWN = registerSimple("spawn", LIGHT, 0, POST_FEATURES, ChunkStatus.Type.PROTOCHUNK, (chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
        chunkGenerator.addMobs(new RegionLimitedWorldAccess(serverLevel, list, chunkStatus, -1));
    });
    public static final ChunkStatus HEIGHTMAPS = registerSimple("heightmaps", SPAWN, 0, POST_FEATURES, ChunkStatus.Type.PROTOCHUNK, (chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
    });
    public static final ChunkStatus FULL = register("full", HEIGHTMAPS, 0, POST_FEATURES, ChunkStatus.Type.LEVELCHUNK, (targetStatus, executor, serverLevel, chunkGenerator, structureManager, threadedLevelLightEngine, function, list, chunkAccess) -> {
        return function.apply(chunkAccess);
    }, (status, world, structureManager, lightingProvider, function, chunk) -> {
        return function.apply(chunk);
    });
    private static final List<ChunkStatus> STATUS_BY_RANGE = ImmutableList.of(FULL, FEATURES, LIQUID_CARVERS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS);
    private static final IntList RANGE_BY_STATUS = SystemUtils.make(new IntArrayList(getStatusList().size()), (intArrayList) -> {
        int i = 0;

        for(int j = getStatusList().size() - 1; j >= 0; --j) {
            while(i + 1 < STATUS_BY_RANGE.size() && j <= STATUS_BY_RANGE.get(i + 1).getIndex()) {
                ++i;
            }

            intArrayList.add(0, i);
        }

    });
    private final String name;
    private final int index;
    private final ChunkStatus parent;
    private final ChunkStatus.GenerationTask generationTask;
    private final ChunkStatus.LoadingTask loadingTask;
    private final int range;
    private final ChunkStatus.Type chunkType;
    private final EnumSet<HeightMap.Type> heightmapsAfter;

    private static CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> lightChunk(ChunkStatus status, LightEngineThreaded lightingProvider, IChunkAccess chunk) {
        boolean bl = isLighted(status, chunk);
        if (!chunk.getChunkStatus().isOrAfter(status)) {
            ((ProtoChunk)chunk).setStatus(status);
        }

        return lightingProvider.lightChunk(chunk, bl).thenApply(Either::left);
    }

    private static ChunkStatus registerSimple(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<HeightMap.Type> heightMapTypes, ChunkStatus.Type chunkType, ChunkStatus.SimpleGenerationTask task) {
        return register(id, previous, taskMargin, heightMapTypes, chunkType, task);
    }

    private static ChunkStatus register(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<HeightMap.Type> heightMapTypes, ChunkStatus.Type chunkType, ChunkStatus.GenerationTask task) {
        return register(id, previous, taskMargin, heightMapTypes, chunkType, task, PASSTHROUGH_LOAD_TASK);
    }

    private static ChunkStatus register(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<HeightMap.Type> heightMapTypes, ChunkStatus.Type chunkType, ChunkStatus.GenerationTask task, ChunkStatus.LoadingTask loadTask) {
        return IRegistry.register(IRegistry.CHUNK_STATUS, id, new ChunkStatus(id, previous, taskMargin, heightMapTypes, chunkType, task, loadTask));
    }

    public static List<ChunkStatus> getStatusList() {
        List<ChunkStatus> list = Lists.newArrayList();

        ChunkStatus chunkStatus;
        for(chunkStatus = FULL; chunkStatus.getParent() != chunkStatus; chunkStatus = chunkStatus.getParent()) {
            list.add(chunkStatus);
        }

        list.add(chunkStatus);
        Collections.reverse(list);
        return list;
    }

    private static boolean isLighted(ChunkStatus status, IChunkAccess chunk) {
        return chunk.getChunkStatus().isOrAfter(status) && chunk.isLightCorrect();
    }

    public static ChunkStatus getStatusAroundFullChunk(int level) {
        if (level >= STATUS_BY_RANGE.size()) {
            return EMPTY;
        } else {
            return level < 0 ? FULL : STATUS_BY_RANGE.get(level);
        }
    }

    public static int maxDistance() {
        return STATUS_BY_RANGE.size();
    }

    public static int getDistance(ChunkStatus status) {
        return RANGE_BY_STATUS.getInt(status.getIndex());
    }

    ChunkStatus(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<HeightMap.Type> heightMapTypes, ChunkStatus.Type chunkType, ChunkStatus.GenerationTask generationTask, ChunkStatus.LoadingTask loadTask) {
        this.name = id;
        this.parent = previous == null ? this : previous;
        this.generationTask = generationTask;
        this.loadingTask = loadTask;
        this.range = taskMargin;
        this.chunkType = chunkType;
        this.heightmapsAfter = heightMapTypes;
        this.index = previous == null ? 0 : previous.getIndex() + 1;
    }

    public int getIndex() {
        return this.index;
    }

    public String getName() {
        return this.name;
    }

    public ChunkStatus getParent() {
        return this.parent;
    }

    public CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> generate(Executor executor, WorldServer world, ChunkGenerator chunkGenerator, DefinedStructureManager structureManager, LightEngineThreaded lightingProvider, Function<IChunkAccess, CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>>> function, List<IChunkAccess> list) {
        return this.generationTask.doWork(this, executor, world, chunkGenerator, structureManager, lightingProvider, function, list, list.get(list.size() / 2));
    }

    public CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> load(WorldServer world, DefinedStructureManager structureManager, LightEngineThreaded lightingProvider, Function<IChunkAccess, CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>>> function, IChunkAccess chunk) {
        return this.loadingTask.doWork(this, world, structureManager, lightingProvider, function, chunk);
    }

    public int getRange() {
        return this.range;
    }

    public ChunkStatus.Type getType() {
        return this.chunkType;
    }

    public static ChunkStatus byName(String id) {
        return IRegistry.CHUNK_STATUS.get(MinecraftKey.tryParse(id));
    }

    public EnumSet<HeightMap.Type> heightmapsAfter() {
        return this.heightmapsAfter;
    }

    public boolean isOrAfter(ChunkStatus chunk) {
        return this.getIndex() >= chunk.getIndex();
    }

    @Override
    public String toString() {
        return IRegistry.CHUNK_STATUS.getKey(this).toString();
    }

    interface GenerationTask {
        CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> doWork(ChunkStatus targetStatus, Executor executor, WorldServer world, ChunkGenerator chunkGenerator, DefinedStructureManager structureManager, LightEngineThreaded lightingProvider, Function<IChunkAccess, CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>>> function, List<IChunkAccess> list, IChunkAccess chunkAccess);
    }

    interface LoadingTask {
        CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> doWork(ChunkStatus targetStatus, WorldServer world, DefinedStructureManager structureManager, LightEngineThreaded lightingProvider, Function<IChunkAccess, CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>>> function, IChunkAccess chunk);
    }

    interface SimpleGenerationTask extends ChunkStatus.GenerationTask {
        @Override
        default CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> doWork(ChunkStatus targetStatus, Executor executor, WorldServer world, ChunkGenerator chunkGenerator, DefinedStructureManager structureManager, LightEngineThreaded lightingProvider, Function<IChunkAccess, CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>>> function, List<IChunkAccess> list, IChunkAccess chunkAccess) {
            if (!chunkAccess.getChunkStatus().isOrAfter(targetStatus)) {
                this.doWork(targetStatus, world, chunkGenerator, list, chunkAccess);
                if (chunkAccess instanceof ProtoChunk) {
                    ((ProtoChunk)chunkAccess).setStatus(targetStatus);
                }
            }

            return CompletableFuture.completedFuture(Either.left(chunkAccess));
        }

        void doWork(ChunkStatus targetStatus, WorldServer world, ChunkGenerator chunkGenerator, List<IChunkAccess> list, IChunkAccess chunkAccess);
    }

    public static enum Type {
        PROTOCHUNK,
        LEVELCHUNK;
    }
}
