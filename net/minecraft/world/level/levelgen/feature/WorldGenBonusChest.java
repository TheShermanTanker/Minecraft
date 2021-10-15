package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntityLootable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.storage.loot.LootTables;

public class WorldGenBonusChest extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenBonusChest(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        Random random = context.random();
        GeneratorAccessSeed worldGenLevel = context.level();
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(context.origin());
        List<Integer> list = IntStream.rangeClosed(chunkPos.getMinBlockX(), chunkPos.getMaxBlockX()).boxed().collect(Collectors.toList());
        Collections.shuffle(list, random);
        List<Integer> list2 = IntStream.rangeClosed(chunkPos.getMinBlockZ(), chunkPos.getMaxBlockZ()).boxed().collect(Collectors.toList());
        Collections.shuffle(list2, random);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(Integer integer : list) {
            for(Integer integer2 : list2) {
                mutableBlockPos.set(integer, 0, integer2);
                BlockPosition blockPos = worldGenLevel.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, mutableBlockPos);
                if (worldGenLevel.isEmpty(blockPos) || worldGenLevel.getType(blockPos).getCollisionShape(worldGenLevel, blockPos).isEmpty()) {
                    worldGenLevel.setTypeAndData(blockPos, Blocks.CHEST.getBlockData(), 2);
                    TileEntityLootable.setLootTable(worldGenLevel, random, blockPos, LootTables.SPAWN_BONUS_CHEST);
                    IBlockData blockState = Blocks.TORCH.getBlockData();

                    for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                        BlockPosition blockPos2 = blockPos.relative(direction);
                        if (blockState.canPlace(worldGenLevel, blockPos2)) {
                            worldGenLevel.setTypeAndData(blockPos2, blockState, 2);
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }
}
