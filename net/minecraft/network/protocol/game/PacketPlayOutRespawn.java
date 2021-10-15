package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.World;
import net.minecraft.world.level.dimension.DimensionManager;

public class PacketPlayOutRespawn implements Packet<PacketListenerPlayOut> {
    private final DimensionManager dimensionType;
    private final ResourceKey<World> dimension;
    private final long seed;
    private final EnumGamemode playerGameType;
    @Nullable
    private final EnumGamemode previousPlayerGameType;
    private final boolean isDebug;
    private final boolean isFlat;
    private final boolean keepAllPlayerData;

    public PacketPlayOutRespawn(DimensionManager dimensionType, ResourceKey<World> dimension, long sha256Seed, EnumGamemode gameMode, @Nullable EnumGamemode previousGameMode, boolean debugWorld, boolean flatWorld, boolean keepPlayerAttributes) {
        this.dimensionType = dimensionType;
        this.dimension = dimension;
        this.seed = sha256Seed;
        this.playerGameType = gameMode;
        this.previousPlayerGameType = previousGameMode;
        this.isDebug = debugWorld;
        this.isFlat = flatWorld;
        this.keepAllPlayerData = keepPlayerAttributes;
    }

    public PacketPlayOutRespawn(PacketDataSerializer buf) {
        this.dimensionType = buf.readWithCodec(DimensionManager.CODEC).get();
        this.dimension = ResourceKey.create(IRegistry.DIMENSION_REGISTRY, buf.readResourceLocation());
        this.seed = buf.readLong();
        this.playerGameType = EnumGamemode.getById(buf.readUnsignedByte());
        this.previousPlayerGameType = EnumGamemode.byNullableId(buf.readByte());
        this.isDebug = buf.readBoolean();
        this.isFlat = buf.readBoolean();
        this.keepAllPlayerData = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeWithCodec(DimensionManager.CODEC, () -> {
            return this.dimensionType;
        });
        buf.writeResourceLocation(this.dimension.location());
        buf.writeLong(this.seed);
        buf.writeByte(this.playerGameType.getId());
        buf.writeByte(EnumGamemode.getNullableId(this.previousPlayerGameType));
        buf.writeBoolean(this.isDebug);
        buf.writeBoolean(this.isFlat);
        buf.writeBoolean(this.keepAllPlayerData);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleRespawn(this);
    }

    public DimensionManager getDimensionType() {
        return this.dimensionType;
    }

    public ResourceKey<World> getDimension() {
        return this.dimension;
    }

    public long getSeed() {
        return this.seed;
    }

    public EnumGamemode getPlayerGameType() {
        return this.playerGameType;
    }

    @Nullable
    public EnumGamemode getPreviousPlayerGameType() {
        return this.previousPlayerGameType;
    }

    public boolean isDebug() {
        return this.isDebug;
    }

    public boolean isFlat() {
        return this.isFlat;
    }

    public boolean shouldKeepAllPlayerData() {
        return this.keepAllPlayerData;
    }
}
