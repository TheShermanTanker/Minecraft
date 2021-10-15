package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.FileUtils;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.PacketPlayOutAbilities;
import net.minecraft.network.protocol.game.PacketPlayOutCustomPayload;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEffect;
import net.minecraft.network.protocol.game.PacketPlayOutEntityStatus;
import net.minecraft.network.protocol.game.PacketPlayOutExperience;
import net.minecraft.network.protocol.game.PacketPlayOutGameStateChange;
import net.minecraft.network.protocol.game.PacketPlayOutHeldItemSlot;
import net.minecraft.network.protocol.game.PacketPlayOutLogin;
import net.minecraft.network.protocol.game.PacketPlayOutNamedSoundEffect;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo;
import net.minecraft.network.protocol.game.PacketPlayOutRecipeUpdate;
import net.minecraft.network.protocol.game.PacketPlayOutRespawn;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.network.protocol.game.PacketPlayOutServerDifficulty;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnPosition;
import net.minecraft.network.protocol.game.PacketPlayOutTags;
import net.minecraft.network.protocol.game.PacketPlayOutUpdateTime;
import net.minecraft.network.protocol.game.PacketPlayOutViewDistance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.AdvancementDataPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ScoreboardServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.ServerStatisticManager;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.border.IWorldBorderListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.storage.SavedFile;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.WorldNBTStorage;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class PlayerList {
    public static final File USERBANLIST_FILE = new File("banned-players.json");
    public static final File IPBANLIST_FILE = new File("banned-ips.json");
    public static final File OPLIST_FILE = new File("ops.json");
    public static final File WHITELIST_FILE = new File("whitelist.json");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int SEND_PLAYER_INFO_INTERVAL = 600;
    private static final SimpleDateFormat BAN_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    private final MinecraftServer server;
    public final List<EntityPlayer> players = Lists.newArrayList();
    private final Map<UUID, EntityPlayer> playersByUUID = Maps.newHashMap();
    private final GameProfileBanList bans = new GameProfileBanList(USERBANLIST_FILE);
    private final IpBanList ipBans = new IpBanList(IPBANLIST_FILE);
    private final OpList ops = new OpList(OPLIST_FILE);
    private final WhiteList whitelist = new WhiteList(WHITELIST_FILE);
    private final Map<UUID, ServerStatisticManager> stats = Maps.newHashMap();
    private final Map<UUID, AdvancementDataPlayer> advancements = Maps.newHashMap();
    public final WorldNBTStorage playerIo;
    private boolean doWhiteList;
    private final IRegistryCustom.Dimension registryHolder;
    protected final int maxPlayers;
    private int viewDistance;
    private boolean allowCheatsForAllPlayers;
    private static final boolean ALLOW_LOGOUTIVATOR = false;
    private int sendAllPlayerInfoIn;

    public PlayerList(MinecraftServer server, IRegistryCustom.Dimension registryManager, WorldNBTStorage saveHandler, int maxPlayers) {
        this.server = server;
        this.registryHolder = registryManager;
        this.maxPlayers = maxPlayers;
        this.playerIo = saveHandler;
    }

    public void placeNewPlayer(NetworkManager connection, EntityPlayer player) {
        GameProfile gameProfile = player.getProfile();
        UserCache gameProfileCache = this.server.getUserCache();
        Optional<GameProfile> optional = gameProfileCache.getProfile(gameProfile.getId());
        String string = optional.map(GameProfile::getName).orElse(gameProfile.getName());
        gameProfileCache.add(gameProfile);
        NBTTagCompound compoundTag = this.load(player);
        ResourceKey<World> resourceKey = compoundTag != null ? DimensionManager.parseLegacy(new Dynamic<>(DynamicOpsNBT.INSTANCE, compoundTag.get("Dimension"))).resultOrPartial(LOGGER::error).orElse(World.OVERWORLD) : World.OVERWORLD;
        WorldServer serverLevel = this.server.getWorldServer(resourceKey);
        WorldServer serverLevel2;
        if (serverLevel == null) {
            LOGGER.warn("Unknown respawn dimension {}, defaulting to overworld", (Object)resourceKey);
            serverLevel2 = this.server.overworld();
        } else {
            serverLevel2 = serverLevel;
        }

        player.spawnIn(serverLevel2);
        String string2 = "local";
        if (connection.getSocketAddress() != null) {
            string2 = connection.getSocketAddress().toString();
        }

        LOGGER.info("{}[{}] logged in with entity id {} at ({}, {}, {})", player.getDisplayName().getString(), string2, player.getId(), player.locX(), player.locY(), player.locZ());
        WorldData levelData = serverLevel2.getWorldData();
        player.loadGameTypes(compoundTag);
        PlayerConnection serverGamePacketListenerImpl = new PlayerConnection(this.server, connection, player);
        GameRules gameRules = serverLevel2.getGameRules();
        boolean bl = gameRules.getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN);
        boolean bl2 = gameRules.getBoolean(GameRules.RULE_REDUCEDDEBUGINFO);
        serverGamePacketListenerImpl.sendPacket(new PacketPlayOutLogin(player.getId(), player.gameMode.getGameMode(), player.gameMode.getPreviousGameModeForPlayer(), BiomeManager.obfuscateSeed(serverLevel2.getSeed()), levelData.isHardcore(), this.server.levelKeys(), this.registryHolder, serverLevel2.getDimensionManager(), serverLevel2.getDimensionKey(), this.getMaxPlayers(), this.viewDistance, bl2, !bl, serverLevel2.isDebugWorld(), serverLevel2.isFlatWorld()));
        serverGamePacketListenerImpl.sendPacket(new PacketPlayOutCustomPayload(PacketPlayOutCustomPayload.BRAND, (new PacketDataSerializer(Unpooled.buffer())).writeUtf(this.getServer().getServerModName())));
        serverGamePacketListenerImpl.sendPacket(new PacketPlayOutServerDifficulty(levelData.getDifficulty(), levelData.isDifficultyLocked()));
        serverGamePacketListenerImpl.sendPacket(new PacketPlayOutAbilities(player.getAbilities()));
        serverGamePacketListenerImpl.sendPacket(new PacketPlayOutHeldItemSlot(player.getInventory().selected));
        serverGamePacketListenerImpl.sendPacket(new PacketPlayOutRecipeUpdate(this.server.getCraftingManager().getRecipes()));
        serverGamePacketListenerImpl.sendPacket(new PacketPlayOutTags(this.server.getTagRegistry().serializeToNetwork(this.registryHolder)));
        this.sendPlayerPermissionLevel(player);
        player.getStatisticManager().markAllDirty();
        player.getRecipeBook().sendInitialRecipeBook(player);
        this.sendScoreboard(serverLevel2.getScoreboard(), player);
        this.server.invalidatePingSample();
        IChatMutableComponent mutableComponent;
        if (player.getProfile().getName().equalsIgnoreCase(string)) {
            mutableComponent = new ChatMessage("multiplayer.player.joined", player.getScoreboardDisplayName());
        } else {
            mutableComponent = new ChatMessage("multiplayer.player.joined.renamed", player.getScoreboardDisplayName(), string);
        }

        this.sendMessage(mutableComponent.withStyle(EnumChatFormat.YELLOW), ChatMessageType.SYSTEM, SystemUtils.NIL_UUID);
        serverGamePacketListenerImpl.teleport(player.locX(), player.locY(), player.locZ(), player.getYRot(), player.getXRot());
        this.players.add(player);
        this.playersByUUID.put(player.getUniqueID(), player);
        this.sendAll(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, player));

        for(int i = 0; i < this.players.size(); ++i) {
            player.connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, this.players.get(i)));
        }

        serverLevel2.addPlayerJoin(player);
        this.server.getBossBattleCustomData().onPlayerConnect(player);
        this.sendLevelInfo(player, serverLevel2);
        if (!this.server.getResourcePack().isEmpty()) {
            player.setResourcePack(this.server.getResourcePack(), this.server.getResourcePackHash(), this.server.isResourcePackRequired(), this.server.getResourcePackPrompt());
        }

        for(MobEffect mobEffectInstance : player.getEffects()) {
            serverGamePacketListenerImpl.sendPacket(new PacketPlayOutEntityEffect(player.getId(), mobEffectInstance));
        }

        if (compoundTag != null && compoundTag.hasKeyOfType("RootVehicle", 10)) {
            NBTTagCompound compoundTag2 = compoundTag.getCompound("RootVehicle");
            Entity entity = EntityTypes.loadEntityRecursive(compoundTag2.getCompound("Entity"), serverLevel2, (vehicle) -> {
                return !serverLevel2.addEntitySerialized(vehicle) ? null : vehicle;
            });
            if (entity != null) {
                UUID uUID;
                if (compoundTag2.hasUUID("Attach")) {
                    uUID = compoundTag2.getUUID("Attach");
                } else {
                    uUID = null;
                }

                if (entity.getUniqueID().equals(uUID)) {
                    player.startRiding(entity, true);
                } else {
                    for(Entity entity2 : entity.getAllPassengers()) {
                        if (entity2.getUniqueID().equals(uUID)) {
                            player.startRiding(entity2, true);
                            break;
                        }
                    }
                }

                if (!player.isPassenger()) {
                    LOGGER.warn("Couldn't reattach entity to player");
                    entity.die();

                    for(Entity entity3 : entity.getAllPassengers()) {
                        entity3.die();
                    }
                }
            }
        }

        player.syncInventory();
    }

    public void sendScoreboard(ScoreboardServer scoreboard, EntityPlayer player) {
        Set<ScoreboardObjective> set = Sets.newHashSet();

        for(ScoreboardTeam playerTeam : scoreboard.getTeams()) {
            player.connection.sendPacket(PacketPlayOutScoreboardTeam.createAddOrModifyPacket(playerTeam, true));
        }

        for(int i = 0; i < 19; ++i) {
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(i);
            if (objective != null && !set.contains(objective)) {
                for(Packet<?> packet : scoreboard.getScoreboardScorePacketsForObjective(objective)) {
                    player.connection.sendPacket(packet);
                }

                set.add(objective);
            }
        }

    }

    public void setPlayerFileData(WorldServer world) {
        world.getWorldBorder().addListener(new IWorldBorderListener() {
            @Override
            public void onBorderSizeSet(WorldBorder border, double size) {
                PlayerList.this.sendAll(new ClientboundSetBorderSizePacket(border));
            }

            @Override
            public void onBorderSizeLerping(WorldBorder border, double fromSize, double toSize, long time) {
                PlayerList.this.sendAll(new ClientboundSetBorderLerpSizePacket(border));
            }

            @Override
            public void onBorderCenterSet(WorldBorder border, double centerX, double centerZ) {
                PlayerList.this.sendAll(new ClientboundSetBorderCenterPacket(border));
            }

            @Override
            public void onBorderSetWarningTime(WorldBorder border, int warningTime) {
                PlayerList.this.sendAll(new ClientboundSetBorderWarningDelayPacket(border));
            }

            @Override
            public void onBorderSetWarningBlocks(WorldBorder border, int warningBlockDistance) {
                PlayerList.this.sendAll(new ClientboundSetBorderWarningDistancePacket(border));
            }

            @Override
            public void onBorderSetDamagePerBlock(WorldBorder border, double damagePerBlock) {
            }

            @Override
            public void onBorderSetDamageSafeZOne(WorldBorder border, double safeZoneRadius) {
            }
        });
    }

    @Nullable
    public NBTTagCompound load(EntityPlayer player) {
        NBTTagCompound compoundTag = this.server.getSaveData().getLoadedPlayerTag();
        NBTTagCompound compoundTag2;
        if (player.getDisplayName().getString().equals(this.server.getSinglePlayerName()) && compoundTag != null) {
            compoundTag2 = compoundTag;
            player.load(compoundTag);
            LOGGER.debug("loading single player");
        } else {
            compoundTag2 = this.playerIo.load(player);
        }

        return compoundTag2;
    }

    protected void savePlayerFile(EntityPlayer player) {
        this.playerIo.save(player);
        ServerStatisticManager serverStatsCounter = this.stats.get(player.getUniqueID());
        if (serverStatsCounter != null) {
            serverStatsCounter.save();
        }

        AdvancementDataPlayer playerAdvancements = this.advancements.get(player.getUniqueID());
        if (playerAdvancements != null) {
            playerAdvancements.save();
        }

    }

    public void disconnect(EntityPlayer player) {
        WorldServer serverLevel = player.getWorldServer();
        player.awardStat(StatisticList.LEAVE_GAME);
        this.savePlayerFile(player);
        if (player.isPassenger()) {
            Entity entity = player.getRootVehicle();
            if (entity.hasSinglePlayerPassenger()) {
                LOGGER.debug("Removing player mount");
                player.stopRiding();
                entity.getPassengersAndSelf().forEach((entity) -> {
                    entity.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
                });
            }
        }

        player.decouple();
        serverLevel.removePlayerImmediately(player, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        player.getAdvancementData().stopListening();
        this.players.remove(player);
        this.server.getBossBattleCustomData().onPlayerDisconnect(player);
        UUID uUID = player.getUniqueID();
        EntityPlayer serverPlayer = this.playersByUUID.get(uUID);
        if (serverPlayer == player) {
            this.playersByUUID.remove(uUID);
            this.stats.remove(uUID);
            this.advancements.remove(uUID);
        }

        this.sendAll(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, player));
    }

    @Nullable
    public IChatBaseComponent attemptLogin(SocketAddress address, GameProfile profile) {
        if (this.bans.isBanned(profile)) {
            GameProfileBanEntry userBanListEntry = this.bans.get(profile);
            IChatMutableComponent mutableComponent = new ChatMessage("multiplayer.disconnect.banned.reason", userBanListEntry.getReason());
            if (userBanListEntry.getExpires() != null) {
                mutableComponent.addSibling(new ChatMessage("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(userBanListEntry.getExpires())));
            }

            return mutableComponent;
        } else if (!this.isWhitelisted(profile)) {
            return new ChatMessage("multiplayer.disconnect.not_whitelisted");
        } else if (this.ipBans.isBanned(address)) {
            IpBanEntry ipBanListEntry = this.ipBans.get(address);
            IChatMutableComponent mutableComponent2 = new ChatMessage("multiplayer.disconnect.banned_ip.reason", ipBanListEntry.getReason());
            if (ipBanListEntry.getExpires() != null) {
                mutableComponent2.addSibling(new ChatMessage("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(ipBanListEntry.getExpires())));
            }

            return mutableComponent2;
        } else {
            return this.players.size() >= this.maxPlayers && !this.canBypassPlayerLimit(profile) ? new ChatMessage("multiplayer.disconnect.server_full") : null;
        }
    }

    public EntityPlayer processLogin(GameProfile profile) {
        UUID uUID = EntityHuman.createPlayerUUID(profile);
        List<EntityPlayer> list = Lists.newArrayList();

        for(int i = 0; i < this.players.size(); ++i) {
            EntityPlayer serverPlayer = this.players.get(i);
            if (serverPlayer.getUniqueID().equals(uUID)) {
                list.add(serverPlayer);
            }
        }

        EntityPlayer serverPlayer2 = this.playersByUUID.get(profile.getId());
        if (serverPlayer2 != null && !list.contains(serverPlayer2)) {
            list.add(serverPlayer2);
        }

        for(EntityPlayer serverPlayer3 : list) {
            serverPlayer3.connection.disconnect(new ChatMessage("multiplayer.disconnect.duplicate_login"));
        }

        return new EntityPlayer(this.server, this.server.overworld(), profile);
    }

    public EntityPlayer moveToWorld(EntityPlayer player, boolean alive) {
        this.players.remove(player);
        player.getWorldServer().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        BlockPosition blockPos = player.getSpawn();
        float f = player.getSpawnAngle();
        boolean bl = player.isSpawnForced();
        WorldServer serverLevel = this.server.getWorldServer(player.getSpawnDimension());
        Optional<Vec3D> optional;
        if (serverLevel != null && blockPos != null) {
            optional = EntityHuman.getBed(serverLevel, blockPos, f, bl, alive);
        } else {
            optional = Optional.empty();
        }

        WorldServer serverLevel2 = serverLevel != null && optional.isPresent() ? serverLevel : this.server.overworld();
        EntityPlayer serverPlayer = new EntityPlayer(this.server, serverLevel2, player.getProfile());
        serverPlayer.connection = player.connection;
        serverPlayer.copyFrom(player, alive);
        serverPlayer.setId(player.getId());
        serverPlayer.setMainArm(player.getMainHand());

        for(String string : player.getScoreboardTags()) {
            serverPlayer.addScoreboardTag(string);
        }

        boolean bl2 = false;
        if (optional.isPresent()) {
            IBlockData blockState = serverLevel2.getType(blockPos);
            boolean bl3 = blockState.is(Blocks.RESPAWN_ANCHOR);
            Vec3D vec3 = optional.get();
            float h;
            if (!blockState.is(TagsBlock.BEDS) && !bl3) {
                h = f;
            } else {
                Vec3D vec32 = Vec3D.atBottomCenterOf(blockPos).subtract(vec3).normalize();
                h = (float)MathHelper.wrapDegrees(MathHelper.atan2(vec32.z, vec32.x) * (double)(180F / (float)Math.PI) - 90.0D);
            }

            serverPlayer.setPositionRotation(vec3.x, vec3.y, vec3.z, h, 0.0F);
            serverPlayer.setRespawnPosition(serverLevel2.getDimensionKey(), blockPos, f, bl, false);
            bl2 = !alive && bl3;
        } else if (blockPos != null) {
            serverPlayer.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
        }

        while(!serverLevel2.getCubes(serverPlayer) && serverPlayer.locY() < (double)serverLevel2.getMaxBuildHeight()) {
            serverPlayer.setPosition(serverPlayer.locX(), serverPlayer.locY() + 1.0D, serverPlayer.locZ());
        }

        WorldData levelData = serverPlayer.level.getWorldData();
        serverPlayer.connection.sendPacket(new PacketPlayOutRespawn(serverPlayer.level.getDimensionManager(), serverPlayer.level.getDimensionKey(), BiomeManager.obfuscateSeed(serverPlayer.getWorldServer().getSeed()), serverPlayer.gameMode.getGameMode(), serverPlayer.gameMode.getPreviousGameModeForPlayer(), serverPlayer.getWorldServer().isDebugWorld(), serverPlayer.getWorldServer().isFlatWorld(), alive));
        serverPlayer.connection.teleport(serverPlayer.locX(), serverPlayer.locY(), serverPlayer.locZ(), serverPlayer.getYRot(), serverPlayer.getXRot());
        serverPlayer.connection.sendPacket(new PacketPlayOutSpawnPosition(serverLevel2.getSpawn(), serverLevel2.getSharedSpawnAngle()));
        serverPlayer.connection.sendPacket(new PacketPlayOutServerDifficulty(levelData.getDifficulty(), levelData.isDifficultyLocked()));
        serverPlayer.connection.sendPacket(new PacketPlayOutExperience(serverPlayer.experienceProgress, serverPlayer.totalExperience, serverPlayer.experienceLevel));
        this.sendLevelInfo(serverPlayer, serverLevel2);
        this.sendPlayerPermissionLevel(serverPlayer);
        serverLevel2.addPlayerRespawn(serverPlayer);
        this.players.add(serverPlayer);
        this.playersByUUID.put(serverPlayer.getUniqueID(), serverPlayer);
        serverPlayer.syncInventory();
        serverPlayer.setHealth(serverPlayer.getHealth());
        if (bl2) {
            serverPlayer.connection.sendPacket(new PacketPlayOutNamedSoundEffect(SoundEffects.RESPAWN_ANCHOR_DEPLETE, SoundCategory.BLOCKS, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), 1.0F, 1.0F));
        }

        return serverPlayer;
    }

    public void sendPlayerPermissionLevel(EntityPlayer player) {
        GameProfile gameProfile = player.getProfile();
        int i = this.server.getProfilePermissions(gameProfile);
        this.sendPlayerPermissionLevel(player, i);
    }

    public void tick() {
        if (++this.sendAllPlayerInfoIn > 600) {
            this.sendAll(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_LATENCY, this.players));
            this.sendAllPlayerInfoIn = 0;
        }

    }

    public void sendAll(Packet<?> packet) {
        for(EntityPlayer serverPlayer : this.players) {
            serverPlayer.connection.sendPacket(packet);
        }

    }

    public void broadcastAll(Packet<?> packet, ResourceKey<World> dimension) {
        for(EntityPlayer serverPlayer : this.players) {
            if (serverPlayer.level.getDimensionKey() == dimension) {
                serverPlayer.connection.sendPacket(packet);
            }
        }

    }

    public void broadcastToTeam(EntityHuman source, IChatBaseComponent message) {
        ScoreboardTeamBase team = source.getScoreboardTeam();
        if (team != null) {
            for(String string : team.getPlayerNameSet()) {
                EntityPlayer serverPlayer = this.getPlayer(string);
                if (serverPlayer != null && serverPlayer != source) {
                    serverPlayer.sendMessage(message, source.getUniqueID());
                }
            }

        }
    }

    public void broadcastToAllExceptTeam(EntityHuman source, IChatBaseComponent message) {
        ScoreboardTeamBase team = source.getScoreboardTeam();
        if (team == null) {
            this.sendMessage(message, ChatMessageType.SYSTEM, source.getUniqueID());
        } else {
            for(int i = 0; i < this.players.size(); ++i) {
                EntityPlayer serverPlayer = this.players.get(i);
                if (serverPlayer.getScoreboardTeam() != team) {
                    serverPlayer.sendMessage(message, source.getUniqueID());
                }
            }

        }
    }

    public String[] getPlayerNamesArray() {
        String[] strings = new String[this.players.size()];

        for(int i = 0; i < this.players.size(); ++i) {
            strings[i] = this.players.get(i).getProfile().getName();
        }

        return strings;
    }

    public GameProfileBanList getProfileBans() {
        return this.bans;
    }

    public IpBanList getIPBans() {
        return this.ipBans;
    }

    public void addOp(GameProfile profile) {
        this.ops.add(new OpListEntry(profile, this.server.getOperatorUserPermissionLevel(), this.ops.canBypassPlayerLimit(profile)));
        EntityPlayer serverPlayer = this.getPlayer(profile.getId());
        if (serverPlayer != null) {
            this.sendPlayerPermissionLevel(serverPlayer);
        }

    }

    public void removeOp(GameProfile profile) {
        this.ops.remove(profile);
        EntityPlayer serverPlayer = this.getPlayer(profile.getId());
        if (serverPlayer != null) {
            this.sendPlayerPermissionLevel(serverPlayer);
        }

    }

    private void sendPlayerPermissionLevel(EntityPlayer player, int permissionLevel) {
        if (player.connection != null) {
            byte b;
            if (permissionLevel <= 0) {
                b = 24;
            } else if (permissionLevel >= 4) {
                b = 28;
            } else {
                b = (byte)(24 + permissionLevel);
            }

            player.connection.sendPacket(new PacketPlayOutEntityStatus(player, b));
        }

        this.server.getCommandDispatcher().sendCommands(player);
    }

    public boolean isWhitelisted(GameProfile profile) {
        return !this.doWhiteList || this.ops.contains(profile) || this.whitelist.contains(profile);
    }

    public boolean isOp(GameProfile profile) {
        return this.ops.contains(profile) || this.server.isSingleplayerOwner(profile) && this.server.getSaveData().getAllowCommands() || this.allowCheatsForAllPlayers;
    }

    @Nullable
    public EntityPlayer getPlayer(String name) {
        for(EntityPlayer serverPlayer : this.players) {
            if (serverPlayer.getProfile().getName().equalsIgnoreCase(name)) {
                return serverPlayer;
            }
        }

        return null;
    }

    public void sendPacketNearby(@Nullable EntityHuman player, double x, double y, double z, double distance, ResourceKey<World> worldKey, Packet<?> packet) {
        for(int i = 0; i < this.players.size(); ++i) {
            EntityPlayer serverPlayer = this.players.get(i);
            if (serverPlayer != player && serverPlayer.level.getDimensionKey() == worldKey) {
                double d = x - serverPlayer.locX();
                double e = y - serverPlayer.locY();
                double f = z - serverPlayer.locZ();
                if (d * d + e * e + f * f < distance * distance) {
                    serverPlayer.connection.sendPacket(packet);
                }
            }
        }

    }

    public void savePlayers() {
        for(int i = 0; i < this.players.size(); ++i) {
            this.savePlayerFile(this.players.get(i));
        }

    }

    public WhiteList getWhitelist() {
        return this.whitelist;
    }

    public String[] getWhitelisted() {
        return this.whitelist.getEntries();
    }

    public OpList getOPs() {
        return this.ops;
    }

    public String[] getOpNames() {
        return this.ops.getEntries();
    }

    public void reloadWhitelist() {
    }

    public void sendLevelInfo(EntityPlayer player, WorldServer world) {
        WorldBorder worldBorder = this.server.overworld().getWorldBorder();
        player.connection.sendPacket(new ClientboundInitializeBorderPacket(worldBorder));
        player.connection.sendPacket(new PacketPlayOutUpdateTime(world.getTime(), world.getDayTime(), world.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
        player.connection.sendPacket(new PacketPlayOutSpawnPosition(world.getSpawn(), world.getSharedSpawnAngle()));
        if (world.isRaining()) {
            player.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.START_RAINING, 0.0F));
            player.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.RAIN_LEVEL_CHANGE, world.getRainLevel(1.0F)));
            player.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.THUNDER_LEVEL_CHANGE, world.getThunderLevel(1.0F)));
        }

    }

    public void updateClient(EntityPlayer player) {
        player.inventoryMenu.updateInventory();
        player.triggerHealthUpdate();
        player.connection.sendPacket(new PacketPlayOutHeldItemSlot(player.getInventory().selected));
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public boolean getHasWhitelist() {
        return this.doWhiteList;
    }

    public void setHasWhitelist(boolean whitelistEnabled) {
        this.doWhiteList = whitelistEnabled;
    }

    public List<EntityPlayer> getPlayersWithAddress(String ip) {
        List<EntityPlayer> list = Lists.newArrayList();

        for(EntityPlayer serverPlayer : this.players) {
            if (serverPlayer.getIpAddress().equals(ip)) {
                list.add(serverPlayer);
            }
        }

        return list;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public NBTTagCompound save() {
        return null;
    }

    public void setAllowCheatsForAllPlayers(boolean cheatsAllowed) {
        this.allowCheatsForAllPlayers = cheatsAllowed;
    }

    public void shutdown() {
        for(int i = 0; i < this.players.size(); ++i) {
            (this.players.get(i)).connection.disconnect(new ChatMessage("multiplayer.disconnect.server_shutdown"));
        }

    }

    public void sendMessage(IChatBaseComponent message, ChatMessageType type, UUID sender) {
        this.server.sendMessage(message, sender);

        for(EntityPlayer serverPlayer : this.players) {
            serverPlayer.sendMessage(message, type, sender);
        }

    }

    public void broadcastMessage(IChatBaseComponent serverMessage, Function<EntityPlayer, IChatBaseComponent> playerMessageFactory, ChatMessageType playerMessageType, UUID sender) {
        this.server.sendMessage(serverMessage, sender);

        for(EntityPlayer serverPlayer : this.players) {
            IChatBaseComponent component = playerMessageFactory.apply(serverPlayer);
            if (component != null) {
                serverPlayer.sendMessage(component, playerMessageType, sender);
            }
        }

    }

    public ServerStatisticManager getStatisticManager(EntityHuman player) {
        UUID uUID = player.getUniqueID();
        ServerStatisticManager serverStatsCounter = uUID == null ? null : this.stats.get(uUID);
        if (serverStatsCounter == null) {
            File file = this.server.getWorldPath(SavedFile.PLAYER_STATS_DIR).toFile();
            File file2 = new File(file, uUID + ".json");
            if (!file2.exists()) {
                File file3 = new File(file, player.getDisplayName().getString() + ".json");
                Path path = file3.toPath();
                if (FileUtils.isPathNormalized(path) && FileUtils.isPathPortable(path) && path.startsWith(file.getPath()) && file3.isFile()) {
                    file3.renameTo(file2);
                }
            }

            serverStatsCounter = new ServerStatisticManager(this.server, file2);
            this.stats.put(uUID, serverStatsCounter);
        }

        return serverStatsCounter;
    }

    public AdvancementDataPlayer getPlayerAdvancements(EntityPlayer player) {
        UUID uUID = player.getUniqueID();
        AdvancementDataPlayer playerAdvancements = this.advancements.get(uUID);
        if (playerAdvancements == null) {
            File file = this.server.getWorldPath(SavedFile.PLAYER_ADVANCEMENTS_DIR).toFile();
            File file2 = new File(file, uUID + ".json");
            playerAdvancements = new AdvancementDataPlayer(this.server.getDataFixer(), this, this.server.getAdvancementData(), file2, player);
            this.advancements.put(uUID, playerAdvancements);
        }

        playerAdvancements.setPlayer(player);
        return playerAdvancements;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
        this.sendAll(new PacketPlayOutViewDistance(viewDistance));

        for(WorldServer serverLevel : this.server.getWorlds()) {
            if (serverLevel != null) {
                serverLevel.getChunkSource().setViewDistance(viewDistance);
            }
        }

    }

    public List<EntityPlayer> getPlayers() {
        return this.players;
    }

    @Nullable
    public EntityPlayer getPlayer(UUID uuid) {
        return this.playersByUUID.get(uuid);
    }

    public boolean canBypassPlayerLimit(GameProfile profile) {
        return false;
    }

    public void reload() {
        for(AdvancementDataPlayer playerAdvancements : this.advancements.values()) {
            playerAdvancements.reload(this.server.getAdvancementData());
        }

        this.sendAll(new PacketPlayOutTags(this.server.getTagRegistry().serializeToNetwork(this.registryHolder)));
        PacketPlayOutRecipeUpdate clientboundUpdateRecipesPacket = new PacketPlayOutRecipeUpdate(this.server.getCraftingManager().getRecipes());

        for(EntityPlayer serverPlayer : this.players) {
            serverPlayer.connection.sendPacket(clientboundUpdateRecipesPacket);
            serverPlayer.getRecipeBook().sendInitialRecipeBook(serverPlayer);
        }

    }

    public boolean isAllowCheatsForAllPlayers() {
        return this.allowCheatsForAllPlayers;
    }
}
