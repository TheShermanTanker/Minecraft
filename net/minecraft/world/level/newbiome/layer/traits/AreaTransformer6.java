package net.minecraft.world.level.newbiome.layer.traits;

import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.context.AreaContextTransformed;
import net.minecraft.world.level.newbiome.context.WorldGenContext;

public interface AreaTransformer6 extends AreaTransformer2, AreaTransformerOffset1 {
    int apply(WorldGenContext context, int se);

    @Override
    default int applyPixel(AreaContextTransformed<?> context, Area parent, int x, int z) {
        int i = parent.get(this.getParentX(x + 1), this.getParentY(z + 1));
        return this.apply(context, i);
    }
}
