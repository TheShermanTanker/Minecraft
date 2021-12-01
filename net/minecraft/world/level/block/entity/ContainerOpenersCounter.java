package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;

public abstract class ContainerOpenersCounter {
    private static final int CHECK_TICK_DELAY = 5;
    private int openCount;

    protected abstract void onOpen(World world, BlockPosition pos, IBlockData state);

    protected abstract void onClose(World world, BlockPosition pos, IBlockData state);

    protected abstract void openerCountChanged(World world, BlockPosition pos, IBlockData state, int oldViewerCount, int newViewerCount);

    protected abstract boolean isOwnContainer(EntityHuman player);

    public void incrementOpeners(EntityHuman player, World world, BlockPosition pos, IBlockData state) {
        int i = this.openCount++;
        if (i == 0) {
            this.onOpen(world, pos, state);
            world.gameEvent(player, GameEvent.CONTAINER_OPEN, pos);
            scheduleRecheck(world, pos, state);
        }

        this.openerCountChanged(world, pos, state, i, this.openCount);
    }

    public void decrementOpeners(EntityHuman player, World world, BlockPosition pos, IBlockData state) {
        int i = this.openCount--;
        if (this.openCount == 0) {
            this.onClose(world, pos, state);
            world.gameEvent(player, GameEvent.CONTAINER_CLOSE, pos);
        }

        this.openerCountChanged(world, pos, state, i, this.openCount);
    }

    private int getOpenCount(World world, BlockPosition pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        float f = 5.0F;
        AxisAlignedBB aABB = new AxisAlignedBB((double)((float)i - 5.0F), (double)((float)j - 5.0F), (double)((float)k - 5.0F), (double)((float)(i + 1) + 5.0F), (double)((float)(j + 1) + 5.0F), (double)((float)(k + 1) + 5.0F));
        return world.getEntities(EntityTypeTest.forClass(EntityHuman.class), aABB, this::isOwnContainer).size();
    }

    public void recheckOpeners(World world, BlockPosition pos, IBlockData state) {
        int i = this.getOpenCount(world, pos);
        int j = this.openCount;
        if (j != i) {
            boolean bl = i != 0;
            boolean bl2 = j != 0;
            if (bl && !bl2) {
                this.onOpen(world, pos, state);
                world.gameEvent((Entity)null, GameEvent.CONTAINER_OPEN, pos);
            } else if (!bl) {
                this.onClose(world, pos, state);
                world.gameEvent((Entity)null, GameEvent.CONTAINER_CLOSE, pos);
            }

            this.openCount = i;
        }

        this.openerCountChanged(world, pos, state, j, i);
        if (i > 0) {
            scheduleRecheck(world, pos, state);
        }

    }

    public int getOpenerCount() {
        return this.openCount;
    }

    private static void scheduleRecheck(World world, BlockPosition pos, IBlockData state) {
        world.scheduleTick(pos, state.getBlock(), 5);
    }
}
