package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class WorldGenFeatureVegetationPatch extends WorldGenerator<VegetationPatchConfiguration> {
    public WorldGenFeatureVegetationPatch(Codec<VegetationPatchConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<VegetationPatchConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        VegetationPatchConfiguration vegetationPatchConfiguration = context.config();
        Random random = context.random();
        BlockPosition blockPos = context.origin();
        Predicate<IBlockData> predicate = getReplaceableTag(vegetationPatchConfiguration);
        int i = vegetationPatchConfiguration.xzRadius.sample(random) + 1;
        int j = vegetationPatchConfiguration.xzRadius.sample(random) + 1;
        Set<BlockPosition> set = this.placeGroundPatch(worldGenLevel, vegetationPatchConfiguration, random, blockPos, predicate, i, j);
        this.distributeVegetation(context, worldGenLevel, vegetationPatchConfiguration, random, set, i, j);
        return !set.isEmpty();
    }

    protected Set<BlockPosition> placeGroundPatch(GeneratorAccessSeed world, VegetationPatchConfiguration config, Random random, BlockPosition pos, Predicate<IBlockData> replaceable, int radiusX, int radiusZ) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();
        BlockPosition.MutableBlockPosition mutableBlockPos2 = mutableBlockPos.mutable();
        EnumDirection direction = config.surface.getDirection();
        EnumDirection direction2 = direction.opposite();
        Set<BlockPosition> set = new HashSet<>();

        for(int i = -radiusX; i <= radiusX; ++i) {
            boolean bl = i == -radiusX || i == radiusX;

            for(int j = -radiusZ; j <= radiusZ; ++j) {
                boolean bl2 = j == -radiusZ || j == radiusZ;
                boolean bl3 = bl || bl2;
                boolean bl4 = bl && bl2;
                boolean bl5 = bl3 && !bl4;
                if (!bl4 && (!bl5 || config.extraEdgeColumnChance != 0.0F && !(random.nextFloat() > config.extraEdgeColumnChance))) {
                    mutableBlockPos.setWithOffset(pos, i, 0, j);

                    for(int k = 0; world.isStateAtPosition(mutableBlockPos, BlockBase.BlockData::isAir) && k < config.verticalRange; ++k) {
                        mutableBlockPos.move(direction);
                    }

                    for(int var25 = 0; world.isStateAtPosition(mutableBlockPos, (state) -> {
                        return !state.isAir();
                    }) && var25 < config.verticalRange; ++var25) {
                        mutableBlockPos.move(direction2);
                    }

                    mutableBlockPos2.setWithOffset(mutableBlockPos, config.surface.getDirection());
                    IBlockData blockState = world.getType(mutableBlockPos2);
                    if (world.isEmpty(mutableBlockPos) && blockState.isFaceSturdy(world, mutableBlockPos2, config.surface.getDirection().opposite())) {
                        int l = config.depth.sample(random) + (config.extraBottomBlockChance > 0.0F && random.nextFloat() < config.extraBottomBlockChance ? 1 : 0);
                        BlockPosition blockPos = mutableBlockPos2.immutableCopy();
                        boolean bl6 = this.placeGround(world, config, replaceable, random, mutableBlockPos2, l);
                        if (bl6) {
                            set.add(blockPos);
                        }
                    }
                }
            }
        }

        return set;
    }

    protected void distributeVegetation(FeaturePlaceContext<VegetationPatchConfiguration> context, GeneratorAccessSeed world, VegetationPatchConfiguration config, Random random, Set<BlockPosition> positions, int radiusX, int radiusZ) {
        for(BlockPosition blockPos : positions) {
            if (config.vegetationChance > 0.0F && random.nextFloat() < config.vegetationChance) {
                this.placeVegetation(world, config, context.chunkGenerator(), random, blockPos);
            }
        }

    }

    protected boolean placeVegetation(GeneratorAccessSeed world, VegetationPatchConfiguration config, ChunkGenerator generator, Random random, BlockPosition pos) {
        return config.vegetationFeature.get().place(world, generator, random, pos.relative(config.surface.getDirection().opposite()));
    }

    protected boolean placeGround(GeneratorAccessSeed world, VegetationPatchConfiguration config, Predicate<IBlockData> replaceable, Random random, BlockPosition.MutableBlockPosition pos, int depth) {
        for(int i = 0; i < depth; ++i) {
            IBlockData blockState = config.groundState.getState(random, pos);
            IBlockData blockState2 = world.getType(pos);
            if (!blockState.is(blockState2.getBlock())) {
                if (!replaceable.test(blockState2)) {
                    return i != 0;
                }

                world.setTypeAndData(pos, blockState, 2);
                pos.move(config.surface.getDirection());
            }
        }

        return true;
    }

    private static Predicate<IBlockData> getReplaceableTag(VegetationPatchConfiguration config) {
        Tag<Block> tag = TagsBlock.getAllTags().getTag(config.replaceable);
        return tag == null ? (state) -> {
            return true;
        } : (state) -> {
            return state.is(tag);
        };
    }
}
