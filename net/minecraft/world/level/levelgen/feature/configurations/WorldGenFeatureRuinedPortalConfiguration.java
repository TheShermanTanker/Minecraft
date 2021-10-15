package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureRuinedPortal;

public class WorldGenFeatureRuinedPortalConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureRuinedPortalConfiguration> CODEC = WorldGenFeatureRuinedPortal.Type.CODEC.fieldOf("portal_type").xmap(WorldGenFeatureRuinedPortalConfiguration::new, (ruinedPortalConfiguration) -> {
        return ruinedPortalConfiguration.portalType;
    }).codec();
    public final WorldGenFeatureRuinedPortal.Type portalType;

    public WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type portalType) {
        this.portalType = portalType;
    }
}
