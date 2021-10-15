package net.minecraft.world.level.gameevent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;

public class EntityPositionSource implements PositionSource {
    public static final Codec<EntityPositionSource> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.INT.fieldOf("source_entity_id").forGetter((entityPositionSource) -> {
            return entityPositionSource.sourceEntityId;
        })).apply(instance, EntityPositionSource::new);
    });
    final int sourceEntityId;
    private Optional<Entity> sourceEntity = Optional.empty();

    public EntityPositionSource(int entityId) {
        this.sourceEntityId = entityId;
    }

    @Override
    public Optional<BlockPosition> getPosition(World world) {
        if (!this.sourceEntity.isPresent()) {
            this.sourceEntity = Optional.ofNullable(world.getEntity(this.sourceEntityId));
        }

        return this.sourceEntity.map(Entity::getChunkCoordinates);
    }

    @Override
    public PositionSourceType<?> getType() {
        return PositionSourceType.ENTITY;
    }

    public static class Type implements PositionSourceType<EntityPositionSource> {
        @Override
        public EntityPositionSource read(PacketDataSerializer friendlyByteBuf) {
            return new EntityPositionSource(friendlyByteBuf.readVarInt());
        }

        @Override
        public void write(PacketDataSerializer buf, EntityPositionSource positionSource) {
            buf.writeVarInt(positionSource.sourceEntityId);
        }

        @Override
        public Codec<EntityPositionSource> codec() {
            return EntityPositionSource.CODEC;
        }
    }
}
