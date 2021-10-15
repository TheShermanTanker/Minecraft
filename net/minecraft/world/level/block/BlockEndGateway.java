package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityEndGateway;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.FluidType;

public class BlockEndGateway extends BlockTileEntity {
    protected BlockEndGateway(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityEndGateway(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return createTickerHelper(type, TileEntityTypes.END_GATEWAY, world.isClientSide ? TileEntityEndGateway::beamAnimationTick : TileEntityEndGateway::teleportTick);
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityEndGateway) {
            int i = ((TileEntityEndGateway)blockEntity).getParticleAmount();

            for(int j = 0; j < i; ++j) {
                double d = (double)pos.getX() + random.nextDouble();
                double e = (double)pos.getY() + random.nextDouble();
                double f = (double)pos.getZ() + random.nextDouble();
                double g = (random.nextDouble() - 0.5D) * 0.5D;
                double h = (random.nextDouble() - 0.5D) * 0.5D;
                double k = (random.nextDouble() - 0.5D) * 0.5D;
                int l = random.nextInt(2) * 2 - 1;
                if (random.nextBoolean()) {
                    f = (double)pos.getZ() + 0.5D + 0.25D * (double)l;
                    k = (double)(random.nextFloat() * 2.0F * (float)l);
                } else {
                    d = (double)pos.getX() + 0.5D + 0.25D * (double)l;
                    g = (double)(random.nextFloat() * 2.0F * (float)l);
                }

                world.addParticle(Particles.PORTAL, d, e, f, g, h, k);
            }

        }
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canBeReplaced(IBlockData state, FluidType fluid) {
        return false;
    }
}
