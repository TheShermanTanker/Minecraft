package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer5;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer7;

public class GenLayerSpecial {
    public static enum Special1 implements AreaTransformer7 {
        INSTANCE;

        @Override
        public int apply(WorldGenContext context, int n, int e, int s, int w, int center) {
            return center != 1 || n != 3 && e != 3 && w != 3 && s != 3 && n != 4 && e != 4 && w != 4 && s != 4 ? center : 2;
        }
    }

    public static enum Special2 implements AreaTransformer7 {
        INSTANCE;

        @Override
        public int apply(WorldGenContext context, int n, int e, int s, int w, int center) {
            return center != 4 || n != 1 && e != 1 && w != 1 && s != 1 && n != 2 && e != 2 && w != 2 && s != 2 ? center : 3;
        }
    }

    public static enum Special3 implements AreaTransformer5 {
        INSTANCE;

        @Override
        public int apply(WorldGenContext context, int value) {
            if (!GenLayers.isShallowOcean(value) && context.nextRandom(13) == 0) {
                value |= 1 + context.nextRandom(15) << 8 & 3840;
            }

            return value;
        }
    }
}
