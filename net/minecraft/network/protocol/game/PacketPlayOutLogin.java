package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.World;
import net.minecraft.world.level.dimension.DimensionManager;

public record PacketPlayOutLogin(int playerId, boolean hardcore, EnumGamemode gameType, @Nullable EnumGamemode previousGameType, Set<ResourceKey<World>> levels, IRegistryCustom.Dimension registryHolder, DimensionManager dimensionType, ResourceKey<World> dimension, long seed, int maxPlayers, int chunkRadius, int simulationDistance, boolean reducedDebugInfo, boolean showDeathScreen, boolean isDebug, boolean isFlat) implements Packet<PacketListenerPlayOut> {
    public PacketPlayOutLogin(PacketDataSerializer buf) {
        this(buf.readInt(), buf.readBoolean(), EnumGamemode.getById(buf.readByte()), EnumGamemode.byNullableId(buf.readByte()), buf.readCollection(Sets::newHashSetWithExpectedSize, (b) -> {
            return ResourceKey.create(IRegistry.DIMENSION_REGISTRY, b.readResourceLocation());
        }), buf.readWithCodec(IRegistryCustom.Dimension.NETWORK_CODEC), buf.readWithCodec(DimensionManager.CODEC).get(), ResourceKey.create(IRegistry.DIMENSION_REGISTRY, buf.readResourceLocation()), buf.readLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public PacketPlayOutLogin(int playerEntityId, boolean bl, EnumGamemode previousGameMode, @Nullable EnumGamemode gameType, Set<ResourceKey<World>> set, IRegistryCustom.Dimension registryHolder, DimensionManager dimensionType, ResourceKey<World> resourceKey, long l, int maxPlayers, int chunkLoadDistance, int i, boolean bl2, boolean bl3, boolean bl4, boolean bl5) {
        this.playerId = playerEntityId;
        this.hardcore = bl;
        this.gameType = previousGameMode;
        this.previousGameType = gameType;
        this.levels = set;
        this.registryHolder = registryHolder;
        this.dimensionType = dimensionType;
        this.dimension = resourceKey;
        this.seed = l;
        this.maxPlayers = maxPlayers;
        this.chunkRadius = chunkLoadDistance;
        this.simulationDistance = i;
        this.reducedDebugInfo = bl2;
        this.showDeathScreen = bl3;
        this.isDebug = bl4;
        this.isFlat = bl5;
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeInt(this.playerId);
        buf.writeBoolean(this.hardcore);
        buf.writeByte(this.gameType.getId());
        buf.writeByte(EnumGamemode.getNullableId(this.previousGameType));
        buf.writeCollection(this.levels, (b, dimension) -> {
            b.writeResourceLocation(dimension.location());
        });
        buf.writeWithCodec(IRegistryCustom.Dimension.NETWORK_CODEC, this.registryHolder);
        buf.writeWithCodec(DimensionManager.CODEC, () -> {
            return this.dimensionType;
        });
        buf.writeResourceLocation(this.dimension.location());
        buf.writeLong(this.seed);
        buf.writeVarInt(this.maxPlayers);
        buf.writeVarInt(this.chunkRadius);
        buf.writeVarInt(this.simulationDistance);
        buf.writeBoolean(this.reducedDebugInfo);
        buf.writeBoolean(this.showDeathScreen);
        buf.writeBoolean(this.isDebug);
        buf.writeBoolean(this.isFlat);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleLogin(this);
    }

    public int playerId() {
        return this.playerId;
    }

    public boolean hardcore() {
        return this.hardcore;
    }

    public EnumGamemode gameType() {
        return this.gameType;
    }

    @Nullable
    public EnumGamemode previousGameType() {
        return this.previousGameType;
    }

    public Set<ResourceKey<World>> levels() {
        return this.levels;
    }

    public IRegistryCustom.Dimension registryHolder() {
        return this.registryHolder;
    }

    public DimensionManager dimensionType() {
        return this.dimensionType;
    }

    public ResourceKey<World> dimension() {
        return this.dimension;
    }

    public long seed() {
        return this.seed;
    }

    public int maxPlayers() {
        return this.maxPlayers;
    }

    public int chunkRadius() {
        return this.chunkRadius;
    }

    public int simulationDistance() {
        return this.simulationDistance;
    }

    public boolean reducedDebugInfo() {
        return this.reducedDebugInfo;
    }

    public boolean showDeathScreen() {
        return this.showDeathScreen;
    }

    public boolean isDebug() {
        return this.isDebug;
    }

    public boolean isFlat() {
        return this.isFlat;
    }
}
