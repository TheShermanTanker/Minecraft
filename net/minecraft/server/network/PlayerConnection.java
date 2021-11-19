package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.EnumChatFormat;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PlayerConnectionUtils;
import net.minecraft.network.protocol.game.PacketListenerPlayIn;
import net.minecraft.network.protocol.game.PacketPlayInAbilities;
import net.minecraft.network.protocol.game.PacketPlayInAdvancements;
import net.minecraft.network.protocol.game.PacketPlayInArmAnimation;
import net.minecraft.network.protocol.game.PacketPlayInAutoRecipe;
import net.minecraft.network.protocol.game.PacketPlayInBEdit;
import net.minecraft.network.protocol.game.PacketPlayInBeacon;
import net.minecraft.network.protocol.game.PacketPlayInBlockDig;
import net.minecraft.network.protocol.game.PacketPlayInBlockPlace;
import net.minecraft.network.protocol.game.PacketPlayInBoatMove;
import net.minecraft.network.protocol.game.PacketPlayInChat;
import net.minecraft.network.protocol.game.PacketPlayInClientCommand;
import net.minecraft.network.protocol.game.PacketPlayInCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayInCustomPayload;
import net.minecraft.network.protocol.game.PacketPlayInDifficultyChange;
import net.minecraft.network.protocol.game.PacketPlayInDifficultyLock;
import net.minecraft.network.protocol.game.PacketPlayInEnchantItem;
import net.minecraft.network.protocol.game.PacketPlayInEntityAction;
import net.minecraft.network.protocol.game.PacketPlayInEntityNBTQuery;
import net.minecraft.network.protocol.game.PacketPlayInFlying;
import net.minecraft.network.protocol.game.PacketPlayInHeldItemSlot;
import net.minecraft.network.protocol.game.PacketPlayInItemName;
import net.minecraft.network.protocol.game.PacketPlayInJigsawGenerate;
import net.minecraft.network.protocol.game.PacketPlayInKeepAlive;
import net.minecraft.network.protocol.game.PacketPlayInPickItem;
import net.minecraft.network.protocol.game.PacketPlayInPong;
import net.minecraft.network.protocol.game.PacketPlayInRecipeDisplayed;
import net.minecraft.network.protocol.game.PacketPlayInRecipeSettings;
import net.minecraft.network.protocol.game.PacketPlayInResourcePackStatus;
import net.minecraft.network.protocol.game.PacketPlayInSetCommandBlock;
import net.minecraft.network.protocol.game.PacketPlayInSetCommandMinecart;
import net.minecraft.network.protocol.game.PacketPlayInSetCreativeSlot;
import net.minecraft.network.protocol.game.PacketPlayInSetJigsaw;
import net.minecraft.network.protocol.game.PacketPlayInSettings;
import net.minecraft.network.protocol.game.PacketPlayInSpectate;
import net.minecraft.network.protocol.game.PacketPlayInSteerVehicle;
import net.minecraft.network.protocol.game.PacketPlayInStruct;
import net.minecraft.network.protocol.game.PacketPlayInTabComplete;
import net.minecraft.network.protocol.game.PacketPlayInTeleportAccept;
import net.minecraft.network.protocol.game.PacketPlayInTileNBTQuery;
import net.minecraft.network.protocol.game.PacketPlayInTrSel;
import net.minecraft.network.protocol.game.PacketPlayInUpdateSign;
import net.minecraft.network.protocol.game.PacketPlayInUseEntity;
import net.minecraft.network.protocol.game.PacketPlayInUseItem;
import net.minecraft.network.protocol.game.PacketPlayInVehicleMove;
import net.minecraft.network.protocol.game.PacketPlayInWindowClick;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutChat;
import net.minecraft.network.protocol.game.PacketPlayOutHeldItemSlot;
import net.minecraft.network.protocol.game.PacketPlayOutKeepAlive;
import net.minecraft.network.protocol.game.PacketPlayOutKickDisconnect;
import net.minecraft.network.protocol.game.PacketPlayOutNBTQuery;
import net.minecraft.network.protocol.game.PacketPlayOutPosition;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.network.protocol.game.PacketPlayOutTabComplete;
import net.minecraft.network.protocol.game.PacketPlayOutVehicleMove;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.UtilColor;
import net.minecraft.util.thread.IAsyncTaskHandler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.IJumpable;
import net.minecraft.world.entity.animal.horse.EntityHorseAbstract;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.EnumChatVisibility;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.inventory.ContainerBeacon;
import net.minecraft.world.inventory.ContainerMerchant;
import net.minecraft.world.inventory.ContainerRecipeBook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemBucket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.CommandBlockListenerAbstract;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockCommand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityCommand;
import net.minecraft.world.level.block.entity.TileEntityJigsaw;
import net.minecraft.world.level.block.entity.TileEntitySign;
import net.minecraft.world.level.block.entity.TileEntityStructure;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlayerConnection implements PlayerConnectionServer, PacketListenerPlayIn {
    static final Logger LOGGER = LogManager.getLogger();
    private static final int LATENCY_CHECK_INTERVAL = 15000;
    public final NetworkManager connection;
    private final MinecraftServer server;
    public EntityPlayer player;
    private int tickCount;
    private long keepAliveTime;
    private boolean keepAlivePending;
    private long keepAliveChallenge;
    private int chatSpamTickCount;
    private int dropSpamTickCount;
    private double firstGoodX;
    private double firstGoodY;
    private double firstGoodZ;
    private double lastGoodX;
    private double lastGoodY;
    private double lastGoodZ;
    @Nullable
    private Entity lastVehicle;
    private double vehicleFirstGoodX;
    private double vehicleFirstGoodY;
    private double vehicleFirstGoodZ;
    private double vehicleLastGoodX;
    private double vehicleLastGoodY;
    private double vehicleLastGoodZ;
    @Nullable
    private Vec3D awaitingPositionFromClient;
    private int awaitingTeleport;
    private int awaitingTeleportTime;
    private boolean clientIsFloating;
    private int aboveGroundTickCount;
    private boolean clientVehicleIsFloating;
    private int aboveGroundVehicleTickCount;
    private int receivedMovePacketCount;
    private int knownMovePacketCount;

    public PlayerConnection(MinecraftServer server, NetworkManager connection, EntityPlayer player) {
        this.server = server;
        this.connection = connection;
        connection.setPacketListener(this);
        this.player = player;
        player.connection = this;
        player.getTextFilter().join();
    }

    public void tick() {
        this.syncPosition();
        this.player.xo = this.player.locX();
        this.player.yo = this.player.locY();
        this.player.zo = this.player.locZ();
        this.player.playerTick();
        this.player.setLocation(this.firstGoodX, this.firstGoodY, this.firstGoodZ, this.player.getYRot(), this.player.getXRot());
        ++this.tickCount;
        this.knownMovePacketCount = this.receivedMovePacketCount;
        if (this.clientIsFloating && !this.player.isSleeping()) {
            if (++this.aboveGroundTickCount > 80) {
                LOGGER.warn("{} was kicked for floating too long!", (Object)this.player.getDisplayName().getString());
                this.disconnect(new ChatMessage("multiplayer.disconnect.flying"));
                return;
            }
        } else {
            this.clientIsFloating = false;
            this.aboveGroundTickCount = 0;
        }

        this.lastVehicle = this.player.getRootVehicle();
        if (this.lastVehicle != this.player && this.lastVehicle.getRidingPassenger() == this.player) {
            this.vehicleFirstGoodX = this.lastVehicle.locX();
            this.vehicleFirstGoodY = this.lastVehicle.locY();
            this.vehicleFirstGoodZ = this.lastVehicle.locZ();
            this.vehicleLastGoodX = this.lastVehicle.locX();
            this.vehicleLastGoodY = this.lastVehicle.locY();
            this.vehicleLastGoodZ = this.lastVehicle.locZ();
            if (this.clientVehicleIsFloating && this.player.getRootVehicle().getRidingPassenger() == this.player) {
                if (++this.aboveGroundVehicleTickCount > 80) {
                    LOGGER.warn("{} was kicked for floating a vehicle too long!", (Object)this.player.getDisplayName().getString());
                    this.disconnect(new ChatMessage("multiplayer.disconnect.flying"));
                    return;
                }
            } else {
                this.clientVehicleIsFloating = false;
                this.aboveGroundVehicleTickCount = 0;
            }
        } else {
            this.lastVehicle = null;
            this.clientVehicleIsFloating = false;
            this.aboveGroundVehicleTickCount = 0;
        }

        this.server.getMethodProfiler().enter("keepAlive");
        long l = SystemUtils.getMonotonicMillis();
        if (l - this.keepAliveTime >= 15000L) {
            if (this.keepAlivePending) {
                this.disconnect(new ChatMessage("disconnect.timeout"));
            } else {
                this.keepAlivePending = true;
                this.keepAliveTime = l;
                this.keepAliveChallenge = l;
                this.sendPacket(new PacketPlayOutKeepAlive(this.keepAliveChallenge));
            }
        }

        this.server.getMethodProfiler().exit();
        if (this.chatSpamTickCount > 0) {
            --this.chatSpamTickCount;
        }

        if (this.dropSpamTickCount > 0) {
            --this.dropSpamTickCount;
        }

        if (this.player.getLastActionTime() > 0L && this.server.getIdleTimeout() > 0 && SystemUtils.getMonotonicMillis() - this.player.getLastActionTime() > (long)(this.server.getIdleTimeout() * 1000 * 60)) {
            this.disconnect(new ChatMessage("multiplayer.disconnect.idling"));
        }

    }

    public void syncPosition() {
        this.firstGoodX = this.player.locX();
        this.firstGoodY = this.player.locY();
        this.firstGoodZ = this.player.locZ();
        this.lastGoodX = this.player.locX();
        this.lastGoodY = this.player.locY();
        this.lastGoodZ = this.player.locZ();
    }

    @Override
    public NetworkManager getConnection() {
        return this.connection;
    }

    private boolean isExemptPlayer() {
        return this.server.isSingleplayerOwner(this.player.getProfile());
    }

    public void disconnect(IChatBaseComponent reason) {
        this.connection.send(new PacketPlayOutKickDisconnect(reason), (future) -> {
            this.connection.close(reason);
        });
        this.connection.stopReading();
        this.server.executeSync(this.connection::handleDisconnection);
    }

    private <T, R> void filterTextPacket(T text, Consumer<R> consumer, BiFunction<ITextFilter, T, CompletableFuture<R>> backingFilterer) {
        IAsyncTaskHandler<?> blockableEventLoop = this.player.getWorldServer().getMinecraftServer();
        Consumer<R> consumer2 = (object) -> {
            if (this.getConnection().isConnected()) {
                consumer.accept(object);
            } else {
                LOGGER.debug("Ignoring packet due to disconnection");
            }

        };
        backingFilterer.apply(this.player.getTextFilter(), text).thenAcceptAsync(consumer2, blockableEventLoop);
    }

    private void filterTextPacket(String text, Consumer<ITextFilter.FilteredText> consumer) {
        this.filterTextPacket(text, consumer, ITextFilter::processStreamMessage);
    }

    private void filterTextPacket(List<String> texts, Consumer<List<ITextFilter.FilteredText>> consumer) {
        this.filterTextPacket(texts, consumer, ITextFilter::processMessageBundle);
    }

    @Override
    public void handlePlayerInput(PacketPlayInSteerVehicle packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.player.setPlayerInput(packet.getXxa(), packet.getZza(), packet.isJumping(), packet.isShiftKeyDown());
    }

    private static boolean containsInvalidValues(double x, double y, double z, float yaw, float pitch) {
        return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || !Floats.isFinite(pitch) || !Floats.isFinite(yaw);
    }

    private static double clampHorizontal(double d) {
        return MathHelper.clamp(d, -3.0E7D, 3.0E7D);
    }

    private static double clampVertical(double d) {
        return MathHelper.clamp(d, -2.0E7D, 2.0E7D);
    }

    @Override
    public void handleMoveVehicle(PacketPlayInVehicleMove packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (containsInvalidValues(packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch())) {
            this.disconnect(new ChatMessage("multiplayer.disconnect.invalid_vehicle_movement"));
        } else {
            Entity entity = this.player.getRootVehicle();
            if (entity != this.player && entity.getRidingPassenger() == this.player && entity == this.lastVehicle) {
                WorldServer serverLevel = this.player.getWorldServer();
                double d = entity.locX();
                double e = entity.locY();
                double f = entity.locZ();
                double g = clampHorizontal(packet.getX());
                double h = clampVertical(packet.getY());
                double i = clampHorizontal(packet.getZ());
                float j = MathHelper.wrapDegrees(packet.getYaw());
                float k = MathHelper.wrapDegrees(packet.getPitch());
                double l = g - this.vehicleFirstGoodX;
                double m = h - this.vehicleFirstGoodY;
                double n = i - this.vehicleFirstGoodZ;
                double o = entity.getMot().lengthSqr();
                double p = l * l + m * m + n * n;
                if (p - o > 100.0D && !this.isExemptPlayer()) {
                    LOGGER.warn("{} (vehicle of {}) moved too quickly! {},{},{}", entity.getDisplayName().getString(), this.player.getDisplayName().getString(), l, m, n);
                    this.connection.sendPacket(new PacketPlayOutVehicleMove(entity));
                    return;
                }

                boolean bl = serverLevel.getCubes(entity, entity.getBoundingBox().shrink(0.0625D));
                l = g - this.vehicleLastGoodX;
                m = h - this.vehicleLastGoodY - 1.0E-6D;
                n = i - this.vehicleLastGoodZ;
                entity.move(EnumMoveType.PLAYER, new Vec3D(l, m, n));
                l = g - entity.locX();
                m = h - entity.locY();
                if (m > -0.5D || m < 0.5D) {
                    m = 0.0D;
                }

                n = i - entity.locZ();
                p = l * l + m * m + n * n;
                boolean bl2 = false;
                if (p > 0.0625D) {
                    bl2 = true;
                    LOGGER.warn("{} (vehicle of {}) moved wrongly! {}", entity.getDisplayName().getString(), this.player.getDisplayName().getString(), Math.sqrt(p));
                }

                entity.setLocation(g, h, i, j, k);
                boolean bl3 = serverLevel.getCubes(entity, entity.getBoundingBox().shrink(0.0625D));
                if (bl && (bl2 || !bl3)) {
                    entity.setLocation(d, e, f, j, k);
                    this.connection.sendPacket(new PacketPlayOutVehicleMove(entity));
                    return;
                }

                this.player.getWorldServer().getChunkSource().movePlayer(this.player);
                this.player.checkMovement(this.player.locX() - d, this.player.locY() - e, this.player.locZ() - f);
                this.clientVehicleIsFloating = m >= -0.03125D && !this.server.getAllowFlight() && this.noBlocksAround(entity);
                this.vehicleLastGoodX = entity.locX();
                this.vehicleLastGoodY = entity.locY();
                this.vehicleLastGoodZ = entity.locZ();
            }

        }
    }

    private boolean noBlocksAround(Entity entity) {
        return entity.level.getBlockStates(entity.getBoundingBox().inflate(0.0625D).expandTowards(0.0D, -0.55D, 0.0D)).allMatch(BlockBase.BlockData::isAir);
    }

    @Override
    public void handleAcceptTeleportPacket(PacketPlayInTeleportAccept packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (packet.getId() == this.awaitingTeleport) {
            this.player.setLocation(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot());
            this.lastGoodX = this.awaitingPositionFromClient.x;
            this.lastGoodY = this.awaitingPositionFromClient.y;
            this.lastGoodZ = this.awaitingPositionFromClient.z;
            if (this.player.isChangingDimension()) {
                this.player.hasChangedDimension();
            }

            this.awaitingPositionFromClient = null;
        }

    }

    @Override
    public void handleRecipeBookSeenRecipePacket(PacketPlayInRecipeDisplayed packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.server.getCraftingManager().getRecipe(packet.getRecipe()).ifPresent(this.player.getRecipeBook()::removeHighlight);
    }

    @Override
    public void handleRecipeBookChangeSettingsPacket(PacketPlayInRecipeSettings packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.player.getRecipeBook().setBookSetting(packet.getBookType(), packet.isOpen(), packet.isFiltering());
    }

    @Override
    public void handleSeenAdvancements(PacketPlayInAdvancements packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (packet.getAction() == PacketPlayInAdvancements.Status.OPENED_TAB) {
            MinecraftKey resourceLocation = packet.getTab();
            Advancement advancement = this.server.getAdvancementData().getAdvancement(resourceLocation);
            if (advancement != null) {
                this.player.getAdvancementData().setSelectedTab(advancement);
            }
        }

    }

    @Override
    public void handleCustomCommandSuggestions(PacketPlayInTabComplete packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        StringReader stringReader = new StringReader(packet.getCommand());
        if (stringReader.canRead() && stringReader.peek() == '/') {
            stringReader.skip();
        }

        ParseResults<CommandListenerWrapper> parseResults = this.server.getCommandDispatcher().getDispatcher().parse(stringReader, this.player.getCommandListener());
        this.server.getCommandDispatcher().getDispatcher().getCompletionSuggestions(parseResults).thenAccept((suggestions) -> {
            this.connection.sendPacket(new PacketPlayOutTabComplete(packet.getId(), suggestions));
        });
    }

    @Override
    public void handleSetCommandBlock(PacketPlayInSetCommandBlock packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (!this.server.getEnableCommandBlock()) {
            this.player.sendMessage(new ChatMessage("advMode.notEnabled"), SystemUtils.NIL_UUID);
        } else if (!this.player.isCreativeAndOp()) {
            this.player.sendMessage(new ChatMessage("advMode.notAllowed"), SystemUtils.NIL_UUID);
        } else {
            CommandBlockListenerAbstract baseCommandBlock = null;
            TileEntityCommand commandBlockEntity = null;
            BlockPosition blockPos = packet.getPos();
            TileEntity blockEntity = this.player.level.getTileEntity(blockPos);
            if (blockEntity instanceof TileEntityCommand) {
                commandBlockEntity = (TileEntityCommand)blockEntity;
                baseCommandBlock = commandBlockEntity.getCommandBlock();
            }

            String string = packet.getCommand();
            boolean bl = packet.isTrackOutput();
            if (baseCommandBlock != null) {
                TileEntityCommand.Type mode = commandBlockEntity.getMode();
                IBlockData blockState = this.player.level.getType(blockPos);
                EnumDirection direction = blockState.get(BlockCommand.FACING);
                IBlockData blockState2;
                switch(packet.getMode()) {
                case SEQUENCE:
                    blockState2 = Blocks.CHAIN_COMMAND_BLOCK.getBlockData();
                    break;
                case AUTO:
                    blockState2 = Blocks.REPEATING_COMMAND_BLOCK.getBlockData();
                    break;
                case REDSTONE:
                default:
                    blockState2 = Blocks.COMMAND_BLOCK.getBlockData();
                }

                IBlockData blockState5 = blockState2.set(BlockCommand.FACING, direction).set(BlockCommand.CONDITIONAL, Boolean.valueOf(packet.isConditional()));
                if (blockState5 != blockState) {
                    this.player.level.setTypeAndData(blockPos, blockState5, 2);
                    blockEntity.setBlockState(blockState5);
                    this.player.level.getChunkAtWorldCoords(blockPos).setTileEntity(blockEntity);
                }

                baseCommandBlock.setCommand(string);
                baseCommandBlock.setTrackOutput(bl);
                if (!bl) {
                    baseCommandBlock.setLastOutput((IChatBaseComponent)null);
                }

                commandBlockEntity.setAutomatic(packet.isAutomatic());
                if (mode != packet.getMode()) {
                    commandBlockEntity.onModeSwitch();
                }

                baseCommandBlock.onUpdated();
                if (!UtilColor.isNullOrEmpty(string)) {
                    this.player.sendMessage(new ChatMessage("advMode.setCommand.success", string), SystemUtils.NIL_UUID);
                }
            }

        }
    }

    @Override
    public void handleSetCommandMinecart(PacketPlayInSetCommandMinecart packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (!this.server.getEnableCommandBlock()) {
            this.player.sendMessage(new ChatMessage("advMode.notEnabled"), SystemUtils.NIL_UUID);
        } else if (!this.player.isCreativeAndOp()) {
            this.player.sendMessage(new ChatMessage("advMode.notAllowed"), SystemUtils.NIL_UUID);
        } else {
            CommandBlockListenerAbstract baseCommandBlock = packet.getCommandBlock(this.player.level);
            if (baseCommandBlock != null) {
                baseCommandBlock.setCommand(packet.getCommand());
                baseCommandBlock.setTrackOutput(packet.isTrackOutput());
                if (!packet.isTrackOutput()) {
                    baseCommandBlock.setLastOutput((IChatBaseComponent)null);
                }

                baseCommandBlock.onUpdated();
                this.player.sendMessage(new ChatMessage("advMode.setCommand.success", packet.getCommand()), SystemUtils.NIL_UUID);
            }

        }
    }

    @Override
    public void handlePickItem(PacketPlayInPickItem packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.player.getInventory().pickSlot(packet.getSlot());
        this.player.connection.sendPacket(new PacketPlayOutSetSlot(-2, 0, this.player.getInventory().selected, this.player.getInventory().getItem(this.player.getInventory().selected)));
        this.player.connection.sendPacket(new PacketPlayOutSetSlot(-2, 0, packet.getSlot(), this.player.getInventory().getItem(packet.getSlot())));
        this.player.connection.sendPacket(new PacketPlayOutHeldItemSlot(this.player.getInventory().selected));
    }

    @Override
    public void handleRenameItem(PacketPlayInItemName packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (this.player.containerMenu instanceof ContainerAnvil) {
            ContainerAnvil anvilMenu = (ContainerAnvil)this.player.containerMenu;
            String string = SharedConstants.filterText(packet.getName());
            if (string.length() <= 50) {
                anvilMenu.setItemName(string);
            }
        }

    }

    @Override
    public void handleSetBeaconPacket(PacketPlayInBeacon packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (this.player.containerMenu instanceof ContainerBeacon) {
            ((ContainerBeacon)this.player.containerMenu).updateEffects(packet.getPrimary(), packet.getSecondary());
        }

    }

    @Override
    public void handleSetStructureBlock(PacketPlayInStruct packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (this.player.isCreativeAndOp()) {
            BlockPosition blockPos = packet.getPos();
            IBlockData blockState = this.player.level.getType(blockPos);
            TileEntity blockEntity = this.player.level.getTileEntity(blockPos);
            if (blockEntity instanceof TileEntityStructure) {
                TileEntityStructure structureBlockEntity = (TileEntityStructure)blockEntity;
                structureBlockEntity.setUsageMode(packet.getMode());
                structureBlockEntity.setStructureName(packet.getName());
                structureBlockEntity.setStructurePos(packet.getOffset());
                structureBlockEntity.setStructureSize(packet.getSize());
                structureBlockEntity.setMirror(packet.getMirror());
                structureBlockEntity.setRotation(packet.getRotation());
                structureBlockEntity.setMetaData(packet.getData());
                structureBlockEntity.setIgnoreEntities(packet.isIgnoreEntities());
                structureBlockEntity.setShowAir(packet.isShowAir());
                structureBlockEntity.setShowBoundingBox(packet.isShowBoundingBox());
                structureBlockEntity.setIntegrity(packet.getIntegrity());
                structureBlockEntity.setSeed(packet.getSeed());
                if (structureBlockEntity.hasStructureName()) {
                    String string = structureBlockEntity.getStructureName();
                    if (packet.getUpdateType() == TileEntityStructure.UpdateType.SAVE_AREA) {
                        if (structureBlockEntity.saveStructure()) {
                            this.player.displayClientMessage(new ChatMessage("structure_block.save_success", string), false);
                        } else {
                            this.player.displayClientMessage(new ChatMessage("structure_block.save_failure", string), false);
                        }
                    } else if (packet.getUpdateType() == TileEntityStructure.UpdateType.LOAD_AREA) {
                        if (!structureBlockEntity.isStructureLoadable()) {
                            this.player.displayClientMessage(new ChatMessage("structure_block.load_not_found", string), false);
                        } else if (structureBlockEntity.loadStructure(this.player.getWorldServer())) {
                            this.player.displayClientMessage(new ChatMessage("structure_block.load_success", string), false);
                        } else {
                            this.player.displayClientMessage(new ChatMessage("structure_block.load_prepare", string), false);
                        }
                    } else if (packet.getUpdateType() == TileEntityStructure.UpdateType.SCAN_AREA) {
                        if (structureBlockEntity.detectSize()) {
                            this.player.displayClientMessage(new ChatMessage("structure_block.size_success", string), false);
                        } else {
                            this.player.displayClientMessage(new ChatMessage("structure_block.size_failure"), false);
                        }
                    }
                } else {
                    this.player.displayClientMessage(new ChatMessage("structure_block.invalid_structure_name", packet.getName()), false);
                }

                structureBlockEntity.update();
                this.player.level.notify(blockPos, blockState, blockState, 3);
            }

        }
    }

    @Override
    public void handleSetJigsawBlock(PacketPlayInSetJigsaw packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (this.player.isCreativeAndOp()) {
            BlockPosition blockPos = packet.getPos();
            IBlockData blockState = this.player.level.getType(blockPos);
            TileEntity blockEntity = this.player.level.getTileEntity(blockPos);
            if (blockEntity instanceof TileEntityJigsaw) {
                TileEntityJigsaw jigsawBlockEntity = (TileEntityJigsaw)blockEntity;
                jigsawBlockEntity.setName(packet.getName());
                jigsawBlockEntity.setTarget(packet.getTarget());
                jigsawBlockEntity.setPool(packet.getPool());
                jigsawBlockEntity.setFinalState(packet.getFinalState());
                jigsawBlockEntity.setJoint(packet.getJoint());
                jigsawBlockEntity.update();
                this.player.level.notify(blockPos, blockState, blockState, 3);
            }

        }
    }

    @Override
    public void handleJigsawGenerate(PacketPlayInJigsawGenerate packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (this.player.isCreativeAndOp()) {
            BlockPosition blockPos = packet.getPos();
            TileEntity blockEntity = this.player.level.getTileEntity(blockPos);
            if (blockEntity instanceof TileEntityJigsaw) {
                TileEntityJigsaw jigsawBlockEntity = (TileEntityJigsaw)blockEntity;
                jigsawBlockEntity.generate(this.player.getWorldServer(), packet.levels(), packet.keepJigsaws());
            }

        }
    }

    @Override
    public void handleSelectTrade(PacketPlayInTrSel packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        int i = packet.getItem();
        Container abstractContainerMenu = this.player.containerMenu;
        if (abstractContainerMenu instanceof ContainerMerchant) {
            ContainerMerchant merchantMenu = (ContainerMerchant)abstractContainerMenu;
            merchantMenu.setSelectionHint(i);
            merchantMenu.tryMoveItems(i);
        }

    }

    @Override
    public void handleEditBook(PacketPlayInBEdit packet) {
        int i = packet.getSlot();
        if (PlayerInventory.isHotbarSlot(i) || i == 40) {
            List<String> list = Lists.newArrayList();
            Optional<String> optional = packet.getTitle();
            optional.ifPresent(list::add);
            packet.getPages().stream().limit(100L).forEach(list::add);
            this.filterTextPacket(list, optional.isPresent() ? (listx) -> {
                this.signBook(listx.get(0), listx.subList(1, listx.size()), i);
            } : (listx) -> {
                this.updateBookContents(listx, i);
            });
        }
    }

    private void updateBookContents(List<ITextFilter.FilteredText> pages, int slotId) {
        ItemStack itemStack = this.player.getInventory().getItem(slotId);
        if (itemStack.is(Items.WRITABLE_BOOK)) {
            this.updateBookPages(pages, UnaryOperator.identity(), itemStack);
        }
    }

    private void signBook(ITextFilter.FilteredText title, List<ITextFilter.FilteredText> pages, int slotId) {
        ItemStack itemStack = this.player.getInventory().getItem(slotId);
        if (itemStack.is(Items.WRITABLE_BOOK)) {
            ItemStack itemStack2 = new ItemStack(Items.WRITTEN_BOOK);
            NBTTagCompound compoundTag = itemStack.getTag();
            if (compoundTag != null) {
                itemStack2.setTag(compoundTag.c());
            }

            itemStack2.addTagElement("author", NBTTagString.valueOf(this.player.getDisplayName().getString()));
            if (this.player.isTextFilteringEnabled()) {
                itemStack2.addTagElement("title", NBTTagString.valueOf(title.getFiltered()));
            } else {
                itemStack2.addTagElement("filtered_title", NBTTagString.valueOf(title.getFiltered()));
                itemStack2.addTagElement("title", NBTTagString.valueOf(title.getRaw()));
            }

            this.updateBookPages(pages, (string) -> {
                return IChatBaseComponent.ChatSerializer.toJson(new ChatComponentText(string));
            }, itemStack2);
            this.player.getInventory().setItem(slotId, itemStack2);
        }
    }

    private void updateBookPages(List<ITextFilter.FilteredText> messages, UnaryOperator<String> postProcessor, ItemStack book) {
        NBTTagList listTag = new NBTTagList();
        if (this.player.isTextFilteringEnabled()) {
            messages.stream().map((message) -> {
                return NBTTagString.valueOf(postProcessor.apply(message.getFiltered()));
            }).forEach(listTag::add);
        } else {
            NBTTagCompound compoundTag = new NBTTagCompound();
            int i = 0;

            for(int j = messages.size(); i < j; ++i) {
                ITextFilter.FilteredText filteredText = messages.get(i);
                String string = filteredText.getRaw();
                listTag.add(NBTTagString.valueOf(postProcessor.apply(string)));
                String string2 = filteredText.getFiltered();
                if (!string.equals(string2)) {
                    compoundTag.setString(String.valueOf(i), postProcessor.apply(string2));
                }
            }

            if (!compoundTag.isEmpty()) {
                book.addTagElement("filtered_pages", compoundTag);
            }
        }

        book.addTagElement("pages", listTag);
    }

    @Override
    public void handleEntityTagQuery(PacketPlayInEntityNBTQuery packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (this.player.hasPermissions(2)) {
            Entity entity = this.player.getWorldServer().getEntity(packet.getEntityId());
            if (entity != null) {
                NBTTagCompound compoundTag = entity.save(new NBTTagCompound());
                this.player.connection.sendPacket(new PacketPlayOutNBTQuery(packet.getTransactionId(), compoundTag));
            }

        }
    }

    @Override
    public void handleBlockEntityTagQuery(PacketPlayInTileNBTQuery packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (this.player.hasPermissions(2)) {
            TileEntity blockEntity = this.player.getWorldServer().getTileEntity(packet.getPos());
            NBTTagCompound compoundTag = blockEntity != null ? blockEntity.save(new NBTTagCompound()) : null;
            this.player.connection.sendPacket(new PacketPlayOutNBTQuery(packet.getTransactionId(), compoundTag));
        }
    }

    @Override
    public void handleMovePlayer(PacketPlayInFlying packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (containsInvalidValues(packet.getX(0.0D), packet.getY(0.0D), packet.getZ(0.0D), packet.getYRot(0.0F), packet.getXRot(0.0F))) {
            this.disconnect(new ChatMessage("multiplayer.disconnect.invalid_player_movement"));
        } else {
            WorldServer serverLevel = this.player.getWorldServer();
            if (!this.player.wonGame) {
                if (this.tickCount == 0) {
                    this.syncPosition();
                }

                if (this.awaitingPositionFromClient != null) {
                    if (this.tickCount - this.awaitingTeleportTime > 20) {
                        this.awaitingTeleportTime = this.tickCount;
                        this.teleport(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot());
                    }

                } else {
                    this.awaitingTeleportTime = this.tickCount;
                    double d = clampHorizontal(packet.getX(this.player.locX()));
                    double e = clampVertical(packet.getY(this.player.locY()));
                    double f = clampHorizontal(packet.getZ(this.player.locZ()));
                    float g = MathHelper.wrapDegrees(packet.getYRot(this.player.getYRot()));
                    float h = MathHelper.wrapDegrees(packet.getXRot(this.player.getXRot()));
                    if (this.player.isPassenger()) {
                        this.player.setLocation(this.player.locX(), this.player.locY(), this.player.locZ(), g, h);
                        this.player.getWorldServer().getChunkSource().movePlayer(this.player);
                    } else {
                        double i = this.player.locX();
                        double j = this.player.locY();
                        double k = this.player.locZ();
                        double l = this.player.locY();
                        double m = d - this.firstGoodX;
                        double n = e - this.firstGoodY;
                        double o = f - this.firstGoodZ;
                        double p = this.player.getMot().lengthSqr();
                        double q = m * m + n * n + o * o;
                        if (this.player.isSleeping()) {
                            if (q > 1.0D) {
                                this.teleport(this.player.locX(), this.player.locY(), this.player.locZ(), g, h);
                            }

                        } else {
                            ++this.receivedMovePacketCount;
                            int r = this.receivedMovePacketCount - this.knownMovePacketCount;
                            if (r > 5) {
                                LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)", this.player.getDisplayName().getString(), r);
                                r = 1;
                            }

                            if (!this.player.isChangingDimension() && (!this.player.getWorldServer().getGameRules().getBoolean(GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK) || !this.player.isGliding())) {
                                float s = this.player.isGliding() ? 300.0F : 100.0F;
                                if (q - p > (double)(s * (float)r) && !this.isExemptPlayer()) {
                                    LOGGER.warn("{} moved too quickly! {},{},{}", this.player.getDisplayName().getString(), m, n, o);
                                    this.teleport(this.player.locX(), this.player.locY(), this.player.locZ(), this.player.getYRot(), this.player.getXRot());
                                    return;
                                }
                            }

                            AxisAlignedBB aABB = this.player.getBoundingBox();
                            m = d - this.lastGoodX;
                            n = e - this.lastGoodY;
                            o = f - this.lastGoodZ;
                            boolean bl = n > 0.0D;
                            if (this.player.isOnGround() && !packet.isOnGround() && bl) {
                                this.player.jump();
                            }

                            this.player.move(EnumMoveType.PLAYER, new Vec3D(m, n, o));
                            m = d - this.player.locX();
                            n = e - this.player.locY();
                            if (n > -0.5D || n < 0.5D) {
                                n = 0.0D;
                            }

                            o = f - this.player.locZ();
                            q = m * m + n * n + o * o;
                            boolean bl2 = false;
                            if (!this.player.isChangingDimension() && q > 0.0625D && !this.player.isSleeping() && !this.player.gameMode.isCreative() && this.player.gameMode.getGameMode() != EnumGamemode.SPECTATOR) {
                                bl2 = true;
                                LOGGER.warn("{} moved wrongly!", (Object)this.player.getDisplayName().getString());
                            }

                            this.player.setLocation(d, e, f, g, h);
                            if (this.player.noPhysics || this.player.isSleeping() || (!bl2 || !serverLevel.getCubes(this.player, aABB)) && !this.isPlayerCollidingWithAnythingNew(serverLevel, aABB)) {
                                this.clientIsFloating = n >= -0.03125D && this.player.gameMode.getGameMode() != EnumGamemode.SPECTATOR && !this.server.getAllowFlight() && !this.player.getAbilities().mayfly && !this.player.hasEffect(MobEffectList.LEVITATION) && !this.player.isGliding() && this.noBlocksAround(this.player);
                                this.player.getWorldServer().getChunkSource().movePlayer(this.player);
                                this.player.doCheckFallDamage(this.player.locY() - l, packet.isOnGround());
                                this.player.setOnGround(packet.isOnGround());
                                if (bl) {
                                    this.player.fallDistance = 0.0F;
                                }

                                this.player.checkMovement(this.player.locX() - i, this.player.locY() - j, this.player.locZ() - k);
                                this.lastGoodX = this.player.locX();
                                this.lastGoodY = this.player.locY();
                                this.lastGoodZ = this.player.locZ();
                            } else {
                                this.teleport(i, j, k, g, h);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isPlayerCollidingWithAnythingNew(IWorldReader world, AxisAlignedBB box) {
        Stream<VoxelShape> stream = world.getCollisions(this.player, this.player.getBoundingBox().shrink((double)1.0E-5F), (entity) -> {
            return true;
        });
        VoxelShape voxelShape = VoxelShapes.create(box.shrink((double)1.0E-5F));
        return stream.anyMatch((voxelShape2) -> {
            return !VoxelShapes.joinIsNotEmpty(voxelShape2, voxelShape, OperatorBoolean.AND);
        });
    }

    public void dismount(double x, double y, double z, float yaw, float pitch) {
        this.teleport(x, y, z, yaw, pitch, Collections.emptySet(), true);
    }

    public void teleport(double x, double y, double z, float yaw, float pitch) {
        this.teleport(x, y, z, yaw, pitch, Collections.emptySet(), false);
    }

    public void teleport(double x, double y, double z, float yaw, float pitch, Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> flags) {
        this.teleport(x, y, z, yaw, pitch, flags, false);
    }

    public void teleport(double x, double y, double z, float yaw, float pitch, Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> flags, boolean shouldDismount) {
        double d = flags.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.X) ? this.player.locX() : 0.0D;
        double e = flags.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y) ? this.player.locY() : 0.0D;
        double f = flags.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Z) ? this.player.locZ() : 0.0D;
        float g = flags.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT) ? this.player.getYRot() : 0.0F;
        float h = flags.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT) ? this.player.getXRot() : 0.0F;
        this.awaitingPositionFromClient = new Vec3D(x, y, z);
        if (++this.awaitingTeleport == Integer.MAX_VALUE) {
            this.awaitingTeleport = 0;
        }

        this.awaitingTeleportTime = this.tickCount;
        this.player.setLocation(x, y, z, yaw, pitch);
        this.player.connection.sendPacket(new PacketPlayOutPosition(x - d, y - e, z - f, yaw - g, pitch - h, flags, this.awaitingTeleport, shouldDismount));
    }

    @Override
    public void handlePlayerAction(PacketPlayInBlockDig packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        BlockPosition blockPos = packet.getPos();
        this.player.resetIdleTimer();
        PacketPlayInBlockDig.EnumPlayerDigType action = packet.getAction();
        switch(action) {
        case SWAP_ITEM_WITH_OFFHAND:
            if (!this.player.isSpectator()) {
                ItemStack itemStack = this.player.getItemInHand(EnumHand.OFF_HAND);
                this.player.setItemInHand(EnumHand.OFF_HAND, this.player.getItemInHand(EnumHand.MAIN_HAND));
                this.player.setItemInHand(EnumHand.MAIN_HAND, itemStack);
                this.player.clearActiveItem();
            }

            return;
        case DROP_ITEM:
            if (!this.player.isSpectator()) {
                this.player.dropItem(false);
            }

            return;
        case DROP_ALL_ITEMS:
            if (!this.player.isSpectator()) {
                this.player.dropItem(true);
            }

            return;
        case RELEASE_USE_ITEM:
            this.player.releaseActiveItem();
            return;
        case START_DESTROY_BLOCK:
        case ABORT_DESTROY_BLOCK:
        case STOP_DESTROY_BLOCK:
            this.player.gameMode.handleBlockBreakAction(blockPos, action, packet.getDirection(), this.player.level.getMaxBuildHeight());
            return;
        default:
            throw new IllegalArgumentException("Invalid player action");
        }
    }

    private static boolean wasBlockPlacementAttempt(EntityPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            Item item = stack.getItem();
            return (item instanceof ItemBlock || item instanceof ItemBucket) && !player.getCooldownTracker().hasCooldown(item);
        }
    }

    @Override
    public void handleUseItemOn(PacketPlayInUseItem packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        WorldServer serverLevel = this.player.getWorldServer();
        EnumHand interactionHand = packet.getHand();
        ItemStack itemStack = this.player.getItemInHand(interactionHand);
        MovingObjectPositionBlock blockHitResult = packet.getHitResult();
        BlockPosition blockPos = blockHitResult.getBlockPosition();
        EnumDirection direction = blockHitResult.getDirection();
        this.player.resetIdleTimer();
        int i = this.player.level.getMaxBuildHeight();
        if (blockPos.getY() < i) {
            if (this.awaitingPositionFromClient == null && this.player.distanceToSqr((double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.5D, (double)blockPos.getZ() + 0.5D) < 64.0D && serverLevel.mayInteract(this.player, blockPos)) {
                EnumInteractionResult interactionResult = this.player.gameMode.useItemOn(this.player, serverLevel, itemStack, interactionHand, blockHitResult);
                if (direction == EnumDirection.UP && !interactionResult.consumesAction() && blockPos.getY() >= i - 1 && wasBlockPlacementAttempt(this.player, itemStack)) {
                    IChatBaseComponent component = (new ChatMessage("build.tooHigh", i - 1)).withStyle(EnumChatFormat.RED);
                    this.player.sendMessage(component, ChatMessageType.GAME_INFO, SystemUtils.NIL_UUID);
                } else if (interactionResult.shouldSwing()) {
                    this.player.swingHand(interactionHand, true);
                }
            }
        } else {
            IChatBaseComponent component2 = (new ChatMessage("build.tooHigh", i - 1)).withStyle(EnumChatFormat.RED);
            this.player.sendMessage(component2, ChatMessageType.GAME_INFO, SystemUtils.NIL_UUID);
        }

        this.player.connection.sendPacket(new PacketPlayOutBlockChange(serverLevel, blockPos));
        this.player.connection.sendPacket(new PacketPlayOutBlockChange(serverLevel, blockPos.relative(direction)));
    }

    @Override
    public void handleUseItem(PacketPlayInBlockPlace packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        WorldServer serverLevel = this.player.getWorldServer();
        EnumHand interactionHand = packet.getHand();
        ItemStack itemStack = this.player.getItemInHand(interactionHand);
        this.player.resetIdleTimer();
        if (!itemStack.isEmpty()) {
            EnumInteractionResult interactionResult = this.player.gameMode.useItem(this.player, serverLevel, itemStack, interactionHand);
            if (interactionResult.shouldSwing()) {
                this.player.swingHand(interactionHand, true);
            }

        }
    }

    @Override
    public void handleTeleportToEntityPacket(PacketPlayInSpectate packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (this.player.isSpectator()) {
            for(WorldServer serverLevel : this.server.getWorlds()) {
                Entity entity = packet.getEntity(serverLevel);
                if (entity != null) {
                    this.player.teleportTo(serverLevel, entity.locX(), entity.locY(), entity.locZ(), entity.getYRot(), entity.getXRot());
                    return;
                }
            }
        }

    }

    @Override
    public void handleResourcePackResponse(PacketPlayInResourcePackStatus packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (packet.getAction() == PacketPlayInResourcePackStatus.EnumResourcePackStatus.DECLINED && this.server.isResourcePackRequired()) {
            LOGGER.info("Disconnecting {} due to resource pack rejection", (Object)this.player.getDisplayName());
            this.disconnect(new ChatMessage("multiplayer.requiredTexturePrompt.disconnect"));
        }

    }

    @Override
    public void handlePaddleBoat(PacketPlayInBoatMove packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        Entity entity = this.player.getVehicle();
        if (entity instanceof EntityBoat) {
            ((EntityBoat)entity).setPaddleState(packet.getLeft(), packet.getRight());
        }

    }

    @Override
    public void handlePong(PacketPlayInPong packet) {
    }

    @Override
    public void onDisconnect(IChatBaseComponent reason) {
        LOGGER.info("{} lost connection: {}", this.player.getDisplayName().getString(), reason.getString());
        this.server.invalidatePingSample();
        this.server.getPlayerList().sendMessage((new ChatMessage("multiplayer.player.left", this.player.getScoreboardDisplayName())).withStyle(EnumChatFormat.YELLOW), ChatMessageType.SYSTEM, SystemUtils.NIL_UUID);
        this.player.disconnect();
        this.server.getPlayerList().disconnect(this.player);
        this.player.getTextFilter().leave();
        if (this.isExemptPlayer()) {
            LOGGER.info("Stopping singleplayer server as player logged out");
            this.server.safeShutdown(false);
        }

    }

    @Override
    public void sendPacket(Packet<?> packet) {
        this.send(packet, (GenericFutureListener<? extends Future<? super Void>>)null);
    }

    public void send(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> listener) {
        try {
            this.connection.send(packet, listener);
        } catch (Throwable var6) {
            CrashReport crashReport = CrashReport.forThrowable(var6, "Sending packet");
            CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Packet being sent");
            crashReportCategory.setDetail("Packet class", () -> {
                return packet.getClass().getCanonicalName();
            });
            throw new ReportedException(crashReport);
        }
    }

    @Override
    public void handleSetCarriedItem(PacketPlayInHeldItemSlot packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (packet.getSlot() >= 0 && packet.getSlot() < PlayerInventory.getHotbarSize()) {
            if (this.player.getInventory().selected != packet.getSlot() && this.player.getRaisedHand() == EnumHand.MAIN_HAND) {
                this.player.clearActiveItem();
            }

            this.player.getInventory().selected = packet.getSlot();
            this.player.resetIdleTimer();
        } else {
            LOGGER.warn("{} tried to set an invalid carried item", (Object)this.player.getDisplayName().getString());
        }
    }

    @Override
    public void handleChat(PacketPlayInChat packet) {
        String string = StringUtils.normalizeSpace(packet.getMessage());

        for(int i = 0; i < string.length(); ++i) {
            if (!SharedConstants.isAllowedChatCharacter(string.charAt(i))) {
                this.disconnect(new ChatMessage("multiplayer.disconnect.illegal_characters"));
                return;
            }
        }

        if (string.startsWith("/")) {
            PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
            this.handleChat(ITextFilter.FilteredText.passThrough(string));
        } else {
            this.filterTextPacket(string, this::handleChat);
        }

    }

    private void handleChat(ITextFilter.FilteredText message) {
        if (this.player.getChatFlags() == EnumChatVisibility.HIDDEN) {
            this.sendPacket(new PacketPlayOutChat((new ChatMessage("chat.disabled.options")).withStyle(EnumChatFormat.RED), ChatMessageType.SYSTEM, SystemUtils.NIL_UUID));
        } else {
            this.player.resetIdleTimer();
            String string = message.getRaw();
            if (string.startsWith("/")) {
                this.handleCommand(string);
            } else {
                String string2 = message.getFiltered();
                IChatBaseComponent component = string2.isEmpty() ? null : new ChatMessage("chat.type.text", this.player.getScoreboardDisplayName(), string2);
                IChatBaseComponent component2 = new ChatMessage("chat.type.text", this.player.getScoreboardDisplayName(), string);
                this.server.getPlayerList().broadcastMessage(component2, (player) -> {
                    return this.player.shouldFilterMessageTo(player) ? component : component2;
                }, ChatMessageType.CHAT, this.player.getUniqueID());
            }

            this.chatSpamTickCount += 20;
            if (this.chatSpamTickCount > 200 && !this.server.getPlayerList().isOp(this.player.getProfile())) {
                this.disconnect(new ChatMessage("disconnect.spam"));
            }

        }
    }

    private void handleCommand(String input) {
        this.server.getCommandDispatcher().performCommand(this.player.getCommandListener(), input);
    }

    @Override
    public void handleAnimate(PacketPlayInArmAnimation packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.player.resetIdleTimer();
        this.player.swingHand(packet.getHand());
    }

    @Override
    public void handlePlayerCommand(PacketPlayInEntityAction packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.player.resetIdleTimer();
        switch(packet.getAction()) {
        case PRESS_SHIFT_KEY:
            this.player.setSneaking(true);
            break;
        case RELEASE_SHIFT_KEY:
            this.player.setSneaking(false);
            break;
        case START_SPRINTING:
            this.player.setSprinting(true);
            break;
        case STOP_SPRINTING:
            this.player.setSprinting(false);
            break;
        case STOP_SLEEPING:
            if (this.player.isSleeping()) {
                this.player.wakeup(false, true);
                this.awaitingPositionFromClient = this.player.getPositionVector();
            }
            break;
        case START_RIDING_JUMP:
            if (this.player.getVehicle() instanceof IJumpable) {
                IJumpable playerRideableJumping = (IJumpable)this.player.getVehicle();
                int i = packet.getData();
                if (playerRideableJumping.canJump() && i > 0) {
                    playerRideableJumping.handleStartJump(i);
                }
            }
            break;
        case STOP_RIDING_JUMP:
            if (this.player.getVehicle() instanceof IJumpable) {
                IJumpable playerRideableJumping2 = (IJumpable)this.player.getVehicle();
                playerRideableJumping2.handleStopJump();
            }
            break;
        case OPEN_INVENTORY:
            if (this.player.getVehicle() instanceof EntityHorseAbstract) {
                ((EntityHorseAbstract)this.player.getVehicle()).openInventory(this.player);
            }
            break;
        case START_FALL_FLYING:
            if (!this.player.tryToStartFallFlying()) {
                this.player.stopGliding();
            }
            break;
        default:
            throw new IllegalArgumentException("Invalid client command!");
        }

    }

    @Override
    public void handleInteract(PacketPlayInUseEntity packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        WorldServer serverLevel = this.player.getWorldServer();
        final Entity entity = packet.getTarget(serverLevel);
        this.player.resetIdleTimer();
        this.player.setSneaking(packet.isUsingSecondaryAction());
        if (entity != null) {
            double d = 36.0D;
            if (this.player.distanceToSqr(entity) < 36.0D) {
                packet.dispatch(new PacketPlayInUseEntity.Handler() {
                    private void performInteraction(EnumHand hand, PlayerConnection.EntityInteraction action) {
                        ItemStack itemStack = PlayerConnection.this.player.getItemInHand(hand).cloneItemStack();
                        EnumInteractionResult interactionResult = action.run(PlayerConnection.this.player, entity, hand);
                        if (interactionResult.consumesAction()) {
                            CriterionTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(PlayerConnection.this.player, itemStack, entity);
                            if (interactionResult.shouldSwing()) {
                                PlayerConnection.this.player.swingHand(hand, true);
                            }
                        }

                    }

                    @Override
                    public void onInteraction(EnumHand hand) {
                        this.performInteraction(hand, EntityHuman::interactOn);
                    }

                    @Override
                    public void onInteraction(EnumHand hand, Vec3D pos) {
                        this.performInteraction(hand, (player, entityx, handx) -> {
                            return entityx.interactAt(player, pos, handx);
                        });
                    }

                    @Override
                    public void onAttack() {
                        if (!(entity instanceof EntityItem) && !(entity instanceof EntityExperienceOrb) && !(entity instanceof EntityArrow) && entity != PlayerConnection.this.player) {
                            PlayerConnection.this.player.attack(entity);
                        } else {
                            PlayerConnection.this.disconnect(new ChatMessage("multiplayer.disconnect.invalid_entity_attacked"));
                            PlayerConnection.LOGGER.warn("Player {} tried to attack an invalid entity", (Object)PlayerConnection.this.player.getDisplayName().getString());
                        }
                    }
                });
            }
        }

    }

    @Override
    public void handleClientCommand(PacketPlayInClientCommand packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.player.resetIdleTimer();
        PacketPlayInClientCommand.EnumClientCommand action = packet.getAction();
        switch(action) {
        case PERFORM_RESPAWN:
            if (this.player.wonGame) {
                this.player.wonGame = false;
                this.player = this.server.getPlayerList().moveToWorld(this.player, true);
                CriterionTriggers.CHANGED_DIMENSION.trigger(this.player, World.END, World.OVERWORLD);
            } else {
                if (this.player.getHealth() > 0.0F) {
                    return;
                }

                this.player = this.server.getPlayerList().moveToWorld(this.player, false);
                if (this.server.isHardcore()) {
                    this.player.setGameMode(EnumGamemode.SPECTATOR);
                    this.player.getWorldServer().getGameRules().get(GameRules.RULE_SPECTATORSGENERATECHUNKS).set(false, this.server);
                }
            }
            break;
        case REQUEST_STATS:
            this.player.getStatisticManager().sendStats(this.player);
        }

    }

    @Override
    public void handleContainerClose(PacketPlayInCloseWindow packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.player.doCloseContainer();
    }

    @Override
    public void handleContainerClick(PacketPlayInWindowClick packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.player.resetIdleTimer();
        if (this.player.containerMenu.containerId == packet.getContainerId()) {
            if (this.player.isSpectator()) {
                this.player.containerMenu.updateInventory();
            } else {
                boolean bl = packet.getStateId() != this.player.containerMenu.getStateId();
                this.player.containerMenu.suppressRemoteUpdates();
                this.player.containerMenu.clicked(packet.getSlotNum(), packet.getButtonNum(), packet.getClickType(), this.player);

                for(Entry<ItemStack> entry : Int2ObjectMaps.fastIterable(packet.getChangedSlots())) {
                    this.player.containerMenu.setRemoteSlotNoCopy(entry.getIntKey(), entry.getValue());
                }

                this.player.containerMenu.setRemoteCarried(packet.getCarriedItem());
                this.player.containerMenu.resumeRemoteUpdates();
                if (bl) {
                    this.player.containerMenu.broadcastFullState();
                } else {
                    this.player.containerMenu.broadcastChanges();
                }
            }
        }

    }

    @Override
    public void handlePlaceRecipe(PacketPlayInAutoRecipe packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.player.resetIdleTimer();
        if (!this.player.isSpectator() && this.player.containerMenu.containerId == packet.getContainerId() && this.player.containerMenu instanceof ContainerRecipeBook) {
            this.server.getCraftingManager().getRecipe(packet.getRecipe()).ifPresent((recipe) -> {
                ((ContainerRecipeBook)this.player.containerMenu).handlePlacement(packet.isShiftDown(), recipe, this.player);
            });
        }
    }

    @Override
    public void handleContainerButtonClick(PacketPlayInEnchantItem packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.player.resetIdleTimer();
        if (this.player.containerMenu.containerId == packet.getContainerId() && !this.player.isSpectator()) {
            this.player.containerMenu.clickMenuButton(this.player, packet.getButtonId());
            this.player.containerMenu.broadcastChanges();
        }

    }

    @Override
    public void handleSetCreativeModeSlot(PacketPlayInSetCreativeSlot packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (this.player.gameMode.isCreative()) {
            boolean bl = packet.getSlotNum() < 0;
            ItemStack itemStack = packet.getItemStack();
            NBTTagCompound compoundTag = itemStack.getTagElement("BlockEntityTag");
            if (!itemStack.isEmpty() && compoundTag != null && compoundTag.hasKey("x") && compoundTag.hasKey("y") && compoundTag.hasKey("z")) {
                BlockPosition blockPos = new BlockPosition(compoundTag.getInt("x"), compoundTag.getInt("y"), compoundTag.getInt("z"));
                TileEntity blockEntity = this.player.level.getTileEntity(blockPos);
                if (blockEntity != null) {
                    NBTTagCompound compoundTag2 = blockEntity.save(new NBTTagCompound());
                    compoundTag2.remove("x");
                    compoundTag2.remove("y");
                    compoundTag2.remove("z");
                    itemStack.addTagElement("BlockEntityTag", compoundTag2);
                }
            }

            boolean bl2 = packet.getSlotNum() >= 1 && packet.getSlotNum() <= 45;
            boolean bl3 = itemStack.isEmpty() || itemStack.getDamage() >= 0 && itemStack.getCount() <= 64 && !itemStack.isEmpty();
            if (bl2 && bl3) {
                this.player.inventoryMenu.getSlot(packet.getSlotNum()).set(itemStack);
                this.player.inventoryMenu.broadcastChanges();
            } else if (bl && bl3 && this.dropSpamTickCount < 200) {
                this.dropSpamTickCount += 20;
                this.player.drop(itemStack, true);
            }
        }

    }

    @Override
    public void handleSignUpdate(PacketPlayInUpdateSign packet) {
        List<String> list = Stream.of(packet.getLines()).map(EnumChatFormat::stripFormatting).collect(Collectors.toList());
        this.filterTextPacket(list, (listx) -> {
            this.updateSignText(packet, listx);
        });
    }

    private void updateSignText(PacketPlayInUpdateSign packet, List<ITextFilter.FilteredText> signText) {
        this.player.resetIdleTimer();
        WorldServer serverLevel = this.player.getWorldServer();
        BlockPosition blockPos = packet.getPos();
        if (serverLevel.isLoaded(blockPos)) {
            IBlockData blockState = serverLevel.getType(blockPos);
            TileEntity blockEntity = serverLevel.getTileEntity(blockPos);
            if (!(blockEntity instanceof TileEntitySign)) {
                return;
            }

            TileEntitySign signBlockEntity = (TileEntitySign)blockEntity;
            if (!signBlockEntity.isEditable() || !this.player.getUniqueID().equals(signBlockEntity.getPlayerWhoMayEdit())) {
                LOGGER.warn("Player {} just tried to change non-editable sign", (Object)this.player.getDisplayName().getString());
                return;
            }

            for(int i = 0; i < signText.size(); ++i) {
                ITextFilter.FilteredText filteredText = signText.get(i);
                if (this.player.isTextFilteringEnabled()) {
                    signBlockEntity.setMessage(i, new ChatComponentText(filteredText.getFiltered()));
                } else {
                    signBlockEntity.setMessage(i, new ChatComponentText(filteredText.getRaw()), new ChatComponentText(filteredText.getFiltered()));
                }
            }

            signBlockEntity.update();
            serverLevel.notify(blockPos, blockState, blockState, 3);
        }

    }

    @Override
    public void handleKeepAlive(PacketPlayInKeepAlive packet) {
        if (this.keepAlivePending && packet.getId() == this.keepAliveChallenge) {
            int i = (int)(SystemUtils.getMonotonicMillis() - this.keepAliveTime);
            this.player.latency = (this.player.latency * 3 + i) / 4;
            this.keepAlivePending = false;
        } else if (!this.isExemptPlayer()) {
            this.disconnect(new ChatMessage("disconnect.timeout"));
        }

    }

    @Override
    public void handlePlayerAbilities(PacketPlayInAbilities packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.player.getAbilities().flying = packet.isFlying() && this.player.getAbilities().mayfly;
    }

    @Override
    public void handleClientInformation(PacketPlayInSettings packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        this.player.updateOptions(packet);
    }

    @Override
    public void handleCustomPayload(PacketPlayInCustomPayload packet) {
    }

    @Override
    public void handleChangeDifficulty(PacketPlayInDifficultyChange packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (this.player.hasPermissions(2) || this.isExemptPlayer()) {
            this.server.setDifficulty(packet.getDifficulty(), false);
        }
    }

    @Override
    public void handleLockDifficulty(PacketPlayInDifficultyLock packet) {
        PlayerConnectionUtils.ensureMainThread(packet, this, this.player.getWorldServer());
        if (this.player.hasPermissions(2) || this.isExemptPlayer()) {
            this.server.setDifficultyLocked(packet.isLocked());
        }
    }

    @Override
    public EntityPlayer getPlayer() {
        return this.player;
    }

    @FunctionalInterface
    interface EntityInteraction {
        EnumInteractionResult run(EntityPlayer player, Entity entity, EnumHand hand);
    }
}
