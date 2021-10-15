package net.minecraft.world.level;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.lighting.LightEngine;

public interface IBlockLightAccess extends IBlockAccess {
    float getShade(EnumDirection direction, boolean shaded);

    LightEngine getLightEngine();

    int getBlockTint(BlockPosition pos, ColorResolver colorResolver);

    default int getBrightness(EnumSkyBlock type, BlockPosition pos) {
        return this.getLightEngine().getLayerListener(type).getLightValue(pos);
    }

    default int getLightLevel(BlockPosition pos, int ambientDarkness) {
        return this.getLightEngine().getRawBrightness(pos, ambientDarkness);
    }

    default boolean canSeeSky(BlockPosition pos) {
        return this.getBrightness(EnumSkyBlock.SKY, pos) >= this.getMaxLightLevel();
    }
}
