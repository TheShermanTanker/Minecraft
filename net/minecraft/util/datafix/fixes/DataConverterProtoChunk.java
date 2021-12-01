package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataConverterProtoChunk extends DataFix {
    private static final int NUM_SECTIONS = 16;

    public DataConverterProtoChunk(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return TypeRewriteRule.seq(this.writeFixAndRead("ChunkToProtoChunkFix", this.getInputSchema().getType(DataConverterTypes.CHUNK), this.getOutputSchema().getType(DataConverterTypes.CHUNK), (dynamic) -> {
            return dynamic.update("Level", DataConverterProtoChunk::fixChunkData);
        }), this.writeAndRead("Structure biome inject", this.getInputSchema().getType(DataConverterTypes.STRUCTURE_FEATURE), this.getOutputSchema().getType(DataConverterTypes.STRUCTURE_FEATURE)));
    }

    private static <T> Dynamic<T> fixChunkData(Dynamic<T> dynamic) {
        boolean bl = dynamic.get("TerrainPopulated").asBoolean(false) && (dynamic.get("LightPopulated").asNumber().result().isEmpty() || dynamic.get("LightPopulated").asBoolean(false));
        Dynamic<T> dynamic2;
        if (bl) {
            dynamic2 = repackTicks(repackBiomes(dynamic));
        } else {
            dynamic2 = createEmptyChunk(dynamic);
        }

        return dynamic2.set("Status", dynamic.createString(bl ? "mobs_spawned" : "empty")).set("hasLegacyStructureData", dynamic.createBoolean(true));
    }

    private static <T> Dynamic<T> repackBiomes(Dynamic<T> dynamic) {
        return dynamic.update("Biomes", (dynamic2) -> {
            return DataFixUtils.orElse(dynamic2.asByteBufferOpt().result().map((byteBuffer) -> {
                int[] is = new int[256];

                for(int i = 0; i < is.length; ++i) {
                    if (i < byteBuffer.capacity()) {
                        is[i] = byteBuffer.get(i) & 255;
                    }
                }

                return dynamic.createIntList(Arrays.stream(is));
            }), dynamic2);
        });
    }

    private static <T> Dynamic<T> repackTicks(Dynamic<T> dynamic) {
        return DataFixUtils.orElse(dynamic.get("TileTicks").asStreamOpt().result().map((stream) -> {
            List<ShortList> list = IntStream.range(0, 16).mapToObj((i) -> {
                return new ShortArrayList();
            }).collect(Collectors.toList());
            stream.forEach((dynamicx) -> {
                int i = dynamicx.get("x").asInt(0);
                int j = dynamicx.get("y").asInt(0);
                int k = dynamicx.get("z").asInt(0);
                short s = packOffsetCoordinates(i, j, k);
                list.get(j >> 4).add(s);
            });
            return dynamic.remove("TileTicks").set("ToBeTicked", dynamic.createList(list.stream().map((shortList) -> {
                return dynamic.createList(shortList.intStream().mapToObj((i) -> {
                    return dynamic.createShort((short)i);
                }));
            })));
        }), dynamic);
    }

    private static <T> Dynamic<T> createEmptyChunk(Dynamic<T> dynamic) {
        Builder<Dynamic<T>, Dynamic<T>> builder = ImmutableMap.builder();
        dynamic.get("xPos").result().ifPresent((dynamic2) -> {
            builder.put(dynamic.createString("xPos"), dynamic2);
        });
        dynamic.get("zPos").result().ifPresent((dynamic2) -> {
            builder.put(dynamic.createString("zPos"), dynamic2);
        });
        return dynamic.createMap(builder.build());
    }

    private static short packOffsetCoordinates(int x, int y, int z) {
        return (short)(x & 15 | (y & 15) << 4 | (z & 15) << 8);
    }
}
