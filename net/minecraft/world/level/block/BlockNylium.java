package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.data.worldgen.BiomeDecoratorGroups;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureNetherForestVegetation;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureTwistingVines;
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
        if (blockState.is(Blocks.CRIMSON_NYLIUM)) {
            WorldGenFeatureNetherForestVegetation.place(world, random, blockPos, BiomeDecoratorGroups.Configs.CRIMSON_FOREST_CONFIG, 3, 1);
        } else if (blockState.is(Blocks.WARPED_NYLIUM)) {
            WorldGenFeatureNetherForestVegetation.place(world, random, blockPos, BiomeDecoratorGroups.Configs.WARPED_FOREST_CONFIG, 3, 1);
            WorldGenFeatureNetherForestVegetation.place(world, random, blockPos, BiomeDecoratorGroups.Configs.NETHER_SPROUTS_CONFIG, 3, 1);
            if (random.nextInt(8) == 0) {
                WorldGenFeatureTwistingVines.place(world, random, blockPos, 3, 1, 2);
            }
        }

    }
}
