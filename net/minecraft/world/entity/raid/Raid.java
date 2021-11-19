package net.minecraft.world.entity.raid;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutNamedSoundEffect;
import net.minecraft.server.level.BossBattleServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.util.MathHelper;
import net.minecraft.world.BossBattle;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPositionTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.EnumBannerPatternType;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.phys.Vec3D;

public class Raid {
    private static final int SECTION_RADIUS_FOR_FINDING_NEW_VILLAGE_CENTER = 2;
    private static final int ATTEMPT_RAID_FARTHEST = 0;
    private static final int ATTEMPT_RAID_CLOSE = 1;
    private static final int ATTEMPT_RAID_INSIDE = 2;
    private static final int VILLAGE_SEARCH_RADIUS = 32;
    private static final int RAID_TIMEOUT_TICKS = 48000;
    private static final int NUM_SPAWN_ATTEMPTS = 3;
    private static final String OMINOUS_BANNER_PATTERN_NAME = "block.minecraft.ominous_banner";
    private static final String RAIDERS_REMAINING = "event.minecraft.raid.raiders_remaining";
    public static final int VILLAGE_RADIUS_BUFFER = 16;
    private static final int POST_RAID_TICK_LIMIT = 40;
    private static final int DEFAULT_PRE_RAID_TICKS = 300;
    public static final int MAX_NO_ACTION_TIME = 2400;
    public static final int MAX_CELEBRATION_TICKS = 600;
    private static final int OUTSIDE_RAID_BOUNDS_TIMEOUT = 30;
    public static final int TICKS_PER_DAY = 24000;
    public static final int DEFAULT_MAX_BAD_OMEN_LEVEL = 5;
    private static final int LOW_MOB_THRESHOLD = 2;
    private static final IChatBaseComponent RAID_NAME_COMPONENT = new ChatMessage("event.minecraft.raid");
    private static final IChatBaseComponent VICTORY = new ChatMessage("event.minecraft.raid.victory");
    private static final IChatBaseComponent DEFEAT = new ChatMessage("event.minecraft.raid.defeat");
    private static final IChatBaseComponent RAID_BAR_VICTORY_COMPONENT = RAID_NAME_COMPONENT.mutableCopy().append(" - ").addSibling(VICTORY);
    private static final IChatBaseComponent RAID_BAR_DEFEAT_COMPONENT = RAID_NAME_COMPONENT.mutableCopy().append(" - ").addSibling(DEFEAT);
    private static final int HERO_OF_THE_VILLAGE_DURATION = 48000;
    public static final int VALID_RAID_RADIUS_SQR = 9216;
    public static final int RAID_REMOVAL_THRESHOLD_SQR = 12544;
    private final Map<Integer, EntityRaider> groupToLeaderMap = Maps.newHashMap();
    private final Map<Integer, Set<EntityRaider>> groupRaiderMap = Maps.newHashMap();
    public final Set<UUID> heroesOfTheVillage = Sets.newHashSet();
    public long ticksActive;
    private BlockPosition center;
    private final WorldServer level;
    private boolean started;
    private final int id;
    public float totalHealth;
    public int badOmenLevel;
    private boolean active;
    private int groupsSpawned;
    private final BossBattleServer raidEvent = new BossBattleServer(RAID_NAME_COMPONENT, BossBattle.BarColor.RED, BossBattle.BarStyle.NOTCHED_10);
    private int postRaidTicks;
    private int raidCooldownTicks;
    private final Random random = new Random();
    public final int numGroups;
    private Raid.Status status;
    private int celebrationTicks;
    private Optional<BlockPosition> waveSpawnPos = Optional.empty();

    public Raid(int id, WorldServer world, BlockPosition pos) {
        this.id = id;
        this.level = world;
        this.active = true;
        this.raidCooldownTicks = 300;
        this.raidEvent.setProgress(0.0F);
        this.center = pos;
        this.numGroups = this.getNumGroups(world.getDifficulty());
        this.status = Raid.Status.ONGOING;
    }

