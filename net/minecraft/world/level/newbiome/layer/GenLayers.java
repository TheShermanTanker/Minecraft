package net.minecraft.world.level.newbiome.layer;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.function.LongFunction;
import net.minecraft.SystemUtils;
import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.area.AreaFactory;
import net.minecraft.world.level.newbiome.area.AreaLazy;
import net.minecraft.world.level.newbiome.context.AreaContextTransformed;
import net.minecraft.world.level.newbiome.context.WorldGenContextArea;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer2;

public class GenLayers implements LayerBiomes {
    protected static final int WARM_ID = 1;
    protected static final int MEDIUM_ID = 2;
    protected static final int COLD_ID = 3;
    protected static final int ICE_ID = 4;
    protected static final int SPECIAL_MASK = 3840;
    protected static final int SPECIAL_SHIFT = 8;
    private static final Int2IntMap CATEGORIES = SystemUtils.make(new Int2IntOpenHashMap(), (map) -> {
        register(map, GenLayers.Type.BEACH, 16);
        register(map, GenLayers.Type.BEACH, 26);
        register(map, GenLayers.Type.DESERT, 2);
        register(map, GenLayers.Type.DESERT, 17);
        register(map, GenLayers.Type.DESERT, 130);
        register(map, GenLayers.Type.EXTREME_HILLS, 131);
        register(map, GenLayers.Type.EXTREME_HILLS, 162);
        register(map, GenLayers.Type.EXTREME_HILLS, 20);
        register(map, GenLayers.Type.EXTREME_HILLS, 3);
        register(map, GenLayers.Type.EXTREME_HILLS, 34);
        register(map, GenLayers.Type.FOREST, 27);
        register(map, GenLayers.Type.FOREST, 28);
        register(map, GenLayers.Type.FOREST, 29);
        register(map, GenLayers.Type.FOREST, 157);
        register(map, GenLayers.Type.FOREST, 132);
        register(map, GenLayers.Type.FOREST, 4);
        register(map, GenLayers.Type.FOREST, 155);
        register(map, GenLayers.Type.FOREST, 156);
        register(map, GenLayers.Type.FOREST, 18);
        register(map, GenLayers.Type.ICY, 140);
        register(map, GenLayers.Type.ICY, 13);
        register(map, GenLayers.Type.ICY, 12);
        register(map, GenLayers.Type.JUNGLE, 168);
        register(map, GenLayers.Type.JUNGLE, 169);
        register(map, GenLayers.Type.JUNGLE, 21);
        register(map, GenLayers.Type.JUNGLE, 23);
        register(map, GenLayers.Type.JUNGLE, 22);
        register(map, GenLayers.Type.JUNGLE, 149);
        register(map, GenLayers.Type.JUNGLE, 151);
        register(map, GenLayers.Type.MESA, 37);
        register(map, GenLayers.Type.MESA, 165);
        register(map, GenLayers.Type.MESA, 167);
        register(map, GenLayers.Type.MESA, 166);
        register(map, GenLayers.Type.BADLANDS_PLATEAU, 39);
        register(map, GenLayers.Type.BADLANDS_PLATEAU, 38);
        register(map, GenLayers.Type.MUSHROOM, 14);
        register(map, GenLayers.Type.MUSHROOM, 15);
        register(map, GenLayers.Type.NONE, 25);
        register(map, GenLayers.Type.OCEAN, 46);
        register(map, GenLayers.Type.OCEAN, 49);
        register(map, GenLayers.Type.OCEAN, 50);
        register(map, GenLayers.Type.OCEAN, 48);
        register(map, GenLayers.Type.OCEAN, 24);
        register(map, GenLayers.Type.OCEAN, 47);
        register(map, GenLayers.Type.OCEAN, 10);
        register(map, GenLayers.Type.OCEAN, 45);
        register(map, GenLayers.Type.OCEAN, 0);
        register(map, GenLayers.Type.OCEAN, 44);
        register(map, GenLayers.Type.PLAINS, 1);
        register(map, GenLayers.Type.PLAINS, 129);
        register(map, GenLayers.Type.RIVER, 11);
        register(map, GenLayers.Type.RIVER, 7);
        register(map, GenLayers.Type.SAVANNA, 35);
        register(map, GenLayers.Type.SAVANNA, 36);
        register(map, GenLayers.Type.SAVANNA, 163);
        register(map, GenLayers.Type.SAVANNA, 164);
        register(map, GenLayers.Type.SWAMP, 6);
        register(map, GenLayers.Type.SWAMP, 134);
        register(map, GenLayers.Type.TAIGA, 160);
        register(map, GenLayers.Type.TAIGA, 161);
        register(map, GenLayers.Type.TAIGA, 32);
        register(map, GenLayers.Type.TAIGA, 33);
        register(map, GenLayers.Type.TAIGA, 30);
        register(map, GenLayers.Type.TAIGA, 31);
        register(map, GenLayers.Type.TAIGA, 158);
        register(map, GenLayers.Type.TAIGA, 5);
        register(map, GenLayers.Type.TAIGA, 19);
        register(map, GenLayers.Type.TAIGA, 133);
    });

