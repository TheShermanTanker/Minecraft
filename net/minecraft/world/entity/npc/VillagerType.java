package net.minecraft.world.entity.npc;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;

public final class VillagerType {
    public static final VillagerType DESERT = register("desert");
    public static final VillagerType JUNGLE = register("jungle");
    public static final VillagerType PLAINS = register("plains");
    public static final VillagerType SAVANNA = register("savanna");
    public static final VillagerType SNOW = register("snow");
    public static final VillagerType SWAMP = register("swamp");
    public static final VillagerType TAIGA = register("taiga");
    private final String name;
    private static final Map<ResourceKey<BiomeBase>, VillagerType> BY_BIOME = SystemUtils.make(Maps.newHashMap(), (map) -> {
        map.put(Biomes.BADLANDS, DESERT);
        map.put(Biomes.BADLANDS_PLATEAU, DESERT);
        map.put(Biomes.DESERT, DESERT);
        map.put(Biomes.DESERT_HILLS, DESERT);
        map.put(Biomes.DESERT_LAKES, DESERT);
        map.put(Biomes.ERODED_BADLANDS, DESERT);
        map.put(Biomes.MODIFIED_BADLANDS_PLATEAU, DESERT);
        map.put(Biomes.MODIFIED_WOODED_BADLANDS_PLATEAU, DESERT);
        map.put(Biomes.WOODED_BADLANDS_PLATEAU, DESERT);
        map.put(Biomes.BAMBOO_JUNGLE, JUNGLE);
        map.put(Biomes.BAMBOO_JUNGLE_HILLS, JUNGLE);
        map.put(Biomes.JUNGLE, JUNGLE);
        map.put(Biomes.JUNGLE_EDGE, JUNGLE);
        map.put(Biomes.JUNGLE_HILLS, JUNGLE);
        map.put(Biomes.MODIFIED_JUNGLE, JUNGLE);
        map.put(Biomes.MODIFIED_JUNGLE_EDGE, JUNGLE);
        map.put(Biomes.SAVANNA_PLATEAU, SAVANNA);
        map.put(Biomes.SAVANNA, SAVANNA);
        map.put(Biomes.SHATTERED_SAVANNA, SAVANNA);
        map.put(Biomes.SHATTERED_SAVANNA_PLATEAU, SAVANNA);
        map.put(Biomes.DEEP_FROZEN_OCEAN, SNOW);
        map.put(Biomes.FROZEN_OCEAN, SNOW);
        map.put(Biomes.FROZEN_RIVER, SNOW);
        map.put(Biomes.ICE_SPIKES, SNOW);
        map.put(Biomes.SNOWY_BEACH, SNOW);
        map.put(Biomes.SNOWY_MOUNTAINS, SNOW);
        map.put(Biomes.SNOWY_TAIGA, SNOW);
        map.put(Biomes.SNOWY_TAIGA_HILLS, SNOW);
        map.put(Biomes.SNOWY_TAIGA_MOUNTAINS, SNOW);
        map.put(Biomes.SNOWY_TUNDRA, SNOW);
        map.put(Biomes.SWAMP, SWAMP);
        map.put(Biomes.SWAMP_HILLS, SWAMP);
        map.put(Biomes.GIANT_SPRUCE_TAIGA, TAIGA);
        map.put(Biomes.GIANT_SPRUCE_TAIGA_HILLS, TAIGA);
        map.put(Biomes.GIANT_TREE_TAIGA, TAIGA);
        map.put(Biomes.GIANT_TREE_TAIGA_HILLS, TAIGA);
        map.put(Biomes.GRAVELLY_MOUNTAINS, TAIGA);
        map.put(Biomes.MODIFIED_GRAVELLY_MOUNTAINS, TAIGA);
        map.put(Biomes.MOUNTAIN_EDGE, TAIGA);
        map.put(Biomes.MOUNTAINS, TAIGA);
        map.put(Biomes.TAIGA, TAIGA);
        map.put(Biomes.TAIGA_HILLS, TAIGA);
        map.put(Biomes.TAIGA_MOUNTAINS, TAIGA);
        map.put(Biomes.WOODED_MOUNTAINS, TAIGA);
    });

    private VillagerType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    private static VillagerType register(String id) {
        return IRegistry.register(IRegistry.VILLAGER_TYPE, new MinecraftKey(id), new VillagerType(id));
    }

    public static VillagerType byBiome(Optional<ResourceKey<BiomeBase>> biomeKey) {
        return biomeKey.flatMap((resourceKey) -> {
            return Optional.ofNullable(BY_BIOME.get(resourceKey));
        }).orElse(PLAINS);
    }
}
