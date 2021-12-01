package net.minecraft.core;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.particles.Particle;
import net.minecraft.core.particles.Particles;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.DispenserRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.stats.StatisticWrapper;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.FloatProviderType;
import net.minecraft.util.valueproviders.IntProviderType;
import net.minecraft.world.effect.MobEffectBase;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.decoration.Paintings;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.dimension.WorldDimension;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.level.levelgen.GeneratorSettingBase;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverAbstract;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverWrapper;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeType;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacers;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProviders;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructurePoolTemplate;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructurePools;
import net.minecraft.world.level.levelgen.feature.treedecorators.WorldGenFeatureTrees;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacers;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureRuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureStructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorList;
import net.minecraft.world.level.levelgen.synth.NormalNoise$NoiseParameters;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.storage.loot.entries.LootEntries;
import net.minecraft.world.level.storage.loot.entries.LootEntryType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraft.world.level.storage.loot.providers.nbt.LootNbtProviderType;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProviders;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.level.storage.loot.providers.score.LootScoreProviderType;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProviders;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class IRegistry<T> implements Keyable, Registry<T> {
    protected static final Logger LOGGER = LogManager.getLogger();
    private static final Map<MinecraftKey, Supplier<?>> LOADERS = Maps.newLinkedHashMap();
    public static final MinecraftKey ROOT_REGISTRY_NAME = new MinecraftKey("root");
    protected static final IRegistryWritable<IRegistryWritable<?>> WRITABLE_REGISTRY = new RegistryMaterials<>(createRegistryKey("root"), Lifecycle.experimental());
    public static final IRegistry<? extends IRegistry<?>> REGISTRY = WRITABLE_REGISTRY;
    public static final ResourceKey<IRegistry<SoundEffect>> SOUND_EVENT_REGISTRY = createRegistryKey("sound_event");
    public static final ResourceKey<IRegistry<FluidType>> FLUID_REGISTRY = createRegistryKey("fluid");
    public static final ResourceKey<IRegistry<MobEffectBase>> MOB_EFFECT_REGISTRY = createRegistryKey("mob_effect");
    public static final ResourceKey<IRegistry<Block>> BLOCK_REGISTRY = createRegistryKey("block");
    public static final ResourceKey<IRegistry<Enchantment>> ENCHANTMENT_REGISTRY = createRegistryKey("enchantment");
    public static final ResourceKey<IRegistry<EntityTypes<?>>> ENTITY_TYPE_REGISTRY = createRegistryKey("entity_type");
    public static final ResourceKey<IRegistry<Item>> ITEM_REGISTRY = createRegistryKey("item");
    public static final ResourceKey<IRegistry<PotionRegistry>> POTION_REGISTRY = createRegistryKey("potion");
    public static final ResourceKey<IRegistry<Particle<?>>> PARTICLE_TYPE_REGISTRY = createRegistryKey("particle_type");
    public static final ResourceKey<IRegistry<TileEntityTypes<?>>> BLOCK_ENTITY_TYPE_REGISTRY = createRegistryKey("block_entity_type");
    public static final ResourceKey<IRegistry<Paintings>> MOTIVE_REGISTRY = createRegistryKey("motive");
    public static final ResourceKey<IRegistry<MinecraftKey>> CUSTOM_STAT_REGISTRY = createRegistryKey("custom_stat");
    public static final ResourceKey<IRegistry<ChunkStatus>> CHUNK_STATUS_REGISTRY = createRegistryKey("chunk_status");
    public static final ResourceKey<IRegistry<DefinedStructureRuleTestType<?>>> RULE_TEST_REGISTRY = createRegistryKey("rule_test");
    public static final ResourceKey<IRegistry<PosRuleTestType<?>>> POS_RULE_TEST_REGISTRY = createRegistryKey("pos_rule_test");
    public static final ResourceKey<IRegistry<Containers<?>>> MENU_REGISTRY = createRegistryKey("menu");
    public static final ResourceKey<IRegistry<Recipes<?>>> RECIPE_TYPE_REGISTRY = createRegistryKey("recipe_type");
    public static final ResourceKey<IRegistry<RecipeSerializer<?>>> RECIPE_SERIALIZER_REGISTRY = createRegistryKey("recipe_serializer");
    public static final ResourceKey<IRegistry<AttributeBase>> ATTRIBUTE_REGISTRY = createRegistryKey("attribute");
    public static final ResourceKey<IRegistry<GameEvent>> GAME_EVENT_REGISTRY = createRegistryKey("game_event");
    public static final ResourceKey<IRegistry<PositionSourceType<?>>> POSITION_SOURCE_TYPE_REGISTRY = createRegistryKey("position_source_type");
    public static final ResourceKey<IRegistry<StatisticWrapper<?>>> STAT_TYPE_REGISTRY = createRegistryKey("stat_type");
    public static final ResourceKey<IRegistry<VillagerType>> VILLAGER_TYPE_REGISTRY = createRegistryKey("villager_type");
    public static final ResourceKey<IRegistry<VillagerProfession>> VILLAGER_PROFESSION_REGISTRY = createRegistryKey("villager_profession");
    public static final ResourceKey<IRegistry<VillagePlaceType>> POINT_OF_INTEREST_TYPE_REGISTRY = createRegistryKey("point_of_interest_type");
    public static final ResourceKey<IRegistry<MemoryModuleType<?>>> MEMORY_MODULE_TYPE_REGISTRY = createRegistryKey("memory_module_type");
    public static final ResourceKey<IRegistry<SensorType<?>>> SENSOR_TYPE_REGISTRY = createRegistryKey("sensor_type");
    public static final ResourceKey<IRegistry<Schedule>> SCHEDULE_REGISTRY = createRegistryKey("schedule");
    public static final ResourceKey<IRegistry<Activity>> ACTIVITY_REGISTRY = createRegistryKey("activity");
    public static final ResourceKey<IRegistry<LootEntryType>> LOOT_ENTRY_REGISTRY = createRegistryKey("loot_pool_entry_type");
    public static final ResourceKey<IRegistry<LootItemFunctionType>> LOOT_FUNCTION_REGISTRY = createRegistryKey("loot_function_type");
    public static final ResourceKey<IRegistry<LootItemConditionType>> LOOT_ITEM_REGISTRY = createRegistryKey("loot_condition_type");
    public static final ResourceKey<IRegistry<LootNumberProviderType>> LOOT_NUMBER_PROVIDER_REGISTRY = createRegistryKey("loot_number_provider_type");
    public static final ResourceKey<IRegistry<LootNbtProviderType>> LOOT_NBT_PROVIDER_REGISTRY = createRegistryKey("loot_nbt_provider_type");
    public static final ResourceKey<IRegistry<LootScoreProviderType>> LOOT_SCORE_PROVIDER_REGISTRY = createRegistryKey("loot_score_provider_type");
    public static final ResourceKey<IRegistry<DimensionManager>> DIMENSION_TYPE_REGISTRY = createRegistryKey("dimension_type");
    public static final ResourceKey<IRegistry<World>> DIMENSION_REGISTRY = createRegistryKey("dimension");
    public static final ResourceKey<IRegistry<WorldDimension>> LEVEL_STEM_REGISTRY = createRegistryKey("dimension");
    public static final RegistryBlocks<GameEvent> GAME_EVENT = registerDefaulted(GAME_EVENT_REGISTRY, "step", () -> {
        return GameEvent.STEP;
    });
    public static final IRegistry<SoundEffect> SOUND_EVENT = registerSimple(SOUND_EVENT_REGISTRY, () -> {
        return SoundEffects.ITEM_PICKUP;
    });
    public static final RegistryBlocks<FluidType> FLUID = registerDefaulted(FLUID_REGISTRY, "empty", () -> {
        return FluidTypes.EMPTY;
    });
    public static final IRegistry<MobEffectBase> MOB_EFFECT = registerSimple(MOB_EFFECT_REGISTRY, () -> {
        return MobEffectList.LUCK;
    });
    public static final RegistryBlocks<Block> BLOCK = registerDefaulted(BLOCK_REGISTRY, "air", () -> {
        return Blocks.AIR;
    });
    public static final IRegistry<Enchantment> ENCHANTMENT = registerSimple(ENCHANTMENT_REGISTRY, () -> {
        return Enchantments.BLOCK_FORTUNE;
    });
    public static final RegistryBlocks<EntityTypes<?>> ENTITY_TYPE = registerDefaulted(ENTITY_TYPE_REGISTRY, "pig", () -> {
        return EntityTypes.PIG;
    });
    public static final RegistryBlocks<Item> ITEM = registerDefaulted(ITEM_REGISTRY, "air", () -> {
        return Items.AIR;
    });
    public static final RegistryBlocks<PotionRegistry> POTION = registerDefaulted(POTION_REGISTRY, "empty", () -> {
        return Potions.EMPTY;
    });
    public static final IRegistry<Particle<?>> PARTICLE_TYPE = registerSimple(PARTICLE_TYPE_REGISTRY, () -> {
        return Particles.BLOCK;
    });
    public static final IRegistry<TileEntityTypes<?>> BLOCK_ENTITY_TYPE = registerSimple(BLOCK_ENTITY_TYPE_REGISTRY, () -> {
        return TileEntityTypes.FURNACE;
    });
    public static final RegistryBlocks<Paintings> MOTIVE = registerDefaulted(MOTIVE_REGISTRY, "kebab", () -> {
        return Paintings.KEBAB;
    });
    public static final IRegistry<MinecraftKey> CUSTOM_STAT = registerSimple(CUSTOM_STAT_REGISTRY, () -> {
        return StatisticList.JUMP;
    });
    public static final RegistryBlocks<ChunkStatus> CHUNK_STATUS = registerDefaulted(CHUNK_STATUS_REGISTRY, "empty", () -> {
        return ChunkStatus.EMPTY;
    });
    public static final IRegistry<DefinedStructureRuleTestType<?>> RULE_TEST = registerSimple(RULE_TEST_REGISTRY, () -> {
        return DefinedStructureRuleTestType.ALWAYS_TRUE_TEST;
    });
    public static final IRegistry<PosRuleTestType<?>> POS_RULE_TEST = registerSimple(POS_RULE_TEST_REGISTRY, () -> {
        return PosRuleTestType.ALWAYS_TRUE_TEST;
    });
    public static final IRegistry<Containers<?>> MENU = registerSimple(MENU_REGISTRY, () -> {
        return Containers.ANVIL;
    });
    public static final IRegistry<Recipes<?>> RECIPE_TYPE = registerSimple(RECIPE_TYPE_REGISTRY, () -> {
        return Recipes.CRAFTING;
    });
    public static final IRegistry<RecipeSerializer<?>> RECIPE_SERIALIZER = registerSimple(RECIPE_SERIALIZER_REGISTRY, () -> {
        return RecipeSerializer.SHAPELESS_RECIPE;
    });
    public static final IRegistry<AttributeBase> ATTRIBUTE = registerSimple(ATTRIBUTE_REGISTRY, () -> {
        return GenericAttributes.LUCK;
    });
    public static final IRegistry<PositionSourceType<?>> POSITION_SOURCE_TYPE = registerSimple(POSITION_SOURCE_TYPE_REGISTRY, () -> {
        return PositionSourceType.BLOCK;
    });
    public static final IRegistry<StatisticWrapper<?>> STAT_TYPE = registerSimple(STAT_TYPE_REGISTRY, () -> {
        return StatisticList.ITEM_USED;
    });
    public static final RegistryBlocks<VillagerType> VILLAGER_TYPE = registerDefaulted(VILLAGER_TYPE_REGISTRY, "plains", () -> {
        return VillagerType.PLAINS;
    });
    public static final RegistryBlocks<VillagerProfession> VILLAGER_PROFESSION = registerDefaulted(VILLAGER_PROFESSION_REGISTRY, "none", () -> {
        return VillagerProfession.NONE;
    });
    public static final RegistryBlocks<VillagePlaceType> POINT_OF_INTEREST_TYPE = registerDefaulted(POINT_OF_INTEREST_TYPE_REGISTRY, "unemployed", () -> {
        return VillagePlaceType.UNEMPLOYED;
    });
    public static final RegistryBlocks<MemoryModuleType<?>> MEMORY_MODULE_TYPE = registerDefaulted(MEMORY_MODULE_TYPE_REGISTRY, "dummy", () -> {
        return MemoryModuleType.DUMMY;
    });
    public static final RegistryBlocks<SensorType<?>> SENSOR_TYPE = registerDefaulted(SENSOR_TYPE_REGISTRY, "dummy", () -> {
        return SensorType.DUMMY;
    });
    public static final IRegistry<Schedule> SCHEDULE = registerSimple(SCHEDULE_REGISTRY, () -> {
        return Schedule.EMPTY;
    });
    public static final IRegistry<Activity> ACTIVITY = registerSimple(ACTIVITY_REGISTRY, () -> {
        return Activity.IDLE;
    });
    public static final IRegistry<LootEntryType> LOOT_POOL_ENTRY_TYPE = registerSimple(LOOT_ENTRY_REGISTRY, () -> {
        return LootEntries.EMPTY;
    });
    public static final IRegistry<LootItemFunctionType> LOOT_FUNCTION_TYPE = registerSimple(LOOT_FUNCTION_REGISTRY, () -> {
        return LootItemFunctions.SET_COUNT;
    });
    public static final IRegistry<LootItemConditionType> LOOT_CONDITION_TYPE = registerSimple(LOOT_ITEM_REGISTRY, () -> {
        return LootItemConditions.INVERTED;
    });
    public static final IRegistry<LootNumberProviderType> LOOT_NUMBER_PROVIDER_TYPE = registerSimple(LOOT_NUMBER_PROVIDER_REGISTRY, () -> {
        return NumberProviders.CONSTANT;
    });
    public static final IRegistry<LootNbtProviderType> LOOT_NBT_PROVIDER_TYPE = registerSimple(LOOT_NBT_PROVIDER_REGISTRY, () -> {
        return NbtProviders.CONTEXT;
    });
    public static final IRegistry<LootScoreProviderType> LOOT_SCORE_PROVIDER_TYPE = registerSimple(LOOT_SCORE_PROVIDER_REGISTRY, () -> {
        return ScoreboardNameProviders.CONTEXT;
    });
    public static final ResourceKey<IRegistry<FloatProviderType<?>>> FLOAT_PROVIDER_TYPE_REGISTRY = createRegistryKey("float_provider_type");
    public static final IRegistry<FloatProviderType<?>> FLOAT_PROVIDER_TYPES = registerSimple(FLOAT_PROVIDER_TYPE_REGISTRY, () -> {
        return FloatProviderType.CONSTANT;
    });
    public static final ResourceKey<IRegistry<IntProviderType<?>>> INT_PROVIDER_TYPE_REGISTRY = createRegistryKey("int_provider_type");
    public static final IRegistry<IntProviderType<?>> INT_PROVIDER_TYPES = registerSimple(INT_PROVIDER_TYPE_REGISTRY, () -> {
        return IntProviderType.CONSTANT;
    });
    public static final ResourceKey<IRegistry<HeightProviderType<?>>> HEIGHT_PROVIDER_TYPE_REGISTRY = createRegistryKey("height_provider_type");
    public static final IRegistry<HeightProviderType<?>> HEIGHT_PROVIDER_TYPES = registerSimple(HEIGHT_PROVIDER_TYPE_REGISTRY, () -> {
        return HeightProviderType.CONSTANT;
    });
    public static final ResourceKey<IRegistry<BlockPredicateType<?>>> BLOCK_PREDICATE_TYPE_REGISTRY = createRegistryKey("block_predicate_type");
    public static final IRegistry<BlockPredicateType<?>> BLOCK_PREDICATE_TYPES = registerSimple(BLOCK_PREDICATE_TYPE_REGISTRY, () -> {
        return BlockPredicateType.NOT;
    });
    public static final ResourceKey<IRegistry<GeneratorSettingBase>> NOISE_GENERATOR_SETTINGS_REGISTRY = createRegistryKey("worldgen/noise_settings");
    public static final ResourceKey<IRegistry<WorldGenCarverWrapper<?>>> CONFIGURED_CARVER_REGISTRY = createRegistryKey("worldgen/configured_carver");
    public static final ResourceKey<IRegistry<WorldGenFeatureConfigured<?, ?>>> CONFIGURED_FEATURE_REGISTRY = createRegistryKey("worldgen/configured_feature");
    public static final ResourceKey<IRegistry<PlacedFeature>> PLACED_FEATURE_REGISTRY = createRegistryKey("worldgen/placed_feature");
    public static final ResourceKey<IRegistry<StructureFeature<?, ?>>> CONFIGURED_STRUCTURE_FEATURE_REGISTRY = createRegistryKey("worldgen/configured_structure_feature");
    public static final ResourceKey<IRegistry<ProcessorList>> PROCESSOR_LIST_REGISTRY = createRegistryKey("worldgen/processor_list");
    public static final ResourceKey<IRegistry<WorldGenFeatureDefinedStructurePoolTemplate>> TEMPLATE_POOL_REGISTRY = createRegistryKey("worldgen/template_pool");
    public static final ResourceKey<IRegistry<BiomeBase>> BIOME_REGISTRY = createRegistryKey("worldgen/biome");
    public static final ResourceKey<IRegistry<NormalNoise$NoiseParameters>> NOISE_REGISTRY = createRegistryKey("worldgen/noise");
    public static final ResourceKey<IRegistry<WorldGenCarverAbstract<?>>> CARVER_REGISTRY = createRegistryKey("worldgen/carver");
    public static final IRegistry<WorldGenCarverAbstract<?>> CARVER = registerSimple(CARVER_REGISTRY, () -> {
        return WorldGenCarverAbstract.CAVE;
    });
    public static final ResourceKey<IRegistry<WorldGenerator<?>>> FEATURE_REGISTRY = createRegistryKey("worldgen/feature");
    public static final IRegistry<WorldGenerator<?>> FEATURE = registerSimple(FEATURE_REGISTRY, () -> {
        return WorldGenerator.ORE;
    });
    public static final ResourceKey<IRegistry<StructureGenerator<?>>> STRUCTURE_FEATURE_REGISTRY = createRegistryKey("worldgen/structure_feature");
    public static final IRegistry<StructureGenerator<?>> STRUCTURE_FEATURE = registerSimple(STRUCTURE_FEATURE_REGISTRY, () -> {
        return StructureGenerator.MINESHAFT;
    });
    public static final ResourceKey<IRegistry<WorldGenFeatureStructurePieceType>> STRUCTURE_PIECE_REGISTRY = createRegistryKey("worldgen/structure_piece");
    public static final IRegistry<WorldGenFeatureStructurePieceType> STRUCTURE_PIECE = registerSimple(STRUCTURE_PIECE_REGISTRY, () -> {
        return WorldGenFeatureStructurePieceType.MINE_SHAFT_ROOM;
    });
    public static final ResourceKey<IRegistry<PlacementModifierType<?>>> PLACEMENT_MODIFIER_REGISTRY = createRegistryKey("worldgen/placement_modifier_type");
    public static final IRegistry<PlacementModifierType<?>> PLACEMENT_MODIFIERS = registerSimple(PLACEMENT_MODIFIER_REGISTRY, () -> {
        return PlacementModifierType.COUNT;
    });
    public static final ResourceKey<IRegistry<WorldGenFeatureStateProviders<?>>> BLOCK_STATE_PROVIDER_TYPE_REGISTRY = createRegistryKey("worldgen/block_state_provider_type");
    public static final ResourceKey<IRegistry<WorldGenFoilagePlacers<?>>> FOLIAGE_PLACER_TYPE_REGISTRY = createRegistryKey("worldgen/foliage_placer_type");
    public static final ResourceKey<IRegistry<TrunkPlacers<?>>> TRUNK_PLACER_TYPE_REGISTRY = createRegistryKey("worldgen/trunk_placer_type");
    public static final ResourceKey<IRegistry<WorldGenFeatureTrees<?>>> TREE_DECORATOR_TYPE_REGISTRY = createRegistryKey("worldgen/tree_decorator_type");
    public static final ResourceKey<IRegistry<FeatureSizeType<?>>> FEATURE_SIZE_TYPE_REGISTRY = createRegistryKey("worldgen/feature_size_type");
    public static final ResourceKey<IRegistry<Codec<? extends WorldChunkManager>>> BIOME_SOURCE_REGISTRY = createRegistryKey("worldgen/biome_source");
    public static final ResourceKey<IRegistry<Codec<? extends ChunkGenerator>>> CHUNK_GENERATOR_REGISTRY = createRegistryKey("worldgen/chunk_generator");
    public static final ResourceKey<IRegistry<Codec<? extends SurfaceRules.ConditionSource>>> CONDITION_REGISTRY = createRegistryKey("worldgen/material_condition");
    public static final ResourceKey<IRegistry<Codec<? extends SurfaceRules.RuleSource>>> RULE_REGISTRY = createRegistryKey("worldgen/material_rule");
    public static final ResourceKey<IRegistry<DefinedStructureStructureProcessorType<?>>> STRUCTURE_PROCESSOR_REGISTRY = createRegistryKey("worldgen/structure_processor");
    public static final ResourceKey<IRegistry<WorldGenFeatureDefinedStructurePools<?>>> STRUCTURE_POOL_ELEMENT_REGISTRY = createRegistryKey("worldgen/structure_pool_element");
    public static final IRegistry<WorldGenFeatureStateProviders<?>> BLOCKSTATE_PROVIDER_TYPES = registerSimple(BLOCK_STATE_PROVIDER_TYPE_REGISTRY, () -> {
        return WorldGenFeatureStateProviders.SIMPLE_STATE_PROVIDER;
    });
    public static final IRegistry<WorldGenFoilagePlacers<?>> FOLIAGE_PLACER_TYPES = registerSimple(FOLIAGE_PLACER_TYPE_REGISTRY, () -> {
        return WorldGenFoilagePlacers.BLOB_FOLIAGE_PLACER;
    });
    public static final IRegistry<TrunkPlacers<?>> TRUNK_PLACER_TYPES = registerSimple(TRUNK_PLACER_TYPE_REGISTRY, () -> {
        return TrunkPlacers.STRAIGHT_TRUNK_PLACER;
    });
    public static final IRegistry<WorldGenFeatureTrees<?>> TREE_DECORATOR_TYPES = registerSimple(TREE_DECORATOR_TYPE_REGISTRY, () -> {
        return WorldGenFeatureTrees.LEAVE_VINE;
    });
    public static final IRegistry<FeatureSizeType<?>> FEATURE_SIZE_TYPES = registerSimple(FEATURE_SIZE_TYPE_REGISTRY, () -> {
        return FeatureSizeType.TWO_LAYERS_FEATURE_SIZE;
    });
    public static final IRegistry<Codec<? extends WorldChunkManager>> BIOME_SOURCE = registerSimple(BIOME_SOURCE_REGISTRY, Lifecycle.stable(), () -> {
        return WorldChunkManager.CODEC;
    });
    public static final IRegistry<Codec<? extends ChunkGenerator>> CHUNK_GENERATOR = registerSimple(CHUNK_GENERATOR_REGISTRY, Lifecycle.stable(), () -> {
        return ChunkGenerator.CODEC;
    });
    public static final IRegistry<Codec<? extends SurfaceRules.ConditionSource>> CONDITION = registerSimple(CONDITION_REGISTRY, SurfaceRules.ConditionSource::bootstrap);
    public static final IRegistry<Codec<? extends SurfaceRules.RuleSource>> RULE = registerSimple(RULE_REGISTRY, SurfaceRules.RuleSource::bootstrap);
    public static final IRegistry<DefinedStructureStructureProcessorType<?>> STRUCTURE_PROCESSOR = registerSimple(STRUCTURE_PROCESSOR_REGISTRY, () -> {
        return DefinedStructureStructureProcessorType.BLOCK_IGNORE;
    });
    public static final IRegistry<WorldGenFeatureDefinedStructurePools<?>> STRUCTURE_POOL_ELEMENT = registerSimple(STRUCTURE_POOL_ELEMENT_REGISTRY, () -> {
        return WorldGenFeatureDefinedStructurePools.EMPTY;
    });
    private final ResourceKey<? extends IRegistry<T>> key;
    private final Lifecycle lifecycle;

    private static <T> ResourceKey<IRegistry<T>> createRegistryKey(String registryId) {
        return ResourceKey.createRegistryKey(new MinecraftKey(registryId));
    }

    public static <T extends IRegistryWritable<?>> void checkRegistry(IRegistryWritable<T> registry) {
        registry.forEach((writableRegistry2) -> {
            if (writableRegistry2.keySet().isEmpty()) {
                SystemUtils.logAndPauseIfInIde("Registry '" + registry.getKey(writableRegistry2) + "' was empty after loading");
            }

            if (writableRegistry2 instanceof RegistryBlocks) {
                MinecraftKey resourceLocation = ((RegistryBlocks)writableRegistry2).getDefaultKey();
                Validate.notNull(writableRegistry2.get(resourceLocation), "Missing default of DefaultedMappedRegistry: " + resourceLocation);
            }

        });
    }

    private static <T> IRegistry<T> registerSimple(ResourceKey<? extends IRegistry<T>> key, Supplier<T> defaultEntry) {
        return registerSimple(key, Lifecycle.experimental(), defaultEntry);
    }

    private static <T> RegistryBlocks<T> registerDefaulted(ResourceKey<? extends IRegistry<T>> key, String defaultId, Supplier<T> defaultEntry) {
        return registerDefaulted(key, defaultId, Lifecycle.experimental(), defaultEntry);
    }

    private static <T> IRegistry<T> registerSimple(ResourceKey<? extends IRegistry<T>> key, Lifecycle lifecycle, Supplier<T> defaultEntry) {
        return internalRegister(key, new RegistryMaterials<>(key, lifecycle), defaultEntry, lifecycle);
    }

    private static <T> RegistryBlocks<T> registerDefaulted(ResourceKey<? extends IRegistry<T>> key, String defaultId, Lifecycle lifecycle, Supplier<T> defaultEntry) {
        return internalRegister(key, new RegistryBlocks<>(defaultId, key, lifecycle), defaultEntry, lifecycle);
    }

    private static <T, R extends IRegistryWritable<T>> R internalRegister(ResourceKey<? extends IRegistry<T>> key, R registry, Supplier<T> defaultEntry, Lifecycle lifecycle) {
        MinecraftKey resourceLocation = key.location();
        LOADERS.put(resourceLocation, defaultEntry);
        IRegistryWritable<R> writableRegistry = WRITABLE_REGISTRY;
        return writableRegistry.register(key, registry, lifecycle);
    }

    protected IRegistry(ResourceKey<? extends IRegistry<T>> key, Lifecycle lifecycle) {
        DispenserRegistry.checkBootstrapCalled(() -> {
            return "registry " + key;
        });
        this.key = key;
        this.lifecycle = lifecycle;
    }

    public ResourceKey<? extends IRegistry<T>> key() {
        return this.key;
    }

    public Lifecycle lifecycle() {
        return this.lifecycle;
    }

    @Override
    public String toString() {
        return "Registry[" + this.key + " (" + this.lifecycle + ")]";
    }

    public Codec<T> byNameCodec() {
        Codec<T> codec = MinecraftKey.CODEC.flatXmap((resourceLocation) -> {
            return Optional.ofNullable(this.get(resourceLocation)).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown registry key in " + this.key + ": " + resourceLocation);
            });
        }, (object) -> {
            return this.getResourceKey(object).map(ResourceKey::location).map(DataResult::success).orElseGet(() -> {
                return DataResult.error("Unknown registry element in " + this.key + ":" + object);
            });
        });
        Codec<T> codec2 = ExtraCodecs.idResolverCodec((object) -> {
            return this.getResourceKey(object).isPresent() ? this.getId(object) : -1;
        }, this::fromId, -1);
        return ExtraCodecs.overrideLifecycle(ExtraCodecs.orCompressed(codec, codec2), this::lifecycle, (object) -> {
            return this.lifecycle;
        });
    }

    public <U> Stream<U> keys(DynamicOps<U> dynamicOps) {
        return this.keySet().stream().map((resourceLocation) -> {
            return dynamicOps.createString(resourceLocation.toString());
        });
    }

    @Nullable
    public abstract MinecraftKey getKey(T entry);

    public abstract Optional<ResourceKey<T>> getResourceKey(T entry);

    @Override
    public abstract int getId(@Nullable T entry);

    @Nullable
    public abstract T get(@Nullable ResourceKey<T> key);

    @Nullable
    public abstract T get(@Nullable MinecraftKey id);

    public abstract Lifecycle lifecycle(T entry);

    public abstract Lifecycle elementsLifecycle();

    public Optional<T> getOptional(@Nullable MinecraftKey id) {
        return Optional.ofNullable(this.get(id));
    }

    public Optional<T> getOptional(@Nullable ResourceKey<T> key) {
        return Optional.ofNullable(this.get(key));
    }

    public T getOrThrow(ResourceKey<T> key) {
        T object = this.get(key);
        if (object == null) {
            throw new IllegalStateException("Missing key in " + this.key + ": " + key);
        } else {
            return object;
        }
    }

    public abstract Set<MinecraftKey> keySet();

    public abstract Set<Entry<ResourceKey<T>, T>> entrySet();

    @Nullable
    public abstract T getRandom(Random random);

    public Stream<T> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    public abstract boolean containsKey(MinecraftKey id);

    public abstract boolean containsKey(ResourceKey<T> key);

    public static <T> T register(IRegistry<? super T> registry, String id, T entry) {
        return register(registry, new MinecraftKey(id), entry);
    }

    public static <V, T extends V> T register(IRegistry<V> registry, MinecraftKey id, T entry) {
        return register(registry, ResourceKey.create(registry.key, id), entry);
    }

    public static <V, T extends V> T register(IRegistry<V> registry, ResourceKey<V> key, T entry) {
        return ((IRegistryWritable)registry).register(key, entry, Lifecycle.stable());
    }

    public static <V, T extends V> T registerMapping(IRegistry<V> registry, int rawId, String id, T entry) {
        return ((IRegistryWritable)registry).registerMapping(rawId, ResourceKey.create(registry.key, new MinecraftKey(id)), entry, Lifecycle.stable());
    }

    static {
        RegistryGeneration.bootstrap();
        LOADERS.forEach((id, defaultEntry) -> {
            if (defaultEntry.get() == null) {
                LOGGER.error("Unable to bootstrap registry '{}'", (Object)id);
            }

        });
        checkRegistry(WRITABLE_REGISTRY);
    }
}
