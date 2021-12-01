package net.minecraft.world.level.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.IRegistryWritable;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManagerMultiNoise;
import net.minecraft.world.level.biome.WorldChunkManagerTheEnd;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.ChunkGeneratorAbstract;
import net.minecraft.world.level.levelgen.GeneratorSettingBase;
import net.minecraft.world.level.levelgen.synth.NormalNoise$NoiseParameters;

public class DimensionManager {
    public static final int BITS_FOR_Y = BlockPosition.PACKED_Y_LENGTH;
    public static final int MIN_HEIGHT = 16;
    public static final int Y_SIZE = (1 << BITS_FOR_Y) - 32;
    public static final int MAX_Y = (Y_SIZE >> 1) - 1;
    public static final int MIN_Y = MAX_Y - Y_SIZE + 1;
    public static final int WAY_ABOVE_MAX_Y = MAX_Y << 4;
    public static final int WAY_BELOW_MIN_Y = MIN_Y << 4;
    public static final MinecraftKey OVERWORLD_EFFECTS = new MinecraftKey("overworld");
    public static final MinecraftKey NETHER_EFFECTS = new MinecraftKey("the_nether");
    public static final MinecraftKey END_EFFECTS = new MinecraftKey("the_end");
    public static final Codec<DimensionManager> DIRECT_CODEC;
    private static final int MOON_PHASES = 8;
    public static final float[] MOON_BRIGHTNESS_PER_PHASE = new float[]{1.0F, 0.75F, 0.5F, 0.25F, 0.0F, 0.25F, 0.5F, 0.75F};
    public static final ResourceKey<DimensionManager> OVERWORLD_LOCATION = ResourceKey.create(IRegistry.DIMENSION_TYPE_REGISTRY, new MinecraftKey("overworld"));
    public static final ResourceKey<DimensionManager> NETHER_LOCATION = ResourceKey.create(IRegistry.DIMENSION_TYPE_REGISTRY, new MinecraftKey("the_nether"));
    public static final ResourceKey<DimensionManager> END_LOCATION = ResourceKey.create(IRegistry.DIMENSION_TYPE_REGISTRY, new MinecraftKey("the_end"));
    protected static final DimensionManager DEFAULT_OVERWORLD = create(OptionalLong.empty(), true, false, false, true, 1.0D, false, false, true, false, true, -64, 384, 384, TagsBlock.INFINIBURN_OVERWORLD.getName(), OVERWORLD_EFFECTS, 0.0F);
    protected static final DimensionManager DEFAULT_NETHER = create(OptionalLong.of(18000L), false, true, true, false, 8.0D, false, true, false, true, false, 0, 256, 128, TagsBlock.INFINIBURN_NETHER.getName(), NETHER_EFFECTS, 0.1F);
    protected static final DimensionManager DEFAULT_END = create(OptionalLong.of(6000L), false, false, false, false, 1.0D, true, false, false, false, true, 0, 256, 256, TagsBlock.INFINIBURN_END.getName(), END_EFFECTS, 0.0F);
    public static final ResourceKey<DimensionManager> OVERWORLD_CAVES_LOCATION = ResourceKey.create(IRegistry.DIMENSION_TYPE_REGISTRY, new MinecraftKey("overworld_caves"));
    protected static final DimensionManager DEFAULT_OVERWORLD_CAVES = create(OptionalLong.empty(), true, true, false, true, 1.0D, false, false, true, false, true, -64, 384, 384, TagsBlock.INFINIBURN_OVERWORLD.getName(), OVERWORLD_EFFECTS, 0.0F);
    public static final Codec<Supplier<DimensionManager>> CODEC = RegistryFileCodec.create(IRegistry.DIMENSION_TYPE_REGISTRY, DIRECT_CODEC);
    private final OptionalLong fixedTime;
    private final boolean hasSkylight;
    private final boolean hasCeiling;
    private final boolean ultraWarm;
    private final boolean natural;
    private final double coordinateScale;
    private final boolean createDragonFight;
    private final boolean piglinSafe;
    private final boolean bedWorks;
    private final boolean respawnAnchorWorks;
    private final boolean hasRaids;
    private final int minY;
    private final int height;
    private final int logicalHeight;
    private final MinecraftKey infiniburn;
    private final MinecraftKey effectsLocation;
    private final float ambientLight;
    private final transient float[] brightnessRamp;

