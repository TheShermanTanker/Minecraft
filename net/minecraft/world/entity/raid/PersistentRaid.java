package net.minecraft.world.entity.raid;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.network.protocol.game.PacketPlayOutEntityStatus;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceRecord;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.saveddata.PersistentBase;
import net.minecraft.world.phys.Vec3D;

public class PersistentRaid extends PersistentBase {
    private static final String RAID_FILE_ID = "raids";
    public final Map<Integer, Raid> raidMap = Maps.newHashMap();
    private final WorldServer level;
    private int nextAvailableID;
    private int tick;

    public PersistentRaid(WorldServer world) {
        this.level = world;
        this.nextAvailableID = 1;
        this.setDirty();
    }

    public Raid get(int id) {
        return this.raidMap.get(id);
    }

    public void tick() {
        ++this.tick;
        Iterator<Raid> iterator = this.raidMap.values().iterator();

        while(iterator.hasNext()) {
            Raid raid = iterator.next();
            if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
                raid.stop();
            }

            if (raid.isStopped()) {
                iterator.remove();
                this.setDirty();
            } else {
                raid.tick();
            }
        }

        if (this.tick % 200 == 0) {
            this.setDirty();
        }

        PacketDebug.sendRaids(this.level, this.raidMap.values());
    }

    public static boolean canJoinRaid(EntityRaider raider, Raid raid) {
        if (raider != null && raid != null && raid.getWorld() != null) {
            return raider.isAlive() && raider.isCanJoinRaid() && raider.getNoActionTime() <= 2400 && raider.level.getDimensionManager() == raid.getWorld().getDimensionManager();
        } else {
            return false;
        }
    }

    @Nullable
    public Raid createOrExtendRaid(EntityPlayer player) {
        if (player.isSpectator()) {
            return null;
        } else if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
            return null;
        } else {
            DimensionManager dimensionType = player.level.getDimensionManager();
            if (!dimensionType.hasRaids()) {
                return null;
            } else {
                BlockPosition blockPos = player.getChunkCoordinates();
                List<VillagePlaceRecord> list = this.level.getPoiManager().getInRange(VillagePlaceType.ALL, blockPos, 64, VillagePlace.Occupancy.IS_OCCUPIED).collect(Collectors.toList());
                int i = 0;
                Vec3D vec3 = Vec3D.ZERO;

                for(VillagePlaceRecord poiRecord : list) {
                    BlockPosition blockPos2 = poiRecord.getPos();
                    vec3 = vec3.add((double)blockPos2.getX(), (double)blockPos2.getY(), (double)blockPos2.getZ());
                    ++i;
                }

                BlockPosition blockPos3;
                if (i > 0) {
                    vec3 = vec3.scale(1.0D / (double)i);
                    blockPos3 = new BlockPosition(vec3);
                } else {
                    blockPos3 = blockPos;
                }

                Raid raid = this.getOrCreateRaid(player.getWorldServer(), blockPos3);
                boolean bl = false;
                if (!raid.isStarted()) {
                    if (!this.raidMap.containsKey(raid.getId())) {
                        this.raidMap.put(raid.getId(), raid);
                    }

                    bl = true;
                } else if (raid.getBadOmenLevel() < raid.getMaxBadOmenLevel()) {
                    bl = true;
                } else {
                    player.removeEffect(MobEffectList.BAD_OMEN);
                    player.connection.sendPacket(new PacketPlayOutEntityStatus(player, (byte)43));
                }

                if (bl) {
                    raid.absorbBadOmen(player);
                    player.connection.sendPacket(new PacketPlayOutEntityStatus(player, (byte)43));
                    if (!raid.hasFirstWaveSpawned()) {
                        player.awardStat(StatisticList.RAID_TRIGGER);
                        CriterionTriggers.BAD_OMEN.trigger(player);
                    }
                }

                this.setDirty();
                return raid;
            }
        }
    }

    private Raid getOrCreateRaid(WorldServer world, BlockPosition pos) {
        Raid raid = world.getRaidAt(pos);
        return raid != null ? raid : new Raid(this.getUniqueId(), world, pos);
    }

    public static PersistentRaid load(WorldServer world, NBTTagCompound nbt) {
        PersistentRaid raids = new PersistentRaid(world);
        raids.nextAvailableID = nbt.getInt("NextAvailableID");
        raids.tick = nbt.getInt("Tick");
        NBTTagList listTag = nbt.getList("Raids", 10);

        for(int i = 0; i < listTag.size(); ++i) {
            NBTTagCompound compoundTag = listTag.getCompound(i);
            Raid raid = new Raid(world, compoundTag);
            raids.raidMap.put(raid.getId(), raid);
        }

        return raids;
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        nbt.setInt("NextAvailableID", this.nextAvailableID);
        nbt.setInt("Tick", this.tick);
        NBTTagList listTag = new NBTTagList();

        for(Raid raid : this.raidMap.values()) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            raid.save(compoundTag);
            listTag.add(compoundTag);
        }

        nbt.set("Raids", listTag);
        return nbt;
    }

    public static String getFileId(DimensionManager dimensionType) {
        return "raids" + dimensionType.getSuffix();
    }

    private int getUniqueId() {
        return ++this.nextAvailableID;
    }

    @Nullable
    public Raid getNearbyRaid(BlockPosition pos, int searchDistance) {
        Raid raid = null;
        double d = (double)searchDistance;

        for(Raid raid2 : this.raidMap.values()) {
            double e = raid2.getCenter().distSqr(pos);
            if (raid2.isActive() && e < d) {
                raid = raid2;
                d = e;
            }
        }

        return raid;
    }
}
