package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityLootable;
import net.minecraft.world.level.block.entity.TileEntityMobSpawner;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootTables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldGenDungeons extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final EntityTypes<?>[] MOBS = new EntityTypes[]{EntityTypes.SKELETON, EntityTypes.ZOMBIE, EntityTypes.ZOMBIE, EntityTypes.SPIDER};
    private static final IBlockData AIR = Blocks.CAVE_AIR.getBlockData();

    public WorldGenDungeons(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        Predicate<IBlockData> predicate = WorldGenerator.isReplaceable(TagsBlock.FEATURES_CANNOT_REPLACE.getName());
        BlockPosition blockPos = context.origin();
        Random random = context.random();
        GeneratorAccessSeed worldGenLevel = context.level();
        int i = 3;
        int j = random.nextInt(2) + 2;
        int k = -j - 1;
        int l = j + 1;
        int m = -1;
        int n = 4;
        int o = random.nextInt(2) + 2;
        int p = -o - 1;
        int q = o + 1;
        int r = 0;

        for(int s = k; s <= l; ++s) {
            for(int t = -1; t <= 4; ++t) {
                for(int u = p; u <= q; ++u) {
                    BlockPosition blockPos2 = blockPos.offset(s, t, u);
                    Material material = worldGenLevel.getType(blockPos2).getMaterial();
                    boolean bl = material.isBuildable();
                    if (t == -1 && !bl) {
                        return false;
                    }

                    if (t == 4 && !bl) {
                        return false;
                    }

                    if ((s == k || s == l || u == p || u == q) && t == 0 && worldGenLevel.isEmpty(blockPos2) && worldGenLevel.isEmpty(blockPos2.above())) {
                        ++r;
                    }
                }
            }
        }

        if (r >= 1 && r <= 5) {
            for(int v = k; v <= l; ++v) {
                for(int w = 3; w >= -1; --w) {
                    for(int x = p; x <= q; ++x) {
                        BlockPosition blockPos3 = blockPos.offset(v, w, x);
                        IBlockData blockState = worldGenLevel.getType(blockPos3);
                        if (v != k && w != -1 && x != p && v != l && w != 4 && x != q) {
                            if (!blockState.is(Blocks.CHEST) && !blockState.is(Blocks.SPAWNER)) {
                                this.safeSetBlock(worldGenLevel, blockPos3, AIR, predicate);
                            }
                        } else if (blockPos3.getY() >= worldGenLevel.getMinBuildHeight() && !worldGenLevel.getType(blockPos3.below()).getMaterial().isBuildable()) {
                            worldGenLevel.setTypeAndData(blockPos3, AIR, 2);
                        } else if (blockState.getMaterial().isBuildable() && !blockState.is(Blocks.CHEST)) {
                            if (w == -1 && random.nextInt(4) != 0) {
                                this.safeSetBlock(worldGenLevel, blockPos3, Blocks.MOSSY_COBBLESTONE.getBlockData(), predicate);
                            } else {
                                this.safeSetBlock(worldGenLevel, blockPos3, Blocks.COBBLESTONE.getBlockData(), predicate);
                            }
                        }
                    }
                }
            }

            for(int y = 0; y < 2; ++y) {
                for(int z = 0; z < 3; ++z) {
                    int aa = blockPos.getX() + random.nextInt(j * 2 + 1) - j;
                    int ab = blockPos.getY();
                    int ac = blockPos.getZ() + random.nextInt(o * 2 + 1) - o;
                    BlockPosition blockPos4 = new BlockPosition(aa, ab, ac);
                    if (worldGenLevel.isEmpty(blockPos4)) {
                        int ad = 0;

                        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                            if (worldGenLevel.getType(blockPos4.relative(direction)).getMaterial().isBuildable()) {
                                ++ad;
                            }
                        }

                        if (ad == 1) {
                            this.safeSetBlock(worldGenLevel, blockPos4, StructurePiece.reorient(worldGenLevel, blockPos4, Blocks.CHEST.getBlockData()), predicate);
                            TileEntityLootable.setLootTable(worldGenLevel, random, blockPos4, LootTables.SIMPLE_DUNGEON);
                            break;
                        }
                    }
                }
            }

            this.safeSetBlock(worldGenLevel, blockPos, Blocks.SPAWNER.getBlockData(), predicate);
            TileEntity blockEntity = worldGenLevel.getTileEntity(blockPos);
            if (blockEntity instanceof TileEntityMobSpawner) {
                ((TileEntityMobSpawner)blockEntity).getSpawner().setMobName(this.randomEntityId(random));
            } else {
                LOGGER.error("Failed to fetch mob spawner entity at ({}, {}, {})", blockPos.getX(), blockPos.getY(), blockPos.getZ());
            }

            return true;
        } else {
            return false;
        }
    }

    private EntityTypes<?> randomEntityId(Random random) {
        return SystemUtils.getRandom(MOBS, random);
    }
}