    private static DataResult<DimensionManager> guardY(DimensionManager type) {
        if (type.getHeight() < 16) {
            return DataResult.error("height has to be at least 16");
        } else if (type.getMinY() + type.getHeight() > MAX_Y + 1) {
            return DataResult.error("min_y + height cannot be higher than: " + (MAX_Y + 1));
        } else if (type.getLogicalHeight() > type.getHeight()) {
            return DataResult.error("logical_height cannot be higher than height");
        } else if (type.getHeight() % 16 != 0) {
            return DataResult.error("height has to be multiple of 16");
        } else {
            return type.getMinY() % 16 != 0 ? DataResult.error("min_y has to be a multiple of 16") : DataResult.success(type);
        }
    }

    private DimensionManager(OptionalLong fixedTime, boolean hasSkylight, boolean hasCeiling, boolean ultrawarm, boolean natural, double coordinateScale, boolean piglinSafe, boolean bedWorks, boolean respawnAnchorWorks, boolean hasRaids, int minimumY, int height, int logicalHeight, MinecraftKey infiniburn, MinecraftKey effects, float ambientLight) {
        this(fixedTime, hasSkylight, hasCeiling, ultrawarm, natural, coordinateScale, false, piglinSafe, bedWorks, respawnAnchorWorks, hasRaids, minimumY, height, logicalHeight, infiniburn, effects, ambientLight);
    }

    public static DimensionManager create(OptionalLong fixedTime, boolean hasSkylight, boolean hasCeiling, boolean ultrawarm, boolean natural, double coordinateScale, boolean hasEnderDragonFight, boolean piglinSafe, boolean bedWorks, boolean respawnAnchorWorks, boolean hasRaids, int minimumY, int height, int logicalHeight, MinecraftKey infiniburn, MinecraftKey effects, float ambientLight) {
        DimensionManager dimensionType = new DimensionManager(fixedTime, hasSkylight, hasCeiling, ultrawarm, natural, coordinateScale, hasEnderDragonFight, piglinSafe, bedWorks, respawnAnchorWorks, hasRaids, minimumY, height, logicalHeight, infiniburn, effects, ambientLight);
        guardY(dimensionType).error().ifPresent((partialResult) -> {
            throw new IllegalStateException(partialResult.message());
        });
        return dimensionType;
    }

    /** @deprecated */
    @Deprecated
    private DimensionManager(OptionalLong fixedTime, boolean hasSkylight, boolean hasCeiling, boolean ultrawarm, boolean natural, double coordinateScale, boolean hasEnderDragonFight, boolean piglinSafe, boolean bedWorks, boolean respawnAnchorWorks, boolean hasRaids, int minimumY, int height, int logicalHeight, MinecraftKey infiniburn, MinecraftKey effects, float ambientLight) {
        this.fixedTime = fixedTime;
        this.hasSkylight = hasSkylight;
        this.hasCeiling = hasCeiling;
        this.ultraWarm = ultrawarm;
        this.natural = natural;
        this.coordinateScale = coordinateScale;
        this.createDragonFight = hasEnderDragonFight;
        this.piglinSafe = piglinSafe;
        this.bedWorks = bedWorks;
        this.respawnAnchorWorks = respawnAnchorWorks;
        this.hasRaids = hasRaids;
        this.minY = minimumY;
        this.height = height;
        this.logicalHeight = logicalHeight;
        this.infiniburn = infiniburn;
        this.effectsLocation = effects;
        this.ambientLight = ambientLight;
        this.brightnessRamp = fillBrightnessRamp(ambientLight);
    }

    private static float[] fillBrightnessRamp(float ambientLight) {
        float[] fs = new float[16];

        for(int i = 0; i <= 15; ++i) {
            float f = (float)i / 15.0F;
            float g = f / (4.0F - 3.0F * f);
            fs[i] = MathHelper.lerp(ambientLight, g, 1.0F);
        }

        return fs;
    }

    /** @deprecated */
    @Deprecated
    public static DataResult<ResourceKey<World>> parseLegacy(Dynamic<?> nbt) {
        Optional<Number> optional = nbt.asNumber().result();
        if (optional.isPresent()) {
            int i = optional.get().intValue();
            if (i == -1) {
                return DataResult.success(World.NETHER);
            }

            if (i == 0) {
                return DataResult.success(World.OVERWORLD);
            }

            if (i == 1) {
                return DataResult.success(World.END);
            }
        }

        return World.RESOURCE_KEY_CODEC.parse(nbt);
    }

