package net.minecraft.world.level.newbiome.layer;

import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.context.WorldGenContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer3;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformerIdentity;

public enum GenLayerRiverMix implements AreaTransformer3, AreaTransformerIdentity {
    INSTANCE;

    @Override
    public int applyPixel(WorldGenContext context, Area sampler1, Area sampler2, int x, int z) {
        int i = sampler1.get(this.getParentX(x), this.getParentY(z));
        int j = sampler2.get(this.getParentX(x), this.getParentY(z));
        if (GenLayers.isOcean(i)) {
            return i;
        } else if (j == 7) {
            if (i == 12) {
                return 11;
            } else {
                return i != 14 && i != 15 ? j & 255 : 15;
            }
        } else {
            return i;
        }
    }
}
