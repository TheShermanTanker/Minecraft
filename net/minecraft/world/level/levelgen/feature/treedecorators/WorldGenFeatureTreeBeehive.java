package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.BlockBeehive;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;

public class WorldGenFeatureTreeBeehive extends WorldGenFeatureTree {
    public static final Codec<WorldGenFeatureTreeBeehive> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(WorldGenFeatureTreeBeehive::new, (decorator) -> {
        return decorator.probability;
    }).codec();
    private final float probability;

    public WorldGenFeatureTreeBeehive(float probability) {
        this.probability = probability;
    }

    @Override
    protected WorldGenFeatureTrees<?> type() {
        return WorldGenFeatureTrees.BEEHIVE;
    }

    @Override
    public void place(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, List<BlockPosition> logPositions, List<BlockPosition> leavesPositions) {
        if (!(random.nextFloat() >= this.probability)) {
            EnumDirection direction = BlockBeehive.getRandomOffset(random);
            int i = !leavesPositions.isEmpty() ? Math.max(leavesPositions.get(0).getY() - 1, logPositions.get(0).getY()) : Math.min(logPositions.get(0).getY() + 1 + random.nextInt(3), logPositions.get(logPositions.size() - 1).getY());
            List<BlockPosition> list = logPositions.stream().filter((pos) -> {
                return pos.getY() == i;
            }).collect(Collectors.toList());
            if (!list.isEmpty()) {
                BlockPosition blockPos = list.get(random.nextInt(list.size()));
                BlockPosition blockPos2 = blockPos.relative(direction);
                if (WorldGenerator.isAir(world, blockPos2) && WorldGenerator.isAir(world, blockPos2.relative(EnumDirection.SOUTH))) {
                    replacer.accept(blockPos2, Blocks.BEE_NEST.getBlockData().set(BlockBeehive.FACING, EnumDirection.SOUTH));
                    world.getBlockEntity(blockPos2, TileEntityTypes.BEEHIVE).ifPresent((blockEntity) -> {
                        int i = 2 + random.nextInt(2);

                        for(int j = 0; j < i; ++j) {
                            NBTTagCompound compoundTag = new NBTTagCompound();
                            compoundTag.setString("id", IRegistry.ENTITY_TYPE.getKey(EntityTypes.BEE).toString());
                            blockEntity.storeBee(compoundTag, random.nextInt(599), false);
                        }

                    });
                }
            }
        }
    }
}
