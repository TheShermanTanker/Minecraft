package net.minecraft.world.level.newbiome.layer.traits;

public interface AreaTransformerIdentity extends AreaTransformer {
    @Override
    default int getParentX(int x) {
        return x;
    }

    @Override
    default int getParentY(int y) {
        return y;
    }
}
