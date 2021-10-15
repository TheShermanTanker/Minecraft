package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer6;

public enum GenLayerJungle implements AreaTransformer6 {
    INSTANCE;

    @Override
    public int apply(WorldGenContext context, int se) {
        return context.nextRandom(10) == 0 && se == 21 ? 168 : se;
    }
}
