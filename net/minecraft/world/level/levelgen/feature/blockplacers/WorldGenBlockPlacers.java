package net.minecraft.world.level.levelgen.feature.blockplacers;

import com.mojang.serialization.Codec;
import net.minecraft.core.IRegistry;

public class WorldGenBlockPlacers<P extends WorldGenBlockPlacer> {
    public static final WorldGenBlockPlacers<WorldGenBlockPlacerSimple> SIMPLE_BLOCK_PLACER = register("simple_block_placer", WorldGenBlockPlacerSimple.CODEC);
    public static final WorldGenBlockPlacers<WorldGenBlockPlacerDoublePlant> DOUBLE_PLANT_PLACER = register("double_plant_placer", WorldGenBlockPlacerDoublePlant.CODEC);
    public static final WorldGenBlockPlacers<WorldGenBlockPlacerColumn> COLUMN_PLACER = register("column_placer", WorldGenBlockPlacerColumn.CODEC);
    private final Codec<P> codec;

    private static <P extends WorldGenBlockPlacer> WorldGenBlockPlacers<P> register(String id, Codec<P> codec) {
        return IRegistry.register(IRegistry.BLOCK_PLACER_TYPES, id, new WorldGenBlockPlacers<>(codec));
    }

    private WorldGenBlockPlacers(Codec<P> codec) {
        this.codec = codec;
    }

    public Codec<P> codec() {
        return this.codec;
    }
}
