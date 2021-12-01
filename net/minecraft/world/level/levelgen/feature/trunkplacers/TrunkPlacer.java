package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.mojang.datafixers.Products.P3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenTrees;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacer;

public abstract class TrunkPlacer {
    public static final Codec<TrunkPlacer> CODEC = IRegistry.TRUNK_PLACER_TYPES.byNameCodec().dispatch(TrunkPlacer::type, TrunkPlacers::codec);
    private static final int MAX_BASE_HEIGHT = 32;
    private static final int MAX_RAND = 24;
    public static final int MAX_HEIGHT = 80;
    protected final int baseHeight;
    protected final int heightRandA;
    protected final int heightRandB;

    protected static <P extends TrunkPlacer> P3<Mu<P>, Integer, Integer, Integer> trunkPlacerParts(Instance<P> instance) {
        return instance.group(Codec.intRange(0, 32).fieldOf("base_height").forGetter((placer) -> {
            return placer.baseHeight;
        }), Codec.intRange(0, 24).fieldOf("height_rand_a").forGetter((placer) -> {
            return placer.heightRandA;
        }), Codec.intRange(0, 24).fieldOf("height_rand_b").forGetter((placer) -> {
            return placer.heightRandB;
        }));
    }

    public TrunkPlacer(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
        this.baseHeight = baseHeight;
        this.heightRandA = firstRandomHeight;
        this.heightRandB = secondRandomHeight;
    }

    protected abstract TrunkPlacers<?> type();

    public abstract List<WorldGenFoilagePlacer.FoliageAttachment> placeTrunk(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, int height, BlockPosition startPos, WorldGenFeatureTreeConfiguration config);

    public int getTreeHeight(Random random) {
        return this.baseHeight + random.nextInt(this.heightRandA + 1) + random.nextInt(this.heightRandB + 1);
    }

    private static boolean isDirt(VirtualWorldReadable world, BlockPosition pos) {
        return world.isStateAtPosition(pos, (state) -> {
            return WorldGenerator.isDirt(state) && !state.is(Blocks.GRASS_BLOCK) && !state.is(Blocks.MYCELIUM);
        });
    }

    protected static void setDirtAt(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, BlockPosition pos, WorldGenFeatureTreeConfiguration config) {
        if (config.forceDirt || !isDirt(world, pos)) {
            replacer.accept(pos, config.dirtProvider.getState(random, pos));
        }

    }

    protected static boolean placeLog(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, BlockPosition pos, WorldGenFeatureTreeConfiguration config) {
        return placeLog(world, replacer, random, pos, config, Function.identity());
    }

    protected static boolean placeLog(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, BlockPosition pos, WorldGenFeatureTreeConfiguration config, Function<IBlockData, IBlockData> stateProvider) {
        if (WorldGenTrees.validTreePos(world, pos)) {
            replacer.accept(pos, stateProvider.apply(config.trunkProvider.getState(random, pos)));
            return true;
        } else {
            return false;
        }
    }

    protected static void placeLogIfFree(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, BlockPosition.MutableBlockPosition pos, WorldGenFeatureTreeConfiguration config) {
        if (WorldGenTrees.isFree(world, pos)) {
            placeLog(world, replacer, random, pos, config);
        }

    }
}
