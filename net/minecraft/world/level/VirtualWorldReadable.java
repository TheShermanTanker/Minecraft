package net.minecraft.world.level;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.material.Fluid;

public interface VirtualWorldReadable {
    boolean isStateAtPosition(BlockPosition pos, Predicate<IBlockData> state);

    boolean isFluidAtPosition(BlockPosition pos, Predicate<Fluid> state);

    <T extends TileEntity> Optional<T> getBlockEntity(BlockPosition pos, TileEntityTypes<T> type);

    BlockPosition getHighestBlockYAt(HeightMap.Type heightmap, BlockPosition pos);
}
