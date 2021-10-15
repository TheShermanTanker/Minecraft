package net.minecraft.world.level.newbiome.layer.traits;

import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.area.AreaFactory;
import net.minecraft.world.level.newbiome.context.AreaContextTransformed;

public interface AreaTransformer2 extends AreaTransformer {
    default <R extends Area> AreaFactory<R> run(AreaContextTransformed<R> context, AreaFactory<R> parent) {
        return () -> {
            R area = parent.make();
            return context.createResult((x, z) -> {
                context.initRandom((long)x, (long)z);
                return this.applyPixel(context, area, x, z);
            }, area);
        };
    }

    int applyPixel(AreaContextTransformed<?> context, Area parent, int x, int z);
}
