package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer4;

public enum GenLayerIsland implements AreaTransformer4 {
    INSTANCE;

    @Override
    public int apply(WorldGenContext context, int sw, int se, int ne, int nw, int center) {
        if (!GenLayers.isShallowOcean(center) || GenLayers.isShallowOcean(nw) && GenLayers.isShallowOcean(ne) && GenLayers.isShallowOcean(sw) && GenLayers.isShallowOcean(se)) {
            if (!GenLayers.isShallowOcean(center) && (GenLayers.isShallowOcean(nw) || GenLayers.isShallowOcean(sw) || GenLayers.isShallowOcean(ne) || GenLayers.isShallowOcean(se)) && context.nextRandom(5) == 0) {
                if (GenLayers.isShallowOcean(nw)) {
                    return center == 4 ? 4 : nw;
                }

                if (GenLayers.isShallowOcean(sw)) {
                    return center == 4 ? 4 : sw;
                }

                if (GenLayers.isShallowOcean(ne)) {
                    return center == 4 ? 4 : ne;
                }

                if (GenLayers.isShallowOcean(se)) {
                    return center == 4 ? 4 : se;
                }
            }

            return center;
        } else {
            int i = 1;
            int j = 1;
            if (!GenLayers.isShallowOcean(nw) && context.nextRandom(i++) == 0) {
                j = nw;
            }

            if (!GenLayers.isShallowOcean(ne) && context.nextRandom(i++) == 0) {
                j = ne;
            }

            if (!GenLayers.isShallowOcean(sw) && context.nextRandom(i++) == 0) {
                j = sw;
            }

            if (!GenLayers.isShallowOcean(se) && context.nextRandom(i++) == 0) {
                j = se;
            }

            if (context.nextRandom(3) == 0) {
                return j;
            } else {
                return j == 4 ? 4 : center;
            }
        }
    }
}
