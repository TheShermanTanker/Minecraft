package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer6;

public enum GenLayerTopSoil implements AreaTransformer6 {
    INSTANCE;

    @Override
    public int apply(WorldGenContext context, int se) {
        if (GenLayers.isShallowOcean(se)) {
            return se;
        } else {
            int i = context.nextRandom(6);
            if (i == 0) {
                return 4;
            } else {
                return i == 1 ? 3 : 1;
            }
        }
    }
}
