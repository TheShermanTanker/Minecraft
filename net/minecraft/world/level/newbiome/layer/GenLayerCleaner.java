package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer5;

public enum GenLayerCleaner implements AreaTransformer5 {
    INSTANCE;

    @Override
    public int apply(WorldGenContext context, int value) {
        return GenLayers.isShallowOcean(value) ? value : context.nextRandom(299999) + 2;
    }
}
