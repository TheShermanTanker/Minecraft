package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.levelgen.Column;

public class CaveSurfaceDecorator extends WorldGenDecorator<CaveDecoratorConfiguration> {
    public CaveSurfaceDecorator(Codec<CaveDecoratorConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, CaveDecoratorConfiguration config, BlockPosition pos) {
        Optional<Column> optional = Column.scan(context.getLevel(), pos, config.floorToCeilingSearchRange, BlockBase.BlockData::isAir, (blockState) -> {
            return blockState.getMaterial().isBuildable();
        });
        if (!optional.isPresent()) {
            return Stream.of();
        } else {
            OptionalInt optionalInt = config.surface == CaveSurface.CEILING ? optional.get().getCeiling() : optional.get().getFloor();
            return !optionalInt.isPresent() ? Stream.of() : Stream.of(pos.atY(optionalInt.getAsInt() - config.surface.getY()));
        }
    }
}
