package net.minecraft.world.level.levelgen.feature.structures;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;

public class WorldGenFeatureDefinedStructureJigsawJunction {
    private final int sourceX;
    private final int sourceGroundY;
    private final int sourceZ;
    private final int deltaY;
    private final WorldGenFeatureDefinedStructurePoolTemplate.Matching destProjection;

    public WorldGenFeatureDefinedStructureJigsawJunction(int sourceX, int sourceGroundY, int sourceZ, int deltaY, WorldGenFeatureDefinedStructurePoolTemplate.Matching destProjection) {
        this.sourceX = sourceX;
        this.sourceGroundY = sourceGroundY;
        this.sourceZ = sourceZ;
        this.deltaY = deltaY;
        this.destProjection = destProjection;
    }

    public int getSourceX() {
        return this.sourceX;
    }

    public int getSourceGroundY() {
        return this.sourceGroundY;
    }

    public int getSourceZ() {
        return this.sourceZ;
    }

    public int getDeltaY() {
        return this.deltaY;
    }

    public WorldGenFeatureDefinedStructurePoolTemplate.Matching getDestProjection() {
        return this.destProjection;
    }

    public <T> Dynamic<T> serialize(DynamicOps<T> dynamicOps) {
        Builder<T, T> builder = ImmutableMap.builder();
        builder.put(dynamicOps.createString("source_x"), dynamicOps.createInt(this.sourceX)).put(dynamicOps.createString("source_ground_y"), dynamicOps.createInt(this.sourceGroundY)).put(dynamicOps.createString("source_z"), dynamicOps.createInt(this.sourceZ)).put(dynamicOps.createString("delta_y"), dynamicOps.createInt(this.deltaY)).put(dynamicOps.createString("dest_proj"), dynamicOps.createString(this.destProjection.getName()));
        return new Dynamic<>(dynamicOps, dynamicOps.createMap(builder.build()));
    }

    public static <T> WorldGenFeatureDefinedStructureJigsawJunction deserialize(Dynamic<T> dynamic) {
        return new WorldGenFeatureDefinedStructureJigsawJunction(dynamic.get("source_x").asInt(0), dynamic.get("source_ground_y").asInt(0), dynamic.get("source_z").asInt(0), dynamic.get("delta_y").asInt(0), WorldGenFeatureDefinedStructurePoolTemplate.Matching.byName(dynamic.get("dest_proj").asString("")));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            WorldGenFeatureDefinedStructureJigsawJunction jigsawJunction = (WorldGenFeatureDefinedStructureJigsawJunction)object;
            if (this.sourceX != jigsawJunction.sourceX) {
                return false;
            } else if (this.sourceZ != jigsawJunction.sourceZ) {
                return false;
            } else if (this.deltaY != jigsawJunction.deltaY) {
                return false;
            } else {
                return this.destProjection == jigsawJunction.destProjection;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int i = this.sourceX;
        i = 31 * i + this.sourceGroundY;
        i = 31 * i + this.sourceZ;
        i = 31 * i + this.deltaY;
        return 31 * i + this.destProjection.hashCode();
    }

    @Override
    public String toString() {
        return "JigsawJunction{sourceX=" + this.sourceX + ", sourceGroundY=" + this.sourceGroundY + ", sourceZ=" + this.sourceZ + ", deltaY=" + this.deltaY + ", destProjection=" + this.destProjection + "}";
    }
}
