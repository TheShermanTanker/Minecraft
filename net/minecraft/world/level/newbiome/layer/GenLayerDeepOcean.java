package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer7;

public enum GenLayerDeepOcean implements AreaTransformer7 {
    INSTANCE;

    @Override
    public int apply(WorldGenContext context, int n, int e, int s, int w, int center) {
        if (GenLayers.isShallowOcean(center)) {
            int i = 0;
            if (GenLayers.isShallowOcean(n)) {
                ++i;
            }

            if (GenLayers.isShallowOcean(e)) {
                ++i;
            }

            if (GenLayers.isShallowOcean(w)) {
                ++i;
            }

            if (GenLayers.isShallowOcean(s)) {
                ++i;
            }

            if (i > 3) {
                if (center == 44) {
                    return 47;
                }

                if (center == 45) {
                    return 48;
                }

                if (center == 0) {
                    return 24;
                }

                if (center == 46) {
                    return 49;
                }

                if (center == 10) {
                    return 50;
                }

                return 24;
            }
        }

        return center;
    }
}