    public static IRegistryCustom registerBuiltin(IRegistryCustom registryManager) {
        IRegistryWritable<DimensionManager> writableRegistry = registryManager.ownedRegistryOrThrow(IRegistry.DIMENSION_TYPE_REGISTRY);
        writableRegistry.register(OVERWORLD_LOCATION, DEFAULT_OVERWORLD, Lifecycle.stable());
        writableRegistry.register(OVERWORLD_CAVES_LOCATION, DEFAULT_OVERWORLD_CAVES, Lifecycle.stable());
        writableRegistry.register(NETHER_LOCATION, DEFAULT_NETHER, Lifecycle.stable());
        writableRegistry.register(END_LOCATION, DEFAULT_END, Lifecycle.stable());
        return registryManager;
    }

    public static RegistryMaterials<WorldDimension> defaultDimensions(IRegistryCustom registryManager, long seed) {
        return defaultDimensions(registryManager, seed, true);
    }

    public static RegistryMaterials<WorldDimension> defaultDimensions(IRegistryCustom registryManager, long seed, boolean bl) {
        RegistryMaterials<WorldDimension> mappedRegistry = new RegistryMaterials<>(IRegistry.LEVEL_STEM_REGISTRY, Lifecycle.experimental());
        IRegistry<DimensionManager> registry = registryManager.registryOrThrow(IRegistry.DIMENSION_TYPE_REGISTRY);
        IRegistry<BiomeBase> registry2 = registryManager.registryOrThrow(IRegistry.BIOME_REGISTRY);
        IRegistry<GeneratorSettingBase> registry3 = registryManager.registryOrThrow(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY);
        IRegistry<NormalNoise$NoiseParameters> registry4 = registryManager.registryOrThrow(IRegistry.NOISE_REGISTRY);
        mappedRegistry.register(WorldDimension.NETHER, new WorldDimension(() -> {
            return registry.getOrThrow(NETHER_LOCATION);
        }, new ChunkGeneratorAbstract(registry4, WorldChunkManagerMultiNoise.Preset.NETHER.biomeSource(registry2, bl), seed, () -> {
            return registry3.getOrThrow(GeneratorSettingBase.NETHER);
        })), Lifecycle.stable());
        mappedRegistry.register(WorldDimension.END, new WorldDimension(() -> {
            return registry.getOrThrow(END_LOCATION);
        }, new ChunkGeneratorAbstract(registry4, new WorldChunkManagerTheEnd(registry2, seed), seed, () -> {
            return registry3.getOrThrow(GeneratorSettingBase.END);
        })), Lifecycle.stable());
        return mappedRegistry;
    }

    public static double getTeleportationScale(DimensionManager fromDimension, DimensionManager toDimension) {
        double d = fromDimension.getCoordinateScale();
        double e = toDimension.getCoordinateScale();
        return d / e;
    }

    /** @deprecated */
    @Deprecated
    public String getSuffix() {
        return this.equalTo(DEFAULT_END) ? "_end" : "";
    }

    public static Path getStorageFolder(ResourceKey<World> worldRef, Path worldDirectory) {
        if (worldRef == World.OVERWORLD) {
            return worldDirectory;
        } else if (worldRef == World.END) {
            return worldDirectory.resolve("DIM1");
        } else {
            return worldRef == World.NETHER ? worldDirectory.resolve("DIM-1") : worldDirectory.resolve("dimensions").resolve(worldRef.location().getNamespace()).resolve(worldRef.location().getKey());
        }
    }

    public boolean hasSkyLight() {
        return this.hasSkylight;
    }

    public boolean hasCeiling() {
        return this.hasCeiling;
    }

    public boolean isNether() {
        return this.ultraWarm;
    }

    public boolean isNatural() {
        return this.natural;
    }

    public double getCoordinateScale() {
        return this.coordinateScale;
    }

    public boolean isPiglinSafe() {
        return this.piglinSafe;
    }

    public boolean isBedWorks() {
        return this.bedWorks;
    }

    public boolean isRespawnAnchorWorks() {
        return this.respawnAnchorWorks;
    }

    public boolean hasRaids() {
        return this.hasRaids;
    }

    public int getMinY() {
        return this.minY;
    }

    public int getHeight() {
        return this.height;
    }

    public int getLogicalHeight() {
        return this.logicalHeight;
    }

