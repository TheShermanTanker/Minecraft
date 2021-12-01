package net.minecraft.data.worldgen;

import net.minecraft.data.RegistryGeneration;
import net.minecraft.util.valueproviders.FloatProviderConstant;
import net.minecraft.util.valueproviders.FloatProviderTrapezoid;
import net.minecraft.util.valueproviders.FloatProviderUniform;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.CanyonCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarverDebugSettings;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverAbstract;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverWrapper;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

public class WorldGenCarvers {
    public static final WorldGenCarverWrapper<CaveCarverConfiguration> CAVE = register("cave", WorldGenCarverAbstract.CAVE.configured(new CaveCarverConfiguration(0.15F, UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(180)), FloatProviderUniform.of(0.1F, 0.9F), VerticalAnchor.aboveBottom(8), CarverDebugSettings.of(false, Blocks.CRIMSON_BUTTON.getBlockData()), FloatProviderUniform.of(0.7F, 1.4F), FloatProviderUniform.of(0.8F, 1.3F), FloatProviderUniform.of(-1.0F, -0.4F))));
    public static final WorldGenCarverWrapper<CaveCarverConfiguration> CAVE_EXTRA_UNDERGROUND = register("cave_extra_underground", WorldGenCarverAbstract.CAVE.configured(new CaveCarverConfiguration(0.07F, UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(47)), FloatProviderUniform.of(0.1F, 0.9F), VerticalAnchor.aboveBottom(8), CarverDebugSettings.of(false, Blocks.OAK_BUTTON.getBlockData()), FloatProviderUniform.of(0.7F, 1.4F), FloatProviderUniform.of(0.8F, 1.3F), FloatProviderUniform.of(-1.0F, -0.4F))));
    public static final WorldGenCarverWrapper<CanyonCarverConfiguration> CANYON = register("canyon", WorldGenCarverAbstract.CANYON.configured(new CanyonCarverConfiguration(0.01F, UniformHeight.of(VerticalAnchor.absolute(10), VerticalAnchor.absolute(67)), FloatProviderConstant.of(3.0F), VerticalAnchor.aboveBottom(8), CarverDebugSettings.of(false, Blocks.WARPED_BUTTON.getBlockData()), FloatProviderUniform.of(-0.125F, 0.125F), new CanyonCarverConfiguration.CanyonShapeConfiguration(FloatProviderUniform.of(0.75F, 1.0F), FloatProviderTrapezoid.of(0.0F, 6.0F, 2.0F), 3, FloatProviderUniform.of(0.75F, 1.0F), 1.0F, 0.0F))));
    public static final WorldGenCarverWrapper<CaveCarverConfiguration> NETHER_CAVE = register("nether_cave", WorldGenCarverAbstract.NETHER_CAVE.configured(new CaveCarverConfiguration(0.2F, UniformHeight.of(VerticalAnchor.absolute(0), VerticalAnchor.belowTop(1)), FloatProviderConstant.of(0.5F), VerticalAnchor.aboveBottom(10), false, FloatProviderConstant.of(1.0F), FloatProviderConstant.of(1.0F), FloatProviderConstant.of(-0.7F))));

    private static <WC extends WorldGenCarverConfiguration> WorldGenCarverWrapper<WC> register(String id, WorldGenCarverWrapper<WC> configuredCarver) {
        return RegistryGeneration.register(RegistryGeneration.CONFIGURED_CARVER, id, configuredCarver);
    }
}
