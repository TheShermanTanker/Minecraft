package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.util.INamable;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BiomeSettingsMobs {
    public static final Logger LOGGER = LogManager.getLogger();
    private static final float DEFAULT_CREATURE_SPAWN_PROBABILITY = 0.1F;
    public static final WeightedRandomList<BiomeSettingsMobs.SpawnerData> EMPTY_MOB_LIST = WeightedRandomList.create();
    public static final BiomeSettingsMobs EMPTY = (new BiomeSettingsMobs.Builder()).build();
    public static final MapCodec<BiomeSettingsMobs> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(Codec.floatRange(0.0F, 0.9999999F).optionalFieldOf("creature_spawn_probability", 0.1F).forGetter((mobSpawnSettings) -> {
            return mobSpawnSettings.creatureGenerationProbability;
        }), Codec.simpleMap(EnumCreatureType.CODEC, WeightedRandomList.codec(BiomeSettingsMobs.SpawnerData.CODEC).promotePartial(SystemUtils.prefix("Spawn data: ", LOGGER::error)), INamable.keys(EnumCreatureType.values())).fieldOf("spawners").forGetter((mobSpawnSettings) -> {
            return mobSpawnSettings.spawners;
        }), Codec.simpleMap(IRegistry.ENTITY_TYPE.byNameCodec(), BiomeSettingsMobs.MobSpawnCost.CODEC, IRegistry.ENTITY_TYPE).fieldOf("spawn_costs").forGetter((mobSpawnSettings) -> {
            return mobSpawnSettings.mobSpawnCosts;
        })).apply(instance, BiomeSettingsMobs::new);
    });
    private final float creatureGenerationProbability;
    private final Map<EnumCreatureType, WeightedRandomList<BiomeSettingsMobs.SpawnerData>> spawners;
    private final Map<EntityTypes<?>, BiomeSettingsMobs.MobSpawnCost> mobSpawnCosts;

    BiomeSettingsMobs(float creatureSpawnProbability, Map<EnumCreatureType, WeightedRandomList<BiomeSettingsMobs.SpawnerData>> spawners, Map<EntityTypes<?>, BiomeSettingsMobs.MobSpawnCost> spawnCosts) {
        this.creatureGenerationProbability = creatureSpawnProbability;
        this.spawners = ImmutableMap.copyOf(spawners);
        this.mobSpawnCosts = ImmutableMap.copyOf(spawnCosts);
    }

    public WeightedRandomList<BiomeSettingsMobs.SpawnerData> getMobs(EnumCreatureType spawnGroup) {
        return this.spawners.getOrDefault(spawnGroup, EMPTY_MOB_LIST);
    }

    @Nullable
    public BiomeSettingsMobs.MobSpawnCost getMobSpawnCost(EntityTypes<?> entityType) {
        return this.mobSpawnCosts.get(entityType);
    }

    public float getCreatureProbability() {
        return this.creatureGenerationProbability;
    }

    public static class Builder {
        private final Map<EnumCreatureType, List<BiomeSettingsMobs.SpawnerData>> spawners = Stream.of(EnumCreatureType.values()).collect(ImmutableMap.toImmutableMap((mobCategory) -> {
            return mobCategory;
        }, (mobCategory) -> {
            return Lists.newArrayList();
        }));
        private final Map<EntityTypes<?>, BiomeSettingsMobs.MobSpawnCost> mobSpawnCosts = Maps.newLinkedHashMap();
        private float creatureGenerationProbability = 0.1F;

        public BiomeSettingsMobs.Builder addSpawn(EnumCreatureType spawnGroup, BiomeSettingsMobs.SpawnerData spawnEntry) {
            this.spawners.get(spawnGroup).add(spawnEntry);
            return this;
        }

        public BiomeSettingsMobs.Builder addMobCharge(EntityTypes<?> entityType, double mass, double gravityLimit) {
            this.mobSpawnCosts.put(entityType, new BiomeSettingsMobs.MobSpawnCost(gravityLimit, mass));
            return this;
        }

        public BiomeSettingsMobs.Builder creatureGenerationProbability(float probability) {
            this.creatureGenerationProbability = probability;
            return this;
        }

        public BiomeSettingsMobs build() {
            return new BiomeSettingsMobs(this.creatureGenerationProbability, this.spawners.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (entry) -> {
                return WeightedRandomList.create(entry.getValue());
            })), ImmutableMap.copyOf(this.mobSpawnCosts));
        }
    }

    public static class MobSpawnCost {
        public static final Codec<BiomeSettingsMobs.MobSpawnCost> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.DOUBLE.fieldOf("energy_budget").forGetter((mobSpawnCost) -> {
                return mobSpawnCost.energyBudget;
            }), Codec.DOUBLE.fieldOf("charge").forGetter((mobSpawnCost) -> {
                return mobSpawnCost.charge;
            })).apply(instance, BiomeSettingsMobs.MobSpawnCost::new);
        });
        private final double energyBudget;
        private final double charge;

        MobSpawnCost(double gravityLimit, double mass) {
            this.energyBudget = gravityLimit;
            this.charge = mass;
        }

        public double getEnergyBudget() {
            return this.energyBudget;
        }

        public double getCharge() {
            return this.charge;
        }
    }

    public static class SpawnerData extends WeightedEntry.IntrusiveBase {
        public static final Codec<BiomeSettingsMobs.SpawnerData> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(IRegistry.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter((spawnerData) -> {
                return spawnerData.type;
            }), Weight.CODEC.fieldOf("weight").forGetter(WeightedEntry.IntrusiveBase::getWeight), Codec.INT.fieldOf("minCount").forGetter((spawnerData) -> {
                return spawnerData.minCount;
            }), Codec.INT.fieldOf("maxCount").forGetter((spawnerData) -> {
                return spawnerData.maxCount;
            })).apply(instance, BiomeSettingsMobs.SpawnerData::new);
        });
        public final EntityTypes<?> type;
        public final int minCount;
        public final int maxCount;

        public SpawnerData(EntityTypes<?> type, int weight, int minGroupSize, int maxGroupSize) {
            this(type, Weight.of(weight), minGroupSize, maxGroupSize);
        }

        public SpawnerData(EntityTypes<?> type, Weight weight, int minGroupSize, int maxGroupSize) {
            super(weight);
            this.type = type.getCategory() == EnumCreatureType.MISC ? EntityTypes.PIG : type;
            this.minCount = minGroupSize;
            this.maxCount = maxGroupSize;
        }

        @Override
        public String toString() {
            return EntityTypes.getName(this.type) + "*(" + this.minCount + "-" + this.maxCount + "):" + this.getWeight();
        }
    }
}
