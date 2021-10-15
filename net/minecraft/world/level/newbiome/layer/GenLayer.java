package net.minecraft.world.level.newbiome.layer;

import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.data.worldgen.biome.BiomeRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.newbiome.area.AreaFactory;
import net.minecraft.world.level.newbiome.area.AreaLazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GenLayer {
    private static final Logger LOGGER = LogManager.getLogger();
    private final AreaLazy area;

    public GenLayer(AreaFactory<AreaLazy> layerFactory) {
        this.area = layerFactory.make();
    }

    public BiomeBase get(IRegistry<BiomeBase> biomeRegistry, int x, int z) {
        int i = this.area.get(x, z);
        ResourceKey<BiomeBase> resourceKey = BiomeRegistry.byId(i);
        if (resourceKey == null) {
            throw new IllegalStateException("Unknown biome id emitted by layers: " + i);
        } else {
            BiomeBase biome = biomeRegistry.get(resourceKey);
            if (biome == null) {
                SystemUtils.logAndPauseIfInIde("Unknown biome id: " + i);
                return biomeRegistry.get(BiomeRegistry.byId(0));
            } else {
                return biome;
            }
        }
    }
}
