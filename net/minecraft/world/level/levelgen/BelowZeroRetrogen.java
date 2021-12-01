package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.BitSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;

public final class BelowZeroRetrogen {
    private static final BitSet EMPTY = new BitSet(0);
    private static final Codec<BitSet> BITSET_CODEC = Codec.LONG_STREAM.xmap((longStream) -> {
        return BitSet.valueOf(longStream.toArray());
    }, (bitSet) -> {
        return LongStream.of(bitSet.toLongArray());
    });
    private static final Codec<ChunkStatus> NON_EMPTY_CHUNK_STATUS = IRegistry.CHUNK_STATUS.byNameCodec().comapFlatMap((chunkStatus) -> {
        return chunkStatus == ChunkStatus.EMPTY ? DataResult.error("target_status cannot be empty") : DataResult.success(chunkStatus);
    }, Function.identity());
    public static final Codec<BelowZeroRetrogen> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(NON_EMPTY_CHUNK_STATUS.fieldOf("target_status").forGetter(BelowZeroRetrogen::targetStatus), BITSET_CODEC.optionalFieldOf("missing_bedrock").forGetter((belowZeroRetrogen) -> {
            return belowZeroRetrogen.missingBedrock.isEmpty() ? Optional.empty() : Optional.of(belowZeroRetrogen.missingBedrock);
        })).apply(instance, BelowZeroRetrogen::new);
    });
    private static final Set<ResourceKey<BiomeBase>> RETAINED_RETROGEN_BIOMES = Set.of(Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES);
    public static final IWorldHeightAccess UPGRADE_HEIGHT_ACCESSOR = new IWorldHeightAccess() {
        @Override
        public int getHeight() {
            return 64;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }
    };
    private final ChunkStatus targetStatus;
    private final BitSet missingBedrock;

    private BelowZeroRetrogen(ChunkStatus targetStatus, Optional<BitSet> missingBedrock) {
        this.targetStatus = targetStatus;
        this.missingBedrock = missingBedrock.orElse(EMPTY);
    }

    @Nullable
    public static BelowZeroRetrogen read(NBTTagCompound nbt) {
        ChunkStatus chunkStatus = ChunkStatus.byName(nbt.getString("target_status"));
        return chunkStatus == ChunkStatus.EMPTY ? null : new BelowZeroRetrogen(chunkStatus, Optional.of(BitSet.valueOf(nbt.getLongArray("missing_bedrock"))));
    }

    public static void replaceOldBedrock(ProtoChunk chunk) {
        int i = 4;
        BlockPosition.betweenClosed(0, 0, 0, 15, 4, 15).forEach((pos) -> {
            if (chunk.getType(pos).is(Blocks.BEDROCK)) {
                chunk.setType(pos, Blocks.DEEPSLATE.getBlockData(), false);
            }

        });
    }

    public void applyBedrockMask(ProtoChunk chunk) {
        IWorldHeightAccess levelHeightAccessor = chunk.getHeightAccessorForGeneration();
        int i = levelHeightAccessor.getMinBuildHeight();
        int j = levelHeightAccessor.getMaxBuildHeight() - 1;

        for(int k = 0; k < 16; ++k) {
            for(int l = 0; l < 16; ++l) {
                if (this.hasBedrockHole(k, l)) {
                    BlockPosition.betweenClosed(k, i, l, k, j, l).forEach((pos) -> {
                        chunk.setType(pos, Blocks.AIR.getBlockData(), false);
                    });
                }
            }
        }

    }

    public ChunkStatus targetStatus() {
        return this.targetStatus;
    }

    public boolean hasBedrockHoles() {
        return !this.missingBedrock.isEmpty();
    }

    public boolean hasBedrockHole(int x, int z) {
        return this.missingBedrock.get((z & 15) * 16 + (x & 15));
    }

    public static BiomeResolver getBiomeResolver(BiomeResolver biomeSupplier, IRegistry<BiomeBase> biomeRegistry, IChunkAccess chunk) {
        if (!chunk.isUpgrading()) {
            return biomeSupplier;
        } else {
            Set<BiomeBase> set = RETAINED_RETROGEN_BIOMES.stream().map(biomeRegistry::get).collect(Collectors.toSet());
            return (x, y, z, noise) -> {
                BiomeBase biome = biomeSupplier.getNoiseBiome(x, y, z, noise);
                return set.contains(biome) ? biome : chunk.getBiome(x, 0, z);
            };
        }
    }
}
