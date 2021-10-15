package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;

public class WorldGenFeatureTreeAlterGround extends WorldGenFeatureTree {
    public static final Codec<WorldGenFeatureTreeAlterGround> CODEC = WorldGenFeatureStateProvider.CODEC.fieldOf("provider").xmap(WorldGenFeatureTreeAlterGround::new, (decorator) -> {
        return decorator.provider;
    }).codec();
    private final WorldGenFeatureStateProvider provider;

    public WorldGenFeatureTreeAlterGround(WorldGenFeatureStateProvider provider) {
        this.provider = provider;
    }

    @Override
    protected WorldGenFeatureTrees<?> type() {
        return WorldGenFeatureTrees.ALTER_GROUND;
    }

    @Override
    public void place(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, List<BlockPosition> logPositions, List<BlockPosition> leavesPositions) {
        if (!logPositions.isEmpty()) {
            int i = logPositions.get(0).getY();
            logPositions.stream().filter((pos) -> {
                return pos.getY() == i;
            }).forEach((pos) -> {
                this.placeCircle(world, replacer, random, pos.west().north());
                this.placeCircle(world, replacer, random, pos.east(2).north());
                this.placeCircle(world, replacer, random, pos.west().south(2));
                this.placeCircle(world, replacer, random, pos.east(2).south(2));

                for(int i = 0; i < 5; ++i) {
                    int j = random.nextInt(64);
                    int k = j % 8;
                    int l = j / 8;
                    if (k == 0 || k == 7 || l == 0 || l == 7) {
                        this.placeCircle(world, replacer, random, pos.offset(-3 + k, 0, -3 + l));
                    }
                }

            });
        }
    }

    private void placeCircle(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, BlockPosition pos) {
        for(int i = -2; i <= 2; ++i) {
            for(int j = -2; j <= 2; ++j) {
                if (Math.abs(i) != 2 || Math.abs(j) != 2) {
                    this.placeBlockAt(world, replacer, random, pos.offset(i, 0, j));
                }
            }
        }

    }

    private void placeBlockAt(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, BlockPosition pos) {
        for(int i = 2; i >= -3; --i) {
            BlockPosition blockPos = pos.above(i);
            if (WorldGenerator.isGrassOrDirt(world, blockPos)) {
                replacer.accept(blockPos, this.provider.getState(random, pos));
                break;
            }

            if (!WorldGenerator.isAir(world, blockPos) && i < 0) {
                break;
            }
        }

    }
}
