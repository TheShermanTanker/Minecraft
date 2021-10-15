package net.minecraft.world.level.newbiome.layer.traits;

import net.minecraft.world.level.newbiome.layer.LayerBiomes;

public interface AreaTransformer extends LayerBiomes {
    int getParentX(int x);

    int getParentY(int y);
}
