package net.minecraft.world.level;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.IBlockData;

public interface IWorldWriter {
    boolean setBlock(BlockPosition pos, IBlockData state, int flags, int maxUpdateDepth);

    default boolean setTypeAndData(BlockPosition pos, IBlockData state, int flags) {
        return this.setBlock(pos, state, flags, 512);
    }

    boolean removeBlock(BlockPosition pos, boolean move);

    default boolean destroyBlock(BlockPosition pos, boolean drop) {
        return this.destroyBlock(pos, drop, (Entity)null);
    }

    default boolean destroyBlock(BlockPosition pos, boolean drop, @Nullable Entity breakingEntity) {
        return this.destroyBlock(pos, drop, breakingEntity, 512);
    }

    boolean destroyBlock(BlockPosition pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth);

    default boolean addEntity(Entity entity) {
        return false;
    }
}
