package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer7;

public enum GenLayerRiver implements AreaTransformer7 {
    INSTANCE;

    @Override
    public int apply(WorldGenContext context, int n, int e, int s, int w, int center) {
        int i = riverFilter(center);
        return i == riverFilter(w) && i == riverFilter(n) && i == riverFilter(e) && i == riverFilter(s) ? -1 : 7;
    }

    private static int riverFilter(int value) {
        return value >= 2 ? 2 + (value & 1) : value;
    }
}
