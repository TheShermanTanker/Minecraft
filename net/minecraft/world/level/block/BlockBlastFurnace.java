package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityBlastFurnace;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockBlastFurnace extends BlockFurnace {
    protected BlockBlastFurnace(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityBlastFurnace(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return createFurnaceTicker(world, type, TileEntityTypes.BLAST_FURNACE);
    }

    @Override
    protected void openContainer(World world, BlockPosition pos, EntityHuman player) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityBlastFurnace) {
            player.openContainer((ITileInventory)blockEntity);
            player.awardStat(StatisticList.INTERACT_WITH_BLAST_FURNACE);
        }

    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (state.get(LIT)) {
            double d = (double)pos.getX() + 0.5D;
            double e = (double)pos.getY();
            double f = (double)pos.getZ() + 0.5D;
            if (random.nextDouble() < 0.1D) {
                world.playLocalSound(d, e, f, SoundEffects.BLASTFURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
            }

            EnumDirection direction = state.get(FACING);
            EnumDirection.EnumAxis axis = direction.getAxis();
            double g = 0.52D;
            double h = random.nextDouble() * 0.6D - 0.3D;
            double i = axis == EnumDirection.EnumAxis.X ? (double)direction.getAdjacentX() * 0.52D : h;
            double j = random.nextDouble() * 9.0D / 16.0D;
            double k = axis == EnumDirection.EnumAxis.Z ? (double)direction.getAdjacentZ() * 0.52D : h;
            world.addParticle(Particles.SMOKE, d + i, e + j, f + k, 0.0D, 0.0D, 0.0D);
        }
    }
}
