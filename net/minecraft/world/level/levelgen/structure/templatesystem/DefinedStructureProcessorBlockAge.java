package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockStairs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyHalf;

public class DefinedStructureProcessorBlockAge extends DefinedStructureProcessor {
    public static final Codec<DefinedStructureProcessorBlockAge> CODEC = Codec.FLOAT.fieldOf("mossiness").xmap(DefinedStructureProcessorBlockAge::new, (blockAgeProcessor) -> {
        return blockAgeProcessor.mossiness;
    }).codec();
    private static final float PROBABILITY_OF_REPLACING_FULL_BLOCK = 0.5F;
    private static final float PROBABILITY_OF_REPLACING_STAIRS = 0.5F;
    private static final float PROBABILITY_OF_REPLACING_OBSIDIAN = 0.15F;
    private static final IBlockData[] NON_MOSSY_REPLACEMENTS = new IBlockData[]{Blocks.STONE_SLAB.getBlockData(), Blocks.STONE_BRICK_SLAB.getBlockData()};
    private final float mossiness;

    public DefinedStructureProcessorBlockAge(float mossiness) {
        this.mossiness = mossiness;
    }

    @Nullable
    @Override
    public DefinedStructure.BlockInfo processBlock(IWorldReader world, BlockPosition pos, BlockPosition pivot, DefinedStructure.BlockInfo structureBlockInfo, DefinedStructure.BlockInfo structureBlockInfo2, DefinedStructureInfo data) {
        Random random = data.getRandom(structureBlockInfo2.pos);
        IBlockData blockState = structureBlockInfo2.state;
        BlockPosition blockPos = structureBlockInfo2.pos;
        IBlockData blockState2 = null;
        if (!blockState.is(Blocks.STONE_BRICKS) && !blockState.is(Blocks.STONE) && !blockState.is(Blocks.CHISELED_STONE_BRICKS)) {
            if (blockState.is(TagsBlock.STAIRS)) {
                blockState2 = this.maybeReplaceStairs(random, structureBlockInfo2.state);
            } else if (blockState.is(TagsBlock.SLABS)) {
                blockState2 = this.maybeReplaceSlab(random);
            } else if (blockState.is(TagsBlock.WALLS)) {
                blockState2 = this.maybeReplaceWall(random);
            } else if (blockState.is(Blocks.OBSIDIAN)) {
                blockState2 = this.maybeReplaceObsidian(random);
            }
        } else {
            blockState2 = this.maybeReplaceFullStoneBlock(random);
        }

        return blockState2 != null ? new DefinedStructure.BlockInfo(blockPos, blockState2, structureBlockInfo2.nbt) : structureBlockInfo2;
    }

    @Nullable
    private IBlockData maybeReplaceFullStoneBlock(Random random) {
        if (random.nextFloat() >= 0.5F) {
            return null;
        } else {
            IBlockData[] blockStates = new IBlockData[]{Blocks.CRACKED_STONE_BRICKS.getBlockData(), getRandomFacingStairs(random, Blocks.STONE_BRICK_STAIRS)};
            IBlockData[] blockStates2 = new IBlockData[]{Blocks.MOSSY_STONE_BRICKS.getBlockData(), getRandomFacingStairs(random, Blocks.MOSSY_STONE_BRICK_STAIRS)};
            return this.getRandomBlock(random, blockStates, blockStates2);
        }
    }

    @Nullable
    private IBlockData maybeReplaceStairs(Random random, IBlockData state) {
        EnumDirection direction = state.get(BlockStairs.FACING);
        BlockPropertyHalf half = state.get(BlockStairs.HALF);
        if (random.nextFloat() >= 0.5F) {
            return null;
        } else {
            IBlockData[] blockStates = new IBlockData[]{Blocks.MOSSY_STONE_BRICK_STAIRS.getBlockData().set(BlockStairs.FACING, direction).set(BlockStairs.HALF, half), Blocks.MOSSY_STONE_BRICK_SLAB.getBlockData()};
            return this.getRandomBlock(random, NON_MOSSY_REPLACEMENTS, blockStates);
        }
    }

    @Nullable
    private IBlockData maybeReplaceSlab(Random random) {
        return random.nextFloat() < this.mossiness ? Blocks.MOSSY_STONE_BRICK_SLAB.getBlockData() : null;
    }

    @Nullable
    private IBlockData maybeReplaceWall(Random random) {
        return random.nextFloat() < this.mossiness ? Blocks.MOSSY_STONE_BRICK_WALL.getBlockData() : null;
    }

    @Nullable
    private IBlockData maybeReplaceObsidian(Random random) {
        return random.nextFloat() < 0.15F ? Blocks.CRYING_OBSIDIAN.getBlockData() : null;
    }

    private static IBlockData getRandomFacingStairs(Random random, Block stairs) {
        return stairs.getBlockData().set(BlockStairs.FACING, EnumDirection.EnumDirectionLimit.HORIZONTAL.getRandomDirection(random)).set(BlockStairs.HALF, BlockPropertyHalf.values()[random.nextInt(BlockPropertyHalf.values().length)]);
    }

    private IBlockData getRandomBlock(Random random, IBlockData[] regularStates, IBlockData[] mossyStates) {
        return random.nextFloat() < this.mossiness ? getRandomBlock(random, mossyStates) : getRandomBlock(random, regularStates);
    }

    private static IBlockData getRandomBlock(Random random, IBlockData[] states) {
        return states[random.nextInt(states.length)];
    }

    @Override
    protected DefinedStructureStructureProcessorType<?> getType() {
        return DefinedStructureStructureProcessorType.BLOCK_AGE;
    }
}
