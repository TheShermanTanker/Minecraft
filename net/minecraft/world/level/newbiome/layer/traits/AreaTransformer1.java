package net.minecraft.world.level.newbiome.layer.traits;

import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.area.AreaFactory;
import net.minecraft.world.level.newbiome.context.AreaContextTransformed;
import net.minecraft.world.level.newbiome.context.WorldGenContext;

public interface AreaTransformer1 {
    default <R extends Area> AreaFactory<R> run(AreaContextTransformed<R> context) {
        return () -> {
            return context.createResult((x, z) -> {
                context.initRandom((long)x, (long)z);
                return this.applyPixel(context, x, z);
            });
        };
    }

    int applyPixel(WorldGenContext context, int x, int y);
}
