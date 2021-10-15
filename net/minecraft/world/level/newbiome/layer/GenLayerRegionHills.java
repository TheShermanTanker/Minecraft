package net.minecraft.world.level.newbiome.layer;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.SystemUtils;
import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer3;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformerOffset1;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum GenLayerRegionHills implements AreaTransformer3, AreaTransformerOffset1 {
    INSTANCE;

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Int2IntMap MUTATIONS = SystemUtils.make(new Int2IntOpenHashMap(), (map) -> {
        map.put(1, 129);
        map.put(2, 130);
        map.put(3, 131);
        map.put(4, 132);
        map.put(5, 133);
        map.put(6, 134);
        map.put(12, 140);
        map.put(21, 149);
        map.put(23, 151);
        map.put(27, 155);
        map.put(28, 156);
        map.put(29, 157);
        map.put(30, 158);
        map.put(32, 160);
        map.put(33, 161);
        map.put(34, 162);
        map.put(35, 163);
        map.put(36, 164);
        map.put(37, 165);
        map.put(38, 166);
        map.put(39, 167);
    });

    @Override
    public int applyPixel(WorldGenContext context, Area sampler1, Area sampler2, int x, int z) {
        int i = sampler1.get(this.getParentX(x + 1), this.getParentY(z + 1));
        int j = sampler2.get(this.getParentX(x + 1), this.getParentY(z + 1));
        if (i > 255) {
            LOGGER.debug("old! {}", (int)i);
        }

        int k = (j - 2) % 29;
        if (!GenLayers.isShallowOcean(i) && j >= 2 && k == 1) {
            return MUTATIONS.getOrDefault(i, i);
        } else {
            if (context.nextRandom(3) == 0 || k == 0) {
                int l = i;
                if (i == 2) {
                    l = 17;
                } else if (i == 4) {
                    l = 18;
                } else if (i == 27) {
                    l = 28;
                } else if (i == 29) {
                    l = 1;
                } else if (i == 5) {
                    l = 19;
                } else if (i == 32) {
                    l = 33;
                } else if (i == 30) {
                    l = 31;
                } else if (i == 1) {
                    l = context.nextRandom(3) == 0 ? 18 : 4;
                } else if (i == 12) {
                    l = 13;
                } else if (i == 21) {
                    l = 22;
                } else if (i == 168) {
                    l = 169;
                } else if (i == 0) {
                    l = 24;
                } else if (i == 45) {
                    l = 48;
                } else if (i == 46) {
                    l = 49;
                } else if (i == 10) {
                    l = 50;
                } else if (i == 3) {
                    l = 34;
                } else if (i == 35) {
                    l = 36;
                } else if (GenLayers.isSame(i, 38)) {
                    l = 37;
                } else if ((i == 24 || i == 48 || i == 49 || i == 50) && context.nextRandom(3) == 0) {
                    l = context.nextRandom(2) == 0 ? 1 : 4;
                }

                if (k == 0 && l != i) {
                    l = MUTATIONS.getOrDefault(l, i);
                }

                if (l != i) {
                    int m = 0;
                    if (GenLayers.isSame(sampler1.get(this.getParentX(x + 1), this.getParentY(z + 0)), i)) {
                        ++m;
                    }

                    if (GenLayers.isSame(sampler1.get(this.getParentX(x + 2), this.getParentY(z + 1)), i)) {
                        ++m;
                    }

                    if (GenLayers.isSame(sampler1.get(this.getParentX(x + 0), this.getParentY(z + 1)), i)) {
                        ++m;
                    }

                    if (GenLayers.isSame(sampler1.get(this.getParentX(x + 1), this.getParentY(z + 2)), i)) {
                        ++m;
                    }

                    if (m >= 3) {
                        return l;
                    }
                }
            }

            return i;
        }
    }
}