    public Raid(WorldServer world, NBTTagCompound nbt) {
        this.level = world;
        this.id = nbt.getInt("Id");
        this.started = nbt.getBoolean("Started");
        this.active = nbt.getBoolean("Active");
        this.ticksActive = nbt.getLong("TicksActive");
        this.badOmenLevel = nbt.getInt("BadOmenLevel");
        this.groupsSpawned = nbt.getInt("GroupsSpawned");
        this.raidCooldownTicks = nbt.getInt("PreRaidTicks");
        this.postRaidTicks = nbt.getInt("PostRaidTicks");
        this.totalHealth = nbt.getFloat("TotalHealth");
        this.center = new BlockPosition(nbt.getInt("CX"), nbt.getInt("CY"), nbt.getInt("CZ"));
        this.numGroups = nbt.getInt("NumGroups");
        this.status = Raid.Status.getByName(nbt.getString("Status"));
        this.heroesOfTheVillage.clear();
        if (nbt.hasKeyOfType("HeroesOfTheVillage", 9)) {
            NBTTagList listTag = nbt.getList("HeroesOfTheVillage", 11);

            for(int i = 0; i < listTag.size(); ++i) {
                this.heroesOfTheVillage.add(GameProfileSerializer.loadUUID(listTag.get(i)));
            }
        }

    }

    public boolean isOver() {
        return this.isVictory() || this.isLoss();
    }

    public boolean isBetweenWaves() {
        return this.hasFirstWaveSpawned() && this.getTotalRaidersAlive() == 0 && this.raidCooldownTicks > 0;
    }

    public boolean hasFirstWaveSpawned() {
        return this.groupsSpawned > 0;
    }

    public boolean isStopped() {
        return this.status == Raid.Status.STOPPED;
    }

    public boolean isVictory() {
        return this.status == Raid.Status.VICTORY;
    }

    public boolean isLoss() {
        return this.status == Raid.Status.LOSS;
    }

    public float getTotalHealth() {
        return this.totalHealth;
    }

    public Set<EntityRaider> getAllRaiders() {
        Set<EntityRaider> set = Sets.newHashSet();

        for(Set<EntityRaider> set2 : this.groupRaiderMap.values()) {
            set.addAll(set2);
        }

        return set;
    }

    public World getWorld() {
        return this.level;
    }

    public boolean isStarted() {
        return this.started;
    }

    public int getGroupsSpawned() {
        return this.groupsSpawned;
    }

    private Predicate<EntityPlayer> validPlayer() {
        return (player) -> {
            BlockPosition blockPos = player.getChunkCoordinates();
            return player.isAlive() && this.level.getRaidAt(blockPos) == this;
        };
    }

    private void updatePlayers() {
        Set<EntityPlayer> set = Sets.newHashSet(this.raidEvent.getPlayers());
        List<EntityPlayer> list = this.level.getPlayers(this.validPlayer());

        for(EntityPlayer serverPlayer : list) {
            if (!set.contains(serverPlayer)) {
                this.raidEvent.addPlayer(serverPlayer);
            }
        }

        for(EntityPlayer serverPlayer2 : set) {
            if (!list.contains(serverPlayer2)) {
                this.raidEvent.removePlayer(serverPlayer2);
            }
        }

    }

    public int getMaxBadOmenLevel() {
        return 5;
    }

    public int getBadOmenLevel() {
        return this.badOmenLevel;
    }

    public void setBadOmenLevel(int level) {
        this.badOmenLevel = level;
    }

    public void absorbBadOmen(EntityHuman player) {
        if (player.hasEffect(MobEffectList.BAD_OMEN)) {
            this.badOmenLevel += player.getEffect(MobEffectList.BAD_OMEN).getAmplifier() + 1;
            this.badOmenLevel = MathHelper.clamp(this.badOmenLevel, 0, this.getMaxBadOmenLevel());
        }

        player.removeEffect(MobEffectList.BAD_OMEN);
    }

    public void stop() {
        this.active = false;
        this.raidEvent.removeAllPlayers();
        this.status = Raid.Status.STOPPED;
    }

