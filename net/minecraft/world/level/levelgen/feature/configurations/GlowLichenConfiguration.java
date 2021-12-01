package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.block.Block;

public class GlowLichenConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<GlowLichenConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.intRange(1, 64).fieldOf("search_range").orElse(10).forGetter((glowLichenConfiguration) -> {
            return glowLichenConfiguration.searchRange;
        }), Codec.BOOL.fieldOf("can_place_on_floor").orElse(false).forGetter((glowLichenConfiguration) -> {
            return glowLichenConfiguration.canPlaceOnFloor;
        }), Codec.BOOL.fieldOf("can_place_on_ceiling").orElse(false).forGetter((glowLichenConfiguration) -> {
            return glowLichenConfiguration.canPlaceOnCeiling;
        }), Codec.BOOL.fieldOf("can_place_on_wall").orElse(false).forGetter((glowLichenConfiguration) -> {
            return glowLichenConfiguration.canPlaceOnWall;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("chance_of_spreading").orElse(0.5F).forGetter((glowLichenConfiguration) -> {
            return glowLichenConfiguration.chanceOfSpreading;
        }), IRegistry.BLOCK.byNameCodec().listOf().fieldOf("can_be_placed_on").forGetter((glowLichenConfiguration) -> {
            return glowLichenConfiguration.canBePlacedOn;
        })).apply(instance, GlowLichenConfiguration::new);
    });
    public final int searchRange;
    public final boolean canPlaceOnFloor;
    public final boolean canPlaceOnCeiling;
    public final boolean canPlaceOnWall;
    public final float chanceOfSpreading;
    public final List<Block> canBePlacedOn;
    public final List<EnumDirection> validDirections;

    public GlowLichenConfiguration(int searchRange, boolean placeOnFloor, boolean placeOnCeiling, boolean placeOnWalls, float spreadChance, List<Block> canPlaceOn) {
        this.searchRange = searchRange;
        this.canPlaceOnFloor = placeOnFloor;
        this.canPlaceOnCeiling = placeOnCeiling;
        this.canPlaceOnWall = placeOnWalls;
        this.chanceOfSpreading = spreadChance;
        this.canBePlacedOn = canPlaceOn;
        List<EnumDirection> list = Lists.newArrayList();
        if (placeOnCeiling) {
            list.add(EnumDirection.UP);
        }

        if (placeOnFloor) {
            list.add(EnumDirection.DOWN);
        }

        if (placeOnWalls) {
            EnumDirection.EnumDirectionLimit.HORIZONTAL.forEach(list::add);
        }

        this.validDirections = Collections.unmodifiableList(list);
    }
}
