package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer4;

public enum GenLayerMushroomIsland implements AreaTransformer4 {
    INSTANCE;

    @Override
    public int apply(WorldGenContext context, int sw, int se, int ne, int nw, int center) {
        return GenLayers.isShallowOcean(center) && GenLayers.isShallowOcean(nw) && GenLayers.isShallowOcean(sw) && GenLayers.isShallowOcean(ne) && GenLayers.isShallowOcean(se) && context.nextRandom(100) == 0 ? 14 : center;
    }
}
