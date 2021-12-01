package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.GlowLichenConfiguration;

public class WorldGenFeatureGlowLichen extends WorldGenerator<GlowLichenConfiguration> {
    public WorldGenFeatureGlowLichen(Codec<GlowLichenConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<GlowLichenConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        Random random = context.random();
        GlowLichenConfiguration glowLichenConfiguration = context.config();
        if (!isAirOrWater(worldGenLevel.getType(blockPos))) {
            return false;
        } else {
            List<EnumDirection> list = getShuffledDirections(glowLichenConfiguration, random);
            if (placeGlowLichenIfPossible(worldGenLevel, blockPos, worldGenLevel.getType(blockPos), glowLichenConfiguration, random, list)) {
                return true;
            } else {
                BlockPosition.MutableBlockPosition mutableBlockPos = blockPos.mutable();

                for(EnumDirection direction : list) {
                    mutableBlockPos.set(blockPos);
                    List<EnumDirection> list2 = getShuffledDirectionsExcept(glowLichenConfiguration, random, direction.opposite());

                    for(int i = 0; i < glowLichenConfiguration.searchRange; ++i) {
                        mutableBlockPos.setWithOffset(blockPos, direction);
                        IBlockData blockState = worldGenLevel.getType(mutableBlockPos);
                        if (!isAirOrWater(blockState) && !blockState.is(Blocks.GLOW_LICHEN)) {
                            break;
                        }

                        if (placeGlowLichenIfPossible(worldGenLevel, mutableBlockPos, blockState, glowLichenConfiguration, random, list2)) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }
    }

    public static boolean placeGlowLichenIfPossible(GeneratorAccessSeed world, BlockPosition pos, IBlockData state, GlowLichenConfiguration config, Random random, List<EnumDirection> directions) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();

        for(EnumDirection direction : directions) {
            IBlockData blockState = world.getType(mutableBlockPos.setWithOffset(pos, direction));
            if (config.canBePlacedOn.contains(blockState.getBlock())) {
                GlowLichenBlock glowLichenBlock = (GlowLichenBlock)Blocks.GLOW_LICHEN;
                IBlockData blockState2 = glowLichenBlock.getStateForPlacement(state, world, pos, direction);
                if (blockState2 == null) {
                    return false;
                }

                world.setTypeAndData(pos, blockState2, 3);
                world.getChunk(pos).markPosForPostprocessing(pos);
                if (random.nextFloat() < config.chanceOfSpreading) {
                    glowLichenBlock.spreadFromFaceTowardRandomDirection(blockState2, world, pos, direction, random, true);
                }

                return true;
            }
        }

        return false;
    }

    public static List<EnumDirection> getShuffledDirections(GlowLichenConfiguration config, Random random) {
        List<EnumDirection> list = Lists.newArrayList(config.validDirections);
        Collections.shuffle(list, random);
        return list;
    }

    public static List<EnumDirection> getShuffledDirectionsExcept(GlowLichenConfiguration config, Random random, EnumDirection excluded) {
        List<EnumDirection> list = config.validDirections.stream().filter((direction) -> {
            return direction != excluded;
        }).collect(Collectors.toList());
        Collections.shuffle(list, random);
        return list;
    }

    private static boolean isAirOrWater(IBlockData state) {
        return state.isAir() || state.is(Blocks.WATER);
    }
}
