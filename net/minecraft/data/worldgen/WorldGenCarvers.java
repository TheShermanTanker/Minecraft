package net.minecraft.data.worldgen;

import net.minecraft.data.RegistryGeneration;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.TrapezoidFloat;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.CanyonCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarverDebugSettings;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverAbstract;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverWrapper;
import net.minecraft.world.level.levelgen.heightproviders.BiasedToBottomHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

public class WorldGenCarvers {
    public static final WorldGenCarverWrapper<CaveCarverConfiguration> CAVE = register("cave", WorldGenCarverAbstract.CAVE.configured(new CaveCarverConfiguration(0.14285715F, BiasedToBottomHeight.of(VerticalAnchor.absolute(0), VerticalAnchor.absolute(127), 8), ConstantFloat.of(0.5F), VerticalAnchor.aboveBottom(10), false, CarverDebugSettings.of(false, Blocks.CRIMSON_BUTTON.getBlockData()), ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), ConstantFloat.of(-0.7F))));
    public static final WorldGenCarverWrapper<CaveCarverConfiguration> PROTOTYPE_CAVE = register("prototype_cave", WorldGenCarverAbstract.CAVE.configured(new CaveCarverConfiguration(0.33333334F, UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(126)), UniformFloat.of(0.1F, 0.9F), VerticalAnchor.aboveBottom(8), false, CarverDebugSettings.of(false, Blocks.CRIMSON_BUTTON.getBlockData()), UniformFloat.of(0.7F, 1.4F), UniformFloat.of(0.8F, 1.3F), UniformFloat.of(-1.0F, -0.4F))));
    public static final WorldGenCarverWrapper<CanyonCarverConfiguration> CANYON = register("canyon", WorldGenCarverAbstract.CANYON.configured(new CanyonCarverConfiguration(0.02F, BiasedToBottomHeight.of(VerticalAnchor.absolute(20), VerticalAnchor.absolute(67), 8), ConstantFloat.of(3.0F), VerticalAnchor.aboveBottom(10), false, CarverDebugSettings.of(false, Blocks.WARPED_BUTTON.getBlockData()), UniformFloat.of(-0.125F, 0.125F), new CanyonCarverConfiguration.CanyonShapeConfiguration(UniformFloat.of(0.75F, 1.0F), TrapezoidFloat.of(0.0F, 6.0F, 2.0F), 3, UniformFloat.of(0.75F, 1.0F), 1.0F, 0.0F))));
    public static final WorldGenCarverWrapper<CanyonCarverConfiguration> PROTOTYPE_CANYON = register("prototype_canyon", WorldGenCarverAbstract.CANYON.configured(new CanyonCarverConfiguration(0.02F, UniformHeight.of(VerticalAnchor.absolute(10), VerticalAnchor.absolute(67)), ConstantFloat.of(3.0F), VerticalAnchor.aboveBottom(8), false, CarverDebugSettings.of(false, Blocks.WARPED_BUTTON.getBlockData()), UniformFloat.of(-0.125F, 0.125F), new CanyonCarverConfiguration.CanyonShapeConfiguration(UniformFloat.of(0.75F, 1.0F), TrapezoidFloat.of(0.0F, 6.0F, 2.0F), 3, UniformFloat.of(0.75F, 1.0F), 1.0F, 0.0F))));
    public static final WorldGenCarverWrapper<CaveCarverConfiguration> OCEAN_CAVE = register("ocean_cave", WorldGenCarverAbstract.CAVE.configured(new CaveCarverConfiguration(0.06666667F, BiasedToBottomHeight.of(VerticalAnchor.absolute(0), VerticalAnchor.absolute(127), 8), ConstantFloat.of(0.5F), VerticalAnchor.aboveBottom(10), false, CarverDebugSettings.of(false, Blocks.CRIMSON_BUTTON.getBlockData()), ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), ConstantFloat.of(-0.7F))));
    public static final WorldGenCarverWrapper<CanyonCarverConfiguration> UNDERWATER_CANYON = register("underwater_canyon", WorldGenCarverAbstract.UNDERWATER_CANYON.configured(new CanyonCarverConfiguration(0.02F, BiasedToBottomHeight.of(VerticalAnchor.absolute(20), VerticalAnchor.absolute(67), 8), ConstantFloat.of(3.0F), VerticalAnchor.aboveBottom(10), false, CarverDebugSettings.of(false, Blocks.WARPED_BUTTON.getBlockData()), UniformFloat.of(-0.125F, 0.125F), new CanyonCarverConfiguration.CanyonShapeConfiguration(UniformFloat.of(0.75F, 1.0F), TrapezoidFloat.of(0.0F, 6.0F, 2.0F), 3, UniformFloat.of(0.75F, 1.0F), 1.0F, 0.0F))));
    public static final WorldGenCarverWrapper<CaveCarverConfiguration> UNDERWATER_CAVE = register("underwater_cave", WorldGenCarverAbstract.UNDERWATER_CAVE.configured(new CaveCarverConfiguration(0.06666667F, BiasedToBottomHeight.of(VerticalAnchor.absolute(0), VerticalAnchor.absolute(127), 8), ConstantFloat.of(0.5F), VerticalAnchor.aboveBottom(10), false, CarverDebugSettings.of(false, Blocks.CRIMSON_BUTTON.getBlockData()), ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), ConstantFloat.of(-0.7F))));
    public static final WorldGenCarverWrapper<CaveCarverConfiguration> NETHER_CAVE = register("nether_cave", WorldGenCarverAbstract.NETHER_CAVE.configured(new CaveCarverConfiguration(0.2F, UniformHeight.of(VerticalAnchor.absolute(0), VerticalAnchor.belowTop(1)), ConstantFloat.of(0.5F), VerticalAnchor.aboveBottom(10), false, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), ConstantFloat.of(-0.7F))));
    public static final WorldGenCarverWrapper<CanyonCarverConfiguration> PROTOTYPE_CREVICE = register("prototype_crevice", WorldGenCarverAbstract.CANYON.configured(new CanyonCarverConfiguration(0.00125F, UniformHeight.of(VerticalAnchor.absolute(40), VerticalAnchor.absolute(80)), UniformFloat.of(6.0F, 8.0F), VerticalAnchor.aboveBottom(8), false, CarverDebugSettings.of(false, Blocks.OAK_BUTTON.getBlockData()), UniformFloat.of(-0.125F, 0.125F), new CanyonCarverConfiguration.CanyonShapeConfiguration(UniformFloat.of(0.5F, 1.0F), UniformFloat.of(0.0F, 1.0F), 6, UniformFloat.of(0.25F, 1.0F), 0.0F, 5.0F))));

    private static <WC extends WorldGenCarverConfiguration> WorldGenCarverWrapper<WC> register(String id, WorldGenCarverWrapper<WC> configuredCarver) {
        return RegistryGeneration.register(RegistryGeneration.CONFIGURED_CARVER, id, configuredCarver);
    }
}
