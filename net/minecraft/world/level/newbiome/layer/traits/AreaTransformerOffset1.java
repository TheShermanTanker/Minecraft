package net.minecraft.world.level.newbiome.layer.traits;

public interface AreaTransformerOffset1 extends AreaTransformer {
    @Override
    default int getParentX(int x) {
        return x - 1;
    }

    @Override
    default int getParentY(int y) {
        return y - 1;
    }
}