    public void tick() {
        if (!this.isStopped()) {
            if (this.status == Raid.Status.ONGOING) {
                boolean bl = this.active;
                this.active = this.level.isLoaded(this.center);
                if (this.level.getDifficulty() == EnumDifficulty.PEACEFUL) {
                    this.stop();
                    return;
                }

                if (bl != this.active) {
                    this.raidEvent.setVisible(this.active);
                }

                if (!this.active) {
                    return;
                }

                if (!this.level.isVillage(this.center)) {
                    this.moveRaidCenterToNearbyVillageSection();
                }

                if (!this.level.isVillage(this.center)) {
                    if (this.groupsSpawned > 0) {
                        this.status = Raid.Status.LOSS;
                    } else {
                        this.stop();
                    }
                }

                ++this.ticksActive;
                if (this.ticksActive >= 48000L) {
                    this.stop();
                    return;
                }

                int i = this.getTotalRaidersAlive();
                if (i == 0 && this.hasMoreWaves()) {
                    if (this.raidCooldownTicks <= 0) {
                        if (this.raidCooldownTicks == 0 && this.groupsSpawned > 0) {
                            this.raidCooldownTicks = 300;
                            this.raidEvent.setName(RAID_NAME_COMPONENT);
                            return;
                        }
                    } else {
                        boolean bl2 = this.waveSpawnPos.isPresent();
                        boolean bl3 = !bl2 && this.raidCooldownTicks % 5 == 0;
                        if (bl2 && !this.level.isPositionEntityTicking(this.waveSpawnPos.get())) {
                            bl3 = true;
                        }

                        if (bl3) {
                            int j = 0;
                            if (this.raidCooldownTicks < 100) {
                                j = 1;
                            } else if (this.raidCooldownTicks < 40) {
                                j = 2;
                            }

                            this.waveSpawnPos = this.getValidSpawnPos(j);
                        }

                        if (this.raidCooldownTicks == 300 || this.raidCooldownTicks % 20 == 0) {
                            this.updatePlayers();
                        }

                        --this.raidCooldownTicks;
                        this.raidEvent.setProgress(MathHelper.clamp((float)(300 - this.raidCooldownTicks) / 300.0F, 0.0F, 1.0F));
                    }
                }

                if (this.ticksActive % 20L == 0L) {
                    this.updatePlayers();
                    this.updateRaiders();
                    if (i > 0) {
                        if (i <= 2) {
                            this.raidEvent.setName(RAID_NAME_COMPONENT.mutableCopy().append(" - ").addSibling(new ChatMessage("event.minecraft.raid.raiders_remaining", i)));
                        } else {
                            this.raidEvent.setName(RAID_NAME_COMPONENT);
                        }
                    } else {
                        this.raidEvent.setName(RAID_NAME_COMPONENT);
                    }
                }

                boolean bl4 = false;
                int k = 0;

                while(this.shouldSpawnGroup()) {
                    BlockPosition blockPos = this.waveSpawnPos.isPresent() ? this.waveSpawnPos.get() : this.findRandomSpawnPos(k, 20);
                    if (blockPos != null) {
                        this.started = true;
                        this.spawnGroup(blockPos);
                        if (!bl4) {
                            this.playSound(blockPos);
                            bl4 = true;
                        }
                    } else {
                        ++k;
                    }

                    if (k > 3) {
                        this.stop();
                        break;
                    }
                }

                if (this.isStarted() && !this.hasMoreWaves() && i == 0) {
                    if (this.postRaidTicks < 40) {
                        ++this.postRaidTicks;
                    } else {
                        this.status = Raid.Status.VICTORY;

                        for(UUID uUID : this.heroesOfTheVillage) {
                            Entity entity = this.level.getEntity(uUID);
                            if (entity instanceof EntityLiving && !entity.isSpectator()) {
                                EntityLiving livingEntity = (EntityLiving)entity;
                                livingEntity.addEffect(new MobEffect(MobEffectList.HERO_OF_THE_VILLAGE, 48000, this.badOmenLevel - 1, false, false, true));
                                if (livingEntity instanceof EntityPlayer) {
                                    EntityPlayer serverPlayer = (EntityPlayer)livingEntity;
                                    serverPlayer.awardStat(StatisticList.RAID_WIN);
                                    CriterionTriggers.RAID_WIN.trigger(serverPlayer);
                                }
                            }
                        }
                    }
                }

                this.setDirty();
            } else if (this.isOver()) {
                ++this.celebrationTicks;
                if (this.celebrationTicks >= 600) {
                    this.stop();
                    return;
                }

                if (this.celebrationTicks % 20 == 0) {
                    this.updatePlayers();
                    this.raidEvent.setVisible(true);
                    if (this.isVictory()) {
                        this.raidEvent.setProgress(0.0F);
                        this.raidEvent.setName(RAID_BAR_VICTORY_COMPONENT);
                    } else {
                        this.raidEvent.setName(RAID_BAR_DEFEAT_COMPONENT);
                    }
                }
            }

        }
    }

