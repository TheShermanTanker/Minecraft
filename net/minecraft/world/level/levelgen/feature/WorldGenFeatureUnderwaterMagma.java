package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.UnderwaterMagmaConfiguration;
import net.minecraft.world.phys.AxisAlignedBB;

public class WorldGenFeatureUnderwaterMagma extends WorldGenerator<UnderwaterMagmaConfiguration> {
    public WorldGenFeatureUnderwaterMagma(Codec<UnderwaterMagmaConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<UnderwaterMagmaConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        UnderwaterMagmaConfiguration underwaterMagmaConfiguration = context.config();
        Random random = context.random();
        OptionalInt optionalInt = getFloorY(worldGenLevel, blockPos, underwaterMagmaConfiguration);
        if (!optionalInt.isPresent()) {
            return false;
        } else {
            BlockPosition blockPos2 = blockPos.atY(optionalInt.getAsInt());
            BaseBlockPosition vec3i = new BaseBlockPosition(underwaterMagmaConfiguration.placementRadiusAroundFloor, underwaterMagmaConfiguration.placementRadiusAroundFloor, underwaterMagmaConfiguration.placementRadiusAroundFloor);
            AxisAlignedBB aABB = new AxisAlignedBB(blockPos2.subtract(vec3i), blockPos2.offset(vec3i));
            return BlockPosition.betweenClosedStream(aABB).filter((pos) -> {
                return random.nextFloat() < underwaterMagmaConfiguration.placementProbabilityPerValidPosition;
            }).filter((pos) -> {
                return this.isValidPlacement(worldGenLevel, pos);
            }).mapToInt((pos) -> {
                worldGenLevel.setTypeAndData(pos, Blocks.MAGMA_BLOCK.getBlockData(), 2);
                return 1;
            }).sum() > 0;
        }
    }

    private static OptionalInt getFloorY(GeneratorAccessSeed world, BlockPosition pos, UnderwaterMagmaConfiguration config) {
        Predicate<IBlockData> predicate = (state) -> {
            return state.is(Blocks.WATER);
        };
        Predicate<IBlockData> predicate2 = (state) -> {
            return !state.is(Blocks.WATER);
        };
        Optional<Column> optional = Column.scan(world, pos, config.floorSearchRange, predicate, predicate2);
        return optional.map(Column::getFloor).orElseGet(OptionalInt::empty);
    }

    private boolean isValidPlacement(GeneratorAccessSeed world, BlockPosition pos) {
        if (!this.isWaterOrAir(world, pos) && !this.isWaterOrAir(world, pos.below())) {
            for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                if (this.isWaterOrAir(world, pos.relative(direction))) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean isWaterOrAir(GeneratorAccess world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        return blockState.is(Blocks.WATER) || blockState.isAir();
    }
}
