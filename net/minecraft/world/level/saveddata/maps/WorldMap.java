package net.minecraft.world.level.saveddata.maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutMap;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.saveddata.PersistentBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldMap extends PersistentBase {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAP_SIZE = 128;
    private static final int HALF_MAP_SIZE = 64;
    public static final int MAX_SCALE = 4;
    public static final int TRACKED_DECORATION_LIMIT = 256;
    public int x;
    public int z;
    public ResourceKey<World> dimension;
    public boolean trackingPosition;
    public boolean unlimitedTracking;
    public byte scale;
    public byte[] colors = new byte[16384];
    public boolean locked;
    public final List<WorldMap.WorldMapHumanTracker> carriedBy = Lists.newArrayList();
    public final Map<EntityHuman, WorldMap.WorldMapHumanTracker> carriedByPlayers = Maps.newHashMap();
    private final Map<String, MapIconBanner> bannerMarkers = Maps.newHashMap();
    public final Map<String, MapIcon> decorations = Maps.newLinkedHashMap();
    private final Map<String, WorldMapFrame> frameMarkers = Maps.newHashMap();
    private int trackedDecorationCount;

    private WorldMap(int centerX, int centerZ, byte scale, boolean showIcons, boolean unlimitedTracking, boolean locked, ResourceKey<World> dimension) {
        this.scale = scale;
        this.x = centerX;
        this.z = centerZ;
        this.dimension = dimension;
        this.trackingPosition = showIcons;
        this.unlimitedTracking = unlimitedTracking;
        this.locked = locked;
        this.setDirty();
    }

    public static WorldMap createFresh(double centerX, double centerZ, byte scale, boolean showIcons, boolean unlimitedTracking, ResourceKey<World> dimension) {
        int i = 128 * (1 << scale);
        int j = MathHelper.floor((centerX + 64.0D) / (double)i);
        int k = MathHelper.floor((centerZ + 64.0D) / (double)i);
        int l = j * i + i / 2 - 64;
        int m = k * i + i / 2 - 64;
        return new WorldMap(l, m, scale, showIcons, unlimitedTracking, false, dimension);
    }

    public static WorldMap createForClient(byte scale, boolean showIcons, ResourceKey<World> dimension) {
        return new WorldMap(0, 0, scale, false, false, showIcons, dimension);
    }

    public static WorldMap load(NBTTagCompound nbt) {
        ResourceKey<World> resourceKey = DimensionManager.parseLegacy(new Dynamic<>(DynamicOpsNBT.INSTANCE, nbt.get("dimension"))).resultOrPartial(LOGGER::error).orElseThrow(() -> {
            return new IllegalArgumentException("Invalid map dimension: " + nbt.get("dimension"));
        });
        int i = nbt.getInt("xCenter");
        int j = nbt.getInt("zCenter");
        byte b = (byte)MathHelper.clamp((int)nbt.getByte("scale"), (int)0, (int)4);
        boolean bl = !nbt.hasKeyOfType("trackingPosition", 1) || nbt.getBoolean("trackingPosition");
        boolean bl2 = nbt.getBoolean("unlimitedTracking");
        boolean bl3 = nbt.getBoolean("locked");
        WorldMap mapItemSavedData = new WorldMap(i, j, b, bl, bl2, bl3, resourceKey);
        byte[] bs = nbt.getByteArray("colors");
        if (bs.length == 16384) {
            mapItemSavedData.colors = bs;
        }

        NBTTagList listTag = nbt.getList("banners", 10);

        for(int k = 0; k < listTag.size(); ++k) {
            MapIconBanner mapBanner = MapIconBanner.load(listTag.getCompound(k));
            mapItemSavedData.bannerMarkers.put(mapBanner.getId(), mapBanner);
            mapItemSavedData.addDecoration(mapBanner.getDecoration(), (GeneratorAccess)null, mapBanner.getId(), (double)mapBanner.getPos().getX(), (double)mapBanner.getPos().getZ(), 180.0D, mapBanner.getName());
        }

        NBTTagList listTag2 = nbt.getList("frames", 10);

        for(int l = 0; l < listTag2.size(); ++l) {
            WorldMapFrame mapFrame = WorldMapFrame.load(listTag2.getCompound(l));
            mapItemSavedData.frameMarkers.put(mapFrame.getId(), mapFrame);
            mapItemSavedData.addDecoration(MapIcon.Type.FRAME, (GeneratorAccess)null, "frame-" + mapFrame.getEntityId(), (double)mapFrame.getPos().getX(), (double)mapFrame.getPos().getZ(), (double)mapFrame.getRotation(), (IChatBaseComponent)null);
        }

        return mapItemSavedData;
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        MinecraftKey.CODEC.encodeStart(DynamicOpsNBT.INSTANCE, this.dimension.location()).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
            nbt.set("dimension", tag);
        });
        nbt.setInt("xCenter", this.x);
        nbt.setInt("zCenter", this.z);
        nbt.setByte("scale", this.scale);
        nbt.setByteArray("colors", this.colors);
        nbt.setBoolean("trackingPosition", this.trackingPosition);
        nbt.setBoolean("unlimitedTracking", this.unlimitedTracking);
        nbt.setBoolean("locked", this.locked);
        NBTTagList listTag = new NBTTagList();

        for(MapIconBanner mapBanner : this.bannerMarkers.values()) {
            listTag.add(mapBanner.save());
        }

        nbt.set("banners", listTag);
        NBTTagList listTag2 = new NBTTagList();

        for(WorldMapFrame mapFrame : this.frameMarkers.values()) {
            listTag2.add(mapFrame.save());
        }

        nbt.set("frames", listTag2);
        return nbt;
    }

    public WorldMap locked() {
        WorldMap mapItemSavedData = new WorldMap(this.x, this.z, this.scale, this.trackingPosition, this.unlimitedTracking, true, this.dimension);
        mapItemSavedData.bannerMarkers.putAll(this.bannerMarkers);
        mapItemSavedData.decorations.putAll(this.decorations);
        mapItemSavedData.trackedDecorationCount = this.trackedDecorationCount;
        System.arraycopy(this.colors, 0, mapItemSavedData.colors, 0, this.colors.length);
        mapItemSavedData.setDirty();
        return mapItemSavedData;
    }

    public WorldMap scaled(int zoomOutScale) {
        return createFresh((double)this.x, (double)this.z, (byte)MathHelper.clamp(this.scale + zoomOutScale, 0, 4), this.trackingPosition, this.unlimitedTracking, this.dimension);
    }

    public void tickCarriedBy(EntityHuman player, ItemStack stack) {
        if (!this.carriedByPlayers.containsKey(player)) {
            WorldMap.WorldMapHumanTracker holdingPlayer = new WorldMap.WorldMapHumanTracker(player);
            this.carriedByPlayers.put(player, holdingPlayer);
            this.carriedBy.add(holdingPlayer);
        }

        if (!player.getInventory().contains(stack)) {
            this.removeDecoration(player.getDisplayName().getString());
        }

        for(int i = 0; i < this.carriedBy.size(); ++i) {
            WorldMap.WorldMapHumanTracker holdingPlayer2 = this.carriedBy.get(i);
            String string = holdingPlayer2.player.getDisplayName().getString();
            if (!holdingPlayer2.player.isRemoved() && (holdingPlayer2.player.getInventory().contains(stack) || stack.isFramed())) {
                if (!stack.isFramed() && holdingPlayer2.player.level.getDimensionKey() == this.dimension && this.trackingPosition) {
                    this.addDecoration(MapIcon.Type.PLAYER, holdingPlayer2.player.level, string, holdingPlayer2.player.locX(), holdingPlayer2.player.locZ(), (double)holdingPlayer2.player.getYRot(), (IChatBaseComponent)null);
                }
            } else {
                this.carriedByPlayers.remove(holdingPlayer2.player);
                this.carriedBy.remove(holdingPlayer2);
                this.removeDecoration(string);
            }
        }

        if (stack.isFramed() && this.trackingPosition) {
            EntityItemFrame itemFrame = stack.getFrame();
            BlockPosition blockPos = itemFrame.getBlockPosition();
            WorldMapFrame mapFrame = this.frameMarkers.get(WorldMapFrame.frameId(blockPos));
            if (mapFrame != null && itemFrame.getId() != mapFrame.getEntityId() && this.frameMarkers.containsKey(mapFrame.getId())) {
                this.removeDecoration("frame-" + mapFrame.getEntityId());
            }

            WorldMapFrame mapFrame2 = new WorldMapFrame(blockPos, itemFrame.getDirection().get2DRotationValue() * 90, itemFrame.getId());
            this.addDecoration(MapIcon.Type.FRAME, player.level, "frame-" + itemFrame.getId(), (double)blockPos.getX(), (double)blockPos.getZ(), (double)(itemFrame.getDirection().get2DRotationValue() * 90), (IChatBaseComponent)null);
            this.frameMarkers.put(mapFrame2.getId(), mapFrame2);
        }

        NBTTagCompound compoundTag = stack.getTag();
        if (compoundTag != null && compoundTag.hasKeyOfType("Decorations", 9)) {
            NBTTagList listTag = compoundTag.getList("Decorations", 10);

            for(int j = 0; j < listTag.size(); ++j) {
                NBTTagCompound compoundTag2 = listTag.getCompound(j);
                if (!this.decorations.containsKey(compoundTag2.getString("id"))) {
                    this.addDecoration(MapIcon.Type.byIcon(compoundTag2.getByte("type")), player.level, compoundTag2.getString("id"), compoundTag2.getDouble("x"), compoundTag2.getDouble("z"), compoundTag2.getDouble("rot"), (IChatBaseComponent)null);
                }
            }
        }

    }

    private void removeDecoration(String id) {
        MapIcon mapDecoration = this.decorations.remove(id);
        if (mapDecoration != null && mapDecoration.getType().shouldTrackCount()) {
            --this.trackedDecorationCount;
        }

        this.flagDecorationsDirty();
    }

    public static void decorateMap(ItemStack stack, BlockPosition pos, String id, MapIcon.Type type) {
        NBTTagList listTag;
        if (stack.hasTag() && stack.getTag().hasKeyOfType("Decorations", 9)) {
            listTag = stack.getTag().getList("Decorations", 10);
        } else {
            listTag = new NBTTagList();
            stack.addTagElement("Decorations", listTag);
        }

        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setByte("type", type.getIcon());
        compoundTag.setString("id", id);
        compoundTag.setDouble("x", (double)pos.getX());
        compoundTag.setDouble("z", (double)pos.getZ());
        compoundTag.setDouble("rot", 180.0D);
        listTag.add(compoundTag);
        if (type.hasMapColor()) {
            NBTTagCompound compoundTag2 = stack.getOrCreateTagElement("display");
            compoundTag2.setInt("MapColor", type.getMapColor());
        }

    }

    private void addDecoration(MapIcon.Type type, @Nullable GeneratorAccess world, String key, double x, double z, double rotation, @Nullable IChatBaseComponent text) {
        int i = 1 << this.scale;
        float f = (float)(x - (double)this.x) / (float)i;
        float g = (float)(z - (double)this.z) / (float)i;
        byte b = (byte)((int)((double)(f * 2.0F) + 0.5D));
        byte c = (byte)((int)((double)(g * 2.0F) + 0.5D));
        int j = 63;
        byte d;
        if (f >= -63.0F && g >= -63.0F && f <= 63.0F && g <= 63.0F) {
            rotation = rotation + (rotation < 0.0D ? -8.0D : 8.0D);
            d = (byte)((int)(rotation * 16.0D / 360.0D));
            if (this.dimension == World.NETHER && world != null) {
                int k = (int)(world.getWorldData().getDayTime() / 10L);
                d = (byte)(k * k * 34187121 + k * 121 >> 15 & 15);
            }
        } else {
            if (type != MapIcon.Type.PLAYER) {
                this.removeDecoration(key);
                return;
            }

            int l = 320;
            if (Math.abs(f) < 320.0F && Math.abs(g) < 320.0F) {
                type = MapIcon.Type.PLAYER_OFF_MAP;
            } else {
                if (!this.unlimitedTracking) {
                    this.removeDecoration(key);
                    return;
                }

                type = MapIcon.Type.PLAYER_OFF_LIMITS;
            }

            d = 0;
            if (f <= -63.0F) {
                b = -128;
            }

            if (g <= -63.0F) {
                c = -128;
            }

            if (f >= 63.0F) {
                b = 127;
            }

            if (g >= 63.0F) {
                c = 127;
            }
        }

        MapIcon mapDecoration = new MapIcon(type, b, c, d, text);
        MapIcon mapDecoration2 = this.decorations.put(key, mapDecoration);
        if (!mapDecoration.equals(mapDecoration2)) {
            if (mapDecoration2 != null && mapDecoration2.getType().shouldTrackCount()) {
                --this.trackedDecorationCount;
            }

            if (type.shouldTrackCount()) {
                ++this.trackedDecorationCount;
            }

            this.flagDecorationsDirty();
        }

    }

    @Nullable
    public Packet<?> getUpdatePacket(int id, EntityHuman player) {
        WorldMap.WorldMapHumanTracker holdingPlayer = this.carriedByPlayers.get(player);
        return holdingPlayer == null ? null : holdingPlayer.nextUpdatePacket(id);
    }

    public void flagDirty(int x, int z) {
        this.setDirty();

        for(WorldMap.WorldMapHumanTracker holdingPlayer : this.carriedBy) {
            holdingPlayer.markColorsDirty(x, z);
        }

    }

    public void flagDecorationsDirty() {
        this.setDirty();
        this.carriedBy.forEach(WorldMap.WorldMapHumanTracker::markDecorationsDirty);
    }

    public WorldMap.WorldMapHumanTracker getHoldingPlayer(EntityHuman player) {
        WorldMap.WorldMapHumanTracker holdingPlayer = this.carriedByPlayers.get(player);
        if (holdingPlayer == null) {
            holdingPlayer = new WorldMap.WorldMapHumanTracker(player);
            this.carriedByPlayers.put(player, holdingPlayer);
            this.carriedBy.add(holdingPlayer);
        }

        return holdingPlayer;
    }

    public boolean toggleBanner(GeneratorAccess world, BlockPosition pos) {
        double d = (double)pos.getX() + 0.5D;
        double e = (double)pos.getZ() + 0.5D;
        int i = 1 << this.scale;
        double f = (d - (double)this.x) / (double)i;
        double g = (e - (double)this.z) / (double)i;
        int j = 63;
        if (f >= -63.0D && g >= -63.0D && f <= 63.0D && g <= 63.0D) {
            MapIconBanner mapBanner = MapIconBanner.fromWorld(world, pos);
            if (mapBanner == null) {
                return false;
            }

            if (this.bannerMarkers.remove(mapBanner.getId(), mapBanner)) {
                this.removeDecoration(mapBanner.getId());
                return true;
            }

            if (!this.isTrackedCountOverLimit(256)) {
                this.bannerMarkers.put(mapBanner.getId(), mapBanner);
                this.addDecoration(mapBanner.getDecoration(), world, mapBanner.getId(), d, e, 180.0D, mapBanner.getName());
                return true;
            }
        }

        return false;
    }

    public void checkBanners(IBlockAccess world, int x, int z) {
        Iterator<MapIconBanner> iterator = this.bannerMarkers.values().iterator();

        while(iterator.hasNext()) {
            MapIconBanner mapBanner = iterator.next();
            if (mapBanner.getPos().getX() == x && mapBanner.getPos().getZ() == z) {
                MapIconBanner mapBanner2 = MapIconBanner.fromWorld(world, mapBanner.getPos());
                if (!mapBanner.equals(mapBanner2)) {
                    iterator.remove();
                    this.removeDecoration(mapBanner.getId());
                }
            }
        }

    }

    public Collection<MapIconBanner> getBanners() {
        return this.bannerMarkers.values();
    }

    public void removedFromFrame(BlockPosition pos, int id) {
        this.removeDecoration("frame-" + id);
        this.frameMarkers.remove(WorldMapFrame.frameId(pos));
    }

    public boolean updateColor(int x, int z, byte color) {
        byte b = this.colors[x + z * 128];
        if (b != color) {
            this.setColor(x, z, color);
            return true;
        } else {
            return false;
        }
    }

    public void setColor(int x, int z, byte color) {
        this.colors[x + z * 128] = color;
        this.flagDirty(x, z);
    }

    public boolean isExplorationMap() {
        for(MapIcon mapDecoration : this.decorations.values()) {
            if (mapDecoration.getType() == MapIcon.Type.MANSION || mapDecoration.getType() == MapIcon.Type.MONUMENT) {
                return true;
            }
        }

        return false;
    }

    public void addClientSideDecorations(List<MapIcon> icons) {
        this.decorations.clear();
        this.trackedDecorationCount = 0;

        for(int i = 0; i < icons.size(); ++i) {
            MapIcon mapDecoration = icons.get(i);
            this.decorations.put("icon-" + i, mapDecoration);
            if (mapDecoration.getType().shouldTrackCount()) {
                ++this.trackedDecorationCount;
            }
        }

    }

    public Iterable<MapIcon> getDecorations() {
        return this.decorations.values();
    }

    public boolean isTrackedCountOverLimit(int i) {
        return this.trackedDecorationCount >= i;
    }

    public static class MapPatch {
        public final int startX;
        public final int startY;
        public final int width;
        public final int height;
        public final byte[] mapColors;

        public MapPatch(int startX, int startZ, int width, int height, byte[] colors) {
            this.startX = startX;
            this.startY = startZ;
            this.width = width;
            this.height = height;
            this.mapColors = colors;
        }

        public void applyToMap(WorldMap mapState) {
            for(int i = 0; i < this.width; ++i) {
                for(int j = 0; j < this.height; ++j) {
                    mapState.setColor(this.startX + i, this.startY + j, this.mapColors[i + j * this.width]);
                }
            }

        }
    }

    public class WorldMapHumanTracker {
        public final EntityHuman player;
        private boolean dirtyData = true;
        private int minDirtyX;
        private int minDirtyY;
        private int maxDirtyX = 127;
        private int maxDirtyY = 127;
        private boolean dirtyDecorations = true;
        private int tick;
        public int step;

        WorldMapHumanTracker(EntityHuman player) {
            this.player = player;
        }

        private WorldMap.MapPatch createPatch() {
            int i = this.minDirtyX;
            int j = this.minDirtyY;
            int k = this.maxDirtyX + 1 - this.minDirtyX;
            int l = this.maxDirtyY + 1 - this.minDirtyY;
            byte[] bs = new byte[k * l];

            for(int m = 0; m < k; ++m) {
                for(int n = 0; n < l; ++n) {
                    bs[m + n * k] = WorldMap.this.colors[i + m + (j + n) * 128];
                }
            }

            return new WorldMap.MapPatch(i, j, k, l, bs);
        }

        @Nullable
        Packet<?> nextUpdatePacket(int mapId) {
            WorldMap.MapPatch mapPatch;
            if (this.dirtyData) {
                this.dirtyData = false;
                mapPatch = this.createPatch();
            } else {
                mapPatch = null;
            }

            Collection<MapIcon> collection;
            if (this.dirtyDecorations && this.tick++ % 5 == 0) {
                this.dirtyDecorations = false;
                collection = WorldMap.this.decorations.values();
            } else {
                collection = null;
            }

            return collection == null && mapPatch == null ? null : new PacketPlayOutMap(mapId, WorldMap.this.scale, WorldMap.this.locked, collection, mapPatch);
        }

        void markColorsDirty(int startX, int startZ) {
            if (this.dirtyData) {
                this.minDirtyX = Math.min(this.minDirtyX, startX);
                this.minDirtyY = Math.min(this.minDirtyY, startZ);
                this.maxDirtyX = Math.max(this.maxDirtyX, startX);
                this.maxDirtyY = Math.max(this.maxDirtyY, startZ);
            } else {
                this.dirtyData = true;
                this.minDirtyX = startX;
                this.minDirtyY = startZ;
                this.maxDirtyX = startX;
                this.maxDirtyY = startZ;
            }

        }

        private void markDecorationsDirty() {
            this.dirtyDecorations = true;
        }
    }
}
