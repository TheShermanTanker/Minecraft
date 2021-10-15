package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityEnderChest extends TileEntity implements LidBlockEntity {
    private final ChestLidController chestLidController = new ChestLidController();
    public final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(World world, BlockPosition pos, IBlockData state) {
            world.playSound((EntityHuman)null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEffects.ENDER_CHEST_OPEN, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        }

        @Override
        protected void onClose(World world, BlockPosition pos, IBlockData state) {
            world.playSound((EntityHuman)null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEffects.ENDER_CHEST_CLOSE, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        }

        @Override
        protected void openerCountChanged(World world, BlockPosition pos, IBlockData state, int oldViewerCount, int newViewerCount) {
            world.playBlockAction(TileEntityEnderChest.this.worldPosition, Blocks.ENDER_CHEST, 1, newViewerCount);
        }

        @Override
        protected boolean isOwnContainer(EntityHuman player) {
            return player.getEnderChest().isActiveChest(TileEntityEnderChest.this);
        }
    };

    public TileEntityEnderChest(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.ENDER_CHEST, pos, state);
    }

    public static void lidAnimateTick(World world, BlockPosition pos, IBlockData state, TileEntityEnderChest blockEntity) {
        blockEntity.chestLidController.tickLid();
    }

    @Override
    public boolean setProperty(int type, int data) {
        if (type == 1) {
            this.chestLidController.shouldBeOpen(data > 0);
            return true;
        } else {
            return super.setProperty(type, data);
        }
    }

    public void startOpen(EntityHuman player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.incrementOpeners(player, this.getWorld(), this.getPosition(), this.getBlock());
        }

    }

    public void stopOpen(EntityHuman player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.decrementOpeners(player, this.getWorld(), this.getPosition(), this.getBlock());
        }

    }

    public boolean stillValid(EntityHuman player) {
        if (this.level.getTileEntity(this.worldPosition) != this) {
            return false;
        } else {
            return !(player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) > 64.0D);
        }
    }

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getWorld(), this.getPosition(), this.getBlock());
        }

    }

    @Override
    public float getOpenNess(float tickDelta) {
        return this.chestLidController.getOpenness(tickDelta);
    }
}
