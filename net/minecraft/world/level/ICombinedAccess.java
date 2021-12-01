package net.minecraft.world.level;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface ICombinedAccess extends IEntityAccess, IWorldReader, VirtualWorldWritable {
    @Override
    default <T extends TileEntity> Optional<T> getBlockEntity(BlockPosition pos, TileEntityTypes<T> type) {
        return IWorldReader.super.getBlockEntity(pos, type);
    }

    @Override
    default List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AxisAlignedBB box) {
        return IEntityAccess.super.getEntityCollisions(entity, box);
    }

    @Override
    default boolean isUnobstructed(@Nullable Entity entity, VoxelShape shape) {
        return IEntityAccess.super.isUnobstructed(entity, shape);
    }

    @Override
    default BlockPosition getHighestBlockYAt(HeightMap.Type heightmap, BlockPosition pos) {
        return IWorldReader.super.getHighestBlockYAt(heightmap, pos);
    }

    IRegistryCustom registryAccess();

    default Optional<ResourceKey<BiomeBase>> getBiomeName(BlockPosition pos) {
        return this.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY).getResourceKey(this.getBiome(pos));
    }
}
