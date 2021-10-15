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

public class PacketPlayOutLogin implements Packet<PacketListenerPlayOut> {
    private static final int HARDCORE_FLAG = 8;
    private final int playerId;
    private final long seed;
    private final boolean hardcore;
    private final EnumGamemode gameType;
    @Nullable
    private final EnumGamemode previousGameType;
    private final Set<ResourceKey<World>> levels;
    private final IRegistryCustom.Dimension registryHolder;
    private final DimensionManager dimensionType;
    private final ResourceKey<World> dimension;
    private final int maxPlayers;
    private final int chunkRadius;
    private final boolean reducedDebugInfo;
    private final boolean showDeathScreen;
    private final boolean isDebug;
    private final boolean isFlat;

    public PacketPlayOutLogin(int playerEntityId, EnumGamemode gameMode, @Nullable EnumGamemode previousGameMode, long sha256Seed, boolean hardcore, Set<ResourceKey<World>> dimensionIds, IRegistryCustom.Dimension registryManager, DimensionManager dimensionType, ResourceKey<World> dimensionId, int maxPlayers, int chunkLoadDistance, boolean reducedDebugInfo, boolean showDeathScreen, boolean debugWorld, boolean flatWorld) {
        this.playerId = playerEntityId;
        this.levels = dimensionIds;
        this.registryHolder = registryManager;
        this.dimensionType = dimensionType;
        this.dimension = dimensionId;
        this.seed = sha256Seed;
        this.gameType = gameMode;
        this.previousGameType = previousGameMode;
        this.maxPlayers = maxPlayers;
        this.hardcore = hardcore;
        this.chunkRadius = chunkLoadDistance;
        this.reducedDebugInfo = reducedDebugInfo;
        this.showDeathScreen = showDeathScreen;
        this.isDebug = debugWorld;
        this.isFlat = flatWorld;
    }

    public PacketPlayOutLogin(PacketDataSerializer buf) {
        this.playerId = buf.readInt();
        this.hardcore = buf.readBoolean();
        this.gameType = EnumGamemode.getById(buf.readByte());
        this.previousGameType = EnumGamemode.byNullableId(buf.readByte());
        this.levels = buf.readCollection(Sets::newHashSetWithExpectedSize, (b) -> {
            return ResourceKey.create(IRegistry.DIMENSION_REGISTRY, b.readResourceLocation());
        });
        this.registryHolder = buf.readWithCodec(IRegistryCustom.Dimension.NETWORK_CODEC);
        this.dimensionType = buf.readWithCodec(DimensionManager.CODEC).get();
        this.dimension = ResourceKey.create(IRegistry.DIMENSION_REGISTRY, buf.readResourceLocation());
        this.seed = buf.readLong();
        this.maxPlayers = buf.readVarInt();
        this.chunkRadius = buf.readVarInt();
        this.reducedDebugInfo = buf.readBoolean();
        this.showDeathScreen = buf.readBoolean();
        this.isDebug = buf.readBoolean();
        this.isFlat = buf.readBoolean();
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
        buf.writeBoolean(this.reducedDebugInfo);
        buf.writeBoolean(this.showDeathScreen);
        buf.writeBoolean(this.isDebug);
        buf.writeBoolean(this.isFlat);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleLogin(this);
    }

    public int getPlayerId() {
        return this.playerId;
    }

    public long getSeed() {
        return this.seed;
    }

    public boolean isHardcore() {
        return this.hardcore;
    }

    public EnumGamemode getGameType() {
        return this.gameType;
    }

    @Nullable
    public EnumGamemode getPreviousGameType() {
        return this.previousGameType;
    }

    public Set<ResourceKey<World>> levels() {
        return this.levels;
    }

    public IRegistryCustom registryAccess() {
        return this.registryHolder;
    }

    public DimensionManager getDimensionType() {
        return this.dimensionType;
    }

    public ResourceKey<World> getDimension() {
        return this.dimension;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public int getChunkRadius() {
        return this.chunkRadius;
    }

    public boolean isReducedDebugInfo() {
        return this.reducedDebugInfo;
    }

    public boolean shouldShowDeathScreen() {
        return this.showDeathScreen;
    }

    public boolean isDebug() {
        return this.isDebug;
    }

    public boolean isFlat() {
        return this.isFlat;
    }
}
