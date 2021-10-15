package net.minecraft.world.level.gameevent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.level.World;

public class BlockPositionSource implements PositionSource {
    public static final Codec<BlockPositionSource> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BlockPosition.CODEC.fieldOf("pos").xmap(Optional::of, Optional::get).forGetter((blockPositionSource) -> {
            return blockPositionSource.pos;
        })).apply(instance, BlockPositionSource::new);
    });
    final Optional<BlockPosition> pos;

    public BlockPositionSource(BlockPosition pos) {
        this(Optional.of(pos));
    }

    public BlockPositionSource(Optional<BlockPosition> pos) {
        this.pos = pos;
    }

    @Override
    public Optional<BlockPosition> getPosition(World world) {
        return this.pos;
    }

    @Override
    public PositionSourceType<?> getType() {
        return PositionSourceType.BLOCK;
    }

    public static class Type implements PositionSourceType<BlockPositionSource> {
        @Override
        public BlockPositionSource read(PacketDataSerializer friendlyByteBuf) {
            return new BlockPositionSource(Optional.of(friendlyByteBuf.readBlockPos()));
        }

        @Override
        public void write(PacketDataSerializer buf, BlockPositionSource positionSource) {
            positionSource.pos.ifPresent(buf::writeBlockPos);
        }

        @Override
        public Codec<BlockPositionSource> codec() {
            return BlockPositionSource.CODEC;
        }
    }
}
