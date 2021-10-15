package net.minecraft.world.level.levelgen;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Random;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManagerOverworld;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.dimension.WorldDimension;
import net.minecraft.world.level.levelgen.flat.GeneratorSettingsFlat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneratorSettings {
    public static final Codec<GeneratorSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.LONG.fieldOf("seed").stable().forGetter(GeneratorSettings::getSeed), Codec.BOOL.fieldOf("generate_features").orElse(true).stable().forGetter(GeneratorSettings::shouldGenerateMapFeatures), Codec.BOOL.fieldOf("bonus_chest").orElse(false).stable().forGetter(GeneratorSettings::generateBonusChest), RegistryMaterials.dataPackCodec(IRegistry.LEVEL_STEM_REGISTRY, Lifecycle.stable(), WorldDimension.CODEC).xmap(WorldDimension::sortMap, Function.identity()).fieldOf("dimensions").forGetter(GeneratorSettings::dimensions), Codec.STRING.optionalFieldOf("legacy_custom_options").stable().forGetter((worldGenSettings) -> {
            return worldGenSettings.legacyCustomOptions;
        })).apply(instance, instance.stable(GeneratorSettings::new));
    }).comapFlatMap(GeneratorSettings::guardExperimental, Function.identity());
    private static final Logger LOGGER = LogManager.getLogger();
    private final long seed;
    private final boolean generateFeatures;
    private final boolean generateBonusChest;
    private final RegistryMaterials<WorldDimension> dimensions;
    private final Optional<String> legacyCustomOptions;

    private DataResult<GeneratorSettings> guardExperimental() {
        WorldDimension levelStem = this.dimensions.get(WorldDimension.OVERWORLD);
        if (levelStem == null) {
            return DataResult.error("Overworld settings missing");
        } else {
            return this.stable() ? DataResult.success(this, Lifecycle.stable()) : DataResult.success(this);
        }
    }

    private boolean stable() {
        return WorldDimension.stable(this.seed, this.dimensions);
    }

    public GeneratorSettings(long seed, boolean generateStructures, boolean bonusChest, RegistryMaterials<WorldDimension> options) {
        this(seed, generateStructures, bonusChest, options, Optional.empty());
        WorldDimension levelStem = options.get(WorldDimension.OVERWORLD);
        if (levelStem == null) {
            throw new IllegalStateException("Overworld settings missing");
        }
    }

    private GeneratorSettings(long seed, boolean generateStructures, boolean bonusChest, RegistryMaterials<WorldDimension> options, Optional<String> legacyCustomOptions) {
        this.seed = seed;
        this.generateFeatures = generateStructures;
        this.generateBonusChest = bonusChest;
        this.dimensions = options;
        this.legacyCustomOptions = legacyCustomOptions;
    }

    public static GeneratorSettings demoSettings(IRegistryCustom registryManager) {
        IRegistry<BiomeBase> registry = registryManager.registryOrThrow(IRegistry.BIOME_REGISTRY);
        int i = "North Carolina".hashCode();
        IRegistry<DimensionManager> registry2 = registryManager.registryOrThrow(IRegistry.DIMENSION_TYPE_REGISTRY);
        IRegistry<GeneratorSettingBase> registry3 = registryManager.registryOrThrow(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY);
        return new GeneratorSettings((long)i, true, true, withOverworld(registry2, DimensionManager.defaultDimensions(registry2, registry, registry3, (long)i), makeDefaultOverworld(registry, registry3, (long)i)));
    }

    public static GeneratorSettings makeDefault(IRegistry<DimensionManager> registry, IRegistry<BiomeBase> registry2, IRegistry<GeneratorSettingBase> registry3) {
        long l = (new Random()).nextLong();
        return new GeneratorSettings(l, true, false, withOverworld(registry, DimensionManager.defaultDimensions(registry, registry2, registry3, l), makeDefaultOverworld(registry2, registry3, l)));
    }

    public static ChunkGeneratorAbstract makeDefaultOverworld(IRegistry<BiomeBase> biomeRegistry, IRegistry<GeneratorSettingBase> chunkGeneratorSettingsRegistry, long seed) {
        return new ChunkGeneratorAbstract(new WorldChunkManagerOverworld(seed, false, false, biomeRegistry), seed, () -> {
            return chunkGeneratorSettingsRegistry.getOrThrow(GeneratorSettingBase.OVERWORLD);
        });
    }

    public long getSeed() {
        return this.seed;
    }

    public boolean shouldGenerateMapFeatures() {
        return this.generateFeatures;
    }

    public boolean generateBonusChest() {
        return this.generateBonusChest;
    }

    public static RegistryMaterials<WorldDimension> withOverworld(IRegistry<DimensionManager> dimensionTypeRegistry, RegistryMaterials<WorldDimension> optionsRegistry, ChunkGenerator overworldGenerator) {
        WorldDimension levelStem = optionsRegistry.get(WorldDimension.OVERWORLD);
        Supplier<DimensionManager> supplier = () -> {
            return levelStem == null ? dimensionTypeRegistry.getOrThrow(DimensionManager.OVERWORLD_LOCATION) : levelStem.type();
        };
        return withOverworld(optionsRegistry, supplier, overworldGenerator);
    }

    public static RegistryMaterials<WorldDimension> withOverworld(RegistryMaterials<WorldDimension> optionsRegistry, Supplier<DimensionManager> overworldDimensionType, ChunkGenerator overworldGenerator) {
        RegistryMaterials<WorldDimension> mappedRegistry = new RegistryMaterials<>(IRegistry.LEVEL_STEM_REGISTRY, Lifecycle.experimental());
        mappedRegistry.register(WorldDimension.OVERWORLD, new WorldDimension(overworldDimensionType, overworldGenerator), Lifecycle.stable());

        for(Entry<ResourceKey<WorldDimension>, WorldDimension> entry : optionsRegistry.entrySet()) {
            ResourceKey<WorldDimension> resourceKey = entry.getKey();
            if (resourceKey != WorldDimension.OVERWORLD) {
                mappedRegistry.register(resourceKey, entry.getValue(), optionsRegistry.lifecycle(entry.getValue()));
            }
        }

        return mappedRegistry;
    }

    public RegistryMaterials<WorldDimension> dimensions() {
        return this.dimensions;
    }

    public ChunkGenerator getChunkGenerator() {
        WorldDimension levelStem = this.dimensions.get(WorldDimension.OVERWORLD);
        if (levelStem == null) {
            throw new IllegalStateException("Overworld settings missing");
        } else {
            return levelStem.generator();
        }
    }

    public ImmutableSet<ResourceKey<World>> levels() {
        return this.dimensions().entrySet().stream().map((entry) -> {
            return ResourceKey.create(IRegistry.DIMENSION_REGISTRY, entry.getKey().location());
        }).collect(ImmutableSet.toImmutableSet());
    }

    public boolean isDebugWorld() {
        return this.getChunkGenerator() instanceof ChunkProviderDebug;
    }

    public boolean isFlatWorld() {
        return this.getChunkGenerator() instanceof ChunkProviderFlat;
    }

    public boolean isOldCustomizedWorld() {
        return this.legacyCustomOptions.isPresent();
    }

    public GeneratorSettings withBonusChest() {
        return new GeneratorSettings(this.seed, this.generateFeatures, true, this.dimensions, this.legacyCustomOptions);
    }

    public GeneratorSettings withFeaturesToggled() {
        return new GeneratorSettings(this.seed, !this.generateFeatures, this.generateBonusChest, this.dimensions);
    }

    public GeneratorSettings withBonusChestToggled() {
        return new GeneratorSettings(this.seed, this.generateFeatures, !this.generateBonusChest, this.dimensions);
    }

    public static GeneratorSettings create(IRegistryCustom registryManager, Properties properties) {
        String string = MoreObjects.firstNonNull((String)properties.get("generator-settings"), "");
        properties.put("generator-settings", string);
        String string2 = MoreObjects.firstNonNull((String)properties.get("level-seed"), "");
        properties.put("level-seed", string2);
        String string3 = (String)properties.get("generate-structures");
        boolean bl = string3 == null || Boolean.parseBoolean(string3);
        properties.put("generate-structures", Objects.toString(bl));
        String string4 = (String)properties.get("level-type");
        String string5 = Optional.ofNullable(string4).map((stringx) -> {
            return stringx.toLowerCase(Locale.ROOT);
        }).orElse("default");
        properties.put("level-type", string5);
        long l = (new Random()).nextLong();
        if (!string2.isEmpty()) {
            try {
                long m = Long.parseLong(string2);
                if (m != 0L) {
                    l = m;
                }
            } catch (NumberFormatException var18) {
                l = (long)string2.hashCode();
            }
        }

        IRegistry<DimensionManager> registry = registryManager.registryOrThrow(IRegistry.DIMENSION_TYPE_REGISTRY);
        IRegistry<BiomeBase> registry2 = registryManager.registryOrThrow(IRegistry.BIOME_REGISTRY);
        IRegistry<GeneratorSettingBase> registry3 = registryManager.registryOrThrow(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY);
        RegistryMaterials<WorldDimension> mappedRegistry = DimensionManager.defaultDimensions(registry, registry2, registry3, l);
        switch(string5) {
        case "flat":
            JsonObject jsonObject = !string.isEmpty() ? ChatDeserializer.parse(string) : new JsonObject();
            Dynamic<JsonElement> dynamic = new Dynamic<>(JsonOps.INSTANCE, jsonObject);
            return new GeneratorSettings(l, bl, false, withOverworld(registry, mappedRegistry, new ChunkProviderFlat(GeneratorSettingsFlat.CODEC.parse(dynamic).resultOrPartial(LOGGER::error).orElseGet(() -> {
                return GeneratorSettingsFlat.getDefault(registry2);
            }))));
        case "debug_all_block_states":
            return new GeneratorSettings(l, bl, false, withOverworld(registry, mappedRegistry, new ChunkProviderDebug(registry2)));
        case "amplified":
            return new GeneratorSettings(l, bl, false, withOverworld(registry, mappedRegistry, new ChunkGeneratorAbstract(new WorldChunkManagerOverworld(l, false, false, registry2), l, () -> {
                return registry3.getOrThrow(GeneratorSettingBase.AMPLIFIED);
            })));
        case "largebiomes":
            return new GeneratorSettings(l, bl, false, withOverworld(registry, mappedRegistry, new ChunkGeneratorAbstract(new WorldChunkManagerOverworld(l, false, true, registry2), l, () -> {
                return registry3.getOrThrow(GeneratorSettingBase.OVERWORLD);
            })));
        default:
            return new GeneratorSettings(l, bl, false, withOverworld(registry, mappedRegistry, makeDefaultOverworld(registry2, registry3, l)));
        }
    }

    public GeneratorSettings withSeed(boolean hardcore, OptionalLong seed) {
        long l = seed.orElse(this.seed);
        RegistryMaterials<WorldDimension> mappedRegistry;
        if (seed.isPresent()) {
            mappedRegistry = new RegistryMaterials<>(IRegistry.LEVEL_STEM_REGISTRY, Lifecycle.experimental());
            long m = seed.getAsLong();

            for(Entry<ResourceKey<WorldDimension>, WorldDimension> entry : this.dimensions.entrySet()) {
                ResourceKey<WorldDimension> resourceKey = entry.getKey();
                mappedRegistry.register(resourceKey, new WorldDimension(entry.getValue().typeSupplier(), entry.getValue().generator().withSeed(m)), this.dimensions.lifecycle(entry.getValue()));
            }
        } else {
            mappedRegistry = this.dimensions;
        }

        GeneratorSettings worldGenSettings;
        if (this.isDebugWorld()) {
            worldGenSettings = new GeneratorSettings(l, false, false, mappedRegistry);
        } else {
            worldGenSettings = new GeneratorSettings(l, this.shouldGenerateMapFeatures(), this.generateBonusChest() && !hardcore, mappedRegistry);
        }

        return worldGenSettings;
    }
}
