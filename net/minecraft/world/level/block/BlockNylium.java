package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.data.worldgen.features.NetherFeatures;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.lighting.LightEngineLayer;

public class BlockNylium extends Block implements IBlockFragilePlantElement {
    protected BlockNylium(BlockBase.Info settings) {
        super(settings);
    }

    private static boolean canBeNylium(IBlockData state, IWorldReader world, BlockPosition pos) {
        BlockPosition blockPos = pos.above();
        IBlockData blockState = world.getType(blockPos);
        int i = LightEngineLayer.getLightBlockInto(world, state, pos, blockState, blockPos, EnumDirection.UP, blockState.getLightBlock(world, blockPos));
        return i < world.getMaxLightLevel();
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (!canBeNylium(state, world, pos)) {
            world.setTypeUpdate(pos, Blocks.NETHERRACK.getBlockData());
        }

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
        IBlockData blockState = world.getType(pos);
        BlockPosition blockPos = pos.above();
        ChunkGenerator chunkGenerator = world.getChunkSource().getChunkGenerator();
        if (blockState.is(Blocks.CRIMSON_NYLIUM)) {
            NetherFeatures.CRIMSON_FOREST_VEGETATION_BONEMEAL.place(world, chunkGenerator, random, blockPos);
        } else if (blockState.is(Blocks.WARPED_NYLIUM)) {
            NetherFeatures.WARPED_FOREST_VEGETATION_BONEMEAL.place(world, chunkGenerator, random, blockPos);
            NetherFeatures.NETHER_SPROUTS_BONEMEAL.place(world, chunkGenerator, random, blockPos);
            if (random.nextInt(8) == 0) {
                NetherFeatures.TWISTING_VINES_BONEMEAL.place(world, chunkGenerator, random, blockPos);
            }
        }

    }
}
