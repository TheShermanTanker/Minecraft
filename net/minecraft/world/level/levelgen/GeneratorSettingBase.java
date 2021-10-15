package net.minecraft.world.level.levelgen;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.IRegistry;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.StructureSettingsFeature;

public final class GeneratorSettingBase {
    public static final Codec<GeneratorSettingBase> DIRECT_CODEC;
    public static final Codec<Supplier<GeneratorSettingBase>> CODEC = RegistryFileCodec.create(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY, DIRECT_CODEC);
    private final StructureSettings structureSettings;
    private final NoiseSettings noiseSettings;
    private final IBlockData defaultBlock;
    private final IBlockData defaultFluid;
    private final int bedrockRoofPosition;
    private final int bedrockFloorPosition;
    private final int seaLevel;
    private final int minSurfaceLevel;
    private final boolean disableMobGeneration;
    private final boolean aquifersEnabled;
    private final boolean noiseCavesEnabled;
    private final boolean deepslateEnabled;
    private final boolean oreVeinsEnabled;
    private final boolean noodleCavesEnabled;
    public static final ResourceKey<GeneratorSettingBase> OVERWORLD = ResourceKey.create(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY, new MinecraftKey("overworld"));
    public static final ResourceKey<GeneratorSettingBase> AMPLIFIED = ResourceKey.create(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY, new MinecraftKey("amplified"));
    public static final ResourceKey<GeneratorSettingBase> NETHER = ResourceKey.create(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY, new MinecraftKey("nether"));
    public static final ResourceKey<GeneratorSettingBase> END = ResourceKey.create(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY, new MinecraftKey("end"));
    public static final ResourceKey<GeneratorSettingBase> CAVES = ResourceKey.create(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY, new MinecraftKey("caves"));
    public static final ResourceKey<GeneratorSettingBase> FLOATING_ISLANDS = ResourceKey.create(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY, new MinecraftKey("floating_islands"));
    private static final GeneratorSettingBase BUILTIN_OVERWORLD = register(OVERWORLD, overworld(new StructureSettings(true), false));

    private GeneratorSettingBase(StructureSettings structuresConfig, NoiseSettings generationShapeConfig, IBlockData defaultBlock, IBlockData defaultFluid, int bedrockCeilingY, int bedrockFloorY, int seaLevel, int minSurfaceLevel, boolean mobGenerationDisabled, boolean aquifers, boolean noiseCaves, boolean deepslate, boolean oreVeins, boolean noodleCaves) {
        this.structureSettings = structuresConfig;
        this.noiseSettings = generationShapeConfig;
        this.defaultBlock = defaultBlock;
        this.defaultFluid = defaultFluid;
        this.bedrockRoofPosition = bedrockCeilingY;
        this.bedrockFloorPosition = bedrockFloorY;
        this.seaLevel = seaLevel;
        this.minSurfaceLevel = minSurfaceLevel;
        this.disableMobGeneration = mobGenerationDisabled;
        this.aquifersEnabled = aquifers;
        this.noiseCavesEnabled = noiseCaves;
        this.deepslateEnabled = deepslate;
        this.oreVeinsEnabled = oreVeins;
        this.noodleCavesEnabled = noodleCaves;
    }

    public StructureSettings structureSettings() {
        return this.structureSettings;
    }

    public NoiseSettings noiseSettings() {
        return this.noiseSettings;
    }

    public IBlockData getDefaultBlock() {
        return this.defaultBlock;
    }

    public IBlockData getDefaultFluid() {
        return this.defaultFluid;
    }

    public int getBedrockRoofPosition() {
        return this.bedrockRoofPosition;
    }

    public int getBedrockFloorPosition() {
        return this.bedrockFloorPosition;
    }

    public int seaLevel() {
        return this.seaLevel;
    }

    public int getMinSurfaceLevel() {
        return this.minSurfaceLevel;
    }

    @Deprecated
    protected boolean disableMobGeneration() {
        return this.disableMobGeneration;
    }

    protected boolean isAquifersEnabled() {
        return this.aquifersEnabled;
    }

    protected boolean isNoiseCavesEnabled() {
        return this.noiseCavesEnabled;
    }

    protected boolean isDeepslateEnabled() {
        return this.deepslateEnabled;
    }

    protected boolean isOreVeinsEnabled() {
        return this.oreVeinsEnabled;
    }

    protected boolean isNoodleCavesEnabled() {
        return this.noodleCavesEnabled;
    }

    public boolean stable(ResourceKey<GeneratorSettingBase> registryKey) {
        return Objects.equals(this, RegistryGeneration.NOISE_GENERATOR_SETTINGS.get(registryKey));
    }

    private static GeneratorSettingBase register(ResourceKey<GeneratorSettingBase> registryKey, GeneratorSettingBase settings) {
        RegistryGeneration.register(RegistryGeneration.NOISE_GENERATOR_SETTINGS, registryKey.location(), settings);
        return settings;
    }

