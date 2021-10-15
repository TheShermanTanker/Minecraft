package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.BlockFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class WorldGenCavesOcean extends WorldGenCaves {
    public WorldGenCavesOcean(Codec<CaveCarverConfiguration> configCodec) {
        super(configCodec);
        this.replaceableBlocks = ImmutableSet.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.GRASS_BLOCK, Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.MYCELIUM, Blocks.SNOW, Blocks.SAND, Blocks.GRAVEL, Blocks.WATER, Blocks.LAVA, Blocks.OBSIDIAN, Blocks.PACKED_ICE);
    }

    @Override
    protected boolean hasDisallowedLiquid(IChunkAccess chunk, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        return false;
    }

    @Override
    protected boolean carveBlock(CarvingContext context, CaveCarverConfiguration config, IChunkAccess chunk, Function<BlockPosition, BiomeBase> posToBiome, BitSet carvingMask, Random random, BlockPosition.MutableBlockPosition pos, BlockPosition.MutableBlockPosition downPos, Aquifer sampler, MutableBoolean foundSurface) {
        return carveBlock(this, chunk, random, pos, downPos, sampler);
    }

    protected static boolean carveBlock(WorldGenCarverAbstract<?> carver, IChunkAccess chunk, Random random, BlockPosition.MutableBlockPosition pos, BlockPosition.MutableBlockPosition downPos, Aquifer sampler) {
        if (sampler.computeState(WorldGenCarverAbstract.STONE_SOURCE, pos.getX(), pos.getY(), pos.getZ(), Double.NEGATIVE_INFINITY).isAir()) {
            return false;
        } else {
            IBlockData blockState = chunk.getType(pos);
            if (!carver.canReplaceBlock(blockState)) {
                return false;
            } else if (pos.getY() == 10) {
                float f = random.nextFloat();
                if ((double)f < 0.25D) {
                    chunk.setType(pos, Blocks.MAGMA_BLOCK.getBlockData(), false);
                    chunk.getBlockTicks().scheduleTick(pos, Blocks.MAGMA_BLOCK, 0);
                } else {
                    chunk.setType(pos, Blocks.OBSIDIAN.getBlockData(), false);
                }

                return true;
            } else if (pos.getY() < 10) {
                chunk.setType(pos, Blocks.LAVA.getBlockData(), false);
                return false;
            } else {
                chunk.setType(pos, WATER.getBlockData(), false);
                int i = chunk.getPos().x;
                int j = chunk.getPos().z;

                for(EnumDirection direction : BlockFluids.POSSIBLE_FLOW_DIRECTIONS) {
                    downPos.setWithOffset(pos, direction);
                    if (SectionPosition.blockToSectionCoord(downPos.getX()) != i || SectionPosition.blockToSectionCoord(downPos.getZ()) != j || chunk.getType(downPos).isAir()) {
                        chunk.getLiquidTicks().scheduleTick(pos, WATER.getType(), 0);
                        break;
                    }
                }

                return true;
            }
        }
    }
}
