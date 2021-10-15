package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.feature.configurations.StructureSettingsFeature;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureChanceDecoratorRangeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfigurationChance;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureOceanRuinConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureRuinedPortalConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureShipwreckConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureVillageConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenMineshaftConfiguration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenFeatureNetherFossil;
import net.minecraft.world.level.levelgen.structure.WorldGenFeatureOceanRuin;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StructureGenerator<C extends WorldGenFeatureConfiguration> {
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
    public static final WorldGenFeatureSwampHut SWAMP_HUT = register("Swamp_Hut", new WorldGenFeatureSwampHut(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> STRONGHOLD = register("Stronghold", new WorldGenStronghold(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.STRONGHOLDS);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> OCEAN_MONUMENT = register("Monument", new WorldGenMonument(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureOceanRuinConfiguration> OCEAN_RUIN = register("Ocean_Ruin", new WorldGenFeatureOceanRuin(WorldGenFeatureOceanRuinConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> NETHER_BRIDGE = register("Fortress", new WorldGenNether(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.UNDERGROUND_DECORATION);
    public static final StructureGenerator<WorldGenFeatureEmptyConfiguration> END_CITY = register("EndCity", new WorldGenEndCity(WorldGenFeatureEmptyConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureConfigurationChance> BURIED_TREASURE = register("Buried_Treasure", new WorldGenBuriedTreasure(WorldGenFeatureConfigurationChance.CODEC), WorldGenStage.Decoration.UNDERGROUND_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureVillageConfiguration> VILLAGE = register("Village", new WorldGenVillage(WorldGenFeatureVillageConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final StructureGenerator<WorldGenFeatureChanceDecoratorRangeConfiguration> NETHER_FOSSIL = register("Nether_Fossil", new WorldGenFeatureNetherFossil(WorldGenFeatureChanceDecoratorRangeConfiguration.CODEC), WorldGenStage.Decoration.UNDERGROUND_DECORATION);
    public static final StructureGenerator<WorldGenFeatureVillageConfiguration> BASTION_REMNANT = register("Bastion_Remnant", new WorldGenFeatureBastionRemnant(WorldGenFeatureVillageConfiguration.CODEC), WorldGenStage.Decoration.SURFACE_STRUCTURES);
    public static final List<StructureGenerator<?>> NOISE_AFFECTING_FEATURES = ImmutableList.of(PILLAGER_OUTPOST, VILLAGE, NETHER_FOSSIL, STRONGHOLD);
    private static final MinecraftKey JIGSAW_RENAME = new MinecraftKey("jigsaw");
    private static final Map<MinecraftKey, MinecraftKey> RENAMES = ImmutableMap.<MinecraftKey, MinecraftKey>builder().put(new MinecraftKey("nvi"), JIGSAW_RENAME).put(new MinecraftKey("pcp"), JIGSAW_RENAME).put(new MinecraftKey("bastionremnant"), JIGSAW_RENAME).put(new MinecraftKey("runtime"), JIGSAW_RENAME).build();
    public static final int MAX_STRUCTURE_RANGE = 8;
    private final Codec<StructureFeature<C, StructureGenerator<C>>> configuredStructureCodec;

    private static <F extends StructureGenerator<?>> F register(String name, F structureFeature, WorldGenStage.Decoration step) {
        STRUCTURES_REGISTRY.put(name.toLowerCase(Locale.ROOT), structureFeature);
        STEP.put(structureFeature, step);
        return IRegistry.register(IRegistry.STRUCTURE_FEATURE, name.toLowerCase(Locale.ROOT), structureFeature);
    }

    public StructureGenerator(Codec<C> codec) {
        this.configuredStructureCodec = codec.fieldOf("config").xmap((featureConfiguration) -> {
            return new StructureFeature<>(this, featureConfiguration);
        }, (configuredStructureFeature) -> {
            return configuredStructureFeature.config;
        }).codec();
    }

    public WorldGenStage.Decoration step() {
        return STEP.get(this);
    }

    public static void bootstrap() {
    }

    @Nullable
    public static StructureStart<?> loadStaticStart(WorldServer world, NBTTagCompound nbt, long worldSeed) {
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
                    StructureStart<?> structureStart = structureFeature.createStart(chunkPos, i, worldSeed);

                    for(int j = 0; j < listTag.size(); ++j) {
                        NBTTagCompound compoundTag = listTag.getCompound(j);
                        String string2 = compoundTag.getString("id").toLowerCase(Locale.ROOT);
                        MinecraftKey resourceLocation = new MinecraftKey(string2);
                        MinecraftKey resourceLocation2 = RENAMES.getOrDefault(resourceLocation, resourceLocation);
                        WorldGenFeatureStructurePieceType structurePieceType = IRegistry.STRUCTURE_PIECE.get(resourceLocation2);
                        if (structurePieceType == null) {
                            LOGGER.error("Unknown structure piece id: {}", (Object)resourceLocation2);
                        } else {
                            try {
                                StructurePiece structurePiece = structurePieceType.load(world, compoundTag);
                                structureStart.addPiece(structurePiece);
                            } catch (Exception var17) {
                                LOGGER.error("Exception loading structure piece with id {}", resourceLocation2, var17);
                            }
                        }
                    }

                    return structureStart;
                } catch (Exception var18) {
                    LOGGER.error("Failed Start with id {}", string, var18);
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

    @Nullable
    public BlockPosition getNearestGeneratedFeature(IWorldReader world, StructureManager structureAccessor, BlockPosition searchStartPos, int searchRadius, boolean skipExistingChunks, long worldSeed, StructureSettingsFeature config) {
        int i = config.spacing();
        int j = SectionPosition.blockToSectionCoord(searchStartPos.getX());
        int k = SectionPosition.blockToSectionCoord(searchStartPos.getZ());
        int l = 0;

        for(SeededRandom worldgenRandom = new SeededRandom(); l <= searchRadius; ++l) {
            for(int m = -l; m <= l; ++m) {
                boolean bl = m == -l || m == l;

                for(int n = -l; n <= l; ++n) {
                    boolean bl2 = n == -l || n == l;
                    if (bl || bl2) {
                        int o = j + i * m;
                        int p = k + i * n;
                        ChunkCoordIntPair chunkPos = this.getPotentialFeatureChunk(config, worldSeed, worldgenRandom, o, p);
                        boolean bl3 = world.getBiomeManager().getPrimaryBiomeAtChunk(chunkPos).getGenerationSettings().isValidStart(this);
                        if (bl3) {
                            IChunkAccess chunkAccess = world.getChunkAt(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
                            StructureStart<?> structureStart = structureAccessor.getStartForFeature(SectionPosition.bottomOf(chunkAccess), this, chunkAccess);
                            if (structureStart != null && structureStart.isValid()) {
                                if (skipExistingChunks && structureStart.canBeReferenced()) {
                                    structureStart.addReference();
                                    return structureStart.getLocatePos();
                                }

                                if (!skipExistingChunks) {
                                    return structureStart.getLocatePos();
                                }
                            }
                        }

                        if (l == 0) {
                            break;
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

    public final ChunkCoordIntPair getPotentialFeatureChunk(StructureSettingsFeature config, long worldSeed, SeededRandom placementRandom, int chunkX, int chunkY) {
        int i = config.spacing();
        int j = config.separation();
        int k = Math.floorDiv(chunkX, i);
        int l = Math.floorDiv(chunkY, i);
        placementRandom.setLargeFeatureWithSalt(worldSeed, k, l, config.salt());
        int m;
        int n;
        if (this.linearSeparation()) {
            m = placementRandom.nextInt(i - j);
            n = placementRandom.nextInt(i - j);
        } else {
            m = (placementRandom.nextInt(i - j) + placementRandom.nextInt(i - j)) / 2;
            n = (placementRandom.nextInt(i - j) + placementRandom.nextInt(i - j)) / 2;
        }

        return new ChunkCoordIntPair(k * i + m, l * i + n);
    }

    protected boolean isFeatureChunk(ChunkGenerator chunkGenerator, WorldChunkManager biomeSource, long worldSeed, SeededRandom random, ChunkCoordIntPair pos, BiomeBase biome, ChunkCoordIntPair chunkPos, C config, IWorldHeightAccess world) {
        return true;
    }

    private StructureStart<C> createStart(ChunkCoordIntPair pos, int i, long l) {
        return this.getStartFactory().create(this, pos, i, l);
    }

    public StructureStart<?> generate(IRegistryCustom registryAccess, ChunkGenerator generator, WorldChunkManager biomeSource, DefinedStructureManager manager, long worldSeed, ChunkCoordIntPair pos, BiomeBase biome, int referenceCount, SeededRandom random, StructureSettingsFeature structureConfig, C config, IWorldHeightAccess world) {
        ChunkCoordIntPair chunkPos = this.getPotentialFeatureChunk(structureConfig, worldSeed, random, pos.x, pos.z);
        if (pos.x == chunkPos.x && pos.z == chunkPos.z && this.isFeatureChunk(generator, biomeSource, worldSeed, random, pos, biome, chunkPos, config, world)) {
            StructureStart<C> structureStart = this.createStart(pos, referenceCount, worldSeed);
            structureStart.generatePieces(registryAccess, generator, manager, pos, biome, config, world);
            if (structureStart.isValid()) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public abstract StructureGenerator.StructureStartFactory<C> getStartFactory();

    public String getFeatureName() {
        return STRUCTURES_REGISTRY.inverse().get(this);
    }

    public WeightedRandomList<BiomeSettingsMobs.SpawnerData> getSpecialEnemies() {
        return BiomeSettingsMobs.EMPTY_MOB_LIST;
    }

    public WeightedRandomList<BiomeSettingsMobs.SpawnerData> getSpecialAnimals() {
        return BiomeSettingsMobs.EMPTY_MOB_LIST;
    }

    public WeightedRandomList<BiomeSettingsMobs.SpawnerData> getSpecialUndergroundWaterAnimals() {
        return BiomeSettingsMobs.EMPTY_MOB_LIST;
    }

    public interface StructureStartFactory<C extends WorldGenFeatureConfiguration> {
        StructureStart<C> create(StructureGenerator<C> feature, ChunkCoordIntPair pos, int references, long seed);
    }
}