    private void moveRaidCenterToNearbyVillageSection() {
        Stream<SectionPosition> stream = SectionPosition.cube(SectionPosition.of(this.center), 2);
        stream.filter(this.level::isVillage).map(SectionPosition::center).min(Comparator.comparingDouble((blockPos) -> {
            return blockPos.distSqr(this.center);
        })).ifPresent(this::setCenter);
    }

    private Optional<BlockPosition> getValidSpawnPos(int proximity) {
        for(int i = 0; i < 3; ++i) {
            BlockPosition blockPos = this.findRandomSpawnPos(proximity, 1);
            if (blockPos != null) {
                return Optional.of(blockPos);
            }
        }

        return Optional.empty();
    }

    private boolean hasMoreWaves() {
        if (this.hasBonusWave()) {
            return !this.hasSpawnedBonusWave();
        } else {
            return !this.isFinalWave();
        }
    }

    private boolean isFinalWave() {
        return this.getGroupsSpawned() == this.numGroups;
    }

    private boolean hasBonusWave() {
        return this.badOmenLevel > 1;
    }

    private boolean hasSpawnedBonusWave() {
        return this.getGroupsSpawned() > this.numGroups;
    }

    private boolean shouldSpawnBonusGroup() {
        return this.isFinalWave() && this.getTotalRaidersAlive() == 0 && this.hasBonusWave();
    }

    private void updateRaiders() {
        Iterator<Set<EntityRaider>> iterator = this.groupRaiderMap.values().iterator();
        Set<EntityRaider> set = Sets.newHashSet();

        while(iterator.hasNext()) {
            Set<EntityRaider> set2 = iterator.next();

            for(EntityRaider raider : set2) {
                BlockPosition blockPos = raider.getChunkCoordinates();
                if (!raider.isRemoved() && raider.level.getDimensionKey() == this.level.getDimensionKey() && !(this.center.distSqr(blockPos) >= 12544.0D)) {
                    if (raider.tickCount > 600) {
                        if (this.level.getEntity(raider.getUniqueID()) == null) {
                            set.add(raider);
                        }

                        if (!this.level.isVillage(blockPos) && raider.getNoActionTime() > 2400) {
                            raider.setTicksOutsideRaid(raider.getTicksOutsideRaid() + 1);
                        }

                        if (raider.getTicksOutsideRaid() >= 30) {
                            set.add(raider);
                        }
                    }
                } else {
                    set.add(raider);
                }
            }
        }

        for(EntityRaider raider2 : set) {
            this.removeFromRaid(raider2, true);
        }

    }

    private void playSound(BlockPosition pos) {
        float f = 13.0F;
        int i = 64;
        Collection<EntityPlayer> collection = this.raidEvent.getPlayers();

        for(EntityPlayer serverPlayer : this.level.getPlayers()) {
            Vec3D vec3 = serverPlayer.getPositionVector();
            Vec3D vec32 = Vec3D.atCenterOf(pos);
            double d = Math.sqrt((vec32.x - vec3.x) * (vec32.x - vec3.x) + (vec32.z - vec3.z) * (vec32.z - vec3.z));
            double e = vec3.x + 13.0D / d * (vec32.x - vec3.x);
            double g = vec3.z + 13.0D / d * (vec32.z - vec3.z);
            if (d <= 64.0D || collection.contains(serverPlayer)) {
                serverPlayer.connection.sendPacket(new PacketPlayOutNamedSoundEffect(SoundEffects.RAID_HORN, EnumSoundCategory.NEUTRAL, e, serverPlayer.locY(), g, 64.0F, 1.0F));
            }
        }

    }

