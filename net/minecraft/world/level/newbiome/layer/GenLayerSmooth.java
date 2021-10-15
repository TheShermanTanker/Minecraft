package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer7;

public enum GenLayerSmooth implements AreaTransformer7 {
    INSTANCE;

    @Override
    public int apply(WorldGenContext context, int n, int e, int s, int w, int center) {
        boolean bl = e == w;
        boolean bl2 = n == s;
        if (bl == bl2) {
            if (bl) {
                return context.nextRandom(2) == 0 ? w : n;
            } else {
                return center;
            }
        } else {
            return bl ? w : n;
        }
    }
}
