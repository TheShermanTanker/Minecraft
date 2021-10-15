package net.minecraft.world.level.newbiome.layer.traits;

import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.area.AreaFactory;
import net.minecraft.world.level.newbiome.context.AreaContextTransformed;
import net.minecraft.world.level.newbiome.context.WorldGenContext;

public interface AreaTransformer3 extends AreaTransformer {
    default <R extends Area> AreaFactory<R> run(AreaContextTransformed<R> context, AreaFactory<R> layer1, AreaFactory<R> layer2) {
        return () -> {
            R area = layer1.make();
            R area2 = layer2.make();
            return context.createResult((x, z) -> {
                context.initRandom((long)x, (long)z);
                return this.applyPixel(context, area, area2, x, z);
            }, area, area2);
        };
    }

    int applyPixel(WorldGenContext context, Area sampler1, Area sampler2, int x, int z);
}