    private void spawnGroup(BlockPosition pos) {
        boolean bl = false;
        int i = this.groupsSpawned + 1;
        this.totalHealth = 0.0F;
        DifficultyDamageScaler difficultyInstance = this.level.getDamageScaler(pos);
        boolean bl2 = this.shouldSpawnBonusGroup();

        for(Raid.Wave raiderType : Raid.Wave.VALUES) {
            int j = this.getDefaultNumSpawns(raiderType, i, bl2) + this.getPotentialBonusSpawns(raiderType, this.random, i, difficultyInstance, bl2);
            int k = 0;

            for(int l = 0; l < j; ++l) {
                EntityRaider raider = raiderType.entityType.create(this.level);
                if (!bl && raider.canBeLeader()) {
                    raider.setPatrolLeader(true);
                    this.setLeader(i, raider);
                    bl = true;
                }

                this.joinRaid(i, raider, pos, false);
                if (raiderType.entityType == EntityTypes.RAVAGER) {
                    EntityRaider raider2 = null;
                    if (i == this.getNumGroups(EnumDifficulty.NORMAL)) {
                        raider2 = EntityTypes.PILLAGER.create(this.level);
                    } else if (i >= this.getNumGroups(EnumDifficulty.HARD)) {
                        if (k == 0) {
                            raider2 = EntityTypes.EVOKER.create(this.level);
                        } else {
                            raider2 = EntityTypes.VINDICATOR.create(this.level);
                        }
                    }

                    ++k;
                    if (raider2 != null) {
                        this.joinRaid(i, raider2, pos, false);
                        raider2.setPositionRotation(pos, 0.0F, 0.0F);
                        raider2.startRiding(raider);
                    }
                }
            }
        }

        this.waveSpawnPos = Optional.empty();
        ++this.groupsSpawned;
        this.updateProgress();
        this.setDirty();
    }

    public void joinRaid(int wave, EntityRaider raider, @Nullable BlockPosition pos, boolean existing) {
        boolean bl = this.addWaveMob(wave, raider);
        if (bl) {
            raider.setCurrentRaid(this);
            raider.setWave(wave);
            raider.setCanJoinRaid(true);
            raider.setTicksOutsideRaid(0);
            if (!existing && pos != null) {
                raider.setPosition((double)pos.getX() + 0.5D, (double)pos.getY() + 1.0D, (double)pos.getZ() + 0.5D);
                raider.prepare(this.level, this.level.getDamageScaler(pos), EnumMobSpawn.EVENT, (GroupDataEntity)null, (NBTTagCompound)null);
                raider.applyRaidBuffs(wave, false);
                raider.setOnGround(true);
                this.level.addAllEntities(raider);
            }
        }

    }

    public void updateProgress() {
        this.raidEvent.setProgress(MathHelper.clamp(this.sumMobHealth() / this.totalHealth, 0.0F, 1.0F));
    }

    public float sumMobHealth() {
        float f = 0.0F;

        for(Set<EntityRaider> set : this.groupRaiderMap.values()) {
            for(EntityRaider raider : set) {
                f += raider.getHealth();
            }
        }

        return f;
    }

    private boolean shouldSpawnGroup() {
        return this.raidCooldownTicks == 0 && (this.groupsSpawned < this.numGroups || this.shouldSpawnBonusGroup()) && this.getTotalRaidersAlive() == 0;
    }

    public int getTotalRaidersAlive() {
        return this.groupRaiderMap.values().stream().mapToInt(Set::size).sum();
    }

    public void removeFromRaid(EntityRaider entity, boolean countHealth) {
        Set<EntityRaider> set = this.groupRaiderMap.get(entity.getWave());
        if (set != null) {
            boolean bl = set.remove(entity);
            if (bl) {
                if (countHealth) {
                    this.totalHealth -= entity.getHealth();
                }

                entity.setCurrentRaid((Raid)null);
                this.updateProgress();
                this.setDirty();
            }
        }

    }

    private void setDirty() {
        this.level.getPersistentRaid().setDirty();
    }

