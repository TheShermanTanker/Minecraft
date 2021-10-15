package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.data.worldgen.BiomeDecoratorGroups;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;

public class BlockMoss extends Block implements IBlockFragilePlantElement {
    public BlockMoss(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return world.getType(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        WorldGenerator.VEGETATION_PATCH.generate(new FeaturePlaceContext<>(world, world.getChunkSource().getChunkGenerator(), random, pos.above(), BiomeDecoratorGroups.MOSS_PATCH_BONEMEAL.config()));
    }
}