    private static <T extends Area, C extends AreaContextTransformed<T>> AreaFactory<T> zoom(long seed, AreaTransformer2 layer, AreaFactory<T> parent, int count, LongFunction<C> contextProvider) {
        AreaFactory<T> areaFactory = parent;

        for(int i = 0; i < count; ++i) {
            areaFactory = layer.run(contextProvider.apply(seed + (long)i), areaFactory);
        }

        return areaFactory;
    }

    private static <T extends Area, C extends AreaContextTransformed<T>> AreaFactory<T> getDefaultLayer(boolean old, int biomeSize, int riverSize, LongFunction<C> contextProvider) {
        AreaFactory<T> areaFactory = LayerIsland.INSTANCE.run(contextProvider.apply(1L));
        areaFactory = GenLayerZoom.FUZZY.run(contextProvider.apply(2000L), areaFactory);
        areaFactory = GenLayerIsland.INSTANCE.run(contextProvider.apply(1L), areaFactory);
        areaFactory = GenLayerZoom.NORMAL.run(contextProvider.apply(2001L), areaFactory);
        areaFactory = GenLayerIsland.INSTANCE.run(contextProvider.apply(2L), areaFactory);
        areaFactory = GenLayerIsland.INSTANCE.run(contextProvider.apply(50L), areaFactory);
        areaFactory = GenLayerIsland.INSTANCE.run(contextProvider.apply(70L), areaFactory);
        areaFactory = GenLayerIcePlains.INSTANCE.run(contextProvider.apply(2L), areaFactory);
        AreaFactory<T> areaFactory2 = GenLayerOceanEdge.INSTANCE.run(contextProvider.apply(2L));
        areaFactory2 = zoom(2001L, GenLayerZoom.NORMAL, areaFactory2, 6, contextProvider);
        areaFactory = GenLayerTopSoil.INSTANCE.run(contextProvider.apply(2L), areaFactory);
        areaFactory = GenLayerIsland.INSTANCE.run(contextProvider.apply(3L), areaFactory);
        areaFactory = GenLayerSpecial.Special1.INSTANCE.run(contextProvider.apply(2L), areaFactory);
        areaFactory = GenLayerSpecial.Special2.INSTANCE.run(contextProvider.apply(2L), areaFactory);
        areaFactory = GenLayerSpecial.Special3.INSTANCE.run(contextProvider.apply(3L), areaFactory);
        areaFactory = GenLayerZoom.NORMAL.run(contextProvider.apply(2002L), areaFactory);
        areaFactory = GenLayerZoom.NORMAL.run(contextProvider.apply(2003L), areaFactory);
        areaFactory = GenLayerIsland.INSTANCE.run(contextProvider.apply(4L), areaFactory);
        areaFactory = GenLayerMushroomIsland.INSTANCE.run(contextProvider.apply(5L), areaFactory);
        areaFactory = GenLayerDeepOcean.INSTANCE.run(contextProvider.apply(4L), areaFactory);
        areaFactory = zoom(1000L, GenLayerZoom.NORMAL, areaFactory, 0, contextProvider);
        AreaFactory<T> areaFactory3 = zoom(1000L, GenLayerZoom.NORMAL, areaFactory, 0, contextProvider);
        areaFactory3 = GenLayerCleaner.INSTANCE.run(contextProvider.apply(100L), areaFactory3);
        AreaFactory<T> areaFactory4 = (new GenLayerBiome(old)).run(contextProvider.apply(200L), areaFactory);
        areaFactory4 = GenLayerJungle.INSTANCE.run(contextProvider.apply(1001L), areaFactory4);
        areaFactory4 = zoom(1000L, GenLayerZoom.NORMAL, areaFactory4, 2, contextProvider);
        areaFactory4 = GenLayerDesert.INSTANCE.run(contextProvider.apply(1000L), areaFactory4);
        AreaFactory<T> areaFactory5 = zoom(1000L, GenLayerZoom.NORMAL, areaFactory3, 2, contextProvider);
        areaFactory4 = GenLayerRegionHills.INSTANCE.run(contextProvider.apply(1000L), areaFactory4, areaFactory5);
        areaFactory3 = zoom(1000L, GenLayerZoom.NORMAL, areaFactory3, 2, contextProvider);
        areaFactory3 = zoom(1000L, GenLayerZoom.NORMAL, areaFactory3, riverSize, contextProvider);
        areaFactory3 = GenLayerRiver.INSTANCE.run(contextProvider.apply(1L), areaFactory3);
        areaFactory3 = GenLayerSmooth.INSTANCE.run(contextProvider.apply(1000L), areaFactory3);
        areaFactory4 = GenLayerPlains.INSTANCE.run(contextProvider.apply(1001L), areaFactory4);

        for(int i = 0; i < biomeSize; ++i) {
            areaFactory4 = GenLayerZoom.NORMAL.run(contextProvider.apply((long)(1000 + i)), areaFactory4);
            if (i == 0) {
                areaFactory4 = GenLayerIsland.INSTANCE.run(contextProvider.apply(3L), areaFactory4);
            }

            if (i == 1 || biomeSize == 1) {
                areaFactory4 = GenLayerMushroomShore.INSTANCE.run(contextProvider.apply(1000L), areaFactory4);
            }
        }

        areaFactory4 = GenLayerSmooth.INSTANCE.run(contextProvider.apply(1000L), areaFactory4);
        areaFactory4 = GenLayerRiverMix.INSTANCE.run(contextProvider.apply(100L), areaFactory4, areaFactory3);
        return GenLayerOcean.INSTANCE.run(contextProvider.apply(100L), areaFactory4, areaFactory2);
    }

