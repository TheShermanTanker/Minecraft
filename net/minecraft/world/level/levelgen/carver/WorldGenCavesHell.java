package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.material.FluidTypes;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class WorldGenCavesHell extends WorldGenCaves {
    public WorldGenCavesHell(Codec<CaveCarverConfiguration> configCodec) {
        super(configCodec);
        this.replaceableBlocks = ImmutableSet.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.GRASS_BLOCK, Blocks.NETHERRACK, Blocks.SOUL_SAND, Blocks.SOUL_SOIL, Blocks.CRIMSON_NYLIUM, Blocks.WARPED_NYLIUM, Blocks.NETHER_WART_BLOCK, Blocks.WARPED_WART_BLOCK, Blocks.BASALT, Blocks.BLACKSTONE);
        this.liquids = ImmutableSet.of(FluidTypes.LAVA, FluidTypes.WATER);
    }

    @Override
    protected int getCaveBound() {
        return 10;
    }

    @Override
    protected float getThickness(Random random) {
        return (random.nextFloat() * 2.0F + random.nextFloat()) * 2.0F;
    }

    @Override
    protected double getYScale() {
        return 5.0D;
    }

    @Override
    protected boolean carveBlock(CarvingContext context, CaveCarverConfiguration config, IChunkAccess chunk, Function<BlockPosition, BiomeBase> posToBiome, CarvingMask mask, BlockPosition.MutableBlockPosition mutableBlockPos, BlockPosition.MutableBlockPosition mutableBlockPos2, Aquifer aquiferSampler, MutableBoolean mutableBoolean) {
        if (this.canReplaceBlock(chunk.getType(mutableBlockPos))) {
            IBlockData blockState;
            if (mutableBlockPos.getY() <= context.getMinGenY() + 31) {
                blockState = LAVA.getBlockData();
            } else {
                blockState = CAVE_AIR;
            }

            chunk.setType(mutableBlockPos, blockState, false);
            return true;
        } else {
            return false;
        }
    }
}
