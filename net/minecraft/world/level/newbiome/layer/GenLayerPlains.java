package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer6;

public enum GenLayerPlains implements AreaTransformer6 {
    INSTANCE;

    @Override
    public int apply(WorldGenContext context, int se) {
        return context.nextRandom(57) == 0 && se == 1 ? 129 : se;
    }
}
