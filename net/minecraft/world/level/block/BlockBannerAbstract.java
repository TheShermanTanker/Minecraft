package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityBanner;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public abstract class BlockBannerAbstract extends BlockTileEntity {
    private final EnumColor color;

    protected BlockBannerAbstract(EnumColor color, BlockBase.Info settings) {
        super(settings);
        this.color = color;
    }

    @Override
    public boolean isPossibleToRespawnInThis() {
        return true;
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityBanner(pos, state, this.color);
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, @Nullable EntityLiving placer, ItemStack itemStack) {
        if (world.isClientSide) {
            world.getBlockEntity(pos, TileEntityTypes.BANNER).ifPresent((blockEntity) -> {
                blockEntity.fromItem(itemStack);
            });
        } else if (itemStack.hasName()) {
            world.getBlockEntity(pos, TileEntityTypes.BANNER).ifPresent((blockEntity) -> {
                blockEntity.setCustomName(itemStack.getName());
            });
        }

    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        TileEntity blockEntity = world.getTileEntity(pos);
        return blockEntity instanceof TileEntityBanner ? ((TileEntityBanner)blockEntity).getItem() : super.getCloneItemStack(world, pos, state);
    }

    public EnumColor getColor() {
        return this.color;
    }
}
