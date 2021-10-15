package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer1;

public enum LayerIsland implements AreaTransformer1 {
    INSTANCE;

    @Override
    public int applyPixel(WorldGenContext context, int x, int y) {
        if (x == 0 && y == 0) {
            return 1;
        } else {
            return context.nextRandom(10) == 0 ? 1 : 0;
        }
    }
}
