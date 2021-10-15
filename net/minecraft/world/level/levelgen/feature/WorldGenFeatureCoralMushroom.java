package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenFeatureCoralMushroom extends WorldGenFeatureCoral {
    public WorldGenFeatureCoralMushroom(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected boolean placeFeature(GeneratorAccess world, Random random, BlockPosition pos, IBlockData state) {
        int i = random.nextInt(3) + 3;
        int j = random.nextInt(3) + 3;
        int k = random.nextInt(3) + 3;
        int l = random.nextInt(3) + 1;
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();

        for(int m = 0; m <= j; ++m) {
            for(int n = 0; n <= i; ++n) {
                for(int o = 0; o <= k; ++o) {
                    mutableBlockPos.set(m + pos.getX(), n + pos.getY(), o + pos.getZ());
                    mutableBlockPos.move(EnumDirection.DOWN, l);
                    if ((m != 0 && m != j || n != 0 && n != i) && (o != 0 && o != k || n != 0 && n != i) && (m != 0 && m != j || o != 0 && o != k) && (m == 0 || m == j || n == 0 || n == i || o == 0 || o == k) && !(random.nextFloat() < 0.1F) && !this.placeCoralBlock(world, random, mutableBlockPos, state)) {
                    }
                }
            }
        }

        return true;
    }
}
