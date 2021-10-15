package net.minecraft.world.level.newbiome.layer;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer7;

public enum GenLayerMushroomShore implements AreaTransformer7 {
    INSTANCE;

    private static final IntSet SNOWY = new IntOpenHashSet(new int[]{26, 11, 12, 13, 140, 30, 31, 158, 10});
    private static final IntSet JUNGLES = new IntOpenHashSet(new int[]{168, 169, 21, 22, 23, 149, 151});

    @Override
    public int apply(WorldGenContext context, int n, int e, int s, int w, int center) {
        if (center == 14) {
            if (GenLayers.isShallowOcean(n) || GenLayers.isShallowOcean(e) || GenLayers.isShallowOcean(s) || GenLayers.isShallowOcean(w)) {
                return 15;
            }
        } else if (JUNGLES.contains(center)) {
            if (!isJungleCompatible(n) || !isJungleCompatible(e) || !isJungleCompatible(s) || !isJungleCompatible(w)) {
                return 23;
            }

            if (GenLayers.isOcean(n) || GenLayers.isOcean(e) || GenLayers.isOcean(s) || GenLayers.isOcean(w)) {
                return 16;
            }
        } else if (center != 3 && center != 34 && center != 20) {
            if (SNOWY.contains(center)) {
                if (!GenLayers.isOcean(center) && (GenLayers.isOcean(n) || GenLayers.isOcean(e) || GenLayers.isOcean(s) || GenLayers.isOcean(w))) {
                    return 26;
                }
            } else if (center != 37 && center != 38) {
                if (!GenLayers.isOcean(center) && center != 7 && center != 6 && (GenLayers.isOcean(n) || GenLayers.isOcean(e) || GenLayers.isOcean(s) || GenLayers.isOcean(w))) {
                    return 16;
                }
            } else if (!GenLayers.isOcean(n) && !GenLayers.isOcean(e) && !GenLayers.isOcean(s) && !GenLayers.isOcean(w) && (!this.isMesa(n) || !this.isMesa(e) || !this.isMesa(s) || !this.isMesa(w))) {
                return 2;
            }
        } else if (!GenLayers.isOcean(center) && (GenLayers.isOcean(n) || GenLayers.isOcean(e) || GenLayers.isOcean(s) || GenLayers.isOcean(w))) {
            return 25;
        }

        return center;
    }

    private static boolean isJungleCompatible(int id) {
        return JUNGLES.contains(id) || id == 4 || id == 5 || GenLayers.isOcean(id);
    }

    private boolean isMesa(int id) {
        return id == 37 || id == 38 || id == 39 || id == 165 || id == 166 || id == 167;
    }
}