    public static ItemStack getLeaderBannerInstance() {
        ItemStack itemStack = new ItemStack(Items.WHITE_BANNER);
        NBTTagCompound compoundTag = itemStack.getOrCreateTagElement("BlockEntityTag");
        NBTTagList listTag = (new EnumBannerPatternType.Builder()).addPattern(EnumBannerPatternType.RHOMBUS_MIDDLE, EnumColor.CYAN).addPattern(EnumBannerPatternType.STRIPE_BOTTOM, EnumColor.LIGHT_GRAY).addPattern(EnumBannerPatternType.STRIPE_CENTER, EnumColor.GRAY).addPattern(EnumBannerPatternType.BORDER, EnumColor.LIGHT_GRAY).addPattern(EnumBannerPatternType.STRIPE_MIDDLE, EnumColor.BLACK).addPattern(EnumBannerPatternType.HALF_HORIZONTAL, EnumColor.LIGHT_GRAY).addPattern(EnumBannerPatternType.CIRCLE_MIDDLE, EnumColor.LIGHT_GRAY).addPattern(EnumBannerPatternType.BORDER, EnumColor.BLACK).toListTag();
        compoundTag.set("Patterns", listTag);
        itemStack.hideTooltipPart(ItemStack.HideFlags.ADDITIONAL);
        itemStack.setHoverName((new ChatMessage("block.minecraft.ominous_banner")).withStyle(EnumChatFormat.GOLD));
        return itemStack;
    }

    @Nullable
    public EntityRaider getLeader(int wave) {
        return this.groupToLeaderMap.get(wave);
    }

    @Nullable
    private BlockPosition findRandomSpawnPos(int proximity, int tries) {
        int i = proximity == 0 ? 2 : 2 - proximity;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int j = 0; j < tries; ++j) {
            float f = this.level.random.nextFloat() * ((float)Math.PI * 2F);
            int k = this.center.getX() + MathHelper.floor(MathHelper.cos(f) * 32.0F * (float)i) + this.level.random.nextInt(5);
            int l = this.center.getZ() + MathHelper.floor(MathHelper.sin(f) * 32.0F * (float)i) + this.level.random.nextInt(5);
            int m = this.level.getHeight(HeightMap.Type.WORLD_SURFACE, k, l);
            mutableBlockPos.set(k, m, l);
            if (!this.level.isVillage(mutableBlockPos) || proximity >= 2) {
                int n = 10;
                if (this.level.hasChunksAt(mutableBlockPos.getX() - 10, mutableBlockPos.getZ() - 10, mutableBlockPos.getX() + 10, mutableBlockPos.getZ() + 10) && this.level.isPositionEntityTicking(mutableBlockPos) && (NaturalSpawner.isSpawnPositionOk(EntityPositionTypes.Surface.ON_GROUND, this.level, mutableBlockPos, EntityTypes.RAVAGER) || this.level.getType(mutableBlockPos.below()).is(Blocks.SNOW) && this.level.getType(mutableBlockPos).isAir())) {
                    return mutableBlockPos;
                }
            }
        }

