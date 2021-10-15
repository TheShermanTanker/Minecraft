package net.minecraft.world.level.gameevent;

import com.mojang.serialization.Codec;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;

public interface PositionSourceType<T extends PositionSource> {
    PositionSourceType<BlockPositionSource> BLOCK = register("block", new BlockPositionSource.Type());
    PositionSourceType<EntityPositionSource> ENTITY = register("entity", new EntityPositionSource.Type());

    T read(PacketDataSerializer buf);

    void write(PacketDataSerializer buf, T positionSource);

    Codec<T> codec();

    static <S extends PositionSourceType<T>, T extends PositionSource> S register(String id, S positionSourceType) {
        return IRegistry.register(IRegistry.POSITION_SOURCE_TYPE, id, positionSourceType);
    }

    static PositionSource fromNetwork(PacketDataSerializer buf) {
        MinecraftKey resourceLocation = buf.readResourceLocation();
        return IRegistry.POSITION_SOURCE_TYPE.getOptional(resourceLocation).orElseThrow(() -> {
            return new IllegalArgumentException("Unknown position source type " + resourceLocation);
        }).read(buf);
    }

    static <T extends PositionSource> void toNetwork(T positionSource, PacketDataSerializer buf) {
        buf.writeResourceLocation(IRegistry.POSITION_SOURCE_TYPE.getKey(positionSource.getType()));
        positionSource.getType().write(buf, positionSource);
    }
}
