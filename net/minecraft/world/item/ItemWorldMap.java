package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MaterialMapColor;
import net.minecraft.world.level.saveddata.maps.WorldMap;

public class ItemWorldMap extends ItemWorldMapBase {
    public static final int IMAGE_WIDTH = 128;
    public static final int IMAGE_HEIGHT = 128;
    private static final int DEFAULT_MAP_COLOR = -12173266;
    private static final String TAG_MAP = "map";

    public ItemWorldMap(Item.Info settings) {
        super(settings);
    }

    public static ItemStack createFilledMapView(World world, int x, int z, byte scale, boolean showIcons, boolean unlimitedTracking) {
        ItemStack itemStack = new ItemStack(Items.FILLED_MAP);
        createAndStoreSavedData(itemStack, world, x, z, scale, showIcons, unlimitedTracking, world.getDimensionKey());
        return itemStack;
    }

    @Nullable
    public static WorldMap getSavedData(@Nullable Integer id, World world) {
        return id == null ? null : world.getMapData(makeKey(id));
    }

    @Nullable
    public static WorldMap getSavedMap(ItemStack map, World world) {
        Integer integer = getMapId(map);
        return getSavedData(integer, world);
    }

    @Nullable
    public static Integer getMapId(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTag();
        return compoundTag != null && compoundTag.hasKeyOfType("map", 99) ? compoundTag.getInt("map") : null;
    }

    public static int createNewSavedData(World world, int x, int z, int scale, boolean showIcons, boolean unlimitedTracking, ResourceKey<World> dimension) {
        WorldMap mapItemSavedData = WorldMap.createFresh((double)x, (double)z, (byte)scale, showIcons, unlimitedTracking, dimension);
        int i = world.getWorldMapCount();
        world.setMapData(makeKey(i), mapItemSavedData);
        return i;
    }

    private static void storeMapData(ItemStack stack, int id) {
        stack.getOrCreateTag().setInt("map", id);
    }

    private static void createAndStoreSavedData(ItemStack stack, World world, int x, int z, int scale, boolean showIcons, boolean unlimitedTracking, ResourceKey<World> dimension) {
        int i = createNewSavedData(world, x, z, scale, showIcons, unlimitedTracking, dimension);
        storeMapData(stack, i);
    }

    public static String makeKey(int mapId) {
        return "map_" + mapId;
    }