        return null;
    }

    private boolean addWaveMob(int wave, EntityRaider entity) {
        return this.addWaveMob(wave, entity, true);
    }

    public boolean addWaveMob(int wave, EntityRaider entity, boolean countHealth) {
        this.groupRaiderMap.computeIfAbsent(wave, (wavex) -> {
            return Sets.newHashSet();
        });
        Set<EntityRaider> set = this.groupRaiderMap.get(wave);
        EntityRaider raider = null;

        for(EntityRaider raider2 : set) {
            if (raider2.getUniqueID().equals(entity.getUniqueID())) {
                raider = raider2;
                break;
            }
        }

        if (raider != null) {
            set.remove(raider);
            set.add(entity);
        }

        set.add(entity);
        if (countHealth) {
            this.totalHealth += entity.getHealth();
        }

        this.updateProgress();
        this.setDirty();
        return true;
    }

    public void setLeader(int wave, EntityRaider entity) {
        this.groupToLeaderMap.put(wave, entity);
        entity.setSlot(EnumItemSlot.HEAD, getLeaderBannerInstance());
        entity.setDropChance(EnumItemSlot.HEAD, 2.0F);
    }

    public void removeLeader(int wave) {
        this.groupToLeaderMap.remove(wave);
    }

    public BlockPosition getCenter() {
        return this.center;
    }

    private void setCenter(BlockPosition center) {
        this.center = center;
    }

    public int getId() {
        return this.id;
    }

    private int getDefaultNumSpawns(Raid.Wave member, int wave, boolean extra) {
        return extra ? member.spawnsPerWaveBeforeBonus[this.numGroups] : member.spawnsPerWaveBeforeBonus[wave];
    }

    private int getPotentialBonusSpawns(Raid.Wave member, Random random, int wave, DifficultyDamageScaler localDifficulty, boolean extra) {
        EnumDifficulty difficulty = localDifficulty.getDifficulty();
        boolean bl = difficulty == EnumDifficulty.EASY;
        boolean bl2 = difficulty == EnumDifficulty.NORMAL;
        int j;
        switch(member) {
        case WITCH:
            if (bl || wave <= 2 || wave == 4) {
                return 0;
            }

            j = 1;
            break;
        case PILLAGER:
        case VINDICATOR:
            if (bl) {
                j = random.nextInt(2);
            } else if (bl2) {
                j = 1;
            } else {
                j = 2;
            }
            break;
        case RAVAGER:
            j = !bl && extra ? 1 : 0;
            break;
        default:
            return 0;
        }

        return j > 0 ? random.nextInt(j + 1) : 0;
    }

    public boolean isActive() {
        return this.active;
    }

    public NBTTagCompound save(NBTTagCompound nbt) {
        nbt.setInt("Id", this.id);
        nbt.setBoolean("Started", this.started);
        nbt.setBoolean("Active", this.active);
        nbt.setLong("TicksActive", this.ticksActive);
        nbt.setInt("BadOmenLevel", this.badOmenLevel);
        nbt.setInt("GroupsSpawned", this.groupsSpawned);
        nbt.setInt("PreRaidTicks", this.raidCooldownTicks);
        nbt.setInt("PostRaidTicks", this.postRaidTicks);
        nbt.setFloat("TotalHealth", this.totalHealth);
        nbt.setInt("NumGroups", this.numGroups);
        nbt.setString("Status", this.status.getName());
        nbt.setInt("CX", this.center.getX());
        nbt.setInt("CY", this.center.getY());
        nbt.setInt("CZ", this.center.getZ());
        NBTTagList listTag = new NBTTagList();

        for(UUID uUID : this.heroesOfTheVillage) {
            listTag.add(GameProfileSerializer.createUUID(uUID));
        }

        nbt.set("HeroesOfTheVillage", listTag);
        return nbt;
    }

    public int getNumGroups(EnumDifficulty difficulty) {
        switch(difficulty) {
        case EASY:
            return 3;
        case NORMAL:
            return 5;
        case HARD:
            return 7;
        default:
            return 0;
        }
    }

    public float getEnchantOdds() {
        int i = this.getBadOmenLevel();
        if (i == 2) {
            return 0.1F;
        } else if (i == 3) {
            return 0.25F;
        } else if (i == 4) {
            return 0.5F;
        } else {
            return i == 5 ? 0.75F : 0.0F;
        }
    }

    public void addHeroOfTheVillage(Entity entity) {
        this.heroesOfTheVillage.add(entity.getUniqueID());
    }

    static enum Status {
        ONGOING,
        VICTORY,
        LOSS,
        STOPPED;

        private static final Raid.Status[] VALUES = values();

        static Raid.Status getByName(String name) {
            for(Raid.Status raidStatus : VALUES) {
                if (name.equalsIgnoreCase(raidStatus.name())) {
                    return raidStatus;
                }
            }

            return ONGOING;
        }

        public String getName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    static enum Wave {
        VINDICATOR(EntityTypes.VINDICATOR, new int[]{0, 0, 2, 0, 1, 4, 2, 5}),
        EVOKER(EntityTypes.EVOKER, new int[]{0, 0, 0, 0, 0, 1, 1, 2}),
        PILLAGER(EntityTypes.PILLAGER, new int[]{0, 4, 3, 3, 4, 4, 4, 2}),
        WITCH(EntityTypes.WITCH, new int[]{0, 0, 0, 0, 3, 0, 0, 1}),
        RAVAGER(EntityTypes.RAVAGER, new int[]{0, 0, 0, 1, 0, 1, 0, 2});

        static final Raid.Wave[] VALUES = values();
        final EntityTypes<? extends EntityRaider> entityType;
        final int[] spawnsPerWaveBeforeBonus;

        private Wave(EntityTypes<? extends EntityRaider> type, int[] countInWave) {
            this.entityType = type;
            this.spawnsPerWaveBeforeBonus = countInWave;
        }
    }
}
