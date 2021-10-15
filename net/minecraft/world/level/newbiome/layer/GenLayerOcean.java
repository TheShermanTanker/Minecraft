package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer3;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformerIdentity;

public enum GenLayerOcean implements AreaTransformer3, AreaTransformerIdentity {
    INSTANCE;

    @Override
    public int applyPixel(WorldGenContext context, Area sampler1, Area sampler2, int x, int z) {
        int i = sampler1.get(this.getParentX(x), this.getParentY(z));
        int j = sampler2.get(this.getParentX(x), this.getParentY(z));
        if (!GenLayers.isOcean(i)) {
            return i;
        } else {
            int k = 8;
            int l = 4;

            for(int m = -8; m <= 8; m += 4) {
                for(int n = -8; n <= 8; n += 4) {
                    int o = sampler1.get(this.getParentX(x + m), this.getParentY(z + n));
                    if (!GenLayers.isOcean(o)) {
                        if (j == 44) {
                            return 45;
                        }

                        if (j == 10) {
                            return 46;
                        }
                    }
                }
            }

            if (i == 24) {
                if (j == 45) {
                    return 48;
                }

                if (j == 0) {
                    return 24;
                }

                if (j == 46) {
                    return 49;
                }

                if (j == 10) {
                    return 50;
                }
            }

            return j;
        }
    }
}
