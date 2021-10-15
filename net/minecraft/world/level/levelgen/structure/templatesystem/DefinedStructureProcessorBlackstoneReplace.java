package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockStairs;
import net.minecraft.world.level.block.BlockStepAbstract;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class DefinedStructureProcessorBlackstoneReplace extends DefinedStructureProcessor {
    public static final Codec<DefinedStructureProcessorBlackstoneReplace> CODEC;
    public static final DefinedStructureProcessorBlackstoneReplace INSTANCE = new DefinedStructureProcessorBlackstoneReplace();
    private final Map<Block, Block> replacements = SystemUtils.make(Maps.newHashMap(), (hashMap) -> {
        hashMap.put(Blocks.COBBLESTONE, Blocks.BLACKSTONE);
        hashMap.put(Blocks.MOSSY_COBBLESTONE, Blocks.BLACKSTONE);
        hashMap.put(Blocks.STONE, Blocks.POLISHED_BLACKSTONE);
        hashMap.put(Blocks.STONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
        hashMap.put(Blocks.MOSSY_STONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
        hashMap.put(Blocks.COBBLESTONE_STAIRS, Blocks.BLACKSTONE_STAIRS);
        hashMap.put(Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.BLACKSTONE_STAIRS);
        hashMap.put(Blocks.STONE_STAIRS, Blocks.POLISHED_BLACKSTONE_STAIRS);
        hashMap.put(Blocks.STONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
        hashMap.put(Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
        hashMap.put(Blocks.COBBLESTONE_SLAB, Blocks.BLACKSTONE_SLAB);
        hashMap.put(Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.BLACKSTONE_SLAB);
        hashMap.put(Blocks.SMOOTH_STONE_SLAB, Blocks.POLISHED_BLACKSTONE_SLAB);
        hashMap.put(Blocks.STONE_SLAB, Blocks.POLISHED_BLACKSTONE_SLAB);
        hashMap.put(Blocks.STONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
        hashMap.put(Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
        hashMap.put(Blocks.STONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        hashMap.put(Blocks.MOSSY_STONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        hashMap.put(Blocks.COBBLESTONE_WALL, Blocks.BLACKSTONE_WALL);
        hashMap.put(Blocks.MOSSY_COBBLESTONE_WALL, Blocks.BLACKSTONE_WALL);
        hashMap.put(Blocks.CHISELED_STONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE);
        hashMap.put(Blocks.CRACKED_STONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        hashMap.put(Blocks.IRON_BARS, Blocks.CHAIN);
    });

    private DefinedStructureProcessorBlackstoneReplace() {
    }

    @Override
    public DefinedStructure.BlockInfo processBlock(IWorldReader world, BlockPosition pos, BlockPosition pivot, DefinedStructure.BlockInfo structureBlockInfo, DefinedStructure.BlockInfo structureBlockInfo2, DefinedStructureInfo data) {
        Block block = this.replacements.get(structureBlockInfo2.state.getBlock());
        if (block == null) {
            return structureBlockInfo2;
        } else {
            IBlockData blockState = structureBlockInfo2.state;
            IBlockData blockState2 = block.getBlockData();
            if (blockState.hasProperty(BlockStairs.FACING)) {
                blockState2 = blockState2.set(BlockStairs.FACING, blockState.get(BlockStairs.FACING));
            }

            if (blockState.hasProperty(BlockStairs.HALF)) {
                blockState2 = blockState2.set(BlockStairs.HALF, blockState.get(BlockStairs.HALF));
            }

            if (blockState.hasProperty(BlockStepAbstract.TYPE)) {
                blockState2 = blockState2.set(BlockStepAbstract.TYPE, blockState.get(BlockStepAbstract.TYPE));
            }

            return new DefinedStructure.BlockInfo(structureBlockInfo2.pos, blockState2, structureBlockInfo2.nbt);
        }
    }

    @Override
    protected DefinedStructureStructureProcessorType<?> getType() {
        return DefinedStructureStructureProcessorType.BLACKSTONE_REPLACE;
    }

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