    public static GeneratorSettingBase bootstrap() {
        return BUILTIN_OVERWORLD;
    }

    private static GeneratorSettingBase endLikePreset(StructureSettings structuresConfig, IBlockData defaultBlock, IBlockData defaultFluid, boolean bl, boolean bl2) {
        return new GeneratorSettingBase(structuresConfig, NoiseSettings.create(0, 128, new NoiseSamplingSettings(2.0D, 1.0D, 80.0D, 160.0D), new NoiseSlideSettings(-3000, 64, -46), new NoiseSlideSettings(-30, 7, 1), 2, 1, 0.0D, 0.0D, true, false, bl2, false), defaultBlock, defaultFluid, Integer.MIN_VALUE, Integer.MIN_VALUE, 0, 0, bl, false, false, false, false, false);
    }

    private static GeneratorSettingBase netherLikePreset(StructureSettings structuresConfig, IBlockData defaultBlock, IBlockData defaultFluid) {
        Map<StructureGenerator<?>, StructureSettingsFeature> map = Maps.newHashMap(StructureSettings.DEFAULTS);
        map.put(StructureGenerator.RUINED_PORTAL, new StructureSettingsFeature(25, 10, 34222645));
        return new GeneratorSettingBase(new StructureSettings(Optional.ofNullable(structuresConfig.stronghold()), map), NoiseSettings.create(0, 128, new NoiseSamplingSettings(1.0D, 3.0D, 80.0D, 60.0D), new NoiseSlideSettings(120, 3, 0), new NoiseSlideSettings(320, 4, -1), 1, 2, 0.0D, 0.019921875D, false, false, false, false), defaultBlock, defaultFluid, 0, 0, 32, 0, false, false, false, false, false, false);
    }

    private static GeneratorSettingBase overworld(StructureSettings structuresConfig, boolean amplified) {
        double d = 0.9999999814507745D;
        return new GeneratorSettingBase(structuresConfig, NoiseSettings.create(0, 256, new NoiseSamplingSettings(0.9999999814507745D, 0.9999999814507745D, 80.0D, 160.0D), new NoiseSlideSettings(-10, 3, 0), new NoiseSlideSettings(15, 3, 0), 1, 2, 1.0D, -0.46875D, true, true, false, amplified), Blocks.STONE.getBlockData(), Blocks.WATER.getBlockData(), Integer.MIN_VALUE, 0, 63, 0, false, false, false, false, false, false);
    }

    static {
        DIRECT_CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(StructureSettings.CODEC.fieldOf("structures").forGetter(GeneratorSettingBase::structureSettings), NoiseSettings.CODEC.fieldOf("noise").forGetter(GeneratorSettingBase::noiseSettings), IBlockData.CODEC.fieldOf("default_block").forGetter(GeneratorSettingBase::getDefaultBlock), IBlockData.CODEC.fieldOf("default_fluid").forGetter(GeneratorSettingBase::getDefaultFluid), Codec.INT.fieldOf("bedrock_roof_position").forGetter(GeneratorSettingBase::getBedrockRoofPosition), Codec.INT.fieldOf("bedrock_floor_position").forGetter(GeneratorSettingBase::getBedrockFloorPosition), Codec.INT.fieldOf("sea_level").forGetter(GeneratorSettingBase::seaLevel), Codec.INT.fieldOf("min_surface_level").forGetter(GeneratorSettingBase::getMinSurfaceLevel), Codec.BOOL.fieldOf("disable_mob_generation").forGetter(GeneratorSettingBase::disableMobGeneration), Codec.BOOL.fieldOf("aquifers_enabled").forGetter(GeneratorSettingBase::isAquifersEnabled), Codec.BOOL.fieldOf("noise_caves_enabled").forGetter(GeneratorSettingBase::isNoiseCavesEnabled), Codec.BOOL.fieldOf("deepslate_enabled").forGetter(GeneratorSettingBase::isDeepslateEnabled), Codec.BOOL.fieldOf("ore_veins_enabled").forGetter(GeneratorSettingBase::isOreVeinsEnabled), Codec.BOOL.fieldOf("noodle_caves_enabled").forGetter(GeneratorSettingBase::isOreVeinsEnabled)).apply(instance, GeneratorSettingBase::new);
        });
        register(AMPLIFIED, overworld(new StructureSettings(true), true));
        register(NETHER, netherLikePreset(new StructureSettings(false), Blocks.NETHERRACK.getBlockData(), Blocks.LAVA.getBlockData()));
        register(END, endLikePreset(new StructureSettings(false), Blocks.END_STONE.getBlockData(), Blocks.AIR.getBlockData(), true, true));
        register(CAVES, netherLikePreset(new StructureSettings(true), Blocks.STONE.getBlockData(), Blocks.WATER.getBlockData()));
        register(FLOATING_ISLANDS, endLikePreset(new StructureSettings(true), Blocks.STONE.getBlockData(), Blocks.WATER.getBlockData(), false, false));
    }
}
