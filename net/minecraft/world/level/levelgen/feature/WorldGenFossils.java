package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import org.apache.commons.lang3.mutable.MutableInt;

public class WorldGenFossils extends WorldGenerator<FossilFeatureConfiguration> {
    public WorldGenFossils(Codec<FossilFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<FossilFeatureConfiguration> context) {
        Random random = context.random();
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        EnumBlockRotation rotation = EnumBlockRotation.getRandom(random);
        FossilFeatureConfiguration fossilFeatureConfiguration = context.config();
        int i = random.nextInt(fossilFeatureConfiguration.fossilStructures.size());
        DefinedStructureManager structureManager = worldGenLevel.getLevel().getMinecraftServer().getDefinedStructureManager();
        DefinedStructure structureTemplate = structureManager.getOrCreate(fossilFeatureConfiguration.fossilStructures.get(i));
        DefinedStructure structureTemplate2 = structureManager.getOrCreate(fossilFeatureConfiguration.overlayStructures.get(i));
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(blockPos);
        StructureBoundingBox boundingBox = new StructureBoundingBox(chunkPos.getMinBlockX(), worldGenLevel.getMinBuildHeight(), chunkPos.getMinBlockZ(), chunkPos.getMaxBlockX(), worldGenLevel.getMaxBuildHeight(), chunkPos.getMaxBlockZ());
        DefinedStructureInfo structurePlaceSettings = (new DefinedStructureInfo()).setRotation(rotation).setBoundingBox(boundingBox).setRandom(random);
        BaseBlockPosition vec3i = structureTemplate.getSize(rotation);
        int j = random.nextInt(16 - vec3i.getX());
        int k = random.nextInt(16 - vec3i.getZ());
        int l = worldGenLevel.getMaxBuildHeight();

        for(int m = 0; m < vec3i.getX(); ++m) {
            for(int n = 0; n < vec3i.getZ(); ++n) {
                l = Math.min(l, worldGenLevel.getHeight(HeightMap.Type.OCEAN_FLOOR_WG, blockPos.getX() + m + j, blockPos.getZ() + n + k));
            }
        }

        int o = Math.max(l - 15 - random.nextInt(10), worldGenLevel.getMinBuildHeight() + 10);
        BlockPosition blockPos2 = structureTemplate.getZeroPositionWithTransform(blockPos.offset(j, 0, k).atY(o), EnumBlockMirror.NONE, rotation);
        if (countEmptyCorners(worldGenLevel, structureTemplate.getBoundingBox(structurePlaceSettings, blockPos2)) > fossilFeatureConfiguration.maxEmptyCornersAllowed) {
            return false;
        } else {
            structurePlaceSettings.clearProcessors();
            fossilFeatureConfiguration.fossilProcessors.get().list().forEach((structureProcessor) -> {
                structurePlaceSettings.addProcessor(structureProcessor);
            });
            structureTemplate.placeInWorld(worldGenLevel, blockPos2, blockPos2, structurePlaceSettings, random, 4);
            structurePlaceSettings.clearProcessors();
            fossilFeatureConfiguration.overlayProcessors.get().list().forEach((structureProcessor) -> {
                structurePlaceSettings.addProcessor(structureProcessor);
            });
            structureTemplate2.placeInWorld(worldGenLevel, blockPos2, blockPos2, structurePlaceSettings, random, 4);
            return true;
        }
    }

    private static int countEmptyCorners(GeneratorAccessSeed world, StructureBoundingBox box) {
        MutableInt mutableInt = new MutableInt(0);
        box.forAllCorners((blockPos) -> {
            IBlockData blockState = world.getType(blockPos);
            if (blockState.isAir() || blockState.is(Blocks.LAVA) || blockState.is(Blocks.WATER)) {
                mutableInt.add(1);
            }

        });
        return mutableInt.getValue();
    }
}
