package net.minecraft.world.level;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;

public interface IWorldHeightAccess {
    int getHeight();

    int getMinBuildHeight();

    default int getMaxBuildHeight() {
        return this.getMinBuildHeight() + this.getHeight();
    }

    default int getSectionsCount() {
        return this.getMaxSection() - this.getMinSection();
    }

    default int getMinSection() {
        return SectionPosition.blockToSectionCoord(this.getMinBuildHeight());
    }

    default int getMaxSection() {
        return SectionPosition.blockToSectionCoord(this.getMaxBuildHeight() - 1) + 1;
    }

    default boolean isOutsideWorld(BlockPosition pos) {
        return this.isOutsideBuildHeight(pos.getY());
    }

    default boolean isOutsideBuildHeight(int y) {
        return y < this.getMinBuildHeight() || y >= this.getMaxBuildHeight();
    }

    default int getSectionIndex(int y) {
        return this.getSectionIndexFromSectionY(SectionPosition.blockToSectionCoord(y));
    }

    default int getSectionIndexFromSectionY(int coord) {
        return coord - this.getMinSection();
    }

    default int getSectionYFromSectionIndex(int index) {
        return index + this.getMinSection();
    }

    static IWorldHeightAccess create(int bottomY, int height) {
        return new IWorldHeightAccess() {
            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public int getMinBuildHeight() {
                return bottomY;
            }
        };
    }
}
