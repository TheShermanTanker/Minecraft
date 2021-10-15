package net.minecraft.world.level.newbiome.layer.traits;

import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.context.AreaContextTransformed;
import net.minecraft.world.level.newbiome.context.WorldGenContext;

public interface AreaTransformer4 extends AreaTransformer2, AreaTransformerOffset1 {
    int apply(WorldGenContext context, int sw, int se, int ne, int nw, int center);

    @Override
    default int applyPixel(AreaContextTransformed<?> context, Area parent, int x, int z) {
        return this.apply(context, parent.get(this.getParentX(x + 0), this.getParentY(z + 2)), parent.get(this.getParentX(x + 2), this.getParentY(z + 2)), parent.get(this.getParentX(x + 2), this.getParentY(z + 0)), parent.get(this.getParentX(x + 0), this.getParentY(z + 0)), parent.get(this.getParentX(x + 1), this.getParentY(z + 1)));
    }
}
