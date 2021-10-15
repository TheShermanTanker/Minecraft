package net.minecraft.world.level.block.state.pattern;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;

public class ShapeDetectorBlock {
    private final IWorldReader level;
    private final BlockPosition pos;
    private final boolean loadChunks;
    private IBlockData state;
    private TileEntity entity;
    private boolean cachedEntity;

    public ShapeDetectorBlock(IWorldReader world, BlockPosition pos, boolean forceLoad) {
        this.level = world;
        this.pos = pos.immutableCopy();
        this.loadChunks = forceLoad;
    }

    public IBlockData getState() {
        if (this.state == null && (this.loadChunks || this.level.isLoaded(this.pos))) {
            this.state = this.level.getType(this.pos);
        }

        return this.state;
    }

    @Nullable
    public TileEntity getEntity() {
        if (this.entity == null && !this.cachedEntity) {
            this.entity = this.level.getTileEntity(this.pos);
            this.cachedEntity = true;
        }

        return this.entity;
    }

    public IWorldReader getLevel() {
        return this.level;
    }

    public BlockPosition getPosition() {
        return this.pos;
    }

    public static Predicate<ShapeDetectorBlock> hasState(Predicate<IBlockData> state) {
        return (blockInWorld) -> {
            return blockInWorld != null && state.test(blockInWorld.getState());
        };
    }
}
