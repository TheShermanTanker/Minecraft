package net.minecraft.server.level;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.EnumChatFormat;
import net.minecraft.ReportedException;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.commands.arguments.ArgumentAnchor;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInSettings;
import net.minecraft.network.protocol.game.PacketPlayOutAbilities;
import net.minecraft.network.protocol.game.PacketPlayOutAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutCamera;
import net.minecraft.network.protocol.game.PacketPlayOutChat;
import net.minecraft.network.protocol.game.PacketPlayOutCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayOutCombatEnter;
import net.minecraft.network.protocol.game.PacketPlayOutCombatExit;
import net.minecraft.network.protocol.game.PacketPlayOutCombatKill;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEffect;
import net.minecraft.network.protocol.game.PacketPlayOutEntityStatus;
import net.minecraft.network.protocol.game.PacketPlayOutExperience;
import net.minecraft.network.protocol.game.PacketPlayOutGameStateChange;
import net.minecraft.network.protocol.game.PacketPlayOutLookAt;
import net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawn;
import net.minecraft.network.protocol.game.PacketPlayOutNamedSoundEffect;
import net.minecraft.network.protocol.game.PacketPlayOutOpenBook;
import net.minecraft.network.protocol.game.PacketPlayOutOpenSignEditor;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindowHorse;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindowMerchant;
import net.minecraft.network.protocol.game.PacketPlayOutRemoveEntityEffect;
import net.minecraft.network.protocol.game.PacketPlayOutResourcePackSend;
import net.minecraft.network.protocol.game.PacketPlayOutRespawn;
import net.minecraft.network.protocol.game.PacketPlayOutServerDifficulty;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.network.protocol.game.PacketPlayOutUnloadChunk;
import net.minecraft.network.protocol.game.PacketPlayOutUpdateHealth;
import net.minecraft.network.protocol.game.PacketPlayOutWindowData;
import net.minecraft.network.protocol.game.PacketPlayOutWindowItems;
import net.minecraft.network.protocol.game.PacketPlayOutWorldEvent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.AdvancementDataPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ITextFilter;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.stats.RecipeBookServer;
import net.minecraft.stats.Statistic;
import net.minecraft.stats.StatisticList;
import net.minecraft.stats.StatisticManagerServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Unit;
import net.minecraft.world.EnumHand;
import net.minecraft.world.IInventory;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumMainHand;
import net.minecraft.world.entity.IEntityAngerable;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.animal.horse.EntityHorseAbstract;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.EnumChatVisibility;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerHorse;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.ICrafting;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SlotResult;
import net.minecraft.world.item.ItemCooldown;
import net.minecraft.world.item.ItemCooldownPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemWorldMapBase;
import net.minecraft.world.item.ItemWrittenBook;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.trading.MerchantRecipeList;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.BlockFacingHorizontal;
import net.minecraft.world.level.block.BlockPortal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityCommand;
import net.minecraft.world.level.block.entity.TileEntitySign;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.ShapeDetectorShape;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.scores.ScoreboardScore;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntityPlayer extends EntityHuman {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_XZ = 32;
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_Y = 10;
    public PlayerConnection connection;
    public final MinecraftServer server;
    public final PlayerInteractManager gameMode;
    private final AdvancementDataPlayer advancements;
    private final StatisticManagerServer stats;
    private float lastRecordedHealthAndAbsorption = Float.MIN_VALUE;
    private int lastRecordedFoodLevel = Integer.MIN_VALUE;
    private int lastRecordedAirLevel = Integer.MIN_VALUE;
    private int lastRecordedArmor = Integer.MIN_VALUE;
    private int lastRecordedLevel = Integer.MIN_VALUE;
    private int lastRecordedExperience = Integer.MIN_VALUE;
    private float lastSentHealth = -1.0E8F;
    private int lastSentFood = -99999999;
    private boolean lastFoodSaturationZero = true;
    public int lastSentExp = -99999999;
    public int spawnInvulnerableTime = 60;
    private EnumChatVisibility chatVisibility = EnumChatVisibility.FULL;
    private boolean canChatColor = true;
    private long lastActionTime = SystemUtils.getMonotonicMillis();
    @Nullable
    private Entity camera;
    public boolean isChangingDimension;
    private boolean seenCredits;
    private final RecipeBookServer recipeBook = new RecipeBookServer();
    @Nullable
    private Vec3D levitationStartPos;
    private int levitationStartTime;
    private boolean disconnected;
    @Nullable
    private Vec3D startingToFallPosition;
    @Nullable
    private Vec3D enteredNetherPosition;
    @Nullable
    private Vec3D enteredLavaOnVehiclePosition;
    private SectionPosition lastSectionPos = SectionPosition.of(0, 0, 0);
    private ResourceKey<World> respawnDimension = World.OVERWORLD;
    @Nullable
    private BlockPosition respawnPosition;
    private boolean respawnForced;
    private float respawnAngle;
    private final ITextFilter textFilter;
    private boolean textFilteringEnabled;
    private boolean allowsListing = true;
    private final ContainerSynchronizer containerSynchronizer = new ContainerSynchronizer() {
        @Override
        public void sendInitialData(Container handler, NonNullList<ItemStack> stacks, ItemStack cursorStack, int[] properties) {
            EntityPlayer.this.connection.sendPacket(new PacketPlayOutWindowItems(handler.containerId, handler.incrementStateId(), stacks, cursorStack));

            for(int i = 0; i < properties.length; ++i) {
                this.broadcastDataValue(handler, i, properties[i]);
            }

        }

        @Override
        public void sendSlotChange(Container handler, int slot, ItemStack stack) {
            EntityPlayer.this.connection.sendPacket(new PacketPlayOutSetSlot(handler.containerId, handler.incrementStateId(), slot, stack));
        }

        @Override
        public void sendCarriedChange(Container handler, ItemStack stack) {
            EntityPlayer.this.connection.sendPacket(new PacketPlayOutSetSlot(-1, handler.incrementStateId(), -1, stack));
        }

        @Override
        public void sendDataChange(Container handler, int property, int value) {
            this.broadcastDataValue(handler, property, value);
        }

        private void broadcastDataValue(Container handler, int property, int value) {
            EntityPlayer.this.connection.sendPacket(new PacketPlayOutWindowData(handler.containerId, property, value));
        }
    };
    private final ICrafting containerListener = new ICrafting() {
        @Override
        public void slotChanged(Container handler, int slotId, ItemStack stack) {
            Slot slot = handler.getSlot(slotId);
            if (!(slot instanceof SlotResult)) {
                if (slot.container == EntityPlayer.this.getInventory()) {
                    CriterionTriggers.INVENTORY_CHANGED.trigger(EntityPlayer.this, EntityPlayer.this.getInventory(), stack);
                }

            }
        }

        @Override
        public void setContainerData(Container handler, int property, int value) {
        }
    };
    private int containerCounter;
    public int latency;
    public boolean wonGame;

    public EntityPlayer(MinecraftServer server, WorldServer world, GameProfile profile) {
        super(world, world.getSpawn(), world.getSharedSpawnAngle(), profile);
        this.textFilter = server.createTextFilterForPlayer(this);
        this.gameMode = server.createGameModeForPlayer(this);
        this.server = server;
        this.stats = server.getPlayerList().getStatisticManager(this);
        this.advancements = server.getPlayerList().getPlayerAdvancements(this);
        this.maxUpStep = 1.0F;
        this.fudgeSpawnLocation(world);
    }

    public void fudgeSpawnLocation(WorldServer world) {
        BlockPosition blockPos = world.getSpawn();
        if (world.getDimensionManager().hasSkyLight() && world.getMinecraftServer().getSaveData().getGameType() != EnumGamemode.ADVENTURE) {
            int i = Math.max(0, this.server.getSpawnRadius(world));
            int j = MathHelper.floor(world.getWorldBorder().getDistanceToBorder((double)blockPos.getX(), (double)blockPos.getZ()));
            if (j < i) {
                i = j;
            }

            if (j <= 1) {
                i = 1;
            }

            long l = (long)(i * 2 + 1);
            long m = l * l;
            int k = m > 2147483647L ? Integer.MAX_VALUE : (int)m;
            int n = this.getCoprime(k);
            int o = (new Random()).nextInt(k);

            for(int p = 0; p < k; ++p) {
                int q = (o + n * p) % k;
                int r = q % (i * 2 + 1);
                int s = q / (i * 2 + 1);
                BlockPosition blockPos2 = WorldProviderNormal.getOverworldRespawnPos(world, blockPos.getX() + r - i, blockPos.getZ() + s - i);
                if (blockPos2 != null) {
                    this.setPositionRotation(blockPos2, 0.0F, 0.0F);
                    if (world.getCubes(this)) {
                        break;
                    }
                }
            }
        } else {
            this.setPositionRotation(blockPos, 0.0F, 0.0F);

            while(!world.getCubes(this) && this.locY() < (double)(world.getMaxBuildHeight() - 1)) {
                this.setPosition(this.locX(), this.locY() + 1.0D, this.locZ());
            }
        }

    }

    private int getCoprime(int horizontalSpawnArea) {
        return horizontalSpawnArea <= 16 ? horizontalSpawnArea - 1 : 17;
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("enteredNetherPosition", 10)) {
            NBTTagCompound compoundTag = nbt.getCompound("enteredNetherPosition");
            this.enteredNetherPosition = new Vec3D(compoundTag.getDouble("x"), compoundTag.getDouble("y"), compoundTag.getDouble("z"));
        }

        this.seenCredits = nbt.getBoolean("seenCredits");
        if (nbt.hasKeyOfType("recipeBook", 10)) {
            this.recipeBook.fromNbt(nbt.getCompound("recipeBook"), this.server.getCraftingManager());
        }

        if (this.isSleeping()) {
            this.entityWakeup();
        }

        if (nbt.hasKeyOfType("SpawnX", 99) && nbt.hasKeyOfType("SpawnY", 99) && nbt.hasKeyOfType("SpawnZ", 99)) {
            this.respawnPosition = new BlockPosition(nbt.getInt("SpawnX"), nbt.getInt("SpawnY"), nbt.getInt("SpawnZ"));
            this.respawnForced = nbt.getBoolean("SpawnForced");
            this.respawnAngle = nbt.getFloat("SpawnAngle");
            if (nbt.hasKey("SpawnDimension")) {
                this.respawnDimension = World.RESOURCE_KEY_CODEC.parse(DynamicOpsNBT.INSTANCE, nbt.get("SpawnDimension")).resultOrPartial(LOGGER::error).orElse(World.OVERWORLD);
            }
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        this.storeGameTypes(nbt);
        nbt.setBoolean("seenCredits", this.seenCredits);
        if (this.enteredNetherPosition != null) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            compoundTag.setDouble("x", this.enteredNetherPosition.x);
            compoundTag.setDouble("y", this.enteredNetherPosition.y);
            compoundTag.setDouble("z", this.enteredNetherPosition.z);
            nbt.set("enteredNetherPosition", compoundTag);
        }

        Entity entity = this.getRootVehicle();
        Entity entity2 = this.getVehicle();
        if (entity2 != null && entity != this && entity.hasSinglePlayerPassenger()) {
            NBTTagCompound compoundTag2 = new NBTTagCompound();
            NBTTagCompound compoundTag3 = new NBTTagCompound();
            entity.save(compoundTag3);
            compoundTag2.putUUID("Attach", entity2.getUniqueID());
            compoundTag2.set("Entity", compoundTag3);
            nbt.set("RootVehicle", compoundTag2);
        }

        nbt.set("recipeBook", this.recipeBook.save());
        nbt.setString("Dimension", this.level.getDimensionKey().location().toString());
        if (this.respawnPosition != null) {
            nbt.setInt("SpawnX", this.respawnPosition.getX());
            nbt.setInt("SpawnY", this.respawnPosition.getY());
            nbt.setInt("SpawnZ", this.respawnPosition.getZ());
            nbt.setBoolean("SpawnForced", this.respawnForced);
            nbt.setFloat("SpawnAngle", this.respawnAngle);
            MinecraftKey.CODEC.encodeStart(DynamicOpsNBT.INSTANCE, this.respawnDimension.location()).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
                nbt.set("SpawnDimension", tag);
            });
        }

    }

    public void setExperiencePoints(int points) {
        float f = (float)this.getExpToLevel();
        float g = (f - 1.0F) / f;
        this.experienceProgress = MathHelper.clamp((float)points / f, 0.0F, g);
        this.lastSentExp = -1;
    }

    public void setExperienceLevels(int level) {
        this.experienceLevel = level;
        this.lastSentExp = -1;
    }

    @Override
    public void levelDown(int levels) {
        super.levelDown(levels);
        this.lastSentExp = -1;
    }

    @Override
    public void enchantDone(ItemStack enchantedItem, int experienceLevels) {
        super.enchantDone(enchantedItem, experienceLevels);
        this.lastSentExp = -1;
    }

    public void initMenu(Container screenHandler) {
        screenHandler.addSlotListener(this.containerListener);
        screenHandler.setSynchronizer(this.containerSynchronizer);
    }

    public void syncInventory() {
        this.initMenu(this.inventoryMenu);
    }

    @Override
    public void enterCombat() {
        super.enterCombat();
        this.connection.sendPacket(new PacketPlayOutCombatEnter());
    }

    @Override
    public void exitCombat() {
        super.exitCombat();
        this.connection.sendPacket(new PacketPlayOutCombatExit(this.getCombatTracker()));
    }

    @Override
    protected void onInsideBlock(IBlockData state) {
        CriterionTriggers.ENTER_BLOCK.trigger(this, state);
    }

    @Override
    protected ItemCooldown createItemCooldowns() {
        return new ItemCooldownPlayer(this);
    }

    @Override
    public void tick() {
        this.gameMode.tick();
        --this.spawnInvulnerableTime;
        if (this.invulnerableTime > 0) {
            --this.invulnerableTime;
        }

        this.containerMenu.broadcastChanges();
        if (!this.level.isClientSide && !this.containerMenu.canUse(this)) {
            this.closeInventory();
            this.containerMenu = this.inventoryMenu;
        }

        Entity entity = this.getSpecatorTarget();
        if (entity != this) {
            if (entity.isAlive()) {
                this.setLocation(entity.locX(), entity.locY(), entity.locZ(), entity.getYRot(), entity.getXRot());
                this.getWorldServer().getChunkSource().movePlayer(this);
                if (this.wantsToStopRiding()) {
                    this.setSpectatorTarget(this);
                }
            } else {
                this.setSpectatorTarget(this);
            }
        }

        CriterionTriggers.TICK.trigger(this);
        if (this.levitationStartPos != null) {
            CriterionTriggers.LEVITATION.trigger(this, this.levitationStartPos, this.tickCount - this.levitationStartTime);
        }

        this.trackStartFallingPosition();
        this.trackEnteredOrExitedLavaOnVehicle();
        this.advancements.flushDirty(this);
    }

    public void playerTick() {
        try {
            if (!this.isSpectator() || !this.touchingUnloadedChunk()) {
                super.tick();
            }

            for(int i = 0; i < this.getInventory().getSize(); ++i) {
                ItemStack itemStack = this.getInventory().getItem(i);
                if (itemStack.getItem().isComplex()) {
                    Packet<?> packet = ((ItemWorldMapBase)itemStack.getItem()).getUpdatePacket(itemStack, this.level, this);
                    if (packet != null) {
                        this.connection.sendPacket(packet);
                    }
                }
            }

            if (this.getHealth() != this.lastSentHealth || this.lastSentFood != this.foodData.getFoodLevel() || this.foodData.getSaturationLevel() == 0.0F != this.lastFoodSaturationZero) {
                this.connection.sendPacket(new PacketPlayOutUpdateHealth(this.getHealth(), this.foodData.getFoodLevel(), this.foodData.getSaturationLevel()));
                this.lastSentHealth = this.getHealth();
                this.lastSentFood = this.foodData.getFoodLevel();
                this.lastFoodSaturationZero = this.foodData.getSaturationLevel() == 0.0F;
            }

            if (this.getHealth() + this.getAbsorptionHearts() != this.lastRecordedHealthAndAbsorption) {
                this.lastRecordedHealthAndAbsorption = this.getHealth() + this.getAbsorptionHearts();
                this.updateScoreForCriteria(IScoreboardCriteria.HEALTH, MathHelper.ceil(this.lastRecordedHealthAndAbsorption));
            }

            if (this.foodData.getFoodLevel() != this.lastRecordedFoodLevel) {
                this.lastRecordedFoodLevel = this.foodData.getFoodLevel();
                this.updateScoreForCriteria(IScoreboardCriteria.FOOD, MathHelper.ceil((float)this.lastRecordedFoodLevel));
            }

            if (this.getAirTicks() != this.lastRecordedAirLevel) {
                this.lastRecordedAirLevel = this.getAirTicks();
                this.updateScoreForCriteria(IScoreboardCriteria.AIR, MathHelper.ceil((float)this.lastRecordedAirLevel));
            }

            if (this.getArmorStrength() != this.lastRecordedArmor) {
                this.lastRecordedArmor = this.getArmorStrength();
                this.updateScoreForCriteria(IScoreboardCriteria.ARMOR, MathHelper.ceil((float)this.lastRecordedArmor));
            }

            if (this.totalExperience != this.lastRecordedExperience) {
                this.lastRecordedExperience = this.totalExperience;
                this.updateScoreForCriteria(IScoreboardCriteria.EXPERIENCE, MathHelper.ceil((float)this.lastRecordedExperience));
            }

            if (this.experienceLevel != this.lastRecordedLevel) {
                this.lastRecordedLevel = this.experienceLevel;
                this.updateScoreForCriteria(IScoreboardCriteria.LEVEL, MathHelper.ceil((float)this.lastRecordedLevel));
            }

            if (this.totalExperience != this.lastSentExp) {
                this.lastSentExp = this.totalExperience;
                this.connection.sendPacket(new PacketPlayOutExperience(this.experienceProgress, this.totalExperience, this.experienceLevel));
            }

            if (this.tickCount % 20 == 0) {
                CriterionTriggers.LOCATION.trigger(this);
            }

        } catch (Throwable var4) {
            CrashReport crashReport = CrashReport.forThrowable(var4, "Ticking player");
            CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Player being ticked");
            this.appendEntityCrashDetails(crashReportCategory);
            throw new ReportedException(crashReport);
        }
    }

    @Override
    public void resetFallDistance() {
        if (this.getHealth() > 0.0F && this.startingToFallPosition != null) {
            CriterionTriggers.FALL_FROM_HEIGHT.trigger(this, this.startingToFallPosition);
        }

        this.startingToFallPosition = null;
        super.resetFallDistance();
    }

    public void trackStartFallingPosition() {
        if (this.fallDistance > 0.0F && this.startingToFallPosition == null) {
            this.startingToFallPosition = this.getPositionVector();
        }

    }

    public void trackEnteredOrExitedLavaOnVehicle() {
        if (this.getVehicle() != null && this.getVehicle().isInLava()) {
            if (this.enteredLavaOnVehiclePosition == null) {
                this.enteredLavaOnVehiclePosition = this.getPositionVector();
            } else {
                CriterionTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.trigger(this, this.enteredLavaOnVehiclePosition);
            }
        }

        if (this.enteredLavaOnVehiclePosition != null && (this.getVehicle() == null || !this.getVehicle().isInLava())) {
            this.enteredLavaOnVehiclePosition = null;
        }

    }

    private void updateScoreForCriteria(IScoreboardCriteria criterion, int score) {
        this.getScoreboard().getObjectivesForCriteria(criterion, this.getName(), (scorex) -> {
            scorex.setScore(score);
        });
    }

    @Override
    public void die(DamageSource source) {
        boolean bl = this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
        if (bl) {
            IChatBaseComponent component = this.getCombatTracker().getDeathMessage();
            this.connection.send(new PacketPlayOutCombatKill(this.getCombatTracker(), component), (future) -> {
                if (!future.isSuccess()) {
                    int i = 256;
                    String string = component.getString(256);
                    IChatBaseComponent component2 = new ChatMessage("death.attack.message_too_long", (new ChatComponentText(string)).withStyle(EnumChatFormat.YELLOW));
                    IChatBaseComponent component3 = (new ChatMessage("death.attack.even_more_magic", this.getScoreboardDisplayName())).format((style) -> {
                        return style.setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, component2));
                    });
                    this.connection.sendPacket(new PacketPlayOutCombatKill(this.getCombatTracker(), component3));
                }

            });
            ScoreboardTeamBase team = this.getScoreboardTeam();
            if (team != null && team.getDeathMessageVisibility() != ScoreboardTeamBase.EnumNameTagVisibility.ALWAYS) {
                if (team.getDeathMessageVisibility() == ScoreboardTeamBase.EnumNameTagVisibility.HIDE_FOR_OTHER_TEAMS) {
                    this.server.getPlayerList().broadcastToTeam(this, component);
                } else if (team.getDeathMessageVisibility() == ScoreboardTeamBase.EnumNameTagVisibility.HIDE_FOR_OWN_TEAM) {
                    this.server.getPlayerList().broadcastToAllExceptTeam(this, component);
                }
            } else {
                this.server.getPlayerList().sendMessage(component, ChatMessageType.SYSTEM, SystemUtils.NIL_UUID);
            }
        } else {
            this.connection.sendPacket(new PacketPlayOutCombatKill(this.getCombatTracker(), ChatComponentText.EMPTY));
        }

        this.releaseShoulderEntities();
        if (this.level.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            this.tellNeutralMobsThatIDied();
        }

        if (!this.isSpectator()) {
            this.dropAllDeathLoot(source);
        }

        this.getScoreboard().getObjectivesForCriteria(IScoreboardCriteria.DEATH_COUNT, this.getName(), ScoreboardScore::incrementScore);
        EntityLiving livingEntity = this.getKillingEntity();
        if (livingEntity != null) {
            this.awardStat(StatisticList.ENTITY_KILLED_BY.get(livingEntity.getEntityType()));
            livingEntity.awardKillScore(this, this.deathScore, source);
            this.createWitherRose(livingEntity);
        }

        this.level.broadcastEntityEffect(this, (byte)3);
        this.awardStat(StatisticList.DEATHS);
        this.resetStat(StatisticList.CUSTOM.get(StatisticList.TIME_SINCE_DEATH));
        this.resetStat(StatisticList.CUSTOM.get(StatisticList.TIME_SINCE_REST));
        this.extinguish();
        this.setTicksFrozen(0);
        this.setSharedFlagOnFire(false);
        this.getCombatTracker().recheckStatus();
    }

    private void tellNeutralMobsThatIDied() {
        AxisAlignedBB aABB = (new AxisAlignedBB(this.getChunkCoordinates())).grow(32.0D, 10.0D, 32.0D);
        this.level.getEntitiesOfClass(EntityInsentient.class, aABB, IEntitySelector.NO_SPECTATORS).stream().filter((entity) -> {
            return entity instanceof IEntityAngerable;
        }).forEach((entity) -> {
            ((IEntityAngerable)entity).playerDied(this);
        });
    }

    @Override
    public void awardKillScore(Entity killer, int score, DamageSource damageSource) {
        if (killer != this) {
            super.awardKillScore(killer, score, damageSource);
            this.addScore(score);
            String string = this.getName();
            String string2 = killer.getName();
            this.getScoreboard().getObjectivesForCriteria(IScoreboardCriteria.KILL_COUNT_ALL, string, ScoreboardScore::incrementScore);
            if (killer instanceof EntityHuman) {
                this.awardStat(StatisticList.PLAYER_KILLS);
                this.getScoreboard().getObjectivesForCriteria(IScoreboardCriteria.KILL_COUNT_PLAYERS, string, ScoreboardScore::incrementScore);
            } else {
                this.awardStat(StatisticList.MOB_KILLS);
            }

            this.handleTeamKill(string, string2, IScoreboardCriteria.TEAM_KILL);
            this.handleTeamKill(string2, string, IScoreboardCriteria.KILLED_BY_TEAM);
            CriterionTriggers.PLAYER_KILLED_ENTITY.trigger(this, killer, damageSource);
        }
    }

    private void handleTeamKill(String playerName, String team, IScoreboardCriteria[] criterions) {
        ScoreboardTeam playerTeam = this.getScoreboard().getPlayerTeam(team);
        if (playerTeam != null) {
            int i = playerTeam.getColor().getId();
            if (i >= 0 && i < criterions.length) {
                this.getScoreboard().getObjectivesForCriteria(criterions[i], playerName, ScoreboardScore::incrementScore);
            }
        }

    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else {
            boolean bl = this.server.isDedicatedServer() && this.canPvP() && "fall".equals(source.msgId);
            if (!bl && this.spawnInvulnerableTime > 0 && source != DamageSource.OUT_OF_WORLD) {
                return false;
            } else {
                if (source instanceof EntityDamageSource) {
                    Entity entity = source.getEntity();
                    if (entity instanceof EntityHuman && !this.canHarmPlayer((EntityHuman)entity)) {
                        return false;
                    }

                    if (entity instanceof EntityArrow) {
                        EntityArrow abstractArrow = (EntityArrow)entity;
                        Entity entity2 = abstractArrow.getShooter();
                        if (entity2 instanceof EntityHuman && !this.canHarmPlayer((EntityHuman)entity2)) {
                            return false;
                        }
                    }
                }

                return super.damageEntity(source, amount);
            }
        }
    }

    @Override
    public boolean canHarmPlayer(EntityHuman player) {
        return !this.canPvP() ? false : super.canHarmPlayer(player);
    }

    private boolean canPvP() {
        return this.server.getPVP();
    }

    @Nullable
    @Override
    protected ShapeDetectorShape findDimensionEntryPoint(WorldServer destination) {
        ShapeDetectorShape portalInfo = super.findDimensionEntryPoint(destination);
        if (portalInfo != null && this.level.getDimensionKey() == World.OVERWORLD && destination.getDimensionKey() == World.END) {
            Vec3D vec3 = portalInfo.pos.add(0.0D, -1.0D, 0.0D);
            return new ShapeDetectorShape(vec3, Vec3D.ZERO, 90.0F, 0.0F);
        } else {
            return portalInfo;
        }
    }

    @Nullable
    @Override
    public Entity changeDimension(WorldServer destination) {
        this.isChangingDimension = true;
        WorldServer serverLevel = this.getWorldServer();
        ResourceKey<World> resourceKey = serverLevel.getDimensionKey();
        if (resourceKey == World.END && destination.getDimensionKey() == World.OVERWORLD) {
            this.decouple();
            this.getWorldServer().removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
            if (!this.wonGame) {
                this.wonGame = true;
                this.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.WIN_GAME, this.seenCredits ? 0.0F : 1.0F));
                this.seenCredits = true;
            }

            return this;
        } else {
            WorldData levelData = destination.getWorldData();
            this.connection.sendPacket(new PacketPlayOutRespawn(destination.getDimensionManager(), destination.getDimensionKey(), BiomeManager.obfuscateSeed(destination.getSeed()), this.gameMode.getGameMode(), this.gameMode.getPreviousGameModeForPlayer(), destination.isDebugWorld(), destination.isFlatWorld(), true));
            this.connection.sendPacket(new PacketPlayOutServerDifficulty(levelData.getDifficulty(), levelData.isDifficultyLocked()));
            PlayerList playerList = this.server.getPlayerList();
            playerList.sendPlayerPermissionLevel(this);
            serverLevel.removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
            this.unsetRemoved();
            ShapeDetectorShape portalInfo = this.findDimensionEntryPoint(destination);
            if (portalInfo != null) {
                serverLevel.getMethodProfiler().enter("moving");
                if (resourceKey == World.OVERWORLD && destination.getDimensionKey() == World.NETHER) {
                    this.enteredNetherPosition = this.getPositionVector();
                } else if (destination.getDimensionKey() == World.END) {
                    this.createEndPlatform(destination, new BlockPosition(portalInfo.pos));
                }

                serverLevel.getMethodProfiler().exit();
                serverLevel.getMethodProfiler().enter("placing");
                this.spawnIn(destination);
                destination.addPlayerPortal(this);
                this.setYawPitch(portalInfo.yRot, portalInfo.xRot);
                this.teleportAndSync(portalInfo.pos.x, portalInfo.pos.y, portalInfo.pos.z);
                serverLevel.getMethodProfiler().exit();
                this.triggerDimensionAdvancements(serverLevel);
                this.connection.sendPacket(new PacketPlayOutAbilities(this.getAbilities()));
                playerList.sendLevelInfo(this, destination);
                playerList.updateClient(this);

                for(MobEffect mobEffectInstance : this.getEffects()) {
                    this.connection.sendPacket(new PacketPlayOutEntityEffect(this.getId(), mobEffectInstance));
                }

                this.connection.sendPacket(new PacketPlayOutWorldEvent(1032, BlockPosition.ZERO, 0, false));
                this.lastSentExp = -1;
                this.lastSentHealth = -1.0F;
                this.lastSentFood = -1;
            }

            return this;
        }
    }

    private void createEndPlatform(WorldServer world, BlockPosition centerPos) {
        BlockPosition.MutableBlockPosition mutableBlockPos = centerPos.mutable();

        for(int i = -2; i <= 2; ++i) {
            for(int j = -2; j <= 2; ++j) {
                for(int k = -1; k < 3; ++k) {
                    IBlockData blockState = k == -1 ? Blocks.OBSIDIAN.getBlockData() : Blocks.AIR.getBlockData();
                    world.setTypeUpdate(mutableBlockPos.set(centerPos).move(j, k, i), blockState);
                }
            }
        }

    }

    @Override
    protected Optional<BlockUtil.Rectangle> getExitPortal(WorldServer destWorld, BlockPosition destPos, boolean destIsNether, WorldBorder worldBorder) {
        Optional<BlockUtil.Rectangle> optional = super.getExitPortal(destWorld, destPos, destIsNether, worldBorder);
        if (optional.isPresent()) {
            return optional;
        } else {
            EnumDirection.EnumAxis axis = this.level.getType(this.portalEntrancePos).getOptionalValue(BlockPortal.AXIS).orElse(EnumDirection.EnumAxis.X);
            Optional<BlockUtil.Rectangle> optional2 = destWorld.getTravelAgent().createPortal(destPos, axis);
            if (!optional2.isPresent()) {
                LOGGER.error("Unable to create a portal, likely target out of worldborder");
            }

            return optional2;
        }
    }

    public void triggerDimensionAdvancements(WorldServer origin) {
        ResourceKey<World> resourceKey = origin.getDimensionKey();
        ResourceKey<World> resourceKey2 = this.level.getDimensionKey();
        CriterionTriggers.CHANGED_DIMENSION.trigger(this, resourceKey, resourceKey2);
        if (resourceKey == World.NETHER && resourceKey2 == World.OVERWORLD && this.enteredNetherPosition != null) {
            CriterionTriggers.NETHER_TRAVEL.trigger(this, this.enteredNetherPosition);
        }

        if (resourceKey2 != World.NETHER) {
            this.enteredNetherPosition = null;
        }

    }

    @Override
    public boolean broadcastToPlayer(EntityPlayer spectator) {
        if (spectator.isSpectator()) {
            return this.getSpecatorTarget() == this;
        } else {
            return this.isSpectator() ? false : super.broadcastToPlayer(spectator);
        }
    }

    @Override
    public void receive(Entity item, int count) {
        super.receive(item, count);
        this.containerMenu.broadcastChanges();
    }

    @Override
    public Either<EntityHuman.EnumBedResult, Unit> sleep(BlockPosition pos) {
        EnumDirection direction = this.level.getType(pos).get(BlockFacingHorizontal.FACING);
        if (!this.isSleeping() && this.isAlive()) {
            if (!this.level.getDimensionManager().isNatural()) {
                return Either.left(EntityHuman.EnumBedResult.NOT_POSSIBLE_HERE);
            } else if (!this.bedInRange(pos, direction)) {
                return Either.left(EntityHuman.EnumBedResult.TOO_FAR_AWAY);
            } else if (this.bedBlocked(pos, direction)) {
                return Either.left(EntityHuman.EnumBedResult.OBSTRUCTED);
            } else {
                this.setRespawnPosition(this.level.getDimensionKey(), pos, this.getYRot(), false, true);
                if (this.level.isDay()) {
                    return Either.left(EntityHuman.EnumBedResult.NOT_POSSIBLE_NOW);
                } else {
                    if (!this.isCreative()) {
                        double d = 8.0D;
                        double e = 5.0D;
                        Vec3D vec3 = Vec3D.atBottomCenterOf(pos);
                        List<EntityMonster> list = this.level.getEntitiesOfClass(EntityMonster.class, new AxisAlignedBB(vec3.getX() - 8.0D, vec3.getY() - 5.0D, vec3.getZ() - 8.0D, vec3.getX() + 8.0D, vec3.getY() + 5.0D, vec3.getZ() + 8.0D), (monster) -> {
                            return monster.isPreventingPlayerRest(this);
                        });
                        if (!list.isEmpty()) {
                            return Either.left(EntityHuman.EnumBedResult.NOT_SAFE);
                        }
                    }

                    Either<EntityHuman.EnumBedResult, Unit> either = super.sleep(pos).ifRight((unit) -> {
                        this.awardStat(StatisticList.SLEEP_IN_BED);
                        CriterionTriggers.SLEPT_IN_BED.trigger(this);
                    });
                    if (!this.getWorldServer().canSleepThroughNights()) {
                        this.displayClientMessage(new ChatMessage("sleep.not_possible"), true);
                    }

                    ((WorldServer)this.level).everyoneSleeping();
                    return either;
                }
            }
        } else {
            return Either.left(EntityHuman.EnumBedResult.OTHER_PROBLEM);
        }
    }

    @Override
    public void entitySleep(BlockPosition pos) {
        this.resetStat(StatisticList.CUSTOM.get(StatisticList.TIME_SINCE_REST));
        super.entitySleep(pos);
    }

    private boolean bedInRange(BlockPosition pos, EnumDirection direction) {
        return this.isReachableBedBlock(pos) || this.isReachableBedBlock(pos.relative(direction.opposite()));
    }

    private boolean isReachableBedBlock(BlockPosition pos) {
        Vec3D vec3 = Vec3D.atBottomCenterOf(pos);
        return Math.abs(this.locX() - vec3.getX()) <= 3.0D && Math.abs(this.locY() - vec3.getY()) <= 2.0D && Math.abs(this.locZ() - vec3.getZ()) <= 3.0D;
    }

    private boolean bedBlocked(BlockPosition pos, EnumDirection direction) {
        BlockPosition blockPos = pos.above();
        return !this.freeAt(blockPos) || !this.freeAt(blockPos.relative(direction.opposite()));
    }

    @Override
    public void wakeup(boolean skipSleepTimer, boolean updateSleepingPlayers) {
        if (this.isSleeping()) {
            this.getWorldServer().getChunkSource().broadcastIncludingSelf(this, new PacketPlayOutAnimation(this, 2));
        }

        super.wakeup(skipSleepTimer, updateSleepingPlayers);
        if (this.connection != null) {
            this.connection.teleport(this.locX(), this.locY(), this.locZ(), this.getYRot(), this.getXRot());
        }

    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        Entity entity2 = this.getVehicle();
        if (!super.startRiding(entity, force)) {
            return false;
        } else {
            Entity entity3 = this.getVehicle();
            if (entity3 != entity2 && this.connection != null) {
                this.connection.teleport(this.locX(), this.locY(), this.locZ(), this.getYRot(), this.getXRot());
            }

            return true;
        }
    }

    @Override
    public void stopRiding() {
        Entity entity = this.getVehicle();
        super.stopRiding();
        Entity entity2 = this.getVehicle();
        if (entity2 != entity && this.connection != null) {
            this.connection.dismount(this.locX(), this.locY(), this.locZ(), this.getYRot(), this.getXRot());
        }

    }

    @Override
    public void dismountTo(double destX, double destY, double destZ) {
        this.removeVehicle();
        if (this.connection != null) {
            this.connection.dismount(destX, destY, destZ, this.getYRot(), this.getXRot());
        }

    }

    @Override
    public boolean isInvulnerable(DamageSource damageSource) {
        return super.isInvulnerable(damageSource) || this.isChangingDimension() || this.getAbilities().invulnerable && damageSource == DamageSource.WITHER;
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, IBlockData landedState, BlockPosition landedPosition) {
    }

    @Override
    protected void onChangedBlock(BlockPosition pos) {
        if (!this.isSpectator()) {
            super.onChangedBlock(pos);
        }

    }

    public void doCheckFallDamage(double heightDifference, boolean onGround) {
        if (!this.touchingUnloadedChunk()) {
            BlockPosition blockPos = this.getOnPos();
            super.checkFallDamage(heightDifference, onGround, this.level.getType(blockPos), blockPos);
        }
    }

    @Override
    public void openSign(TileEntitySign sign) {
        sign.setAllowedPlayerEditor(this.getUniqueID());
        this.connection.sendPacket(new PacketPlayOutBlockChange(this.level, sign.getPosition()));
        this.connection.sendPacket(new PacketPlayOutOpenSignEditor(sign.getPosition()));
    }

    public void nextContainerCounter() {
        this.containerCounter = this.containerCounter % 100 + 1;
    }

    @Override
    public OptionalInt openContainer(@Nullable ITileInventory factory) {
        if (factory == null) {
            return OptionalInt.empty();
        } else {
            if (this.containerMenu != this.inventoryMenu) {
                this.closeInventory();
            }

            this.nextContainerCounter();
            Container abstractContainerMenu = factory.createMenu(this.containerCounter, this.getInventory(), this);
            if (abstractContainerMenu == null) {
                if (this.isSpectator()) {
                    this.displayClientMessage((new ChatMessage("container.spectatorCantOpen")).withStyle(EnumChatFormat.RED), true);
                }

                return OptionalInt.empty();
            } else {
                this.connection.sendPacket(new PacketPlayOutOpenWindow(abstractContainerMenu.containerId, abstractContainerMenu.getType(), factory.getScoreboardDisplayName()));
                this.initMenu(abstractContainerMenu);
                this.containerMenu = abstractContainerMenu;
                return OptionalInt.of(this.containerCounter);
            }
        }
    }

    @Override
    public void openTrade(int syncId, MerchantRecipeList offers, int levelProgress, int experience, boolean leveled, boolean refreshable) {
        this.connection.sendPacket(new PacketPlayOutOpenWindowMerchant(syncId, offers, levelProgress, experience, leveled, refreshable));
    }

    @Override
    public void openHorseInventory(EntityHorseAbstract horse, IInventory inventory) {
        if (this.containerMenu != this.inventoryMenu) {
            this.closeInventory();
        }

        this.nextContainerCounter();
        this.connection.sendPacket(new PacketPlayOutOpenWindowHorse(this.containerCounter, inventory.getSize(), horse.getId()));
        this.containerMenu = new ContainerHorse(this.containerCounter, this.getInventory(), inventory, horse);
        this.initMenu(this.containerMenu);
    }

    @Override
    public void openBook(ItemStack book, EnumHand hand) {
        if (book.is(Items.WRITTEN_BOOK)) {
            if (ItemWrittenBook.resolveBookComponents(book, this.getCommandListener(), this)) {
                this.containerMenu.broadcastChanges();
            }

            this.connection.sendPacket(new PacketPlayOutOpenBook(hand));
        }

    }

    @Override
    public void openCommandBlock(TileEntityCommand commandBlock) {
        this.connection.sendPacket(PacketPlayOutTileEntityData.create(commandBlock, TileEntity::saveWithoutMetadata));
    }

    @Override
    public void closeInventory() {
        this.connection.sendPacket(new PacketPlayOutCloseWindow(this.containerMenu.containerId));
        this.doCloseContainer();
    }

    public void doCloseContainer() {
        this.containerMenu.removed(this);
        this.inventoryMenu.transferState(this.containerMenu);
        this.containerMenu = this.inventoryMenu;
    }

    public void setPlayerInput(float sidewaysSpeed, float forwardSpeed, boolean jumping, boolean sneaking) {
        if (this.isPassenger()) {
            if (sidewaysSpeed >= -1.0F && sidewaysSpeed <= 1.0F) {
                this.xxa = sidewaysSpeed;
            }

            if (forwardSpeed >= -1.0F && forwardSpeed <= 1.0F) {
                this.zza = forwardSpeed;
            }

            this.jumping = jumping;
            this.setSneaking(sneaking);
        }

    }

    @Override
    public void awardStat(Statistic<?> stat, int amount) {
        this.stats.increment(this, stat, amount);
        this.getScoreboard().getObjectivesForCriteria(stat, this.getName(), (score) -> {
            score.addScore(amount);
        });
    }

    @Override
    public void resetStat(Statistic<?> stat) {
        this.stats.setStatistic(this, stat, 0);
        this.getScoreboard().getObjectivesForCriteria(stat, this.getName(), ScoreboardScore::reset);
    }

    @Override
    public int discoverRecipes(Collection<IRecipe<?>> recipes) {
        return this.recipeBook.addRecipes(recipes, this);
    }

    @Override
    public void awardRecipesByKey(MinecraftKey[] ids) {
        List<IRecipe<?>> list = Lists.newArrayList();

        for(MinecraftKey resourceLocation : ids) {
            this.server.getCraftingManager().getRecipe(resourceLocation).ifPresent(list::add);
        }

        this.discoverRecipes(list);
    }

    @Override
    public int undiscoverRecipes(Collection<IRecipe<?>> recipes) {
        return this.recipeBook.removeRecipes(recipes, this);
    }

    @Override
    public void giveExp(int experience) {
        super.giveExp(experience);
        this.lastSentExp = -1;
    }

    public void disconnect() {
        this.disconnected = true;
        this.ejectPassengers();
        if (this.isSleeping()) {
            this.wakeup(true, false);
        }

    }

    public boolean hasDisconnected() {
        return this.disconnected;
    }

    public void triggerHealthUpdate() {
        this.lastSentHealth = -1.0E8F;
    }

    @Override
    public void displayClientMessage(IChatBaseComponent message, boolean actionBar) {
        this.sendMessage(message, actionBar ? ChatMessageType.GAME_INFO : ChatMessageType.CHAT, SystemUtils.NIL_UUID);
    }

    @Override
    protected void completeUsingItem() {
        if (!this.useItem.isEmpty() && this.isHandRaised()) {
            this.connection.sendPacket(new PacketPlayOutEntityStatus(this, (byte)9));
            super.completeUsingItem();
        }

    }

    @Override
    public void lookAt(ArgumentAnchor.Anchor anchorPoint, Vec3D target) {
        super.lookAt(anchorPoint, target);
        this.connection.sendPacket(new PacketPlayOutLookAt(anchorPoint, target.x, target.y, target.z));
    }

    public void lookAt(ArgumentAnchor.Anchor anchorPoint, Entity targetEntity, ArgumentAnchor.Anchor targetAnchor) {
        Vec3D vec3 = targetAnchor.apply(targetEntity);
        super.lookAt(anchorPoint, vec3);
        this.connection.sendPacket(new PacketPlayOutLookAt(anchorPoint, targetEntity, targetAnchor));
    }

    public void copyFrom(EntityPlayer oldPlayer, boolean alive) {
        this.textFilteringEnabled = oldPlayer.textFilteringEnabled;
        this.gameMode.setGameModeForPlayer(oldPlayer.gameMode.getGameMode(), oldPlayer.gameMode.getPreviousGameModeForPlayer());
        if (alive) {
            this.getInventory().replaceWith(oldPlayer.getInventory());
            this.setHealth(oldPlayer.getHealth());
            this.foodData = oldPlayer.foodData;
            this.experienceLevel = oldPlayer.experienceLevel;
            this.totalExperience = oldPlayer.totalExperience;
            this.experienceProgress = oldPlayer.experienceProgress;
            this.setScore(oldPlayer.getScore());
            this.portalEntrancePos = oldPlayer.portalEntrancePos;
        } else if (this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || oldPlayer.isSpectator()) {
            this.getInventory().replaceWith(oldPlayer.getInventory());
            this.experienceLevel = oldPlayer.experienceLevel;
            this.totalExperience = oldPlayer.totalExperience;
            this.experienceProgress = oldPlayer.experienceProgress;
            this.setScore(oldPlayer.getScore());
        }

        this.enchantmentSeed = oldPlayer.enchantmentSeed;
        this.enderChestInventory = oldPlayer.enderChestInventory;
        this.getDataWatcher().set(DATA_PLAYER_MODE_CUSTOMISATION, oldPlayer.getDataWatcher().get(DATA_PLAYER_MODE_CUSTOMISATION));
        this.lastSentExp = -1;
        this.lastSentHealth = -1.0F;
        this.lastSentFood = -1;
        this.recipeBook.copyOverData(oldPlayer.recipeBook);
        this.seenCredits = oldPlayer.seenCredits;
        this.enteredNetherPosition = oldPlayer.enteredNetherPosition;
        this.setShoulderEntityLeft(oldPlayer.getShoulderEntityLeft());
        this.setShoulderEntityRight(oldPlayer.getShoulderEntityRight());
    }

    @Override
    protected void onEffectAdded(MobEffect effect, @Nullable Entity source) {
        super.onEffectAdded(effect, source);
        this.connection.sendPacket(new PacketPlayOutEntityEffect(this.getId(), effect));
        if (effect.getMobEffect() == MobEffectList.LEVITATION) {
            this.levitationStartTime = this.tickCount;
            this.levitationStartPos = this.getPositionVector();
        }

        CriterionTriggers.EFFECTS_CHANGED.trigger(this, source);
    }

    @Override
    protected void onEffectUpdated(MobEffect effect, boolean reapplyEffect, @Nullable Entity source) {
        super.onEffectUpdated(effect, reapplyEffect, source);
        this.connection.sendPacket(new PacketPlayOutEntityEffect(this.getId(), effect));
        CriterionTriggers.EFFECTS_CHANGED.trigger(this, source);
    }

    @Override
    protected void onEffectRemoved(MobEffect effect) {
        super.onEffectRemoved(effect);
        this.connection.sendPacket(new PacketPlayOutRemoveEntityEffect(this.getId(), effect.getMobEffect()));
        if (effect.getMobEffect() == MobEffectList.LEVITATION) {
            this.levitationStartPos = null;
        }

        CriterionTriggers.EFFECTS_CHANGED.trigger(this, (Entity)null);
    }

    @Override
    public void enderTeleportTo(double destX, double destY, double destZ) {
        this.connection.teleport(destX, destY, destZ, this.getYRot(), this.getXRot());
    }

    @Override
    public void teleportAndSync(double x, double y, double z) {
        this.enderTeleportTo(x, y, z);
        this.connection.syncPosition();
    }

    @Override
    public void crit(Entity target) {
        this.getWorldServer().getChunkSource().broadcastIncludingSelf(this, new PacketPlayOutAnimation(target, 4));
    }

    @Override
    public void magicCrit(Entity target) {
        this.getWorldServer().getChunkSource().broadcastIncludingSelf(this, new PacketPlayOutAnimation(target, 5));
    }

    @Override
    public void updateAbilities() {
        if (this.connection != null) {
            this.connection.sendPacket(new PacketPlayOutAbilities(this.getAbilities()));
            this.updateInvisibilityStatus();
        }
    }

    @Override
    public WorldServer getWorldServer() {
        return (WorldServer)this.level;
    }

    public boolean setGameMode(EnumGamemode gameMode) {
        if (!this.gameMode.setGameMode(gameMode)) {
            return false;
        } else {
            this.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.CHANGE_GAME_MODE, (float)gameMode.getId()));
            if (gameMode == EnumGamemode.SPECTATOR) {
                this.releaseShoulderEntities();
                this.stopRiding();
            } else {
                this.setSpectatorTarget(this);
            }

            this.updateAbilities();
            this.updateEffectVisibility();
            return true;
        }
    }

    @Override
    public boolean isSpectator() {
        return this.gameMode.getGameMode() == EnumGamemode.SPECTATOR;
    }

    @Override
    public boolean isCreative() {
        return this.gameMode.getGameMode() == EnumGamemode.CREATIVE;
    }

    @Override
    public void sendMessage(IChatBaseComponent message, UUID sender) {
        this.sendMessage(message, ChatMessageType.SYSTEM, sender);
    }

    public void sendMessage(IChatBaseComponent message, ChatMessageType type, UUID sender) {
        if (this.acceptsChat(type)) {
            this.connection.send(new PacketPlayOutChat(message, type, sender), (future) -> {
                if (!future.isSuccess() && (type == ChatMessageType.GAME_INFO || type == ChatMessageType.SYSTEM) && this.acceptsChat(ChatMessageType.SYSTEM)) {
                    int i = 256;
                    String string = message.getString(256);
                    IChatBaseComponent component2 = (new ChatComponentText(string)).withStyle(EnumChatFormat.YELLOW);
                    this.connection.sendPacket(new PacketPlayOutChat((new ChatMessage("multiplayer.message_not_delivered", component2)).withStyle(EnumChatFormat.RED), ChatMessageType.SYSTEM, sender));
                }

            });
        }
    }

    public String getIpAddress() {
        String string = this.connection.connection.getSocketAddress().toString();
        string = string.substring(string.indexOf("/") + 1);
        return string.substring(0, string.indexOf(":"));
    }

    public void updateOptions(PacketPlayInSettings packet) {
        this.chatVisibility = packet.chatVisibility();
        this.canChatColor = packet.chatColors();
        this.textFilteringEnabled = packet.textFilteringEnabled();
        this.allowsListing = packet.allowsListing();
        this.getDataWatcher().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte)packet.modelCustomisation());
        this.getDataWatcher().set(DATA_PLAYER_MAIN_HAND, (byte)(packet.mainHand() == EnumMainHand.LEFT ? 0 : 1));
    }

    public boolean canChatInColor() {
        return this.canChatColor;
    }

    public EnumChatVisibility getChatFlags() {
        return this.chatVisibility;
    }

    private boolean acceptsChat(ChatMessageType type) {
        switch(this.chatVisibility) {
        case HIDDEN:
            return type == ChatMessageType.GAME_INFO;
        case SYSTEM:
            return type == ChatMessageType.SYSTEM || type == ChatMessageType.GAME_INFO;
        case FULL:
        default:
            return true;
        }
    }

    public void setResourcePack(String url, String hash, boolean required, @Nullable IChatBaseComponent resourcePackPrompt) {
        this.connection.sendPacket(new PacketPlayOutResourcePackSend(url, hash, required, resourcePackPrompt));
    }

    @Override
    protected int getPermissionLevel() {
        return this.server.getProfilePermissions(this.getProfile());
    }

    public void resetIdleTimer() {
        this.lastActionTime = SystemUtils.getMonotonicMillis();
    }

    public StatisticManagerServer getStatisticManager() {
        return this.stats;
    }

    public RecipeBookServer getRecipeBook() {
        return this.recipeBook;
    }

    @Override
    protected void updateInvisibilityStatus() {
        if (this.isSpectator()) {
            this.removeEffectParticles();
            this.setInvisible(true);
        } else {
            super.updateInvisibilityStatus();
        }

    }

    public Entity getSpecatorTarget() {
        return (Entity)(this.camera == null ? this : this.camera);
    }

    public void setSpectatorTarget(@Nullable Entity entity) {
        Entity entity2 = this.getSpecatorTarget();
        this.camera = (Entity)(entity == null ? this : entity);
        if (entity2 != this.camera) {
            this.connection.sendPacket(new PacketPlayOutCamera(this.camera));
            this.enderTeleportTo(this.camera.locX(), this.camera.locY(), this.camera.locZ());
        }

    }

    @Override
    protected void processPortalCooldown() {
        if (!this.isChangingDimension) {
            super.processPortalCooldown();
        }

    }

    @Override
    public void attack(Entity target) {
        if (this.gameMode.getGameMode() == EnumGamemode.SPECTATOR) {
            this.setSpectatorTarget(target);
        } else {
            super.attack(target);
        }

    }

    public long getLastActionTime() {
        return this.lastActionTime;
    }

    @Nullable
    public IChatBaseComponent getPlayerListName() {
        return null;
    }

    @Override
    public void swingHand(EnumHand hand) {
        super.swingHand(hand);
        this.resetAttackCooldown();
    }

    public boolean isChangingDimension() {
        return this.isChangingDimension;
    }

    public void hasChangedDimension() {
        this.isChangingDimension = false;
    }

    public AdvancementDataPlayer getAdvancementData() {
        return this.advancements;
    }

    public void teleportTo(WorldServer targetWorld, double x, double y, double z, float yaw, float pitch) {
        this.setSpectatorTarget(this);
        this.stopRiding();
        if (targetWorld == this.level) {
            this.connection.teleport(x, y, z, yaw, pitch);
        } else {
            WorldServer serverLevel = this.getWorldServer();
            WorldData levelData = targetWorld.getWorldData();
            this.connection.sendPacket(new PacketPlayOutRespawn(targetWorld.getDimensionManager(), targetWorld.getDimensionKey(), BiomeManager.obfuscateSeed(targetWorld.getSeed()), this.gameMode.getGameMode(), this.gameMode.getPreviousGameModeForPlayer(), targetWorld.isDebugWorld(), targetWorld.isFlatWorld(), true));
            this.connection.sendPacket(new PacketPlayOutServerDifficulty(levelData.getDifficulty(), levelData.isDifficultyLocked()));
            this.server.getPlayerList().sendPlayerPermissionLevel(this);
            serverLevel.removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
            this.unsetRemoved();
            this.setPositionRotation(x, y, z, yaw, pitch);
            this.spawnIn(targetWorld);
            targetWorld.addPlayerCommand(this);
            this.triggerDimensionAdvancements(serverLevel);
            this.connection.teleport(x, y, z, yaw, pitch);
            this.server.getPlayerList().sendLevelInfo(this, targetWorld);
            this.server.getPlayerList().updateClient(this);
        }

    }

    @Nullable
    public BlockPosition getSpawn() {
        return this.respawnPosition;
    }

    public float getSpawnAngle() {
        return this.respawnAngle;
    }

    public ResourceKey<World> getSpawnDimension() {
        return this.respawnDimension;
    }

    public boolean isSpawnForced() {
        return this.respawnForced;
    }

    public void setRespawnPosition(ResourceKey<World> dimension, @Nullable BlockPosition pos, float angle, boolean forced, boolean sendMessage) {
        if (pos != null) {
            boolean bl = pos.equals(this.respawnPosition) && dimension.equals(this.respawnDimension);
            if (sendMessage && !bl) {
                this.sendMessage(new ChatMessage("block.minecraft.set_spawn"), SystemUtils.NIL_UUID);
            }

            this.respawnPosition = pos;
            this.respawnDimension = dimension;
            this.respawnAngle = angle;
            this.respawnForced = forced;
        } else {
            this.respawnPosition = null;
            this.respawnDimension = World.OVERWORLD;
            this.respawnAngle = 0.0F;
            this.respawnForced = false;
        }

    }

    public void trackChunk(ChunkCoordIntPair chunkPos, Packet<?> chunkDataPacket) {
        this.connection.sendPacket(chunkDataPacket);
    }

    public void untrackChunk(ChunkCoordIntPair chunkPos) {
        if (this.isAlive()) {
            this.connection.sendPacket(new PacketPlayOutUnloadChunk(chunkPos.x, chunkPos.z));
        }

    }

    public SectionPosition getLastSectionPos() {
        return this.lastSectionPos;
    }

    public void setLastSectionPos(SectionPosition section) {
        this.lastSectionPos = section;
    }

    @Override
    public void playNotifySound(SoundEffect event, EnumSoundCategory category, float volume, float pitch) {
        this.connection.sendPacket(new PacketPlayOutNamedSoundEffect(event, category, this.locX(), this.locY(), this.locZ(), volume, pitch));
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutNamedEntitySpawn(this);
    }

    @Override
    public EntityItem drop(ItemStack stack, boolean throwRandomly, boolean retainOwnership) {
        EntityItem itemEntity = super.drop(stack, throwRandomly, retainOwnership);
        if (itemEntity == null) {
            return null;
        } else {
            this.level.addEntity(itemEntity);
            ItemStack itemStack = itemEntity.getItemStack();
            if (retainOwnership) {
                if (!itemStack.isEmpty()) {
                    this.awardStat(StatisticList.ITEM_DROPPED.get(itemStack.getItem()), stack.getCount());
                }

                this.awardStat(StatisticList.DROP);
            }

            return itemEntity;
        }
    }

    public ITextFilter getTextFilter() {
        return this.textFilter;
    }

    public void spawnIn(WorldServer world) {
        this.level = world;
        this.gameMode.setLevel(world);
    }

    @Nullable
    private static EnumGamemode readPlayerMode(@Nullable NBTTagCompound nbt, String key) {
        return nbt != null && nbt.hasKeyOfType(key, 99) ? EnumGamemode.getById(nbt.getInt(key)) : null;
    }

    private EnumGamemode calculateGameModeForNewPlayer(@Nullable EnumGamemode backupGameMode) {
        EnumGamemode gameType = this.server.getForcedGameType();
        if (gameType != null) {
            return gameType;
        } else {
            return backupGameMode != null ? backupGameMode : this.server.getGamemode();
        }
    }

    public void loadGameTypes(@Nullable NBTTagCompound nbt) {
        this.gameMode.setGameModeForPlayer(this.calculateGameModeForNewPlayer(readPlayerMode(nbt, "playerGameType")), readPlayerMode(nbt, "previousPlayerGameType"));
    }

    private void storeGameTypes(NBTTagCompound nbt) {
        nbt.setInt("playerGameType", this.gameMode.getGameMode().getId());
        EnumGamemode gameType = this.gameMode.getPreviousGameModeForPlayer();
        if (gameType != null) {
            nbt.setInt("previousPlayerGameType", gameType.getId());
        }

    }

    public boolean isTextFilteringEnabled() {
        return this.textFilteringEnabled;
    }

    public boolean shouldFilterMessageTo(EntityPlayer player) {
        if (player == this) {
            return false;
        } else {
            return this.textFilteringEnabled || player.textFilteringEnabled;
        }
    }

    @Override
    public boolean mayInteract(World world, BlockPosition pos) {
        return super.mayInteract(world, pos) && world.mayInteract(this, pos);
    }

    @Override
    protected void updateUsingItem(ItemStack stack) {
        CriterionTriggers.USING_ITEM.trigger(this, stack);
        super.updateUsingItem(stack);
    }

    public boolean dropItem(boolean entireStack) {
        PlayerInventory inventory = this.getInventory();
        ItemStack itemStack = inventory.removeFromSelected(entireStack);
        this.containerMenu.findSlot(inventory, inventory.selected).ifPresent((i) -> {
            this.containerMenu.setRemoteSlot(i, inventory.getItemInHand());
        });
        return this.drop(itemStack, false, true) != null;
    }

    public boolean allowsListing() {
        return this.allowsListing;
    }
}
