package net.minecraft.world.level.levelgen;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.DataBits;
import net.minecraft.util.INamable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.block.BlockLeaves;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HeightMap {
    private static final Logger LOGGER = LogManager.getLogger();
    static final Predicate<IBlockData> NOT_AIR = (state) -> {
        return !state.isAir();
    };
    static final Predicate<IBlockData> MATERIAL_MOTION_BLOCKING = (state) -> {
        return state.getMaterial().isSolid();
    };
    private final DataBits data;
    private final Predicate<IBlockData> isOpaque;
    private final IChunkAccess chunk;

    public HeightMap(IChunkAccess chunk, HeightMap.Type type) {
        this.isOpaque = type.isOpaque();
        this.chunk = chunk;
        int i = MathHelper.ceillog2(chunk.getHeight() + 1);
        this.data = new DataBits(i, 256);
    }

    public static void primeHeightmaps(IChunkAccess chunk, Set<HeightMap.Type> types) {
        int i = types.size();
        ObjectList<HeightMap> objectList = new ObjectArrayList<>(i);
        ObjectListIterator<HeightMap> objectListIterator = objectList.iterator();
        int j = chunk.getHighestSectionPosition() + 16;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int k = 0; k < 16; ++k) {
            for(int l = 0; l < 16; ++l) {
                for(HeightMap.Type types2 : types) {
                    objectList.add(chunk.getOrCreateHeightmapUnprimed(types2));
                }

                for(int m = j - 1; m >= chunk.getMinBuildHeight(); --m) {
                    mutableBlockPos.set(k, m, l);
                    IBlockData blockState = chunk.getType(mutableBlockPos);
                    if (!blockState.is(Blocks.AIR)) {
                        while(objectListIterator.hasNext()) {
                            HeightMap heightmap = objectListIterator.next();
                            if (heightmap.isOpaque.test(blockState)) {
                                heightmap.setHeight(k, l, m + 1);
                                objectListIterator.remove();
                            }
                        }

                        if (objectList.isEmpty()) {
                            break;
                        }

                        objectListIterator.back(i);
                    }
                }
            }
        }

    }

    public boolean update(int x, int y, int z, IBlockData state) {
        int i = this.getFirstAvailable(x, z);
        if (y <= i - 2) {
            return false;
        } else {
            if (this.isOpaque.test(state)) {
                if (y >= i) {
                    this.setHeight(x, z, y + 1);
                    return true;
                }
            } else if (i - 1 == y) {
                BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

                for(int j = y - 1; j >= this.chunk.getMinBuildHeight(); --j) {
                    mutableBlockPos.set(x, j, z);
                    if (this.isOpaque.test(this.chunk.getType(mutableBlockPos))) {
                        this.setHeight(x, z, j + 1);
                        return true;
                    }
                }

                this.setHeight(x, z, this.chunk.getMinBuildHeight());
                return true;
            }

            return false;
        }
    }

    public int getFirstAvailable(int x, int z) {
        return this.getFirstAvailable(getIndex(x, z));
    }

    public int getHighestTaken(int i, int j) {
        return this.getFirstAvailable(getIndex(i, j)) - 1;
    }

    private int getFirstAvailable(int index) {
        return this.data.get(index) + this.chunk.getMinBuildHeight();
    }

    private void setHeight(int x, int z, int height) {
        this.data.set(getIndex(x, z), height - this.chunk.getMinBuildHeight());
    }

    public void setRawData(IChunkAccess chunkAccess, HeightMap.Type types, long[] ls) {
        long[] ms = this.data.getRaw();
        if (ms.length == ls.length) {
            System.arraycopy(ls, 0, ms, 0, ls.length);
        } else {
            LOGGER.warn("Ignoring heightmap data for chunk " + chunkAccess.getPos() + ", size does not match; expected: " + ms.length + ", got: " + ls.length);
            primeHeightmaps(chunkAccess, EnumSet.of(types));
        }
    }

    public long[] getRawData() {
        return this.data.getRaw();
    }

    private static int getIndex(int x, int z) {
        return x + z * 16;
    }

    public static enum Type implements INamable {
        WORLD_SURFACE_WG("WORLD_SURFACE_WG", HeightMap.Use.WORLDGEN, HeightMap.NOT_AIR),
        WORLD_SURFACE("WORLD_SURFACE", HeightMap.Use.CLIENT, HeightMap.NOT_AIR),
        OCEAN_FLOOR_WG("OCEAN_FLOOR_WG", HeightMap.Use.WORLDGEN, HeightMap.MATERIAL_MOTION_BLOCKING),
        OCEAN_FLOOR("OCEAN_FLOOR", HeightMap.Use.LIVE_WORLD, HeightMap.MATERIAL_MOTION_BLOCKING),
        MOTION_BLOCKING("MOTION_BLOCKING", HeightMap.Use.CLIENT, (blockState) -> {
            return blockState.getMaterial().isSolid() || !blockState.getFluid().isEmpty();
        }),
        MOTION_BLOCKING_NO_LEAVES("MOTION_BLOCKING_NO_LEAVES", HeightMap.Use.LIVE_WORLD, (blockState) -> {
            return (blockState.getMaterial().isSolid() || !blockState.getFluid().isEmpty()) && !(blockState.getBlock() instanceof BlockLeaves);
        });

        public static final Codec<HeightMap.Type> CODEC = INamable.fromEnum(HeightMap.Type::values, HeightMap.Type::getFromKey);
        private final String serializationKey;
        private final HeightMap.Use usage;
        private final Predicate<IBlockData> isOpaque;
        private static final Map<String, HeightMap.Type> REVERSE_LOOKUP = SystemUtils.make(Maps.newHashMap(), (hashMap) -> {
            for(HeightMap.Type types : values()) {
                hashMap.put(types.serializationKey, types);
            }

        });

        private Type(String name, HeightMap.Use purpose, Predicate<IBlockData> blockPredicate) {
            this.serializationKey = name;
            this.usage = purpose;
            this.isOpaque = blockPredicate;
        }

        public String getSerializationKey() {
            return this.serializationKey;
        }

        public boolean sendToClient() {
            return this.usage == HeightMap.Use.CLIENT;
        }

        public boolean keepAfterWorldgen() {
            return this.usage != HeightMap.Use.WORLDGEN;
        }

        @Nullable
        public static HeightMap.Type getFromKey(String name) {
            return REVERSE_LOOKUP.get(name);
        }

        public Predicate<IBlockData> isOpaque() {
            return this.isOpaque;
        }

        @Override
        public String getSerializedName() {
            return this.serializationKey;
        }
    }

    public static enum Use {
        WORLDGEN,
        LIVE_WORLD,
        CLIENT;
    }
}
