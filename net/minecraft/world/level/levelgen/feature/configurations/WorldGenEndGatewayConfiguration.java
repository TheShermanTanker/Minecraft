package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPosition;

public class WorldGenEndGatewayConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenEndGatewayConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BlockPosition.CODEC.optionalFieldOf("exit").forGetter((endGatewayConfiguration) -> {
            return endGatewayConfiguration.exit;
        }), Codec.BOOL.fieldOf("exact").forGetter((endGatewayConfiguration) -> {
            return endGatewayConfiguration.exact;
        })).apply(instance, WorldGenEndGatewayConfiguration::new);
    });
    private final Optional<BlockPosition> exit;
    private final boolean exact;

    private WorldGenEndGatewayConfiguration(Optional<BlockPosition> exitPos, boolean exact) {
        this.exit = exitPos;
        this.exact = exact;
    }

    public static WorldGenEndGatewayConfiguration knownExit(BlockPosition exitPortalPosition, boolean exitsAtSpawn) {
        return new WorldGenEndGatewayConfiguration(Optional.of(exitPortalPosition), exitsAtSpawn);
    }

    public static WorldGenEndGatewayConfiguration delayedExitSearch() {
        return new WorldGenEndGatewayConfiguration(Optional.empty(), false);
    }

    public Optional<BlockPosition> getExit() {
        return this.exit;
    }

    public boolean isExitExact() {
        return this.exact;
    }
}
