package net.minecraft.world.level.newbiome.layer.traits;

import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.context.AreaContextTransformed;
import net.minecraft.world.level.newbiome.context.WorldGenContext;

public interface AreaTransformer5 extends AreaTransformer2, AreaTransformerIdentity {
    int apply(WorldGenContext context, int value);

    @Override
    default int applyPixel(AreaContextTransformed<?> context, Area parent, int x, int z) {
        return this.apply(context, parent.get(this.getParentX(x), this.getParentY(z)));
    }
}
