package net.minecraft.tags;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.material.FluidType;

public final class TagsFluid {
    protected static final TagUtil<FluidType> HELPER = TagStatic.create(IRegistry.FLUID_REGISTRY, "tags/fluids");
    private static final List<Tag<FluidType>> KNOWN_TAGS = Lists.newArrayList();
    public static final Tag.Named<FluidType> WATER = bind("water");
    public static final Tag.Named<FluidType> LAVA = bind("lava");

    private TagsFluid() {
    }

    private static Tag.Named<FluidType> bind(String id) {
        Tag.Named<FluidType> named = HELPER.bind(id);
        KNOWN_TAGS.add(named);
        return named;
    }

    public static Tags<FluidType> getAllTags() {
        return HELPER.getAllTags();
    }

    @Deprecated
    public static List<Tag<FluidType>> getStaticTags() {
        return KNOWN_TAGS;
    }
}
