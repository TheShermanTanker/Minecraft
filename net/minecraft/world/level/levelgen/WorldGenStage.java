package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.util.INamable;

public class WorldGenStage {
    public static enum Decoration {
        RAW_GENERATION,
        LAKES,
        LOCAL_MODIFICATIONS,
        UNDERGROUND_STRUCTURES,
        SURFACE_STRUCTURES,
        STRONGHOLDS,
        UNDERGROUND_ORES,
        UNDERGROUND_DECORATION,
        FLUID_SPRINGS,
        VEGETAL_DECORATION,
        TOP_LAYER_MODIFICATION;
    }

    public static enum Features implements INamable {
        AIR("air"),
        LIQUID("liquid");

        public static final Codec<WorldGenStage.Features> CODEC = INamable.fromEnum(WorldGenStage.Features::values, WorldGenStage.Features::byName);
        private static final Map<String, WorldGenStage.Features> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(WorldGenStage.Features::getName, (carving) -> {
            return carving;
        }));
        private final String name;

        private Features(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        @Nullable
        public static WorldGenStage.Features byName(String name) {
            return BY_NAME.get(name);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
