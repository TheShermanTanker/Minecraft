package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.world.level.BlockAccessAir;
import net.minecraft.world.level.BlockColumn;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureChanceDecoratorRangeConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenFeatureNetherFossil extends StructureGenerator<WorldGenFeatureChanceDecoratorRangeConfiguration> {
    public WorldGenFeatureNetherFossil(Codec<WorldGenFeatureChanceDecoratorRangeConfiguration> codec) {
        super(codec);
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureChanceDecoratorRangeConfiguration> getStartFactory() {
        return WorldGenFeatureNetherFossil.FeatureStart::new;
    }

    public static class FeatureStart extends NoiseAffectingStructureStart<WorldGenFeatureChanceDecoratorRangeConfiguration> {
        public FeatureStart(StructureGenerator<WorldGenFeatureChanceDecoratorRangeConfiguration> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureChanceDecoratorRangeConfiguration config, IWorldHeightAccess world) {
            int i = pos.getMinBlockX() + this.random.nextInt(16);
            int j = pos.getMinBlockZ() + this.random.nextInt(16);
            int k = chunkGenerator.getSeaLevel();
            WorldGenerationContext worldGenerationContext = new WorldGenerationContext(chunkGenerator, world);
            int l = config.height.sample(this.random, worldGenerationContext);
            BlockColumn noiseColumn = chunkGenerator.getBaseColumn(i, j, world);

            for(BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(i, l, j); l > k; --l) {
                IBlockData blockState = noiseColumn.getBlockState(mutableBlockPos);
                mutableBlockPos.move(EnumDirection.DOWN);
                IBlockData blockState2 = noiseColumn.getBlockState(mutableBlockPos);
                if (blockState.isAir() && (blockState2.is(Blocks.SOUL_SAND) || blockState2.isFaceSturdy(BlockAccessAir.INSTANCE, mutableBlockPos, EnumDirection.UP))) {
                    break;
                }
            }

            if (l > k) {
                WorldGenNetherFossil.addPieces(manager, this, this.random, new BlockPosition(i, l, j));
            }
        }
    }
}