    public void update(World world, Entity entity, WorldMap state) {
        if (world.getDimensionKey() == state.dimension && entity instanceof EntityHuman) {
            int i = 1 << state.scale;
            int j = state.x;
            int k = state.z;
            int l = MathHelper.floor(entity.locX() - (double)j) / i + 64;
            int m = MathHelper.floor(entity.locZ() - (double)k) / i + 64;
            int n = 128 / i;
            if (world.getDimensionManager().hasCeiling()) {
                n /= 2;
            }

            WorldMap.WorldMapHumanTracker holdingPlayer = state.getHoldingPlayer((EntityHuman)entity);
            ++holdingPlayer.step;
            boolean bl = false;

            for(int o = l - n + 1; o < l + n; ++o) {
                if ((o & 15) == (holdingPlayer.step & 15) || bl) {
                    bl = false;
                    double d = 0.0D;

                    for(int p = m - n - 1; p < m + n; ++p) {
                        if (o >= 0 && p >= -1 && o < 128 && p < 128) {
                            int q = o - l;
                            int r = p - m;
                            boolean bl2 = q * q + r * r > (n - 2) * (n - 2);
                            int s = (j / i + o - 64) * i;
                            int t = (k / i + p - 64) * i;
                            Multiset<MaterialMapColor> multiset = LinkedHashMultiset.create();
                            Chunk levelChunk = world.getChunkAtWorldCoords(new BlockPosition(s, 0, t));
                            if (!levelChunk.isEmpty()) {
                                ChunkCoordIntPair chunkPos = levelChunk.getPos();
                                int u = s & 15;
                                int v = t & 15;
                                int w = 0;
                                double e = 0.0D;
                                if (world.getDimensionManager().hasCeiling()) {
                                    int x = s + t * 231871;
                                    x = x * x * 31287121 + x * 11;
                                    if ((x >> 20 & 1) == 0) {
                                        multiset.add(Blocks.DIRT.getBlockData().getMapColor(world, BlockPosition.ZERO), 10);
                                    } else {
                                        multiset.add(Blocks.STONE.getBlockData().getMapColor(world, BlockPosition.ZERO), 100);
                                    }

                                    e = 100.0D;
                                } else {
                                    BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
                                    BlockPosition.MutableBlockPosition mutableBlockPos2 = new BlockPosition.MutableBlockPosition();

                                    for(int y = 0; y < i; ++y) {
                                        for(int z = 0; z < i; ++z) {
                                            int aa = levelChunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, y + u, z + v) + 1;
                                            IBlockData blockState3;
                                            if (aa <= world.getMinBuildHeight() + 1) {
                                                blockState3 = Blocks.BEDROCK.getBlockData();
                                            } else {
                                                do {
                                                    --aa;
                                                    mutableBlockPos.set(chunkPos.getMinBlockX() + y + u, aa, chunkPos.getMinBlockZ() + z + v);
                                                    blockState3 = levelChunk.getType(mutableBlockPos);
                                                } while(blockState3.getMapColor(world, mutableBlockPos) == MaterialMapColor.NONE && aa > world.getMinBuildHeight());

                                                if (aa > world.getMinBuildHeight() && !blockState3.getFluid().isEmpty()) {
                                                    int ab = aa - 1;
                                                    mutableBlockPos2.set(mutableBlockPos);

                                                    IBlockData blockState2;
                                                    do {
                                                        mutableBlockPos2.setY(ab--);
                                                        blockState2 = levelChunk.getType(mutableBlockPos2);
                                                        ++w;
                                                    } while(ab > world.getMinBuildHeight() && !blockState2.getFluid().isEmpty());

                                                    blockState3 = this.getCorrectStateForFluidBlock(world, blockState3, mutableBlockPos);
                                                }
                                            }

                                            state.checkBanners(world, chunkPos.getMinBlockX() + y + u, chunkPos.getMinBlockZ() + z + v);
                                            e += (double)aa / (double)(i * i);
                                            multiset.add(blockState3.getMapColor(world, mutableBlockPos));
                                        }
                                    }
                                }

                                w = w / (i * i);
                                double f = (e - d) * 4.0D / (double)(i + 4) + ((double)(o + p & 1) - 0.5D) * 0.4D;
                                int ac = 1;
                                if (f > 0.6D) {
                                    ac = 2;
                                }

                                if (f < -0.6D) {
                                    ac = 0;
                                }

                                MaterialMapColor materialColor = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MaterialMapColor.NONE);
                                if (materialColor == MaterialMapColor.WATER) {
                                    f = (double)w * 0.1D + (double)(o + p & 1) * 0.2D;
                                    ac = 1;
                                    if (f < 0.5D) {
                                        ac = 2;
                                    }

                                    if (f > 0.9D) {
                                        ac = 0;
                                    }
                                }

                                d = e;
                                if (p >= 0 && q * q + r * r < n * n && (!bl2 || (o + p & 1) != 0)) {
                                    bl |= state.updateColor(o, p, (byte)(materialColor.id * 4 + ac));
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private IBlockData getCorrectStateForFluidBlock(World world, IBlockData state, BlockPosition pos) {
        Fluid fluidState = state.getFluid();
        return !fluidState.isEmpty() && !state.isFaceSturdy(world, pos, EnumDirection.UP) ? fluidState.getBlockData() : state;
    }

    private static boolean isLand(BiomeBase[] biomes, int scale, int x, int z) {
        return biomes[x * scale + z * scale * 128 * scale].getDepth() >= 0.0F;
    }

    public static void applySepiaFilter(WorldServer world, ItemStack map) {
        WorldMap mapItemSavedData = getSavedMap(map, world);
        if (mapItemSavedData != null) {
            if (world.getDimensionKey() == mapItemSavedData.dimension) {
                int i = 1 << mapItemSavedData.scale;
                int j = mapItemSavedData.x;
                int k = mapItemSavedData.z;
                BiomeBase[] biomes = new BiomeBase[128 * i * 128 * i];

                for(int l = 0; l < 128 * i; ++l) {
                    for(int m = 0; m < 128 * i; ++m) {
                        biomes[l * 128 * i + m] = world.getBiome(new BlockPosition((j / i - 64) * i + m, 0, (k / i - 64) * i + l));
                    }
                }

                for(int n = 0; n < 128; ++n) {
                    for(int o = 0; o < 128; ++o) {
                        if (n > 0 && o > 0 && n < 127 && o < 127) {
                            BiomeBase biome = biomes[n * i + o * i * 128 * i];
                            int p = 8;
                            if (isLand(biomes, i, n - 1, o - 1)) {
                                --p;
                            }

                            if (isLand(biomes, i, n - 1, o + 1)) {
                                --p;
                            }

                            if (isLand(biomes, i, n - 1, o)) {
                                --p;
                            }

                            if (isLand(biomes, i, n + 1, o - 1)) {
                                --p;
                            }

                            if (isLand(biomes, i, n + 1, o + 1)) {
                                --p;
                            }

                            if (isLand(biomes, i, n + 1, o)) {
                                --p;
                            }

                            if (isLand(biomes, i, n, o - 1)) {
                                --p;
                            }

                            if (isLand(biomes, i, n, o + 1)) {
                                --p;
                            }

                            int q = 3;
                            MaterialMapColor materialColor = MaterialMapColor.NONE;
                            if (biome.getDepth() < 0.0F) {
                                materialColor = MaterialMapColor.COLOR_ORANGE;
                                if (p > 7 && o % 2 == 0) {
                                    q = (n + (int)(MathHelper.sin((float)o + 0.0F) * 7.0F)) / 8 % 5;
                                    if (q == 3) {
                                        q = 1;
                                    } else if (q == 4) {
                                        q = 0;
                                    }
                                } else if (p > 7) {
                                    materialColor = MaterialMapColor.NONE;
                                } else if (p > 5) {
                                    q = 1;
                                } else if (p > 3) {
                                    q = 0;
                                } else if (p > 1) {
                                    q = 0;
                                }
                            } else if (p > 0) {
                                materialColor = MaterialMapColor.COLOR_BROWN;
                                if (p > 3) {
                                    q = 1;
                                } else {
                                    q = 3;
                                }
                            }

                            if (materialColor != MaterialMapColor.NONE) {
                                mapItemSavedData.setColor(n, o, (byte)(materialColor.id * 4 + q));
                            }
                        }
                    }
                }

            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClientSide) {
            WorldMap mapItemSavedData = getSavedMap(stack, world);
            if (mapItemSavedData != null) {
                if (entity instanceof EntityHuman) {
                    EntityHuman player = (EntityHuman)entity;
                    mapItemSavedData.tickCarriedBy(player, stack);
                }

                if (!mapItemSavedData.locked && (selected || entity instanceof EntityHuman && ((EntityHuman)entity).getItemInOffHand() == stack)) {
                    this.update(world, entity, mapItemSavedData);
                }

            }
        }
    }

    @Nullable
    @Override
    public Packet<?> getUpdatePacket(ItemStack stack, World world, EntityHuman player) {
        Integer integer = getMapId(stack);
        WorldMap mapItemSavedData = getSavedData(integer, world);
        return mapItemSavedData != null ? mapItemSavedData.getUpdatePacket(integer, player) : null;
    }

    @Override
    public void onCraftedBy(ItemStack stack, World world, EntityHuman player) {
        NBTTagCompound compoundTag = stack.getTag();
        if (compoundTag != null && compoundTag.hasKeyOfType("map_scale_direction", 99)) {
            scaleMap(stack, world, compoundTag.getInt("map_scale_direction"));
            compoundTag.remove("map_scale_direction");
        } else if (compoundTag != null && compoundTag.hasKeyOfType("map_to_lock", 1) && compoundTag.getBoolean("map_to_lock")) {
            lockMap(world, stack);
            compoundTag.remove("map_to_lock");
        }

    }

    private static void scaleMap(ItemStack map, World world, int amount) {
        WorldMap mapItemSavedData = getSavedMap(map, world);
        if (mapItemSavedData != null) {
            int i = world.getWorldMapCount();
            world.setMapData(makeKey(i), mapItemSavedData.scaled(amount));
            storeMapData(map, i);
        }

    }

    public static void lockMap(World world, ItemStack stack) {
        WorldMap mapItemSavedData = getSavedMap(stack, world);
        if (mapItemSavedData != null) {
            int i = world.getWorldMapCount();
            String string = makeKey(i);
            WorldMap mapItemSavedData2 = mapItemSavedData.locked();
            world.setMapData(string, mapItemSavedData2);
            storeMapData(stack, i);
        }

    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        Integer integer = getMapId(stack);
        WorldMap mapItemSavedData = world == null ? null : getSavedData(integer, world);
        if (mapItemSavedData != null && mapItemSavedData.locked) {
            tooltip.add((new ChatMessage("filled_map.locked", integer)).withStyle(EnumChatFormat.GRAY));
        }

        if (context.isAdvanced()) {
            if (mapItemSavedData != null) {
                tooltip.add((new ChatMessage("filled_map.id", integer)).withStyle(EnumChatFormat.GRAY));
                tooltip.add((new ChatMessage("filled_map.scale", 1 << mapItemSavedData.scale)).withStyle(EnumChatFormat.GRAY));
                tooltip.add((new ChatMessage("filled_map.level", mapItemSavedData.scale, 4)).withStyle(EnumChatFormat.GRAY));
            } else {
                tooltip.add((new ChatMessage("filled_map.unknown")).withStyle(EnumChatFormat.GRAY));
            }
        }

    }

    public static int getColor(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTagElement("display");
        if (compoundTag != null && compoundTag.hasKeyOfType("MapColor", 99)) {
            int i = compoundTag.getInt("MapColor");
            return -16777216 | i & 16777215;
        } else {
            return -12173266;
        }
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        IBlockData blockState = context.getWorld().getType(context.getClickPosition());
        if (blockState.is(TagsBlock.BANNERS)) {
            if (!context.getWorld().isClientSide) {
                WorldMap mapItemSavedData = getSavedMap(context.getItemStack(), context.getWorld());
                if (mapItemSavedData != null && !mapItemSavedData.toggleBanner(context.getWorld(), context.getClickPosition())) {
                    return EnumInteractionResult.FAIL;
                }
            }

            return EnumInteractionResult.sidedSuccess(context.getWorld().isClientSide);
        } else {
            return super.useOn(context);
        }
    }
}
