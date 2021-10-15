package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacer;

public class TrunkPlacerMegaJungle extends TrunkPlacerGiant {
    public static final Codec<TrunkPlacerMegaJungle> CODEC = RecordCodecBuilder.create((instance) -> {
        return trunkPlacerParts(instance).apply(instance, TrunkPlacerMegaJungle::new);
    });

    public TrunkPlacerMegaJungle(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
        super(baseHeight, firstRandomHeight, secondRandomHeight);
    }

    @Override
    protected TrunkPlacers<?> type() {
        return TrunkPlacers.MEGA_JUNGLE_TRUNK_PLACER;
    }

    @Override
    public List<WorldGenFoilagePlacer.FoliageAttachment> placeTrunk(VirtualWorldReadable world, BiConsumer<BlockPosition, IBlockData> replacer, Random random, int height, BlockPosition startPos, WorldGenFeatureTreeConfiguration config) {
        List<WorldGenFoilagePlacer.FoliageAttachment> list = Lists.newArrayList();
        list.addAll(super.placeTrunk(world, replacer, random, height, startPos, config));

        for(int i = height - 2 - random.nextInt(4); i > height / 2; i -= 2 + random.nextInt(4)) {
            float f = random.nextFloat() * ((float)Math.PI * 2F);
            int j = 0;
            int k = 0;

            for(int l = 0; l < 5; ++l) {
                j = (int)(1.5F + MathHelper.cos(f) * (float)l);
                k = (int)(1.5F + MathHelper.sin(f) * (float)l);
                BlockPosition blockPos = startPos.offset(j, i - 3 + l / 2, k);
                placeLog(world, replacer, random, blockPos, config);
            }

            list.add(new WorldGenFoilagePlacer.FoliageAttachment(startPos.offset(j, i, k), -2, false));
        }

        return list;
    }
}
