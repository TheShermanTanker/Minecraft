package net.minecraft.world.level.dimension.end;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.data.worldgen.BiomeDecoratorGroups;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.BossBattleServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.PlayerChunk;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Unit;
import net.minecraft.world.BossBattle;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderCrystal;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonControllerPhase;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityEnderPortal;
import net.minecraft.world.level.block.state.pattern.ShapeDetector;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBuilder;
import net.minecraft.world.level.block.state.predicate.BlockPredicate;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenEndTrophy;
import net.minecraft.world.level.levelgen.feature.WorldGenEnder;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.phys.AxisAlignedBB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EnderDragonBattle {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_TICKS_BEFORE_DRAGON_RESPAWN = 1200;
    private static final int TIME_BETWEEN_CRYSTAL_SCANS = 100;
    private static final int TIME_BETWEEN_PLAYER_SCANS = 20;
    private static final int ARENA_SIZE_CHUNKS = 8;
    public static final int ARENA_TICKET_LEVEL = 9;
    private static final int GATEWAY_COUNT = 20;
    private static final int GATEWAY_DISTANCE = 96;
    public static final int DRAGON_SPAWN_Y = 128;
    private static final Predicate<Entity> VALID_PLAYER = IEntitySelector.ENTITY_STILL_ALIVE.and(IEntitySelector.withinDistance(0.0D, 128.0D, 0.0D, 192.0D));
    public final BossBattleServer dragonEvent = (BossBattleServer)(new BossBattleServer(new ChatMessage("entity.minecraft.ender_dragon"), BossBattle.BarColor.PINK, BossBattle.BarStyle.PROGRESS)).setPlayMusic(true).setCreateFog(true);
    public final WorldServer level;
    private final List<Integer> gateways = Lists.newArrayList();
    private final ShapeDetector exitPortalPattern;
    private int ticksSinceDragonSeen;
    private int crystalsAlive;
    private int ticksSinceCrystalsScanned;
    private int ticksSinceLastPlayerScan;
    private boolean dragonKilled;
    private boolean previouslyKilled;
    public UUID dragonUUID;
    private boolean needsStateScanning = true;
    public BlockPosition portalLocation;
    public EnumDragonRespawn respawnStage;
    private int respawnTime;
    private List<EntityEnderCrystal> respawnCrystals;

    public EnderDragonBattle(WorldServer world, long gatewaysSeed, NBTTagCompound nbt) {
        this.level = world;
        if (nbt.hasKey("NeedsStateScanning")) {
            this.needsStateScanning = nbt.getBoolean("NeedsStateScanning");
        }

        if (nbt.hasKeyOfType("DragonKilled", 99)) {
            if (nbt.hasUUID("Dragon")) {
                this.dragonUUID = nbt.getUUID("Dragon");
            }

            this.dragonKilled = nbt.getBoolean("DragonKilled");
            this.previouslyKilled = nbt.getBoolean("PreviouslyKilled");
            if (nbt.getBoolean("IsRespawning")) {
                this.respawnStage = EnumDragonRespawn.START;
            }

            if (nbt.hasKeyOfType("ExitPortalLocation", 10)) {
                this.portalLocation = GameProfileSerializer.readBlockPos(nbt.getCompound("ExitPortalLocation"));
            }
        } else {
            this.dragonKilled = true;
            this.previouslyKilled = true;
        }

        if (nbt.hasKeyOfType("Gateways", 9)) {
            NBTTagList listTag = nbt.getList("Gateways", 3);

            for(int i = 0; i < listTag.size(); ++i) {
                this.gateways.add(listTag.getInt(i));
            }
        } else {
            this.gateways.addAll(ContiguousSet.create(Range.closedOpen(0, 20), DiscreteDomain.integers()));
            Collections.shuffle(this.gateways, new Random(gatewaysSeed));
        }

        this.exitPortalPattern = ShapeDetectorBuilder.start().aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ").aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ").aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ").aisle("  ###  ", " #   # ", "#     #", "#  #  #", "#     #", " #   # ", "  ###  ").aisle("       ", "  ###  ", " ##### ", " ##### ", " ##### ", "  ###  ", "       ").where('#', ShapeDetectorBlock.hasState(BlockPredicate.forBlock(Blocks.BEDROCK))).build();
    }

    public NBTTagCompound saveData() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setBoolean("NeedsStateScanning", this.needsStateScanning);
        if (this.dragonUUID != null) {
            compoundTag.putUUID("Dragon", this.dragonUUID);
        }

        compoundTag.setBoolean("DragonKilled", this.dragonKilled);
        compoundTag.setBoolean("PreviouslyKilled", this.previouslyKilled);
        if (this.portalLocation != null) {
            compoundTag.set("ExitPortalLocation", GameProfileSerializer.writeBlockPos(this.portalLocation));
        }

        NBTTagList listTag = new NBTTagList();

        for(int i : this.gateways) {
            listTag.add(NBTTagInt.valueOf(i));
        }

        compoundTag.set("Gateways", listTag);
        return compoundTag;
    }

    public void tick() {
        this.dragonEvent.setVisible(!this.dragonKilled);
        if (++this.ticksSinceLastPlayerScan >= 20) {
            this.updatePlayers();
            this.ticksSinceLastPlayerScan = 0;
        }

        if (!this.dragonEvent.getPlayers().isEmpty()) {
            this.level.getChunkSource().addTicket(TicketType.DRAGON, new ChunkCoordIntPair(0, 0), 9, Unit.INSTANCE);
            boolean bl = this.isArenaLoaded();
            if (this.needsStateScanning && bl) {
                this.scanState();
                this.needsStateScanning = false;
            }

            if (this.respawnStage != null) {
                if (this.respawnCrystals == null && bl) {
                    this.respawnStage = null;
                    this.initiateRespawn();
                }

                this.respawnStage.tick(this.level, this, this.respawnCrystals, this.respawnTime++, this.portalLocation);
            }

            if (!this.dragonKilled) {
                if ((this.dragonUUID == null || ++this.ticksSinceDragonSeen >= 1200) && bl) {
                    this.findOrCreateDragon();
                    this.ticksSinceDragonSeen = 0;
                }

                if (++this.ticksSinceCrystalsScanned >= 100 && bl) {
                    this.updateCrystalCount();
                    this.ticksSinceCrystalsScanned = 0;
                }
            }
        } else {
            this.level.getChunkSource().removeTicket(TicketType.DRAGON, new ChunkCoordIntPair(0, 0), 9, Unit.INSTANCE);
        }

    }

    private void scanState() {
        LOGGER.info("Scanning for legacy world dragon fight...");
        boolean bl = this.hasActiveExitPortal();
        if (bl) {
            LOGGER.info("Found that the dragon has been killed in this world already.");
            this.previouslyKilled = true;
        } else {
            LOGGER.info("Found that the dragon has not yet been killed in this world.");
            this.previouslyKilled = false;
            if (this.getExitPortalShape() == null) {
                this.generateExitPortal(false);
            }
        }

        List<? extends EntityEnderDragon> list = this.level.getDragons();
        if (list.isEmpty()) {
            this.dragonKilled = true;
        } else {
            EntityEnderDragon enderDragon = list.get(0);
            this.dragonUUID = enderDragon.getUniqueID();
            LOGGER.info("Found that there's a dragon still alive ({})", (Object)enderDragon);
            this.dragonKilled = false;
            if (!bl) {
                LOGGER.info("But we didn't have a portal, let's remove it.");
                enderDragon.die();
                this.dragonUUID = null;
            }
        }

        if (!this.previouslyKilled && this.dragonKilled) {
            this.dragonKilled = false;
        }

    }

    private void findOrCreateDragon() {
        List<? extends EntityEnderDragon> list = this.level.getDragons();
        if (list.isEmpty()) {
            LOGGER.debug("Haven't seen the dragon, respawning it");
            this.createNewDragon();
        } else {
            LOGGER.debug("Haven't seen our dragon, but found another one to use.");
            this.dragonUUID = list.get(0).getUniqueID();
        }

    }

    public void setRespawnPhase(EnumDragonRespawn spawnState) {
        if (this.respawnStage == null) {
            throw new IllegalStateException("Dragon respawn isn't in progress, can't skip ahead in the animation.");
        } else {
            this.respawnTime = 0;
            if (spawnState == EnumDragonRespawn.END) {
                this.respawnStage = null;
                this.dragonKilled = false;
                EntityEnderDragon enderDragon = this.createNewDragon();

                for(EntityPlayer serverPlayer : this.dragonEvent.getPlayers()) {
                    CriterionTriggers.SUMMONED_ENTITY.trigger(serverPlayer, enderDragon);
                }
            } else {
                this.respawnStage = spawnState;
            }

        }
    }

    private boolean hasActiveExitPortal() {
        for(int i = -8; i <= 8; ++i) {
            for(int j = -8; j <= 8; ++j) {
                Chunk levelChunk = this.level.getChunk(i, j);

                for(TileEntity blockEntity : levelChunk.getTileEntities().values()) {
                    if (blockEntity instanceof TileEntityEnderPortal) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Nullable
    public ShapeDetector.ShapeDetectorCollection getExitPortalShape() {
        for(int i = -8; i <= 8; ++i) {
            for(int j = -8; j <= 8; ++j) {
                Chunk levelChunk = this.level.getChunk(i, j);

                for(TileEntity blockEntity : levelChunk.getTileEntities().values()) {
                    if (blockEntity instanceof TileEntityEnderPortal) {
                        ShapeDetector.ShapeDetectorCollection blockPatternMatch = this.exitPortalPattern.find(this.level, blockEntity.getPosition());
                        if (blockPatternMatch != null) {
                            BlockPosition blockPos = blockPatternMatch.getBlock(3, 3, 3).getPosition();
                            if (this.portalLocation == null) {
                                this.portalLocation = blockPos;
                            }

                            return blockPatternMatch;
                        }
                    }
                }
            }
        }

        int k = this.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING, WorldGenEndTrophy.END_PODIUM_LOCATION).getY();

        for(int l = k; l >= this.level.getMinBuildHeight(); --l) {
            ShapeDetector.ShapeDetectorCollection blockPatternMatch2 = this.exitPortalPattern.find(this.level, new BlockPosition(WorldGenEndTrophy.END_PODIUM_LOCATION.getX(), l, WorldGenEndTrophy.END_PODIUM_LOCATION.getZ()));
            if (blockPatternMatch2 != null) {
                if (this.portalLocation == null) {
                    this.portalLocation = blockPatternMatch2.getBlock(3, 3, 3).getPosition();
                }

                return blockPatternMatch2;
            }
        }

        return null;
    }

    private boolean isArenaLoaded() {
        for(int i = -8; i <= 8; ++i) {
            for(int j = 8; j <= 8; ++j) {
                IChunkAccess chunkAccess = this.level.getChunkAt(i, j, ChunkStatus.FULL, false);
                if (!(chunkAccess instanceof Chunk)) {
                    return false;
                }

                PlayerChunk.State fullChunkStatus = ((Chunk)chunkAccess).getState();
                if (!fullChunkStatus.isAtLeast(PlayerChunk.State.TICKING)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void updatePlayers() {
        Set<EntityPlayer> set = Sets.newHashSet();

        for(EntityPlayer serverPlayer : this.level.getPlayers(VALID_PLAYER)) {
            this.dragonEvent.addPlayer(serverPlayer);
            set.add(serverPlayer);
        }

        Set<EntityPlayer> set2 = Sets.newHashSet(this.dragonEvent.getPlayers());
        set2.removeAll(set);

        for(EntityPlayer serverPlayer2 : set2) {
            this.dragonEvent.removePlayer(serverPlayer2);
        }

    }

    private void updateCrystalCount() {
        this.ticksSinceCrystalsScanned = 0;
        this.crystalsAlive = 0;

        for(WorldGenEnder.Spike endSpike : WorldGenEnder.getSpikesForLevel(this.level)) {
            this.crystalsAlive += this.level.getEntitiesOfClass(EntityEnderCrystal.class, endSpike.getTopBoundingBox()).size();
        }

        LOGGER.debug("Found {} end crystals still alive", (int)this.crystalsAlive);
    }

    public void setDragonKilled(EntityEnderDragon dragon) {
        if (dragon.getUniqueID().equals(this.dragonUUID)) {
            this.dragonEvent.setProgress(0.0F);
            this.dragonEvent.setVisible(false);
            this.generateExitPortal(true);
            this.spawnNewGateway();
            if (!this.previouslyKilled) {
                this.level.setTypeUpdate(this.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING, WorldGenEndTrophy.END_PODIUM_LOCATION), Blocks.DRAGON_EGG.getBlockData());
            }

            this.previouslyKilled = true;
            this.dragonKilled = true;
        }

    }

    private void spawnNewGateway() {
        if (!this.gateways.isEmpty()) {
            int i = this.gateways.remove(this.gateways.size() - 1);
            int j = MathHelper.floor(96.0D * Math.cos(2.0D * (-Math.PI + 0.15707963267948966D * (double)i)));
            int k = MathHelper.floor(96.0D * Math.sin(2.0D * (-Math.PI + 0.15707963267948966D * (double)i)));
            this.spawnNewGateway(new BlockPosition(j, 75, k));
        }
    }

    private void spawnNewGateway(BlockPosition pos) {
        this.level.triggerEffect(3000, pos, 0);
        BiomeDecoratorGroups.END_GATEWAY_DELAYED.place(this.level, this.level.getChunkSource().getChunkGenerator(), new Random(), pos);
    }

    public void generateExitPortal(boolean previouslyKilled) {
        WorldGenEndTrophy endPodiumFeature = new WorldGenEndTrophy(previouslyKilled);
        if (this.portalLocation == null) {
            for(this.portalLocation = this.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, WorldGenEndTrophy.END_PODIUM_LOCATION).below(); this.level.getType(this.portalLocation).is(Blocks.BEDROCK) && this.portalLocation.getY() > this.level.getSeaLevel(); this.portalLocation = this.portalLocation.below()) {
            }
        }

        endPodiumFeature.configured(WorldGenFeatureConfiguration.NONE).place(this.level, this.level.getChunkSource().getChunkGenerator(), new Random(), this.portalLocation);
    }

    private EntityEnderDragon createNewDragon() {
        this.level.getChunkAtWorldCoords(new BlockPosition(0, 128, 0));
        EntityEnderDragon enderDragon = EntityTypes.ENDER_DRAGON.create(this.level);
        enderDragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.HOLDING_PATTERN);
        enderDragon.setPositionRotation(0.0D, 128.0D, 0.0D, this.level.random.nextFloat() * 360.0F, 0.0F);
        this.level.addEntity(enderDragon);
        this.dragonUUID = enderDragon.getUniqueID();
        return enderDragon;
    }

    public void updateDragon(EntityEnderDragon dragon) {
        if (dragon.getUniqueID().equals(this.dragonUUID)) {
            this.dragonEvent.setProgress(dragon.getHealth() / dragon.getMaxHealth());
            this.ticksSinceDragonSeen = 0;
            if (dragon.hasCustomName()) {
                this.dragonEvent.setName(dragon.getScoreboardDisplayName());
            }
        }

    }

    public int getCrystalsAlive() {
        return this.crystalsAlive;
    }

    public void onCrystalDestroyed(EntityEnderCrystal enderCrystal, DamageSource source) {
        if (this.respawnStage != null && this.respawnCrystals.contains(enderCrystal)) {
            LOGGER.debug("Aborting respawn sequence");
            this.respawnStage = null;
            this.respawnTime = 0;
            this.resetCrystals();
            this.generateExitPortal(true);
        } else {
            this.updateCrystalCount();
            Entity entity = this.level.getEntity(this.dragonUUID);
            if (entity instanceof EntityEnderDragon) {
                ((EntityEnderDragon)entity).onCrystalDestroyed(enderCrystal, enderCrystal.getChunkCoordinates(), source);
            }
        }

    }

    public boolean isPreviouslyKilled() {
        return this.previouslyKilled;
    }

    public void initiateRespawn() {
        if (this.dragonKilled && this.respawnStage == null) {
            BlockPosition blockPos = this.portalLocation;
            if (blockPos == null) {
                LOGGER.debug("Tried to respawn, but need to find the portal first.");
                ShapeDetector.ShapeDetectorCollection blockPatternMatch = this.getExitPortalShape();
                if (blockPatternMatch == null) {
                    LOGGER.debug("Couldn't find a portal, so we made one.");
                    this.generateExitPortal(true);
                } else {
                    LOGGER.debug("Found the exit portal & saved its location for next time.");
                }

                blockPos = this.portalLocation;
            }

            List<EntityEnderCrystal> list = Lists.newArrayList();
            BlockPosition blockPos2 = blockPos.above(1);

            for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                List<EntityEnderCrystal> list2 = this.level.getEntitiesOfClass(EntityEnderCrystal.class, new AxisAlignedBB(blockPos2.relative(direction, 2)));
                if (list2.isEmpty()) {
                    return;
                }

                list.addAll(list2);
            }

            LOGGER.debug("Found all crystals, respawning dragon.");
            this.respawnDragon(list);
        }

    }

    private void respawnDragon(List<EntityEnderCrystal> crystals) {
        if (this.dragonKilled && this.respawnStage == null) {
            for(ShapeDetector.ShapeDetectorCollection blockPatternMatch = this.getExitPortalShape(); blockPatternMatch != null; blockPatternMatch = this.getExitPortalShape()) {
                for(int i = 0; i < this.exitPortalPattern.getWidth(); ++i) {
                    for(int j = 0; j < this.exitPortalPattern.getHeight(); ++j) {
                        for(int k = 0; k < this.exitPortalPattern.getDepth(); ++k) {
                            ShapeDetectorBlock blockInWorld = blockPatternMatch.getBlock(i, j, k);
                            if (blockInWorld.getState().is(Blocks.BEDROCK) || blockInWorld.getState().is(Blocks.END_PORTAL)) {
                                this.level.setTypeUpdate(blockInWorld.getPosition(), Blocks.END_STONE.getBlockData());
                            }
                        }
                    }
                }
            }

            this.respawnStage = EnumDragonRespawn.START;
            this.respawnTime = 0;
            this.generateExitPortal(false);
            this.respawnCrystals = crystals;
        }

    }

    public void resetCrystals() {
        for(WorldGenEnder.Spike endSpike : WorldGenEnder.getSpikesForLevel(this.level)) {
            for(EntityEnderCrystal endCrystal : this.level.getEntitiesOfClass(EntityEnderCrystal.class, endSpike.getTopBoundingBox())) {
                endCrystal.setInvulnerable(false);
                endCrystal.setBeamTarget((BlockPosition)null);
            }
        }

    }
}