    public boolean isCreateDragonBattle() {
        return this.createDragonFight;
    }

    public boolean isFixedTime() {
        return this.fixedTime.isPresent();
    }

    public float timeOfDay(long time) {
        double d = MathHelper.frac((double)this.fixedTime.orElse(time) / 24000.0D - 0.25D);
        double e = 0.5D - Math.cos(d * Math.PI) / 2.0D;
        return (float)(d * 2.0D + e) / 3.0F;
    }

    public int moonPhase(long time) {
        return (int)(time / 24000L % 8L + 8L) % 8;
    }

    public float brightness(int lightLevel) {
        return this.brightnessRamp[lightLevel];
    }

    public Tag<Block> infiniburn() {
        Tag<Block> tag = TagsBlock.getAllTags().getTag(this.infiniburn);
        return (Tag<Block>)(tag != null ? tag : TagsBlock.INFINIBURN_OVERWORLD);
    }

    public MinecraftKey effectsLocation() {
        return this.effectsLocation;
    }

    public boolean equalTo(DimensionManager dimensionType) {
        if (this == dimensionType) {
            return true;
        } else {
            return this.hasSkylight == dimensionType.hasSkylight && this.hasCeiling == dimensionType.hasCeiling && this.ultraWarm == dimensionType.ultraWarm && this.natural == dimensionType.natural && this.coordinateScale == dimensionType.coordinateScale && this.createDragonFight == dimensionType.createDragonFight && this.piglinSafe == dimensionType.piglinSafe && this.bedWorks == dimensionType.bedWorks && this.respawnAnchorWorks == dimensionType.respawnAnchorWorks && this.hasRaids == dimensionType.hasRaids && this.minY == dimensionType.minY && this.height == dimensionType.height && this.logicalHeight == dimensionType.logicalHeight && Float.compare(dimensionType.ambientLight, this.ambientLight) == 0 && this.fixedTime.equals(dimensionType.fixedTime) && this.infiniburn.equals(dimensionType.infiniburn) && this.effectsLocation.equals(dimensionType.effectsLocation);
        }
    }

    static {
        DIRECT_CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.LONG.optionalFieldOf("fixed_time").xmap((optional) -> {
                return optional.map(OptionalLong::of).orElseGet(OptionalLong::empty);
            }, (optionalLong) -> {
                return optionalLong.isPresent() ? Optional.of(optionalLong.getAsLong()) : Optional.empty();
            }).forGetter((dimensionType) -> {
                return dimensionType.fixedTime;
            }), Codec.BOOL.fieldOf("has_skylight").forGetter(DimensionManager::hasSkyLight), Codec.BOOL.fieldOf("has_ceiling").forGetter(DimensionManager::hasCeiling), Codec.BOOL.fieldOf("ultrawarm").forGetter(DimensionManager::isNether), Codec.BOOL.fieldOf("natural").forGetter(DimensionManager::isNatural), Codec.doubleRange((double)1.0E-5F, 3.0E7D).fieldOf("coordinate_scale").forGetter(DimensionManager::getCoordinateScale), Codec.BOOL.fieldOf("piglin_safe").forGetter(DimensionManager::isPiglinSafe), Codec.BOOL.fieldOf("bed_works").forGetter(DimensionManager::isBedWorks), Codec.BOOL.fieldOf("respawn_anchor_works").forGetter(DimensionManager::isRespawnAnchorWorks), Codec.BOOL.fieldOf("has_raids").forGetter(DimensionManager::hasRaids), Codec.intRange(MIN_Y, MAX_Y).fieldOf("min_y").forGetter(DimensionManager::getMinY), Codec.intRange(16, Y_SIZE).fieldOf("height").forGetter(DimensionManager::getHeight), Codec.intRange(0, Y_SIZE).fieldOf("logical_height").forGetter(DimensionManager::getLogicalHeight), MinecraftKey.CODEC.fieldOf("infiniburn").forGetter((dimensionType) -> {
                return dimensionType.infiniburn;
            }), MinecraftKey.CODEC.fieldOf("effects").orElse(OVERWORLD_EFFECTS).forGetter((dimensionType) -> {
                return dimensionType.effectsLocation;
            }), Codec.FLOAT.fieldOf("ambient_light").forGetter((dimensionType) -> {
                return dimensionType.ambientLight;
            })).apply(instance, DimensionManager::new);
        }).comapFlatMap(DimensionManager::guardY, Function.identity());
    }
}
