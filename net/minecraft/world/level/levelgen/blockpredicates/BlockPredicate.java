package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.FluidType;

public interface BlockPredicate extends BiPredicate<GeneratorAccessSeed, BlockPosition> {
    Codec<BlockPredicate> CODEC = IRegistry.BLOCK_PREDICATE_TYPES.byNameCodec().dispatch(BlockPredicate::type, BlockPredicateType::codec);
    BlockPredicate ONLY_IN_AIR_PREDICATE = matchesBlock(Blocks.AIR, BlockPosition.ZERO);
    BlockPredicate ONLY_IN_AIR_OR_WATER_PREDICATE = matchesBlocks(List.of(Blocks.AIR, Blocks.WATER), BlockPosition.ZERO);

    BlockPredicateType<?> type();

    static BlockPredicate allOf(List<BlockPredicate> predicates) {
        return new AllOfPredicate(predicates);
    }

    static BlockPredicate allOf(BlockPredicate... predicates) {
        return allOf(List.of(predicates));
    }

    static BlockPredicate allOf(BlockPredicate first, BlockPredicate second) {
        return allOf(List.of(first, second));
    }

    static BlockPredicate anyOf(List<BlockPredicate> predicates) {
        return new AnyOfPredicate(predicates);
    }

    static BlockPredicate anyOf(BlockPredicate... predicates) {
        return anyOf(List.of(predicates));
    }

    static BlockPredicate anyOf(BlockPredicate first, BlockPredicate second) {
        return anyOf(List.of(first, second));
    }

    static BlockPredicate matchesBlocks(List<Block> blocks, BaseBlockPosition offset) {
        return new MatchingBlocksPredicate(offset, blocks);
    }

    static BlockPredicate matchesBlocks(List<Block> blocks) {
        return matchesBlocks(blocks, BaseBlockPosition.ZERO);
    }

    static BlockPredicate matchesBlock(Block block, BaseBlockPosition offset) {
        return matchesBlocks(List.of(block), offset);
    }

    static BlockPredicate matchesTag(Tag<Block> tag, BaseBlockPosition offset) {
        return new MatchingBlockTagPredicate(offset, tag);
    }

    static BlockPredicate matchesTag(Tag<Block> offset) {
        return matchesTag(offset, BaseBlockPosition.ZERO);
    }

    static BlockPredicate matchesFluids(List<FluidType> fluids, BaseBlockPosition offset) {
        return new MatchingFluidsPredicate(offset, fluids);
    }

    static BlockPredicate matchesFluid(FluidType fluid, BaseBlockPosition offset) {
        return matchesFluids(List.of(fluid), offset);
    }

    static BlockPredicate not(BlockPredicate predicate) {
        return new NotPredicate(predicate);
    }

    static BlockPredicate replaceable(BaseBlockPosition offset) {
        return new ReplaceablePredicate(offset);
    }

    static BlockPredicate replaceable() {
        return replaceable(BaseBlockPosition.ZERO);
    }

    static BlockPredicate wouldSurvive(IBlockData state, BaseBlockPosition offset) {
        return new WouldSurvivePredicate(offset, state);
    }

    static BlockPredicate hasSturdyFace(BaseBlockPosition offset, EnumDirection face) {
        return new HasSturdyFacePredicate(offset, face);
    }

    static BlockPredicate hasSturdyFace(EnumDirection face) {
        return hasSturdyFace(BaseBlockPosition.ZERO, face);
    }

    static BlockPredicate solid(BaseBlockPosition offset) {
        return new SolidPredicate(offset);
    }

    static BlockPredicate solid() {
        return solid(BaseBlockPosition.ZERO);
    }

    static BlockPredicate insideWorld(BaseBlockPosition offset) {
        return new InsideWorldBoundsPredicate(offset);
    }

    static BlockPredicate alwaysTrue() {
        return TrueBlockPredicate.INSTANCE;
    }
}