    public static GenLayer getDefaultLayer(long seed, boolean old, int biomeSize, int riverSize) {
        int i = 25;
        AreaFactory<AreaLazy> areaFactory = getDefaultLayer(old, biomeSize, riverSize, (salt) -> {
            return new WorldGenContextArea(25, seed, salt);
        });
        return new GenLayer(areaFactory);
    }

    public static boolean isSame(int id1, int id2) {
        if (id1 == id2) {
            return true;
        } else {
            return CATEGORIES.get(id1) == CATEGORIES.get(id2);
        }
    }

    private static void register(Int2IntOpenHashMap map, GenLayers.Type category, int id) {
        map.put(id, category.ordinal());
    }

    protected static boolean isOcean(int id) {
        return id == 44 || id == 45 || id == 0 || id == 46 || id == 10 || id == 47 || id == 48 || id == 24 || id == 49 || id == 50;
    }

    protected static boolean isShallowOcean(int id) {
        return id == 44 || id == 45 || id == 0 || id == 46 || id == 10;
    }

    static enum Type {
        NONE,
        TAIGA,
        EXTREME_HILLS,
        JUNGLE,
        MESA,
        BADLANDS_PLATEAU,
        PLAINS,
        SAVANNA,
        ICY,
        BEACH,
        FOREST,
        OCEAN,
        DESERT,
        RIVER,
        SWAMP,
        MUSHROOM;
    }
}
