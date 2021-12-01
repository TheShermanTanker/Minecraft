package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.feature.configurations.RangeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.StructureSettingsFeature;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfigurationChance;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureOceanRuinConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureRuinedPortalConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureShipwreckConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureVillageConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenMineshaftConfiguration;
import net.minecraft.world.level.levelgen.structure.PostPlacementProcessor;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenFeatureNetherFossil;
import net.minecraft.world.level.levelgen.structure.WorldGenFeatureOceanRuin;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StructureGenerator<C extends WorldGenFeatureConfiguration> {
    public static final BiMap<String, StructureGenerator<?>> STRUCTURES_REGISTRY = HashBiMap.create();
    private static final Map<StructureGenerator<?>, WorldGenStage.Decoration> STEP = Maps.newHashMap();
    private static final Logger LOGGER = LogManager.getLogger();
    public static final StructureGenerator<WorldGenFeatureVillageConfiguration> PILLAGER_OUTPOST = register("Pillager_Outpost", new WorldGenFeaturePillagerOutpost(WorldGenFeatureVillageConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenMineshaftConfiguration> MINESHAFT = register("Mineshaft", new WorldGenMineshaft(WorldGenMineshaftConfiguration.CODEC), WorldGenStage.Decoration.UNDERGROUND_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> WOODLAND_MANSION = register("Mansion", new WorldGenWoodlandMansion(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> JUNGLE_TEMPLE = register("Jungle_Pyramid", new WorldGenFeatureJunglePyramid(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> DESERT_PYRAMID = register("Desert_Pyramid", new WorldGenFeatureDesertPyramid(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> IGLOO = register("Igloo", new WorldGenFeatureIgloo(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureRuinedPortalConfiguration> RUINED_PORTAL = register("Ruined_Portal", new WorldGenFeatureRuinedPortal(WorldGenFeatureRuinedPortalConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureShipwreckConfiguration> SHIPWRECK = register("Shipwreck", new WorldGenFeatureShipwreck(WorldGenFeatureShipwreckConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> SWAMP_HUT = register("Swamp_Hut", new WorldGenFeatureSwampHut(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> STRONGHOLD = register("Stronghold", new WorldGenStronghold(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.STRONGHOLDS);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> OCEAN_MONUMENT = register("Monument", new WorldGenMonument(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureOceanRuinConfiguration> OCEAN_RUIN = register("Ocean_Ruin", new WorldGenFeatureOceanRuin(WorldGenFeatureOceanRuinConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> NETHER_BRIDGE = register("Fortress", new WorldGenNether(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.UNDERGROUND_DECORATION);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> END_CITY = register("EndCity", new WorldGenEndCity(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureConfigurationChance> BURIED_TREASURE = register("Buried_Treasure", new WorldGenBuriedTreasure(WorldGenFeatureConfigurationChance.CODEC), WorldGenStage.Decoration.UNDERGROUND_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureVillageConfiguration> VILLAGE = register("Village", new WorldGenVillage(WorldGenFeatureVillageConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<RangeConfiguration> NETHER_FOSSIL = register("Nether_Fossil", new WorldGenFeatureNetherFossil(RangeConfiguration.CODEC), WorldGenStage.Decoration.UNDERGROUND_DECORATION);
    public static final StructureGenerator<WorldGenFeatureVillageConfiguration> BASTION_REMNANT = register("Bastion_Remnant", new WorldGenFeatureBastionRemnant(WorldGenFeatureVillageConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final List<StructureGenerator<?>> NOISE_AFFECTING_FEATURES = ImmutableList.of(PILLAGER_OUTPOST, VILLAGE, NETHER_FOSSIL, STRONGHOLD);
    public static final int MAX_STRUCTURE_RANGE = 8;
    private final Codec<StructureFeature<C, StructureGenerator<C>>> configuredStructureCodec;
    private final PieceGeneratorSupplier<C> pieceGenerator;
    private final PostPlacementProcessor postPlacementProcessor;

    private static <F extends StructureGenerator<?>> F register(String name, F structureFeature, WorldGenStage.Decoration step) {
        STRUCTURES_REGISTRY.put(name.toLowerCase(Locale.ROOT), structureFeature);
        STEP.put(structureFeature, step);
        return IRegistry.register(IRegistry.STRUCTURE_FEATURE, name.toLowerCase(Locale.ROOT), structureFeature);
    }

    public StructureGenerator(Codec<C> configCodec, PieceGeneratorSupplier<C> piecesGenerator) {
        this(configCodec, piecesGenerator, PostPlacementProcessor.NONE);
    }

    public StructureGenerator(Codec<C> configCodec, PieceGeneratorSupplier<C> piecesGenerator, PostPlacementProcessor postPlacementProcessor) {
        this.configuredStructureCodec = configCodec.fieldOf("config").xmap((config) -> {
            return new StructureFeature<>(this, config);
        }, (configuredFeature) -> {
            return configuredFeature.config;
        }).codec();
        this.pieceGenerator = piecesGenerator;
        this.postPlacementProcessor = postPlacementProcessor;
    }

    public WorldGenStage.Decoration step() {
        return STEP.get(this);
    }

    public static void bootstrap() {
    }

    @Nullable
    public static StructureStart<?> loadStaticStart(StructurePieceSerializationContext context, NBTTagCompound nbt, long worldSeed) {
        String string = nbt.getString("id");
        if ("INVALID".equals(string)) {
            return StructureStart.INVALID_START;
        } else {
            StructureGenerator<?> structureFeature = IRegistry.STRUCTURE_FEATURE.get(new MinecraftKey(string.toLowerCase(Locale.ROOT)));
            if (structureFeature == null) {
                LOGGER.error("Unknown feature id: {}", (Object)string);
                return null;
            } else {
                ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(nbt.getInt("ChunkX"), nbt.getInt("ChunkZ"));
                int i = nbt.getInt("references");
                NBTTagList listTag = nbt.getList("Children", 10);

                try {
                    PiecesContainer piecesContainer = PiecesContainer.load(listTag, context);
                    if (structureFeature == OCEAN_MONUMENT) {
                        piecesContainer = WorldGenMonument.regeneratePiecesAfterLoad(chunkPos, worldSeed, piecesContainer);
                    }

                    return new StructureStart<>(structureFeature, chunkPos, i, piecesContainer);
                } catch (Exception var10) {
                    LOGGER.error("Failed Start with id {}", string, var10);
                    return null;
                }
            }
        }
    }

    public Codec<StructureFeature<C, StructureGenerator<C>>> configuredStructureCodec() {
        return this.configuredStructureCodec;
    }

    public StructureFeature<C, ? extends StructureGenerator<C>> configured(C config) {
        return new StructureFeature<>(this, config);
    }

    public BlockPosition getLocatePos(ChunkCoordIntPair chunkPos) {
        return new BlockPosition(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ());
    }

    @Nullable
    public BlockPosition getNearestGeneratedFeature(IWorldReader world, StructureManager structureAccessor, BlockPosition searchStartPos, int searchRadius, boolean skipExistingChunks, long worldSeed, StructureSettingsFeature config) {
        int i = config.spacing();
        int j = SectionPosition.blockToSectionCoord(searchStartPos.getX());
        int k = SectionPosition.blockToSectionCoord(searchStartPos.getZ());

        for(int l = 0; l <= searchRadius; ++l) {
            for(int m = -l; m <= l; ++m) {
                boolean bl = m == -l || m == l;

                for(int n = -l; n <= l; ++n) {
                    boolean bl2 = n == -l || n == l;
                    if (bl || bl2) {
                        int o = j + i * m;
                        int p = k + i * n;
                        ChunkCoordIntPair chunkPos = this.getPotentialFeatureChunk(config, worldSeed, o, p);
                        StructureCheckResult structureCheckResult = structureAccessor.checkStructurePresence(chunkPos, this, skipExistingChunks);
                        if (structureCheckResult != StructureCheckResult.START_NOT_PRESENT) {
                            if (!skipExistingChunks && structureCheckResult == StructureCheckResult.START_PRESENT) {
                                return this.getLocatePos(chunkPos);
                            }

                            IChunkAccess chunkAccess = world.getChunkAt(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
                            StructureStart<?> structureStart = structureAccessor.getStartForFeature(SectionPosition.bottomOf(chunkAccess), this, chunkAccess);
                            if (structureStart != null && structureStart.isValid()) {
                                if (skipExistingChunks && structureStart.canBeReferenced()) {
                                    structureAccessor.addReference(structureStart);
                                    return this.getLocatePos(structureStart.getChunkPos());
                                }

                                if (!skipExistingChunks) {
                                    return this.getLocatePos(structureStart.getChunkPos());
                                }
                            }

                            if (l == 0) {
                                break;
                            }
                        }
                    }
                }

                if (l == 0) {
                    break;
                }
            }
        }

        return null;
    }

    protected boolean linearSeparation() {
        return true;
    }

    public final ChunkCoordIntPair getPotentialFeatureChunk(StructureSettingsFeature config, long seed, int x, int z) {
        int i = config.spacing();
        int j = config.separation();
        int k = Math.floorDiv(x, i);
        int l = Math.floorDiv(z, i);
        SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(0L));
        worldgenRandom.setLargeFeatureWithSalt(seed, k, l, config.salt());
        int m;
        int n;
        if (this.linearSeparation()) {
            m = worldgenRandom.nextInt(i - j);
            n = worldgenRandom.nextInt(i - j);
        } else {
            m = (worldgenRandom.nextInt(i - j) + worldgenRandom.nextInt(i - j)) / 2;
            n = (worldgenRandom.nextInt(i - j) + worldgenRandom.nextInt(i - j)) / 2;
        }

        return new ChunkCoordIntPair(k * i + m, l * i + n);
    }

    public StructureStart<?> generate(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, DefinedStructureManager structureManager, long worldSeed, ChunkCoordIntPair pos, int structureReferences, StructureSettingsFeature structureConfig, C config, IWorldHeightAccess world, Predicate<BiomeBase> biomePredicate) {
        ChunkCoordIntPair chunkPos = this.getPotentialFeatureChunk(structureConfig, worldSeed, pos.x, pos.z);
        if (pos.x == chunkPos.x && pos.z == chunkPos.z) {
            Optional<PieceGenerator<C>> optional = this.pieceGenerator.createGenerator(new PieceGeneratorSupplier.Context<>(chunkGenerator, biomeSource, worldSeed, pos, config, world, biomePredicate, structureManager, registryManager));
            if (optional.isPresent()) {
                StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
                SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(0L));
                worldgenRandom.setLargeFeatureSeed(worldSeed, pos.x, pos.z);
                optional.get().generatePieces(structurePiecesBuilder, new PieceGenerator.Context<>(config, chunkGenerator, structureManager, pos, world, worldgenRandom, worldSeed));
                StructureStart<C> structureStart = new StructureStart<>(this, pos, structureReferences, structurePiecesBuilder.build());
                if (structureStart.isValid()) {
                    return structureStart;
                }
            }
        }

        return StructureStart.INVALID_START;
    }

    public boolean canGenerate(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, DefinedStructureManager structureManager, long worldSeed, ChunkCoordIntPair pos, C config, IWorldHeightAccess world, Predicate<BiomeBase> biomePredicate) {
        return this.pieceGenerator.createGenerator(new PieceGeneratorSupplier.Context<>(chunkGenerator, biomeSource, worldSeed, pos, config, world, biomePredicate, structureManager, registryManager)).isPresent();
    }

    public PostPlacementProcessor getPostPlacementProcessor() {
        return this.postPlacementProcessor;
    }

    public String getFeatureName() {
        return STRUCTURES_REGISTRY.inverse().get(this);
    }

    public StructureBoundingBox adjustBoundingBox(StructureBoundingBox box) {
        return box;
    }
}
