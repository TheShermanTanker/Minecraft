package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer7;

public enum GenLayerIcePlains implements AreaTransformer7 {
    INSTANCE;

    @Override
    public int apply(WorldGenContext context, int n, int e, int s, int w, int center) {
        return GenLayers.isShallowOcean(center) && GenLayers.isShallowOcean(n) && GenLayers.isShallowOcean(e) && GenLayers.isShallowOcean(w) && GenLayers.isShallowOcean(s) && context.nextRandom(2) == 0 ? 1 : center;
    }
}
