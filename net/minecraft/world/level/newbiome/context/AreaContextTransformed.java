package net.minecraft.world.level.newbiome.context;

import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer8;

public interface AreaContextTransformed<R extends Area> extends WorldGenContext {
    void initRandom(long x, long y);

    R createResult(AreaTransformer8 operator);

    default R createResult(AreaTransformer8 operator, R parent) {
        return this.createResult(operator);
    }

    default R createResult(AreaTransformer8 operator, R firstParent, R secondParent) {
        return this.createResult(operator);
    }

    default int random(int a, int b) {
        return this.nextRandom(2) == 0 ? a : b;
    }

    default int random(int a, int b, int c, int d) {
        int i = this.nextRandom(4);
        if (i == 0) {
            return a;
        } else if (i == 1) {
            return b;
        } else {
            return i == 2 ? c : d;
        }
    }
}
